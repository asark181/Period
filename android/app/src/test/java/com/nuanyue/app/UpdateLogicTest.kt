package com.nuanyue.app

import com.nuanyue.app.sync.VersionInfo
import com.nuanyue.app.update.ApkFetcher
import com.nuanyue.app.update.ByteSource
import com.nuanyue.app.update.CancelledDownload
import com.nuanyue.app.update.HttpStatusException
import com.nuanyue.app.update.Opened
import com.nuanyue.app.update.downloadUrls
import com.nuanyue.app.update.looksLikeApk
import com.nuanyue.app.update.offerOf
import com.nuanyue.app.update.sha256Hex
import com.nuanyue.app.update.sha256Matches
import com.nuanyue.app.update.shouldNotify
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class UpdateLogicTest {
    private val domain = "https://leafy.fxxkopenai.com"
    private val mirror = "http://129.151.21.101"
    private val apk = "$domain/download/nuanyue.apk"
    private val mirrorApk = "$mirror/download/nuanyue.apk"

    @Test fun blankApkUrlUsesTheDomainPath() {
        val offer = offerOf(VersionInfo(8, "1.7.0", "https://leafy.fxxkopenai.com/"), domain)
        assertEquals(apk, offer.apkUrl)
        assertEquals("", offer.sha256)
        assertEquals(listOf(apk, mirrorApk), downloadUrls(offer, mirror))
    }

    @Test fun explicitApkUrlIsKeptAndNotDuplicated() {
        val offer = offerOf(
            VersionInfo(8, "1.7.0", "https://leafy.fxxkopenai.com/", apkUrl = mirrorApk, sha256 = "AbC"),
            "$domain/",
        )
        assertEquals(mirrorApk, offer.apkUrl)
        assertEquals("abc", offer.sha256)
        assertEquals(listOf(mirrorApk), downloadUrls(offer, mirror))
    }

    @Test fun hashCheck() {
        val bytes = "nuanyue".toByteArray()
        val hex = sha256Hex(bytes)
        assertTrue(sha256Matches(hex, ""))
        assertTrue(sha256Matches(hex, hex.uppercase()))
        assertFalse(sha256Matches(hex, "00"))
        assertTrue(looksLikeApk(byteArrayOf(0x50, 0x4B, 0x03, 0x04)))
        assertFalse(looksLikeApk(byteArrayOf(0x00, 0x01)))
    }

    @Test fun notifyOncePerVersion() {
        assertTrue(shouldNotify(8, 0))
        assertFalse(shouldNotify(8, 8))
        assertTrue(shouldNotify(9, 8))
    }

    @Test fun oldVersionJsonStillDecodes() {
        val json = Json { ignoreUnknownKeys = true }
        val info = json.decodeFromString<VersionInfo>("""{"versionCode":7,"versionName":"1.6.0","url":"https://leafy.fxxkopenai.com/","notes":"上一版"}""")
        assertEquals(7, info.versionCode)
        assertEquals("", info.apkUrl)
        assertEquals("", info.sha256)
        assertEquals("上一版", info.notes)
        val extra = json.decodeFromString<VersionInfo>("""{"versionCode":8,"versionName":"1.7.0","url":"https://leafy.fxxkopenai.com/","apkUrl":"$apk","sha256":"abc","size":"12MB","notes":""}""")
        assertEquals(apk, extra.apkUrl)
        assertEquals("abc", extra.sha256)
    }

    @Test fun networkErrorFallsBackHttpStatusDoesNot() {
        val body = byteArrayOf(0x50, 0x4B, 1, 2, 3)
        val source = FakeSource(
            files = mapOf(mirrorApk to body),
            fail = setOf(apk),
        )
        val dest = File.createTempFile("apk", ".part")
        dest.delete()
        ApkFetcher(source).fetch(listOf(apk, mirrorApk), dest, { false }) { _, _ -> }
        assertEquals(listOf(apk, apk, mirrorApk), source.opened)
        assertTrue(dest.readBytes().contentEquals(body))
        dest.delete()
    }

    @Test fun httpStatusIsFinal() {
        val source = FakeSource(status = mapOf(apk to 404), files = mapOf(mirrorApk to byteArrayOf(1)))
        val dest = File.createTempFile("apk", ".part")
        val error = runCatching {
            ApkFetcher(source).fetch(listOf(apk, mirrorApk), dest, { false }) { _, _ -> }
        }.exceptionOrNull()
        assertTrue(error is HttpStatusException)
        assertEquals(404, (error as HttpStatusException).code)
        assertEquals(listOf(apk), source.opened)
        dest.delete()
    }

    @Test fun resumeSendsThePartialLength() {
        val full = byteArrayOf(0x50, 0x4B, 9, 8, 7, 6)
        val source = FakeSource(files = mapOf(apk to full))
        val dest = File.createTempFile("apk", ".part")
        dest.writeBytes(full.copyOfRange(0, 3))
        ApkFetcher(source).fetch(listOf(apk), dest, { false }) { _, _ -> }
        assertEquals(3L, source.ranges.single())
        assertTrue(dest.readBytes().contentEquals(full))
        dest.delete()
    }

    @Test fun cancelBeforeOpen() {
        val source = FakeSource(files = mapOf(apk to byteArrayOf(1, 2, 3, 4)))
        val dest = File.createTempFile("apk", ".part")
        val error = runCatching {
            ApkFetcher(source).fetch(listOf(apk), dest, { true }) { _, _ -> }
        }.exceptionOrNull()
        assertTrue(error is CancelledDownload)
        assertTrue(source.opened.isEmpty())
        dest.delete()
    }
}

private class FakeSource(
    private val files: Map<String, ByteArray>,
    private val fail: Set<String> = emptySet(),
    private val status: Map<String, Int> = emptyMap(),
) : ByteSource {
    val opened = mutableListOf<String>()
    val ranges = mutableListOf<Long>()

    override fun open(url: String, rangeFrom: Long): Opened {
        opened += url
        ranges += rangeFrom
        status[url]?.let { throw HttpStatusException(it) }
        if (url in fail) throw IOException("down")
        val bytes = files[url] ?: throw IOException("missing")
        val slice = if (rangeFrom > 0) bytes.copyOfRange(rangeFrom.toInt(), bytes.size) else bytes
        val code = if (rangeFrom > 0) 206 else 200
        return Opened(code, bytes.size.toLong(), slice.inputStream())
    }
}
