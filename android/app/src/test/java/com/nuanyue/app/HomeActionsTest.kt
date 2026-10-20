package com.nuanyue.app

import com.nuanyue.app.domain.Period
import com.nuanyue.app.ui.home.dayActions
import com.nuanyue.app.ui.home.weekRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HomeActionsTest {
    private val today = LocalDate.of(2026, 10, 20)

    @Test fun october2026UsesFiveWeekRows() {
        // 1 Oct 2026 is a Thursday, so the month fits in five Monday-first rows.
        assertEquals(5, weekRows(YearMonth.of(2026, 10)))
    }

    @Test fun aSundayStartWith31DaysNeedsSixRows() {
        assertEquals(6, weekRows(YearMonth.of(2026, 3)))
    }

    @Test fun duringAPeriodTheEndActionIsPrimary() {
        val periods = listOf(Period("b", LocalDate.of(2026, 10, 18), null))
        val actions = dayActions(today, today, periods)
        assertEquals("月经结束了", actions.primary.label)
        assertEquals(false, actions.primary.pending.start)
        assertEquals("这天记为开始", actions.secondary?.label)
        assertNull(actions.blockedReason)
    }

    @Test fun theStartDayOffersCancelAsTheSecondaryAction() {
        val start = LocalDate.of(2026, 10, 18)
        val periods = listOf(Period("b", start, null))
        val actions = dayActions(start, today, periods)
        assertEquals("月经结束了", actions.primary.label)
        assertEquals("取消开始", actions.secondary?.label)
        assertEquals(true, actions.secondary?.pending?.undo)
    }

    @Test fun outsideAPeriodTheStartActionIsPrimary() {
        val day = LocalDate.of(2026, 10, 10)
        val periods = listOf(Period("a", LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 16)))
        val actions = dayActions(day, today, periods)
        assertEquals("月经来了", actions.primary.label)
        assertEquals(true, actions.primary.pending.start)
        assertNull(actions.secondary)
    }

    @Test fun aFutureDayKeepsTheLabelButIsBlocked() {
        val day = LocalDate.of(2026, 10, 28)
        val actions = dayActions(day, today, emptyList())
        assertEquals("月经来了", actions.primary.label)
        assertEquals("这一天还没到，不能记", actions.blockedReason)
    }

    @Test fun anEndedDayOffersUndoFirst() {
        val end = LocalDate.of(2026, 9, 16)
        val periods = listOf(Period("a", LocalDate.of(2026, 9, 12), end))
        val actions = dayActions(end, today, periods)
        assertEquals("取消结束", actions.primary.label)
        assertEquals(true, actions.primary.pending.undo)
        assertEquals(false, actions.primary.pending.start)
    }
}
