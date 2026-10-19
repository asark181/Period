package com.nuanyue.app.data

import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Period
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class Repository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    /** Called after data changes; `local` is true for her own edits (which should be uploaded). */
    private val onChanged: (local: Boolean) -> Unit,
) {
    val periods: Flow<List<Period>> = db.periodDao().observeAll().map { list -> list.map { it.toDomain() } }
    val records: Flow<List<DayRecordEntity>> = db.dayRecordDao().observeAll()

    suspend fun periodsNow(): List<Period> = db.periodDao().getAll().map { it.toDomain() }
    suspend fun recordsNow(): List<DayRecordEntity> = db.dayRecordDao().getAll()
    suspend fun record(day: LocalDate): DayRecordEntity? = db.dayRecordDao().get(day.toEpochDay())

    /** "大姨妈来了" on [date]. */
    suspend fun markStart(date: LocalDate, defaultPeriod: Int) {
        val all = db.periodDao().getAll().sortedBy { it.startDay }
        val day = date.toEpochDay()
        // Already a start.
        if (all.any { it.startDay == day }) return
        // Inside an existing period: move that period's start to here only if earlier.
        val containing = all.lastOrNull { it.startDay <= day && day <= (it.endDay ?: (it.startDay + defaultPeriod - 1)) }
        if (containing != null) {
            // Split: close the old one the day before and start fresh.
            save(containing.copy(endDay = day - 1))
            save(PeriodEntity(id = newId(), startDay = day))
            return
        }
        // A period starting shortly after: extend it backwards.
        val soonAfter = all.firstOrNull { it.startDay > day && it.startDay - day <= 7 }
        if (soonAfter != null) {
            save(soonAfter.copy(startDay = day))
            return
        }
        // Close a dangling previous period so it does not swallow this one.
        val prev = all.lastOrNull { it.startDay < day }
        if (prev != null && prev.endDay == null) {
            val assumedEnd = minOf(prev.startDay + defaultPeriod - 1, day - 1)
            save(prev.copy(endDay = assumedEnd))
        }
        val next = all.firstOrNull { it.startDay > day }
        // Back-filling an old period: keep it from overlapping the following one.
        val end = next?.takeIf { day + defaultPeriod - 1 >= it.startDay }?.let { it.startDay - 1 }
        save(PeriodEntity(id = newId(), startDay = day, endDay = end))
    }

    /** Undo "来了": removes the period that starts on [date]. */
    suspend fun unmarkStart(date: LocalDate) {
        val p = db.periodDao().getAll().firstOrNull { it.startDay == date.toEpochDay() } ?: return
        save(p.copy(deleted = true))
    }

    /** "大姨妈走了" on [date]. */
    suspend fun markEnd(date: LocalDate) {
        val day = date.toEpochDay()
        val p = db.periodDao().getAll()
            .lastOrNull { it.startDay <= day && day - it.startDay < CycleEngine.MAX_PERIOD_DAYS } ?: return
        save(p.copy(endDay = day))
    }

    suspend fun unmarkEnd(date: LocalDate) {
        val p = db.periodDao().getAll().firstOrNull { it.endDay == date.toEpochDay() } ?: return
        save(p.copy(endDay = null))
    }

    suspend fun saveRecord(record: DayRecordEntity) {
        val r = record.copy(updatedAt = System.currentTimeMillis(), deleted = record.isEmpty)
        db.dayRecordDao().upsert(r)
        touch()
    }

    suspend fun addPeriod(start: LocalDate, end: LocalDate?) {
        save(PeriodEntity(id = newId(), startDay = start.toEpochDay(), endDay = end?.toEpochDay()))
    }

    suspend fun clearAll() {
        db.periodDao().clear()
        db.dayRecordDao().clear()
        touch()
    }

    // ---- Sync support ----

    suspend fun exportSnapshot(): BackupData = BackupData(
        version = 1,
        exportedAt = System.currentTimeMillis(),
        periods = db.periodDao().getAllIncludingDeleted(),
        records = db.dayRecordDao().getAllIncludingDeleted(),
    )

    /** Record-level merge: for each key keep the newest version (tombstones included). */
    suspend fun merge(remote: BackupData) {
        val localPeriods = db.periodDao().getAllIncludingDeleted().associateBy { it.id }
        val periods = remote.periods.filter { r -> (localPeriods[r.id]?.updatedAt ?: -1) < r.updatedAt }
        db.periodDao().upsertAll(periods)
        val localRecords = db.dayRecordDao().getAllIncludingDeleted().associateBy { it.day }
        val records = remote.records.filter { r -> (localRecords[r.day]?.updatedAt ?: -1) < r.updatedAt }
        db.dayRecordDao().upsertAll(records)
        if (periods.isNotEmpty() || records.isNotEmpty()) onChanged(false)
    }

    /** Partner mode: mirror the remote exactly. */
    suspend fun replaceWith(remote: BackupData) {
        db.periodDao().clear()
        db.dayRecordDao().clear()
        db.periodDao().upsertAll(remote.periods)
        db.dayRecordDao().upsertAll(remote.records)
        onChanged(false)
    }

    private suspend fun save(p: PeriodEntity) {
        db.periodDao().upsert(p.copy(updatedAt = System.currentTimeMillis()))
        touch()
    }

    private suspend fun touch() {
        settings.update { it.copy(localDirtyAt = System.currentTimeMillis()) }
        onChanged(true)
    }

    private fun newId() = UUID.randomUUID().toString()
}

@kotlinx.serialization.Serializable
data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val periods: List<PeriodEntity>,
    val records: List<DayRecordEntity>,
)

fun PeriodEntity.toDomain() = Period(
    id = id,
    start = LocalDate.ofEpochDay(startDay),
    end = endDay?.let { LocalDate.ofEpochDay(it) },
)
