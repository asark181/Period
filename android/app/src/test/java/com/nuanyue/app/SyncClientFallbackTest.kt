package com.nuanyue.app

import com.nuanyue.app.sync.Exchange
import com.nuanyue.app.sync.PutBackup
import com.nuanyue.app.sync.SyncClient
import com.nuanyue.app.sync.SyncException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class SyncClientFallbackTest {
    private val space = "ab".repeat(32)
    private val auth = "cd".repeat(32)

    @Test fun configuredServers() {
        assertEquals("https://leafy.fxxkopenai.com", BuildConfig.SERVER_URL)
        assertEquals("http://129.151.21.101", BuildConfig.SERVER_FALLBACK_URL)
    }

    @Test fun primarySuccessDoesNotCallFallback() {
        val calls = mutableListOf<String>()
        val client = SyncClient(PRIMARY, FALLBACK, Exchange { url, _, _, _ ->
            calls += url
            200 to """{"blob":"cipher","updatedAt":7}"""
        })
        val remote = client.fetch(space, auth)
        assertEquals("cipher", remote?.blob)
        assertEquals(listOf("$PRIMARY/api/backup/$space"), calls)
    }

    @Test fun httpErrorIsNotRetried() {
        val calls = mutableListOf<String>()
        val client = SyncClient(PRIMARY, FALLBACK, Exchange { url, _, _, _ ->
            calls += url
            403 to """{"detail":"wrong"}"""
        })
        val error = runCatching { client.fetch(space, auth) }.exceptionOrNull()
        assertTrue(error is SyncException)
        assertEquals(403, (error as SyncException).code)
        assertEquals("账号或密码不对", error.message)
        assertEquals(listOf("$PRIMARY/api/backup/$space"), calls)
    }

    @Test fun missingSpaceOnPrimaryIsNotRetried() {
        val calls = mutableListOf<String>()
        val client = SyncClient(PRIMARY, FALLBACK, Exchange { url, _, _, _ ->
            calls += url
            404 to """{"detail":"not found"}"""
        })
        assertNull(client.fetch(space, auth))
        assertEquals(1, calls.size)
    }

    @Test fun networkFailureRetriesTheSameRequestOnce() {
        val calls = mutableListOf<String>()
        val client = SyncClient(PRIMARY, FALLBACK, Exchange { url, method, token, body ->
            calls += "$method $url auth=$token body=$body"
            if (url.startsWith(PRIMARY)) throw IOException("domain down")
            assertEquals("PUT", method)
            assertEquals(auth, token)
            assertTrue(body!!.contains(""""blob":"cipher-1""""))
            assertTrue(body.contains(""""updatedAt":42"""))
            200 to """{"ok":true}"""
        })
        client.push(space, auth, PutBackup("cipher-1", 42))
        assertEquals(
            listOf(
                "PUT $PRIMARY/api/backup/$space auth=$auth body={\"blob\":\"cipher-1\",\"updatedAt\":42}",
                "PUT $FALLBACK/api/backup/$space auth=$auth body={\"blob\":\"cipher-1\",\"updatedAt\":42}",
            ),
            calls,
        )
    }

    @Test fun secondNetworkFailureSurfacesThePrimaryError() {
        val calls = mutableListOf<String>()
        val client = SyncClient(PRIMARY, FALLBACK, Exchange { url, _, _, _ ->
            calls += url
            throw IOException(if (url.startsWith(PRIMARY)) "domain down" else "origin down")
        })
        val error = runCatching { client.latestVersion() }.exceptionOrNull()
        assertTrue(error is IOException)
        assertTrue(error !is SyncException)
        assertEquals("domain down", error?.message)
        assertEquals(1, error?.suppressed?.size)
        assertEquals("origin down", error?.suppressed?.first()?.message)
        assertEquals(listOf("$PRIMARY/api/version", "$FALLBACK/api/version"), calls)
    }

    @Test fun identicalFallbackIsNotTriedTwice() {
        var hits = 0
        val client = SyncClient(PRIMARY, PRIMARY, Exchange { _, _, _, _ ->
            hits += 1
            throw IOException("down")
        })
        val error = runCatching { client.latestVersion() }.exceptionOrNull()
        assertEquals("down", error?.message)
        assertEquals(1, hits)
    }

    @Test fun realHttpFallsBackWhenPrimaryRefusesTheConnection() {
        val dead = ServerSocket(0).use { it.localPort }
        val body = """{"versionCode":6,"versionName":"1.5.0","url":"https://leafy.fxxkopenai.com/","notes":""}"""
        OneShotHttp(200, body).use { fallback ->
            val client = SyncClient("http://127.0.0.1:$dead", "http://127.0.0.1:${fallback.port}")
            val version = client.latestVersion()
            assertEquals(6, version.versionCode)
            assertEquals("1.5.0", version.versionName)
            assertEquals("https://leafy.fxxkopenai.com/", version.url)
            assertEquals(1, fallback.hits)
            assertEquals("GET", fallback.method)
            assertEquals("/api/version", fallback.path)
        }
    }

    @Test fun realHttpDoesNotFallBackOnAnHttpError() {
        OneShotHttp(500, """{"detail":"no"}""").use { primary ->
            OneShotHttp(200, """{"versionCode":1,"versionName":"nope","url":"http://fallback","notes":""}""").use { fallback ->
                val client = SyncClient(
                    "http://127.0.0.1:${primary.port}",
                    "http://127.0.0.1:${fallback.port}",
                )
                val error = runCatching { client.latestVersion() }.exceptionOrNull()
                assertTrue(error is SyncException)
                assertEquals(500, (error as SyncException).code)
                assertEquals(1, primary.hits)
                assertEquals(0, fallback.hits)
            }
        }
    }

    private companion object {
        const val PRIMARY = "https://leafy.fxxkopenai.com"
        const val FALLBACK = "http://129.151.21.101"
    }
}

/** Minimal HTTP/1.1 listener so the real client, not a fake exchange, can be exercised. */
private class OneShotHttp(private val status: Int, private val responseBody: String) : AutoCloseable {
    private val server = ServerSocket(0)
    val port: Int = server.localPort
    private val hitCount = java.util.concurrent.atomic.AtomicInteger()
    val hits: Int get() = hitCount.get()
    @Volatile var method: String = ""
    @Volatile var path: String = ""

    private val thread = Thread {
        while (!server.isClosed) {
            val socket = try {
                server.accept()
            } catch (_: IOException) {
                break
            }
            socket.use { handle(it) }
        }
    }.apply { isDaemon = true; start() }

    private fun handle(socket: Socket) {
        socket.soTimeout = 3_000
        val input = socket.getInputStream()
        val collected = ByteArrayOutputStream()
        val tmp = ByteArray(1024)
        var headerEnd = -1
        while (headerEnd < 0) {
            val n = input.read(tmp)
            if (n < 0) return
            collected.write(tmp, 0, n)
            headerEnd = indexOf(collected.toByteArray(), HEADER_END)
        }
        val raw = collected.toByteArray()
        val header = String(raw, 0, headerEnd, StandardCharsets.ISO_8859_1)
        val requestLine = header.lineSequence().first()
        method = requestLine.substringBefore(' ')
        path = requestLine.substringAfter(' ').substringBefore(' ')
        hitCount.incrementAndGet()
        val payload = responseBody.toByteArray(StandardCharsets.UTF_8)
        val reason = if (status == 200) "OK" else "ERR"
        val head = "HTTP/1.1 $status $reason\r\nContent-Type: application/json\r\nContent-Length: ${payload.size}\r\nConnection: close\r\n\r\n"
        val out = socket.getOutputStream()
        out.write(head.toByteArray(StandardCharsets.US_ASCII))
        out.write(payload)
        out.flush()
    }

    override fun close() {
        server.close()
    }

    private fun indexOf(data: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..data.size - needle.size) {
            for (j in needle.indices) if (data[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }

    private companion object {
        val HEADER_END = "\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
    }
}
