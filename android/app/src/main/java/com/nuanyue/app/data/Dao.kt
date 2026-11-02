package com.nuanyue.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods WHERE deleted = 0 ORDER BY startDay")
    fun observeAll(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods WHERE deleted = 0 ORDER BY startDay")
    suspend fun getAll(): List<PeriodEntity>

    @Query("SELECT * FROM periods")
    suspend fun getAllIncludingDeleted(): List<PeriodEntity>

    @Upsert
    suspend fun upsert(vararg periods: PeriodEntity)

    @Upsert
    suspend fun upsertAll(periods: List<PeriodEntity>)

    @Query("DELETE FROM periods")
    suspend fun clear()
}

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records WHERE deleted = 0 ORDER BY day")
    fun observeAll(): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM day_records WHERE deleted = 0 ORDER BY day")
    suspend fun getAll(): List<DayRecordEntity>

    @Query("SELECT * FROM day_records")
    suspend fun getAllIncludingDeleted(): List<DayRecordEntity>

    @Query("SELECT * FROM day_records WHERE day = :day AND deleted = 0")
    suspend fun get(day: Long): DayRecordEntity?

    @Upsert
    suspend fun upsert(record: DayRecordEntity)

    @Upsert
    suspend fun upsertAll(records: List<DayRecordEntity>)

    @Query("DELETE FROM day_records")
    suspend fun clear()
}
