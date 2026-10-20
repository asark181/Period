package com.nuanyue.app.update

import com.nuanyue.app.sync.VersionInfo
import java.security.MessageDigest

const val APK_PATH = "/download/nuanyue.apk"

/** What the client decided to install, after filling in optional version.json fields. */
data class UpdateOffer(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val sha256: String,
)

fun offerOf(info: VersionInfo, serverUrl: String): UpdateOffer {
    val server = serverUrl.trimEnd('/')
    val apk = info.apkUrl.trim().ifBlank { server + APK_PATH }
    return UpdateOffer(
        versionCode = info.versionCode,
        versionName = info.versionName,
        notes = info.notes,
        pageUrl = info.url,
        apkUrl = apk,
        sha256 = info.sha256.trim().lowercase(),
    )
}

/**
 * Primary APK, then the origin-IP mirror. The mirror is only used after a
 * network failure, the same rule as the API client.
 */
fun downloadUrls(offer: UpdateOffer, fallbackServer: String): List<String> {
    val mirror = fallbackServer.trim().trimEnd('/').takeIf { it.isNotEmpty() }?.plus(APK_PATH)
    return if (mirror != null && mirror != offer.apkUrl) listOf(offer.apkUrl, mirror) else listOf(offer.apkUrl)
}

/** One notification per versionCode. A newer code notifies again. */
fun shouldNotify(versionCode: Int, alreadyNotified: Int): Boolean = versionCode > alreadyNotified

/** Inline state of the Me-page “检查更新” row. Not a toast. */
sealed interface ManualCheck {
    data object Idle : ManualCheck
    data object Checking : ManualCheck
    data class UpToDate(val versionName: String) : ManualCheck
    data object Failed : ManualCheck
}

/** What the right side of the row shows. */
data class UpdateRowModel(
    val hint: String?,
    val error: Boolean,
    val spinner: Boolean,
    val clickable: Boolean,
)

fun updateRowModel(manual: ManualCheck, versionName: String, newerName: String?): UpdateRowModel = when (manual) {
    ManualCheck.Checking -> UpdateRowModel(hint = null, error = false, spinner = true, clickable = false)
    is ManualCheck.UpToDate -> UpdateRowModel("已是最新 · ${manual.versionName}", error = false, spinner = false, clickable = true)
    ManualCheck.Failed -> UpdateRowModel("检查失败，点此重试", error = true, spinner = false, clickable = true)
    ManualCheck.Idle -> UpdateRowModel(
        hint = newerName?.let { "有新版本 $it" } ?: "当前 $versionName",
        error = false,
        spinner = false,
        clickable = true,
    )
}

/** Log line for a manual check. [remote] is null when the request never returned a version. */
fun manualCheckLog(remote: Int?, local: Int, result: CheckResult): String {
    val outcome = when (result) {
        CheckResult.Current -> "up-to-date"
        CheckResult.Failed -> "error"
        CheckResult.Newer -> "update"
    }
    return "manual check: remote=${remote?.toString() ?: "?"} local=$local -> $outcome"
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** A blank expected hash means the server did not publish one, so the file is accepted. */
fun sha256Matches(actualHex: String, expected: String): Boolean {
    if (expected.isBlank()) return true
    return actualHex.equals(expected.trim(), ignoreCase = true)
}

fun looksLikeApk(header: ByteArray): Boolean =
    header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
