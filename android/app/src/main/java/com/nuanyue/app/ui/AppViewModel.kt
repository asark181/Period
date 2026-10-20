package com.nuanyue.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.CycleSnapshot
import com.nuanyue.app.notify.Reminders
import com.nuanyue.app.sync.SyncClient
import com.nuanyue.app.sync.VersionInfo
import com.nuanyue.app.update.UpdateUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class UiState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val snapshot: CycleSnapshot = CycleEngine.snapshot(emptyList(), 28, 5),
    val today: LocalDate = LocalDate.now(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NuanYueApp
    private val repo = app.repository
    private val today = MutableStateFlow(LocalDate.now())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _phrase = MutableStateFlow<com.nuanyue.app.data.PhraseEntity?>(null)
    val phrase: StateFlow<com.nuanyue.app.data.PhraseEntity?> = _phrase.asStateFlow()
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    val update: StateFlow<UpdateUi?> = app.updates.ui

    val state: StateFlow<UiState> = combine(app.settings.flow, repo.periods, today) { s, periods, day ->
        UiState(
            loaded = true,
            settings = s,
            snapshot = CycleEngine.snapshot(periods, s.defaultCycle, s.defaultPeriod, day),
            today = day,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        viewModelScope.launch {
            app.updates.events.collect { msg ->
                if (msg != null) {
                    toast(msg)
                    app.updates.consumeEvent()
                }
            }
        }
    }

    /** Picks another line for the phrase card, matched to today's phase. */
    suspend fun nextPhrase() {
        val st = state.first { it.loaded }
        app.phrases.next(st.snapshot.today.phase, st.settings.isPartner, _phrase.value?.id ?: -1)?.let { _phrase.value = it }
    }

    /** Called on every resume: fresh date after midnight, a new phrase, fresh partner data. */
    fun refresh() {
        today.value = LocalDate.now()
        viewModelScope.launch { nextPhrase() }
        app.updates.maybeCheck()
        viewModelScope.launch {
            val s = app.settings.current()
            if (s.syncEnabled) app.sync.syncNow()
        }
    }

    fun consumeMessage() { _message.value = null }
    fun toast(msg: String) { _message.value = msg }

    private val readOnly get() = state.value.settings.isPartner

    private fun edit(block: suspend () -> Unit) {
        if (readOnly) { toast("伴侣模式只能查看"); return }
        viewModelScope.launch { block() }
    }

    fun markStart(date: LocalDate) = edit { repo.markStart(date, state.value.snapshot.avgPeriod) }
    fun unmarkStart(date: LocalDate) = edit { repo.unmarkStart(date) }
    fun markEnd(date: LocalDate) = edit { repo.markEnd(date) }
    fun unmarkEnd(date: LocalDate) = edit { repo.unmarkEnd(date) }

    fun finishOnboarding(nickname: String, lastStart: LocalDate?, periodLen: Int, cycleLen: Int) {
        viewModelScope.launch {
            app.settings.update {
                it.copy(onboarded = true, nickname = nickname.trim(), defaultCycle = cycleLen, defaultPeriod = periodLen)
            }
            if (lastStart != null && repo.periodsNow().isEmpty()) {
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
        val (t, b) = com.nuanyue.app.domain.Copy.beforePeriod(
            s.remindBeforeDays, LocalDate.now().plusDays(s.remindBeforeDays.toLong()), s.isPartner,
        )
        Reminders.notify(app, 1999, t, b)
    }

    /** Onboarding or settings: join an existing space with an invite code. */
    fun joinInvite(code: String, asPartner: Boolean, onDone: (ok: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            _syncing.value = true
            val r = app.sync.joinInvite(code, asPartner)
            if (r.isSuccess) {
                app.sync.schedulePeriodic()
                val sr = app.sync.syncNow()
                app.settings.update { it.copy(onboarded = true) }
                Reminders.schedule(app, app.settings.current())
                toast(if (sr.isSuccess) "数据已经接上啦" else (sr.exceptionOrNull()?.message ?: "同步失败"))
            }
            _syncing.value = false
            onDone(r.isSuccess, r.exceptionOrNull()?.message)
        }
    }

    /** Her phone: create a fresh invite code from the data already on this phone. */
    fun createInvite() {
        viewModelScope.launch {
            _syncing.value = true
            val r = app.sync.joinInvite(com.nuanyue.app.sync.Crypto.newInvite(), asPartner = false, create = true)
            if (r.isSuccess) {
                app.sync.schedulePeriodic()
                app.sync.syncNow()
                toast("邀请码生成好啦，发给他就可以一起看了")
            } else toast(r.exceptionOrNull()?.message ?: "生成失败，检查一下网络")
            _syncing.value = false
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
        toast("已退出同步，本机数据还在")
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

    fun checkFromUser() = app.updates.checkFromUser()
    fun updatePrimary(context: android.content.Context) = app.updates.onPrimary(context)
    fun dismissUpdate() = app.updates.dismissSheet()
    fun openUpdatePage(context: android.content.Context) = app.updates.openPage(context)
}
