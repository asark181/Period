package com.nuanyue.app.sync

import android.content.Context
import android.util.Base64
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.data.BackupData
import com.nuanyue.app.data.Repository
import com.nuanyue.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class SyncManager(
    private val context: Context,
    private val repo: Repository,
    private val settings: SettingsStore,
    private val client: SyncClient = SyncClient(),
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    /** Verifies the credentials against the server and stores the derived key. */
    suspend fun login(account: String, password: String, asPartner: Boolean): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(account.trim().length >= 3) { "账号至少 3 个字符" }
            require(password.length >= 6) { "密码至少 6 位" }
            val material = Crypto.deriveKeyMaterial(account, password)
            val keys = Crypto.split(material)
            val remote = client.fetch(Crypto.spaceId(account), Crypto.hex(keys.authToken))
            if (remote != null) decode(keys.encKey, remote.blob) // throws on wrong password
            if (asPartner && remote == null) error("这个账号还没有数据。先在她的手机上开启同步")
            settings.update {
                it.copy(
                    syncAccount = Crypto.normalizeAccount(account),
                    syncKey = Base64.encodeToString(material, Base64.NO_WRAP),
                    role = if (asPartner) com.nuanyue.app.data.Role.PARTNER else com.nuanyue.app.data.Role.SELF,
                    lastSyncAt = 0,
                )
            }
            if (remote == null) "已创建同步账号" else "已连接"
        }
    }

    suspend fun logout() {
        settings.update { it.copy(syncAccount = "", syncKey = "", lastSyncAt = 0, role = com.nuanyue.app.data.Role.SELF) }
    }

    /** Pull + merge + push. Partner mode only pulls. */
    suspend fun syncNow(): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val s = settings.current()
                if (!s.syncEnabled) error("还没开启同步")
                val keys = Crypto.split(Base64.decode(s.syncKey, Base64.NO_WRAP))
                val space = Crypto.spaceId(s.syncAccount)
                val auth = Crypto.hex(keys.authToken)
                val remote = client.fetch(space, auth)

                if (s.isPartner) {
                    if (remote == null) error("她还没有上传过数据")
                    repo.replaceWith(decode(keys.encKey, remote.blob))
                    settings.update { it.copy(lastSyncAt = System.currentTimeMillis()) }
                    return@runCatching "已更新到最新"
                }

                if (remote != null && remote.updatedAt > s.lastSyncAt) {
                    repo.merge(decode(keys.encKey, remote.blob))
                }
                val needPush = remote == null || s.localDirtyAt > s.lastSyncAt || remote.updatedAt > s.lastSyncAt
                val now = System.currentTimeMillis()
                if (needPush) {
                    val plain = json.encodeToString(BackupData.serializer(), repo.exportSnapshot()).toByteArray()
                    val blob = Base64.encodeToString(Crypto.encrypt(keys.encKey, plain), Base64.NO_WRAP)
                    client.push(space, auth, PutBackup(blob, now))
                }
                settings.update { it.copy(lastSyncAt = now) }
                "同步完成"
            }
        }
    }

    private fun decode(key: ByteArray, blob: String): BackupData {
        val plain = try {
            Crypto.decrypt(key, Base64.decode(blob, Base64.NO_WRAP))
        } catch (e: Exception) {
            throw IllegalStateException("密码不对，解不开云端数据")
        }
        return json.decodeFromString(BackupData.serializer(), String(plain))
    }

    /** Debounced upload after a local change. */
    fun requestSoon() {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync-soon", ExistingWorkPolicy.REPLACE, req)
    }

    fun schedulePeriodic() {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NuanYueApp
        if (!app.settings.current().syncEnabled) return Result.success()
        return if (app.sync.syncNow().isSuccess) Result.success() else Result.retry()
    }
}
