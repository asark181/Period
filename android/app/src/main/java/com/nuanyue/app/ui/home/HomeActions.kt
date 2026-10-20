package com.nuanyue.app.ui.home

import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Period
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** What the confirm sheet is asking about. */
data class Pending(val date: LocalDate, val start: Boolean, val undo: Boolean)

data class DayAction(val label: String, val pending: Pending)

/**
 * One primary action for the selected day, plus the other action as a text button
 * when it still does something. A future day keeps the same labels but is blocked.
 */
data class DayActionSet(
    val primary: DayAction,
    val secondary: DayAction?,
    val blockedReason: String?,
)

/** Monday-first week rows actually occupied by this month. Never an empty trailing row. */
fun weekRows(ym: YearMonth): Int {
    val lead = ym.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    return (lead + ym.lengthOfMonth() + 6) / 7
}

fun dayActions(date: LocalDate, today: LocalDate, periods: List<Period>): DayActionSet {
    val startHere = periods.any { it.start == date }
    val endHere = periods.any { it.end == date }
    val canEnd = periods.any { !it.start.isAfter(date) && CycleEngine.days(it.start, date) < CycleEngine.MAX_PERIOD_DAYS }
    val inPeriod = periods.any { period ->
        !date.isBefore(period.start) &&
            (period.end == null || !date.isAfter(period.end)) &&
            CycleEngine.days(period.start, date) < CycleEngine.MAX_PERIOD_DAYS
    }
    val start = DayAction(
        label = when {
            startHere -> "取消开始"
            inPeriod -> "这天记为开始"
            else -> "月经来了"
        },
        pending = Pending(date, start = true, undo = startHere),
    )
    val end = DayAction(
        label = if (endHere) "取消结束" else "月经结束了",
        pending = Pending(date, start = false, undo = endHere),
    )
    val showEnd = endHere || canEnd
    val (primary, secondary) = if (inPeriod && showEnd) end to start else start to if (showEnd) end else null
    return DayActionSet(
        primary = primary,
        secondary = secondary?.takeUnless { it.label == primary.label && it.pending == primary.pending },
        blockedReason = if (date.isAfter(today)) "这一天还没到，不能记" else null,
    )
}
