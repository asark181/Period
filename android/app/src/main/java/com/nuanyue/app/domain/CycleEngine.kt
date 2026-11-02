package com.nuanyue.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A period in domain form. `end` is null while she has not marked it as finished. */
data class Period(val id: String, val start: LocalDate, val end: LocalDate?)

enum class DayType {
    /** Logged menstruation. */
    PERIOD,

    /** Predicted menstruation (future, or remaining days of an ongoing period). */
    PREDICTED_PERIOD,

    /** Fertile window. */
    FERTILE,

    /** Predicted ovulation day. */
    OVULATION,

    /** Safe days. */
    SAFE,

    /** Before the first record: nothing known. */
    UNKNOWN,
}

enum class Phase(val title: String) {
    MENSTRUAL("月经期"),
    FOLLICULAR("安全期"),
    FERTILE("易孕期"),
    OVULATION("排卵日"),
    LUTEAL("安全期"),
    LATE("推迟中"),
    NONE("未记录"),
}

data class DayInfo(
    val date: LocalDate,
    val type: DayType,
    /** 1-based day index within its cycle, or null if unknown. */
    val cycleDay: Int?,
    val isPeriodStart: Boolean = false,
    val isPeriodEnd: Boolean = false,
)

data class CycleRecord(
    val start: LocalDate,
    /** Days until the next period started; null for the current (unfinished) cycle. */
    val cycleLength: Int?,
    val periodLength: Int,
)

data class Today(
    val phase: Phase,
    val cycleDay: Int?,
    /** When in a period: which day of it (1-based). */
    val periodDay: Int?,
    /** When in a period: how many days are expected to remain (incl. today). */
    val periodDaysLeft: Int?,
    /** Days until the next predicted period start (0 = today). Null when no data. */
    val daysUntilNext: Int?,
    /** Positive when the period is overdue. */
    val lateDays: Int,
    val nextStart: LocalDate?,
    val nextOvulation: LocalDate?,
    val pregnancyChance: String,
    /** Progress through the current cycle, 0..1 for the ring. */
    val progress: Float,
)

data class CycleSnapshot(
    val periods: List<Period>,
    val avgCycle: Int,
    val avgPeriod: Int,
    val cycles: List<CycleRecord>,
    val nextStart: LocalDate?,
    val today: Today,
    /** Cycle length standard deviation, null when fewer than 3 cycles. */
    val cycleStdDev: Double?,
) {
    val hasData: Boolean get() = periods.isNotEmpty()

    val regularity: String
        get() = when {
            cycleStdDev == null -> "数据积累中"
            cycleStdDev <= 2.0 -> "非常规律"
            cycleStdDev <= 4.0 -> "比较规律"
            cycleStdDev <= 7.0 -> "略有波动"
            else -> "不太规律"
        }
}

/**
 * Cycle math. Rules follow common practice in period-tracking apps:
 * - cycle length = weighted mean of the last 6 plausible cycles (15..90 days), recent cycles weigh more;
 * - ovulation = 14 days before the next period; fertile window = ovulation -5 .. +4 (10 days).
 */
object CycleEngine {
    const val MAX_PERIOD_DAYS = 14
    private const val MIN_CYCLE = 15
    private const val MAX_CYCLE = 90
    private const val LUTEAL = 14
    private const val FERTILE_BEFORE = 5
    private const val FERTILE_AFTER = 4

    fun snapshot(
        rawPeriods: List<Period>,
        defaultCycle: Int,
        defaultPeriod: Int,
        today: LocalDate = LocalDate.now(),
    ): CycleSnapshot {
        val periods = rawPeriods.sortedBy { it.start }
        val validCycles = dropOutliers(
            periods.zipWithNext { a, b -> days(a.start, b.start) }.filter { it in MIN_CYCLE..MAX_CYCLE },
        )
        val avgCycle = weightedMean(validCycles.takeLast(6)) ?: defaultCycle.coerceIn(MIN_CYCLE, MAX_CYCLE)

        val finishedLengths = periods.mapNotNull { p -> p.end?.let { days(p.start, it) + 1 } }
            .filter { it in 1..MAX_PERIOD_DAYS }
        val avgPeriod = (weightedMean(finishedLengths.takeLast(6)) ?: defaultPeriod).coerceIn(1, MAX_PERIOD_DAYS)

        val cycles = periods.mapIndexed { i, p ->
            val next = periods.getOrNull(i + 1)
            CycleRecord(
                start = p.start,
                cycleLength = next?.let { days(p.start, it.start) },
                periodLength = effectiveLength(p, avgPeriod, next, today),
            )
        }

        val std = if (validCycles.size >= 3) stdDev(validCycles.takeLast(6)) else null
        val last = periods.lastOrNull()
        val nextStart = last?.start?.plusDays(avgCycle.toLong())

        return CycleSnapshot(
            periods = periods,
            avgCycle = avgCycle,
            avgPeriod = avgPeriod,
            cycles = cycles,
            nextStart = nextStart,
            today = computeToday(periods, avgCycle, avgPeriod, today),
            cycleStdDev = std,
        )
    }

