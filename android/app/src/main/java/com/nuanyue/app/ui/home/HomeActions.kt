package com.nuanyue.app.ui.home

import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.CycleSnapshot
import com.nuanyue.app.domain.DayInfo
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Period
import com.nuanyue.app.domain.Phase
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

/** What the growing card under the month says about the selected day. */
data class DayBrief(val title: String, val detail: String, val progress: Float)

fun dayBrief(date: LocalDate, today: LocalDate, info: DayInfo?, snapshot: CycleSnapshot): DayBrief {
    if (!snapshot.hasData || info == null) {
        return DayBrief("还没有记录", "记下第一次月经开始，这里会显示周期和第几天", 0f)
    }
    val t = snapshot.today
    val cycle = info.cycleDay
    val title = when (info.type) {
        DayType.PERIOD -> if (date == today && t.periodDay != null) "经期第 ${t.periodDay} 天" else "经期" + (cycle?.let { " · 第${it}天" } ?: "")
        DayType.PREDICTED_PERIOD -> "预测经期"
        DayType.FERTILE -> "易孕期" + (cycle?.let { " · 第${it}天" } ?: "")
        DayType.OVULATION -> "排卵日"
        DayType.SAFE -> "安全期" + (cycle?.let { " · 第${it}天" } ?: "")
        else -> "这一天"
    }
    val next = snapshot.nextStart
    val detail = when {
        date == today && t.phase == Phase.MENSTRUAL && t.periodDaysLeft != null ->
            "这次大概还有 ${t.periodDaysLeft} 天，下次月经预计 ${md(t.nextStart)}"
        date == today && t.lateDays > 0 -> "已推迟 ${t.lateDays} 天"
        date == today && (t.phase == Phase.FERTILE || t.phase == Phase.OVULATION) ->
            "排卵日 ${md(t.nextOvulation)}，受孕机会${t.pregnancyChance}"
        info.type == DayType.PREDICTED_PERIOD -> "预计这天来月经"
        next != null && !date.isAfter(next) -> "距离下次月经还有 ${CycleEngine.days(date, next)} 天，${md(next)}"
        cycle != null -> "周期第 ${cycle} 天"
        else -> "点一天，看它在周期里的位置"
    }
    val progress = when {
        date == today -> t.progress
        cycle != null && snapshot.avgCycle > 0 -> (cycle.toFloat() / snapshot.avgCycle).coerceIn(0f, 1f)
        else -> 0f
    }
    return DayBrief(title, detail, progress)
}

private fun md(date: LocalDate?) = if (date == null) "还不确定" else "${date.monthValue}月${date.dayOfMonth}日"
