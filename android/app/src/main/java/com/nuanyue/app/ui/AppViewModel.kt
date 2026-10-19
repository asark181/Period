package com.nuanyue.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.CycleSnapshot
import com.nuanyue.app.notify.Reminders
import com.nuanyue.app.sync.SyncClient
import com.nuanyue.app.sync.VersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class UiState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val snapshot: CycleSnapshot = CycleEngine.snapshot(emptyList(), 28, 5),
    val records: Map<LocalDate, DayRecordEntity> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NuanYueApp
    private val repo = app.repository
    private val today = MutableStateFlow(LocalDate.now())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    val state: StateFlow<UiState> = combine(app.settings.flow, repo.periods, repo.records, today) { s, periods, records, day ->
        UiState(
            loaded = true,
            settings = s,
            snapshot = CycleEngine.snapshot(periods, s.defaultCycle, s.defaultPeriod, day),
            records = records.associateBy { LocalDate.ofEpochDay(it.day) },
            today = day,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    /** Called on resume so the app is right after midnight, and partner data is fresh. */
    fun refresh() {
        today.value = LocalDate.now()
        viewModelScope.launch {
            val s = app.settings.current()
            if (s.syncEnabled) app.sync.syncNow()
        }
    }

    fun consumeMessage() { _message.value = null }
    fun toast(msg: String) { _message.value = msg }

    private val readOnly get() = state.value.settings.isPartner

    private fun edit(block: suspend () -> Unit) {
        if (readOnly) { toast("伴侣模式下只能查看哦"); return }
        viewModelScope.launch { block() }
    }

    fun markStart(date: LocalDate) = edit { repo.markStart(date, state.value.snapshot.avgPeriod) }
    fun unmarkStart(date: LocalDate) = edit { repo.unmarkStart(date) }
    fun markEnd(date: LocalDate) = edit { repo.markEnd(date) }
    fun unmarkEnd(date: LocalDate) = edit { repo.unmarkEnd(date) }
    fun saveRecord(r: DayRecordEntity) = edit { repo.saveRecord(r) }

    fun finishOnboarding(nickname: String, lastStart: LocalDate?, periodLen: Int, cycleLen: Int) {
        viewModelScope.launch {
            app.settings.update {
                it.copy(onboarded = true, nickname = nickname.ifBlank { "宝贝" }, defaultCycle = cycleLen, defaultPeriod = periodLen)
            }
            if (lastStart != null) {
                val end = lastStart.plusDays((periodLen - 1).toLong())
                repo.addPeriod(lastStart, if (end.isBefore(LocalDate.now())) end else null)
            }
            Reminders.schedule(app, app.settings.current())
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            app.settings.update(transform)
            Reminders.schedule(app, app.settings.current())
            com.nuanyue.app.widget.PeriodWidget.updateAll(app)
        }
    }

    fun testNotification() {
        val s = state.value.settings
        val (t, b) = com.nuanyue.app.domain.Copy.beforePeriod(s.remindBeforeDays, s.nickname, s.isPartner)
        Reminders.notify(app, 1999, t, b)
    }

    fun syncLogin(account: String, password: String, asPartner: Boolean, onDone: (ok: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            _syncing.value = true
            val r = app.sync.login(account, password, asPartner)
            if (r.isSuccess) {
                app.sync.schedulePeriodic()
                val sr = app.sync.syncNow()
                toast(r.getOrNull() + if (sr.isSuccess) "，" + sr.getOrNull() else "")
            }
            _syncing.value = false
            onDone(r.isSuccess, if (r.isSuccess) null else (r.exceptionOrNull()?.message ?: "连接失败，请检查网络"))
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncing.value = true
            val r = app.sync.syncNow()
            toast(r.getOrNull() ?: (r.exceptionOrNull()?.message ?: "同步失败"))
            _syncing.value = false
        }
    }

    fun syncLogout() = viewModelScope.launch {
        app.sync.logout()
        toast("已退出云同步，本机数据保留")
    }

    fun clearAll() = edit {
        repo.clearAll()
        toast("已清空所有记录")
    }

    suspend fun exportJson(): String {
        val data = repo.exportSnapshot()
        return kotlinx.serialization.json.Json { prettyPrint = true }
            .encodeToString(com.nuanyue.app.data.BackupData.serializer(), data)
    }

    suspend fun checkUpdate(): Result<VersionInfo> = withContext(Dispatchers.IO) {
        runCatching { SyncClient().latestVersion() }
    }
}