    /** Number of logged-or-assumed days for a period. */
    private fun effectiveLength(p: Period, avgPeriod: Int, next: Period?, today: LocalDate): Int {
        p.end?.let { return (days(p.start, it) + 1).coerceAtLeast(1) }
        var len = avgPeriod
        if (next == null) {
            val elapsed = if (!today.isBefore(p.start)) days(p.start, today) + 1 else 0
            if (elapsed in 1..MAX_PERIOD_DAYS) {
                len = maxOf(len, elapsed)
            }
        } else {
            // An open period cannot run into the next one.
            len = minOf(len, days(p.start, next.start))
        }
        return len.coerceAtLeast(1)
    }

    private fun computeToday(periods: List<Period>, avgCycle: Int, avgPeriod: Int, today: LocalDate): Today {
        val last = periods.lastOrNull { !it.start.isAfter(today) }
        if (last == null) {
            val upcoming = periods.firstOrNull()
            return Today(
                phase = Phase.NONE, cycleDay = null, periodDay = null, periodDaysLeft = null,
                daysUntilNext = upcoming?.let { days(today, it.start) }, lateDays = 0,
                nextStart = upcoming?.start, nextOvulation = null, pregnancyChance = "—", progress = 0f,
            )
        }
        val cycleDay = days(last.start, today) + 1
        val periodLen = effectiveLength(last, avgPeriod, null, today)
        val inPeriod = if (last.end != null) !today.isAfter(last.end) else cycleDay <= maxOf(periodLen, 1) && cycleDay <= MAX_PERIOD_DAYS

        val nextStart = last.start.plusDays(avgCycle.toLong())
        val untilNext = days(today, nextStart)
        val late = if (untilNext < 0) -untilNext else 0
        val ovulation = nextStart.minusDays(LUTEAL.toLong())
        val toOvulation = days(today, ovulation)

        val phase = when {
            inPeriod -> Phase.MENSTRUAL
            late > 0 -> Phase.LATE
            toOvulation == 0 -> Phase.OVULATION
            toOvulation in -FERTILE_AFTER..FERTILE_BEFORE -> Phase.FERTILE
            toOvulation > 0 -> Phase.FOLLICULAR
            else -> Phase.LUTEAL
        }
        val chance = when (phase) {
            Phase.OVULATION -> "极高"
            Phase.FERTILE -> if (abs(toOvulation) <= 2) "高" else "较高"
            Phase.MENSTRUAL -> "低"
            Phase.LATE -> "—"
            else -> "低"
        }
        val periodDay = if (inPeriod) cycleDay else null
        val periodLeft = if (inPeriod) {
            val end = last.end ?: last.start.plusDays((periodLen - 1).toLong())
            (days(today, end) + 1).coerceAtLeast(1)
        } else null

        return Today(
            phase = phase,
            cycleDay = cycleDay,
            periodDay = periodDay,
            periodDaysLeft = periodLeft,
            daysUntilNext = untilNext.coerceAtLeast(0),
            lateDays = late,
            nextStart = if (late > 0) today else nextStart,
            nextOvulation = if (toOvulation >= 0) ovulation else ovulation.plusDays(avgCycle.toLong()),
            pregnancyChance = chance,
            progress = (cycleDay.toFloat() / avgCycle).coerceIn(0f, 1f),
        )
    }

