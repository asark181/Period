package com.nuanyue.app.widget

import com.nuanyue.app.domain.CycleSnapshot
import com.nuanyue.app.domain.Phase
import java.time.LocalDate
import kotlin.math.roundToInt

enum class WidgetTone { PERIOD, FERTILE, CALM, EMPTY }

/** 只整理已有预测供桌面展示，不参与预测和记录写入。 */
data class WidgetState(
    val headline: String,
    val detail: String,
    val nextDate: String,
    val cycleLabel: String,
    val progressPercent: Int,
    val hasCycle: Boolean,
    val tone: WidgetTone,
) {
    companion object {
        fun from(snapshot: CycleSnapshot): WidgetState {
            val today = snapshot.today
            if (!snapshot.hasData || today.cycleDay == null) {
                return WidgetState("开始记录", "点我记录第一次经期", "", "记录后显示周期进度", 0, false, WidgetTone.EMPTY)
            }
            val menstrual = today.phase == Phase.MENSTRUAL
            val late = !menstrual && today.lateDays > 0
            val headline = when {
                menstrual -> "经期第 ${today.periodDay} 天"
                late -> "已推迟 ${today.lateDays} 天"
                today.daysUntilNext == 0 -> "预计今天来"
                else -> "还有 ${today.daysUntilNext} 天"
            }
            val detail = if (menstrual) "预计还有 ${today.periodDaysLeft} 天结束" else today.phase.title
            // Today.nextStart 在推迟时会移到今天；桌面必须保留原预测日期。
            val next = if (late) snapshot.nextStart else today.nextStart
            val nextDate = next?.let { "${if (late) "原预计" else "下次"} ${date(it)}" }.orEmpty()
            val tone = when (today.phase) {
                Phase.MENSTRUAL, Phase.LATE -> WidgetTone.PERIOD
                Phase.FERTILE, Phase.OVULATION -> WidgetTone.FERTILE
                else -> WidgetTone.CALM
            }
            return WidgetState(
                headline, detail, nextDate,
                "周期第 ${today.cycleDay} 天 / ${snapshot.avgCycle} 天",
                (today.progress * 100).roundToInt().coerceIn(0, 100), true, tone,
            )
        }

        private fun date(date: LocalDate) = "${date.monthValue}月${date.dayOfMonth}日"
    }
}
