package com.nuanyue.app

import com.nuanyue.app.data.PeriodEntity
import com.nuanyue.app.data.rejectsFutureRecord
import com.nuanyue.app.data.withoutFutureRecords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Future period starts and ends are refused on the local write path and on sync import. */
class PeriodWriteTest {
    private val today = LocalDate.of(2026, 10, 20)

    @Test fun aFutureStartIsRejected() {
        val start = today.plusDays(4).toEpochDay()
        val period = PeriodEntity("future", startDay = start)
        assertTrue(period.rejectsFutureRecord(today))
    }

    @Test fun aFutureEndIsRejected() {
        val period = PeriodEntity("open", startDay = today.minusDays(2).toEpochDay(), endDay = today.plusDays(1).toEpochDay())
        assertTrue(period.rejectsFutureRecord(today))
    }

    @Test fun todayAndThePastAreKept() {
        val period = PeriodEntity("now", startDay = today.toEpochDay(), endDay = today.toEpochDay())
        assertFalse(period.rejectsFutureRecord(today))
        val earlier = PeriodEntity("past", startDay = today.minusDays(10).toEpochDay(), endDay = today.minusDays(6).toEpochDay())
        assertFalse(earlier.rejectsFutureRecord(today))
        val stillOpen = PeriodEntity("open", startDay = today.minusDays(2).toEpochDay(), endDay = null)
        assertFalse(stillOpen.rejectsFutureRecord(today))
    }

    @Test fun aTombstoneCanStillDeleteAFuturePeriod() {
        val gone = PeriodEntity("future", startDay = today.plusDays(1).toEpochDay(), deleted = true)
        assertFalse(gone.rejectsFutureRecord(today))
    }

    @Test fun syncImportDropsFutureStartsAndEnds() {
        val kept = PeriodEntity("kept", startDay = today.minusDays(30).toEpochDay(), endDay = today.minusDays(26).toEpochDay())
        val futureStart = PeriodEntity("later", startDay = today.plusDays(8).toEpochDay())
        val futureEnd = PeriodEntity("end", startDay = today.toEpochDay(), endDay = today.plusDays(2).toEpochDay())
        val tombstone = PeriodEntity("gone", startDay = today.plusDays(3).toEpochDay(), deleted = true)
        val imported = listOf(kept, futureStart, futureEnd, tombstone).withoutFutureRecords(today)
        assertEquals(listOf("kept", "gone"), imported.map { it.id })
    }
}
