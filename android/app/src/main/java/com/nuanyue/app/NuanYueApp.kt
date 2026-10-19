package com.nuanyue.app

import android.app.Application
import com.nuanyue.app.data.AppDatabase
import com.nuanyue.app.data.PhraseRepository
import com.nuanyue.app.data.Repository
import com.nuanyue.app.data.SettingsStore
import com.nuanyue.app.notify.Reminders
import com.nuanyue.app.sync.SyncManager
import com.nuanyue.app.widget.PeriodWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NuanYueApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var settings: SettingsStore
        private set
    lateinit var repository: Repository
        private set
    lateinit var sync: SyncManager
        private set
    lateinit var phrases: PhraseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        val db = AppDatabase.create(this)
        repository = Repository(db, settings) { local -> onDataChanged(local) }
        sync = SyncManager(this, repository, settings)
        phrases = PhraseRepository(this, db.phraseDao())
        Reminders.createChannel(this)
        appScope.launch { phrases.ensureSeeded() }
        appScope.launch {
            val s = settings.current()
            Reminders.schedule(this@NuanYueApp, s)
            if (s.syncEnabled) sync.schedulePeriodic()
        }
    }

    private fun onDataChanged(local: Boolean) {
        PeriodWidget.updateAll(this)
        appScope.launch {
            val s = settings.current()
            if (local && s.syncEnabled && !s.isPartner) sync.requestSoon()
        }
    }
}