    /** Classifies every day in [from, to] for the calendar. */
    fun dayInfos(snapshot: CycleSnapshot, from: LocalDate, to: LocalDate, today: LocalDate = LocalDate.now()): Map<LocalDate, DayInfo> {
        val result = HashMap<LocalDate, DayInfo>()
        val periods = snapshot.periods
        if (periods.isEmpty()) {
            var d = from
            while (!d.isAfter(to)) { result[d] = DayInfo(d, DayType.UNKNOWN, null); d = d.plusDays(1) }
            return result
        }
        val avgCycle = snapshot.avgCycle
        val avgPeriod = snapshot.avgPeriod

        // Build a list of cycle starts: logged ones, then predictions until past `to`.
        data class CycleStart(val start: LocalDate, val period: Period?, val predicted: Boolean)
        val starts = periods.map { CycleStart(it.start, it, false) }.toMutableList()
        var nextPredicted = periods.last().start.plusDays(avgCycle.toLong())
        // If the period is overdue, show the prediction starting tomorrow instead of in the past.
        if (nextPredicted.isBefore(today.plusDays(1))) {
            val lastInfo = snapshot.today
            if (lastInfo.phase == Phase.LATE) nextPredicted = today.plusDays(1)
            else while (!nextPredicted.isAfter(today)) nextPredicted = nextPredicted.plusDays(avgCycle.toLong())
        }
        while (!nextPredicted.isAfter(to.plusDays(avgCycle.toLong()))) {
            starts += CycleStart(nextPredicted, null, true)
            nextPredicted = nextPredicted.plusDays(avgCycle.toLong())
        }

        for (i in starts.indices) {
            val cs = starts[i]
            val next = starts.getOrNull(i + 1)
            val cycleEnd = next?.start?.minusDays(1) ?: cs.start.plusDays((avgCycle - 1).toLong())
            if (cycleEnd.isBefore(from) || cs.start.isAfter(to)) continue

            val periodLen = if (cs.period != null) {
                effectiveLength(cs.period, avgPeriod, next?.period, today)
            } else avgPeriod
            val periodEnd = cs.start.plusDays((periodLen - 1).toLong())
            // Ovulation is anchored on the following cycle's start.
            val nextStartForOv = next?.start ?: cs.start.plusDays(avgCycle.toLong())
            val ovulation = nextStartForOv.minusDays(LUTEAL.toLong())
            val fertileStart = ovulation.minusDays(FERTILE_BEFORE.toLong())
            val fertileEnd = ovulation.plusDays(FERTILE_AFTER.toLong())

            var d = maxOf(cs.start, from)
            val last = minOf(cycleEnd, to)
            while (!d.isAfter(last)) {
                val inPeriod = !d.isAfter(periodEnd)
                val type = when {
                    inPeriod && cs.predicted -> DayType.PREDICTED_PERIOD
                    inPeriod && cs.period?.end == null && d.isAfter(today) -> DayType.PREDICTED_PERIOD
                    inPeriod -> DayType.PERIOD
                    d == ovulation -> DayType.OVULATION
                    !d.isBefore(fertileStart) && !d.isAfter(fertileEnd) -> DayType.FERTILE
                    else -> DayType.SAFE
                }
                result[d] = DayInfo(
                    date = d,
                    type = type,
                    cycleDay = days(cs.start, d) + 1,
                    isPeriodStart = !cs.predicted && d == cs.start,
                    isPeriodEnd = !cs.predicted && cs.period?.end == d,
                )
                d = d.plusDays(1)
            }
        }
        var d = from
        while (!d.isAfter(to)) {
            result.getOrPut(d) { DayInfo(d, DayType.UNKNOWN, null) }
            d = d.plusDays(1)
        }
        return result
    }

    /** The period a date belongs to (logged or open-ended within the assumed length), if any. */
    fun periodContaining(snapshot: CycleSnapshot, date: LocalDate): Period? {
        val periods = snapshot.periods
        for (i in periods.indices.reversed()) {
            val p = periods[i]
            if (p.start.isAfter(date)) continue
            val len = p.end?.let { days(p.start, it) + 1 }
                ?: snapshot.cycles.getOrNull(i)?.periodLength
                ?: snapshot.avgPeriod
            val end = p.start.plusDays((len - 1).toLong())
            if (!date.isAfter(end)) return p
        }
        return null
    }

    fun days(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()

    /** A cycle far from the median usually means a forgotten record, not a real cycle. */
    private fun dropOutliers(cycles: List<Int>): List<Int> {
        if (cycles.size < 3) return cycles
        val median = cycles.sorted()[cycles.size / 2]
        return cycles.filter { it >= median * 0.6 && it <= median * 1.6 }
    }

    private fun weightedMean(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        var sum = 0.0
        var weight = 0.0
        values.forEachIndexed { i, v ->
            val w = 1.0 + i * 0.5
            sum += v * w
            weight += w
        }
        return (sum / weight).roundToInt()
    }

    private fun stdDev(values: List<Int>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }
}
