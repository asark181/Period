package com.nuanyue.app

import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Period
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.notify.Reminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {
    private fun p(start: String, end: String?) = Period(start, LocalDate.parse(start), end?.let(LocalDate::parse))

    private val history = listOf(
        p("2026-06-01", "2026-06-05"),
        p("2026-06-29", "2026-07-03"),
        p("2026-07-27", "2026-07-31"),
        p("2026-08-24", "2026-08-28"),
    )

    @Test fun learnsAverageFromHistory() {
        val s = CycleEngine.snapshot(history, defaultCycle = 35, defaultPeriod = 7, today = LocalDate.parse("2026-09-10"))
        assertEquals(28, s.avgCycle)
        assertEquals(5, s.avgPeriod)
        assertEquals(LocalDate.parse("2026-09-21"), s.nextStart)
        assertEquals(11, s.today.daysUntilNext)
        assertEquals("非常规律", s.regularity)
    }

    @Test fun usesDefaultsWithSingleRecord() {
        val s = CycleEngine.snapshot(listOf(p("2026-09-01", null)), 30, 6, LocalDate.parse("2026-09-03"))
        assertEquals(30, s.avgCycle)
        assertEquals(Phase.MENSTRUAL, s.today.phase)
        assertEquals(3, s.today.periodDay)
        assertEquals(4, s.today.periodDaysLeft)
        assertNull(s.cycleStdDev)
    }

    @Test fun ovulationAndFertileWindow() {
        // next start 09-21 → ovulation 09-07, fertile 09-02..09-11
        val s = CycleEngine.snapshot(history, 28, 5, LocalDate.parse("2026-09-07"))
        assertEquals(Phase.OVULATION, s.today.phase)
        val fertile = CycleEngine.snapshot(history, 28, 5, LocalDate.parse("2026-09-03"))
        assertEquals(Phase.FERTILE, fertile.today.phase)
        val safe = CycleEngine.snapshot(history, 28, 5, LocalDate.parse("2026-09-15"))
        assertEquals(Phase.LUTEAL, safe.today.phase)
    }

    @Test fun detectsLatePeriod() {
        val s = CycleEngine.snapshot(history, 28, 5, LocalDate.parse("2026-09-24"))
        assertEquals(Phase.LATE, s.today.phase)
        assertEquals(3, s.today.lateDays)
    }

    @Test fun calendarClassification() {
        val today = LocalDate.parse("2026-09-10")
        val s = CycleEngine.snapshot(history, 28, 5, today)
        val infos = CycleEngine.dayInfos(s, LocalDate.parse("2026-08-20"), LocalDate.parse("2026-10-31"), today)
        assertEquals(DayType.PERIOD, infos[LocalDate.parse("2026-08-24")]!!.type)
        assertTrue(infos[LocalDate.parse("2026-08-24")]!!.isPeriodStart)
        assertEquals(DayType.PERIOD, infos[LocalDate.parse("2026-08-28")]!!.type)
        assertEquals(DayType.SAFE, infos[LocalDate.parse("2026-08-29")]!!.type)
        assertEquals(DayType.OVULATION, infos[LocalDate.parse("2026-09-07")]!!.type)
        assertEquals(DayType.FERTILE, infos[LocalDate.parse("2026-09-02")]!!.type)
        assertEquals(DayType.PREDICTED_PERIOD, infos[LocalDate.parse("2026-09-21")]!!.type)
        assertEquals(DayType.PREDICTED_PERIOD, infos[LocalDate.parse("2026-10-19")]!!.type)
        assertEquals(1, infos[LocalDate.parse("2026-09-21")]!!.cycleDay)
        val before = CycleEngine.dayInfos(s, LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-01"), today)
        assertEquals(DayType.UNKNOWN, before[LocalDate.parse("2026-05-01")]!!.type)
    }

    @Test fun ignoresOutlierCycles() {
        // A forgotten month (56 days) must not drag the average.
        val withGap = history + p("2026-10-19", null)
        val s = CycleEngine.snapshot(withGap, 28, 5, LocalDate.parse("2026-10-20"))
        assertEquals(28, s.avgCycle)
    }

    @Test fun remindersFireOnTheRightDays() {
        val settings = AppSettings(nickname = "宝贝", remindBeforeDays = 2)
        assertEquals(1, Reminders.due(history, settings, LocalDate.parse("2026-09-19")).size)
        assertTrue(Reminders.due(history, settings, LocalDate.parse("2026-09-19"))[0].first.contains("2 天后"))
        assertTrue(Reminders.due(history, settings, LocalDate.parse("2026-09-21"))[0].first.contains("今天"))
        assertTrue(Reminders.due(history, settings, LocalDate.parse("2026-09-18")).isEmpty())
        assertTrue(Reminders.due(history, settings, LocalDate.parse("2026-09-22"))[0].first.contains("推迟"))
        val partner = Reminders.due(history, settings.copy(role = com.nuanyue.app.data.Role.PARTNER), LocalDate.parse("2026-09-19"))
        assertTrue(partner[0].first.startsWith("TA"))
        // Open period past its average length → ask whether it ended.
        val open = history.dropLast(1) + p("2026-08-24", null)
        assertTrue(Reminders.due(open, settings, LocalDate.parse("2026-08-29")).any { it.first.contains("走了吗") })
    }
}
