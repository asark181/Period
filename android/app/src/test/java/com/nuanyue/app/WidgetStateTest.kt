package com.nuanyue.app

import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Period
import com.nuanyue.app.widget.WidgetState
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WidgetStateTest {
    private val start = LocalDate.of(2026, 10, 18)

    private fun state(day: Int, endDay: Int? = null): WidgetState {
        val p = Period("synthetic", start, endDay?.let { start.plusDays(it.toLong() - 1) })
        return WidgetState.from(CycleEngine.snapshot(listOf(p), 29, 5, start.plusDays(day.toLong() - 1)))
    }

    @Test fun ongoingPeriodShowsInclusiveDaysLeftAndNextDate() {
        val s = state(3)
        assertEquals("经期第 3 天", s.headline)
        assertEquals("预计还有 3 天结束", s.detail)
        assertEquals("下次 11月16日", s.nextDate)
        assertEquals("周期第 3 天 / 29 天", s.cycleLabel)
        assertEquals(10, s.progressPercent)
    }

    @Test fun countdownIncludesDateAndPhase() {
        val s = state(8, 5)
        assertEquals("还有 22 天", s.headline)
        assertEquals("安全期", s.detail)
        assertEquals("下次 11月16日", s.nextDate)
    }

    @Test fun predictedDayDoesNotClaimAnActualPeriod() {
        val s = state(30, 5)
        assertEquals("预计今天来", s.headline)
        assertEquals("下次 11月16日", s.nextDate)
    }

    @Test fun lateKeepsOriginalPredictionInsteadOfMovingItToToday() {
        val s = state(34, 5)
        assertEquals("已推迟 4 天", s.headline)
        assertEquals("原预计 11月16日", s.nextDate)
        assertEquals(100, s.progressPercent)
    }

    @Test fun fertileAndOvulationUseTheirActualPhase() {
        assertEquals("易孕期", state(12, 5).detail)
        assertEquals("排卵日", state(16, 5).detail)
    }

    @Test fun emptyDoesNotInventDatesOrCycleProgress() {
        val s = WidgetState.from(CycleEngine.snapshot(emptyList(), 29, 5, start))
        assertEquals("开始记录", s.headline)
        assertEquals("点我记录第一次经期", s.detail)
        assertEquals("", s.nextDate)
        assertFalse(s.hasCycle)
        assertEquals(0, s.progressPercent)
    }

    @Test fun futureOnlyRecordsDoNotInventCurrentCycle() {
        val p = Period("future", start.plusDays(5), start.plusDays(9))
        val s = WidgetState.from(CycleEngine.snapshot(listOf(p), 29, 5, start))
        assertFalse(s.hasCycle)
        assertEquals("开始记录", s.headline)
    }
}
