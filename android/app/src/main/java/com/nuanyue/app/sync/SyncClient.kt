package com.nuanyue.app.sync

import com.nuanyue.app.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class RemoteBackup(val blob: String, val updatedAt: Long)

@Serializable
data class PutBackup(val blob: String, val updatedAt: Long)

@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val notes: String = "",
    /** Direct APK. Empty means `{server}/download/nuanyue.apk`. */
    val apkUrl: String = "",
    /** Lowercase hex of the APK. Empty means the client skips the hash check. */
    val sha256: String = "",
)

class SyncException(val code: Int, message: String) : IOException(message)

/**
 * One HTTP round trip. Throws [IOException] only when no HTTP status comes back
 * (DNS, connect, timeout, TLS). An HTTP error status is a normal result.
 */
fun interface Exchange {
    fun call(url: String, method: String, auth: String?, body: String?): Pair<Int, String>
}

/** Tiny HTTP client: no third-party networking library needed for three endpoints. */
class SyncClient(
    base: String = BuildConfig.SERVER_URL,
    fallbackBase: String = BuildConfig.SERVER_FALLBACK_URL,
    private val exchange: Exchange = HttpExchange,
) {
    private val base = base.trimEnd('/')
    private val fallbackBase = fallbackBase.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns null when the space does not exist yet. */
    fun fetch(spaceId: String, auth: String): RemoteBackup? {
        val (code, body) = request("GET", "/api/backup/$spaceId", auth, null)
        return when (code) {
            200 -> json.decodeFromString<RemoteBackup>(body)
            404 -> null
            else -> throw error(code, body)
        }
    }

    fun push(spaceId: String, auth: String, backup: PutBackup) {
        val (code, body) = request("PUT", "/api/backup/$spaceId", auth, json.encodeToString(PutBackup.serializer(), backup))
        if (code !in 200..299) throw error(code, body)
    }

    fun latestVersion(): VersionInfo {
        val (code, body) = request("GET", "/api/version", null, null)
        if (code != 200) throw error(code, body)
        return json.decodeFromString(body)
    }

    private fun error(code: Int, body: String) = SyncException(
        code,
        when (code) {
            401, 403 -> "账号或密码不对"
            413 -> "数据太大了"
            429 -> "操作太频繁，过一会儿再试"
            else -> "服务器出错了（$code）"
        },
    )

    /**
     * Prefer the HTTPS domain. If that attempt fails before any HTTP status
     * (Cloudflare is flaky on some mainland networks), send the same request
     * once to the origin IP over cleartext. A 4xx/5xx is the server answering,
     * so it is not retried.
     */
    private fun request(method: String, path: String, auth: String?, body: String?): Pair<Int, String> {
        return try {
            exchange.call(base + path, method, auth, body)
        } catch (primary: IOException) {
            if (fallbackBase.isEmpty() || fallbackBase == base) throw primary
            try {
                exchange.call(fallbackBase + path, method, auth, body)
            } catch (fallback: IOException) {
                primary.addSuppressed(fallback)
                throw primary
            }
        }
    }
}

private object HttpExchange : Exchange {
    override fun call(url: String, method: String, auth: String?, body: String?): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (auth != null) setRequestProperty("X-Auth", auth)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to text
        } finally {
            conn.disconnect()
        }
    }
}
