package com.nuanyue.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class Role { SELF, PARTNER }

data class AppSettings(
    val onboarded: Boolean = false,
    val nickname: String = "宝贝",
    val defaultCycle: Int = 28,
    val defaultPeriod: Int = 5,
    val remindEnabled: Boolean = true,
    val remindHour: Int = 9,
    val remindMinute: Int = 0,
    val remindBeforeDays: Int = 2,
    val remindBefore: Boolean = true,
    val remindLate: Boolean = true,
    val remindEnd: Boolean = true,
    val remindFertile: Boolean = false,
    val remindDaily: Boolean = false,
    val loveNote: String = "",
    val role: Role = Role.SELF,
    val syncAccount: String = "",
    /** base64 of the derived key material; the password itself is never stored */
    val syncKey: String = "",
    val lastSyncAt: Long = 0,
    val localDirtyAt: Long = 0,
) {
    val syncEnabled: Boolean get() = syncAccount.isNotBlank() && syncKey.isNotBlank()
    val isPartner: Boolean get() = role == Role.PARTNER
}

class SettingsStore(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val nickname = stringPreferencesKey("nickname")
        val defaultCycle = intPreferencesKey("default_cycle")
        val defaultPeriod = intPreferencesKey("default_period")
        val remindEnabled = booleanPreferencesKey("remind_enabled")
        val remindHour = intPreferencesKey("remind_hour")
        val remindMinute = intPreferencesKey("remind_minute")
        val remindBeforeDays = intPreferencesKey("remind_before_days")
        val remindBefore = booleanPreferencesKey("remind_before")
        val remindLate = booleanPreferencesKey("remind_late")
        val remindEnd = booleanPreferencesKey("remind_end")
        val remindFertile = booleanPreferencesKey("remind_fertile")
        val remindDaily = booleanPreferencesKey("remind_daily")
        val loveNote = stringPreferencesKey("love_note")
        val role = stringPreferencesKey("role")
        val syncAccount = stringPreferencesKey("sync_account")
        val syncKey = stringPreferencesKey("sync_key")
        val lastSyncAt = longPreferencesKey("last_sync_at")
        val localDirtyAt = longPreferencesKey("local_dirty_at")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[K.onboarded] = s.onboarded
            prefs[K.nickname] = s.nickname
            prefs[K.defaultCycle] = s.defaultCycle
            prefs[K.defaultPeriod] = s.defaultPeriod
            prefs[K.remindEnabled] = s.remindEnabled
            prefs[K.remindHour] = s.remindHour
            prefs[K.remindMinute] = s.remindMinute
            prefs[K.remindBeforeDays] = s.remindBeforeDays
            prefs[K.remindBefore] = s.remindBefore
            prefs[K.remindLate] = s.remindLate
            prefs[K.remindEnd] = s.remindEnd
            prefs[K.remindFertile] = s.remindFertile
            prefs[K.remindDaily] = s.remindDaily
            prefs[K.loveNote] = s.loveNote
            prefs[K.role] = s.role.name
            prefs[K.syncAccount] = s.syncAccount
            prefs[K.syncKey] = s.syncKey
            prefs[K.lastSyncAt] = s.lastSyncAt
            prefs[K.localDirtyAt] = s.localDirtyAt
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        onboarded = this[K.onboarded] ?: false,
        nickname = this[K.nickname] ?: "宝贝",
        defaultCycle = this[K.defaultCycle] ?: 28,
        defaultPeriod = this[K.defaultPeriod] ?: 5,
        remindEnabled = this[K.remindEnabled] ?: true,
        remindHour = this[K.remindHour] ?: 9,
        remindMinute = this[K.remindMinute] ?: 0,
        remindBeforeDays = this[K.remindBeforeDays] ?: 2,
        remindBefore = this[K.remindBefore] ?: true,
        remindLate = this[K.remindLate] ?: true,
        remindEnd = this[K.remindEnd] ?: true,
        remindFertile = this[K.remindFertile] ?: false,
        remindDaily = this[K.remindDaily] ?: false,
        loveNote = this[K.loveNote] ?: "",
        role = runCatching { Role.valueOf(this[K.role] ?: "SELF") }.getOrDefault(Role.SELF),
        syncAccount = this[K.syncAccount] ?: "",
        syncKey = this[K.syncKey] ?: "",
        lastSyncAt = this[K.lastSyncAt] ?: 0,
        localDirtyAt = this[K.localDirtyAt] ?: 0,
    )
}
