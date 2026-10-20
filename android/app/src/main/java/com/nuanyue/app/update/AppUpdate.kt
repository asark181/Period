package com.nuanyue.app.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nuanyue.app.BuildConfig
import com.nuanyue.app.MainActivity
import com.nuanyue.app.R
import com.nuanyue.app.sync.SyncClient
import com.nuanyue.app.sync.VersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class UpdatePhase { Available, Downloading, Ready, Failed, NeedsPermission }

data class UpdateUi(
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val phase: UpdatePhase,
    val received: Long = 0,
    val total: Long = 0,
    val message: String = "",
    val pageUrl: String = "",
    val sheet: Boolean = false,
)

@Serializable
private data class SavedUpdate(
    val versionCode: Int,
    val versionName: String,
    val notes: String = "",
    val pageUrl: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
    val notifiedCode: Int = 0,
    val presentedCode: Int = 0,
    val cancelled: Boolean = false,
    val lastCheckAt: Long = 0,
)

/**
 * Finds a newer APK, downloads it in the background, checks sha256 when the
 * server published one, then hands it to the system installer.
 */
class AppUpdate(
    private val context: Context,
    private val scope: CoroutineScope,
    private val currentCode: Int = BuildConfig.VERSION_CODE,
    private val serverUrl: String = BuildConfig.SERVER_URL,
    private val fallbackUrl: String = BuildConfig.SERVER_FALLBACK_URL,
    private val fetcher: ApkFetcher = ApkFetcher(HttpByteSource),
    private val versions: () -> VersionInfo = { SyncClient().latestVersion() },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val gate = Mutex()
    private val cancelFlag = AtomicBoolean(false)
    private var job: Job? = null
    private var awaitingPermission = false
    private var offer: UpdateOffer? = null
    @Volatile private var forceSheet = false

    private val _ui = MutableStateFlow<UpdateUi?>(null)
    val ui: StateFlow<UpdateUi?> = _ui.asStateFlow()

    private val dir: File get() = File(context.filesDir, "updates").also { it.mkdirs() }
    private val stateFile: File get() = File(dir, "state.json")

    fun start() {
        scheduleChecks(context)
        scope.launch {
            val saved = load() ?: return@launch
            if (saved.versionCode <= currentCode) {
                clearFiles()
                return@launch
            }
            val next = saved.toOffer()
            offer = next
            val apk = apkFile(next.versionCode)
            if (apk.exists() && verifyFile(apk, next.sha256)) {
                publish(next, UpdatePhase.Ready, present = false)
                return@launch
            }
            if (!saved.cancelled) download(next, present = false)
            else publish(next, UpdatePhase.Available, present = false)
        }
    }

    /** Quiet check from resume or the periodic worker. */
    fun checkInBackground(notify: Boolean) {
        scope.launch { check(notify = notify, present = false) }
    }

    /** On resume, look again if the last check is more than six hours old. */
    fun maybeCheck() {
        scope.launch {
            val saved = load()
            val stale = saved == null || System.currentTimeMillis() - saved.lastCheckAt > 6 * 60 * 60 * 1000
            if (stale) check(notify = true, present = false)
        }
    }

    /** The button on the Me screen. */
    fun checkFromUser() {
        forceSheet = true
        scope.launch { check(notify = false, present = true) }
    }

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()
    fun consumeEvent() { _events.value = null }

    suspend fun check(notify: Boolean, present: Boolean): CheckResult = withContext(Dispatchers.IO) {
        gate.withLock {
            val saved = load()
            val info = try {
                versions()
            } catch (e: Exception) {
                if (present) _events.value = "检查失败：${e.message ?: "网络异常"}"
                return@withLock CheckResult.Failed
            }
            val now = System.currentTimeMillis()
            if (info.versionCode <= currentCode) {
                clearFiles()
                _ui.value = null
                save((saved ?: emptySaved()).copy(lastCheckAt = now, versionCode = info.versionCode))
                if (present) _events.value = "已是最新版本"
                return@withLock CheckResult.Current
            }
            val next = offerOf(info, serverUrl)
            offer = next
            val same = saved != null && saved.versionCode == next.versionCode
            if (saved != null && !same) {
                apkFile(saved.versionCode).delete()
                partFile(saved.versionCode).delete()
            }
            if (same && saved.sha256 != next.sha256) {
                apkFile(next.versionCode).delete()
                partFile(next.versionCode).delete()
            }
            // A cancelled download stays cancelled until the user taps again.
            // A different versionCode starts fresh. Background checks do not pop the sheet.
            val cancelled = same && saved.cancelled
            val base = (saved ?: emptySaved()).copy(
                versionCode = next.versionCode,
                versionName = next.versionName,
                notes = next.notes,
                pageUrl = next.pageUrl,
                apkUrl = next.apkUrl,
                sha256 = next.sha256,
                lastCheckAt = now,
                cancelled = cancelled,
            )
            var stored = base
            if (notify && shouldNotify(next.versionCode, base.notifiedCode)) {
                notifyNew(next)
                stored = base.copy(notifiedCode = next.versionCode)
            }
            save(stored)
            val apk = apkFile(next.versionCode)
            when {
                apk.exists() && verifyFile(apk, next.sha256) -> publish(next, UpdatePhase.Ready, present)
                cancelled -> publish(next, UpdatePhase.Available, present)
                else -> download(next, present)
            }
            CheckResult.Newer
        }
    }

    fun present() {
        forceSheet = true
        val ui = _ui.value
        if (ui != null) _ui.value = ui.copy(sheet = true)
        else scope.launch { check(notify = false, present = true) }
    }

    fun dismissSheet() {
        forceSheet = false
        _ui.value = _ui.value?.copy(sheet = false)
    }

    fun onPrimary(context: Context) {
        val ui = _ui.value ?: return
        val next = offer ?: return
        when (ui.phase) {
            UpdatePhase.Available, UpdatePhase.Failed -> {
                scope.launch {
                    val saved = load()
                    if (saved != null) save(saved.copy(cancelled = false))
                    download(next, present = true)
                }
            }
            UpdatePhase.Downloading -> cancel()
            UpdatePhase.Ready, UpdatePhase.NeedsPermission -> installOrAsk(context, next)
        }
    }

    fun onHostResume(context: Context) {
        if (!awaitingPermission) return
        val next = offer ?: return
        if (canInstall(context)) {
            awaitingPermission = false
            installOrAsk(context, next)
        }
    }

    fun cancel() {
        cancelFlag.set(true)
        job?.cancel()
        val next = offer ?: return
        partFile(next.versionCode).delete()
        scope.launch {
            val saved = load()
            if (saved != null) save(saved.copy(cancelled = true))
        }
        publish(next, UpdatePhase.Available, present = true, message = "已取消下载")
        notificationManager().cancel(NOTIF_PROGRESS)
    }

    fun openPage(context: Context) {
        val url = _ui.value?.pageUrl?.takeIf { it.isNotBlank() } ?: return
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun download(next: UpdateOffer, present: Boolean = true) {
        job?.cancel()
        cancelFlag.set(false)
        publish(next, UpdatePhase.Downloading, present, received = partFile(next.versionCode).length())
        job = scope.launch(Dispatchers.IO) {
            val part = partFile(next.versionCode)
            try {
                fetcher.fetch(downloadUrls(next, fallbackUrl), part, { cancelFlag.get() }) { got, total ->
                    publish(next, UpdatePhase.Downloading, present = false, received = got, total = total)
                    progressNotification(next, got, total)
                }
                if (!verifyFile(part, next.sha256)) {
                    part.delete()
                    publish(next, UpdatePhase.Failed, present = false, message = "安装包校验没通过")
                    notificationManager().cancel(NOTIF_PROGRESS)
                    return@launch
                }
                val apk = apkFile(next.versionCode)
                if (apk.exists()) apk.delete()
                if (!part.renameTo(apk)) part.copyTo(apk, overwrite = true).also { part.delete() }
                publish(next, UpdatePhase.Ready, present = false)
                readyNotification(next)
            } catch (_: CancelledDownload) {
                publish(next, UpdatePhase.Available, present = false, message = "已取消下载")
            } catch (e: Exception) {
                publish(next, UpdatePhase.Failed, present = false, message = e.message ?: "下载没成功")
                notificationManager().cancel(NOTIF_PROGRESS)
            }
        }
    }

    private fun installOrAsk(context: Context, next: UpdateOffer) {
        val apk = apkFile(next.versionCode)
        if (!apk.exists() || !verifyFile(apk, next.sha256)) {
            publish(next, UpdatePhase.Failed, message = "安装包不在了，再下一遍")
            return
        }
        if (!canInstall(context)) {
            awaitingPermission = true
            publish(next, UpdatePhase.NeedsPermission, message = "需要允许暖月安装未知应用，允许后会接着安装")
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        awaitingPermission = false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun publish(
        next: UpdateOffer,
        phase: UpdatePhase,
        present: Boolean = true,
        received: Long = 0,
        total: Long = 0,
        message: String = "",
    ) {
        val sheet = present || forceSheet || _ui.value?.sheet == true
        _ui.value = UpdateUi(
            versionName = next.versionName,
            versionCode = next.versionCode,
            notes = next.notes,
            phase = phase,
            received = received,
            total = total,
            message = message,
            pageUrl = next.pageUrl,
            sheet = sheet,
        )
    }

    private fun apkFile(code: Int) = File(dir, "nuanyue-$code.apk")
    private fun partFile(code: Int) = File(dir, "nuanyue-$code.apk.part")

    private fun load(): SavedUpdate? = try {
        if (!stateFile.exists()) null else json.decodeFromString(SavedUpdate.serializer(), stateFile.readText())
    } catch (_: Exception) {
        null
    }

    private fun save(saved: SavedUpdate) {
        dir.mkdirs()
        stateFile.writeText(json.encodeToString(SavedUpdate.serializer(), saved))
    }

    private fun clearFiles() {
        dir.listFiles()?.forEach { it.delete() }
        _ui.value = null
        offer = null
    }

    private fun emptySaved() = SavedUpdate(versionCode = 0, versionName = "")

    private fun SavedUpdate.toOffer() = UpdateOffer(versionCode, versionName, notes, pageUrl, apkUrl, sha256)

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notificationManager() = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "版本更新", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "有新版本时提醒一次"
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun openIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_UPDATE, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 7, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notifyNew(next: UpdateOffer) {
        if (!canNotify()) return
        val body = next.notes.ifBlank { "点开暖月，接着下载安装" }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE9899C.toInt())
            .setContentTitle("新版本 ${next.versionName}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(openIntent())
            .build()
        androidx.core.app.NotificationManagerCompat.from(context).notify(NOTIF_NEW, n)
    }

    private fun progressNotification(next: UpdateOffer, got: Long, total: Long) {
        if (!canNotify()) return
        val pct = if (total > 0) ((got * 100) / total).toInt().coerceIn(0, 100) else 0
        val cancel = PendingIntent.getBroadcast(
            context, 8,
            Intent(context, UpdateCancelReceiver::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE9899C.toInt())
            .setContentTitle("正在下载 ${next.versionName}")
            .setContentText(if (total > 0) "$pct%" else "下载中")
            .setProgress(100, if (total > 0) pct else 0, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent())
            .addAction(0, "取消", cancel)
            .build()
        androidx.core.app.NotificationManagerCompat.from(context).notify(NOTIF_PROGRESS, n)
    }

    private fun readyNotification(next: UpdateOffer) {
        if (!canNotify()) return
        notificationManager().cancel(NOTIF_PROGRESS)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE9899C.toInt())
            .setContentTitle("${next.versionName} 下载好了")
            .setContentText("点开暖月安装")
            .setAutoCancel(true)
            .setContentIntent(openIntent())
            .build()
        androidx.core.app.NotificationManagerCompat.from(context).notify(NOTIF_NEW, n)
    }

    companion object {
        const val CHANNEL_ID = "app_updates"
        const val EXTRA_OPEN_UPDATE = "nuanyue.update"
        const val ACTION_CANCEL = "com.nuanyue.app.UPDATE_CANCEL"
        private const val NOTIF_NEW = 3100
        private const val NOTIF_PROGRESS = 3101
        private const val WORK = "update-check"

        fun scheduleChecks(context: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}

enum class CheckResult { Newer, Current, Failed }

private fun canInstall(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

fun verifyFile(file: File, sha256: String): Boolean {
    if (!file.exists() || file.length() < 4) return false
    val header = ByteArray(4)
    file.inputStream().use { if (it.read(header) < 2) return false }
    if (!looksLikeApk(header)) return false
    if (sha256.isBlank()) return true
    val dig = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            dig.update(buf, 0, n)
        }
    }
    val hex = dig.digest().joinToString("") { "%02x".format(it) }
    return sha256Matches(hex, sha256)
}

class UpdateCancelReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as com.nuanyue.app.NuanYueApp).updates.cancel()
    }
}
