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
)

class SyncException(val code: Int, message: String) : IOException(message)

/** Tiny HTTP client: no third-party networking library needed for three endpoints. */
class SyncClient(private val base: String = BuildConfig.SERVER_URL) {
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
            401, 403 -> "密码不对哦，请检查同步账号和密码"
            413 -> "数据太大了"
            429 -> "请求太频繁，稍后再试"
            else -> "服务器开小差了 ($code)"
        },
    )

    private fun request(method: String, path: String, auth: String?, body: String?): Pair<Int, String> {
        val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
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
