package com.nuanyue.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nuanyue.app.MainActivity
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.R
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Phase
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeriodWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { render(context, manager, ids) } finally { pending.finish() }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PeriodWidget::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch { render(context, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as NuanYueApp
            val s = app.settings.current()
            val snap = CycleEngine.snapshot(app.repository.periodsNow(), s.defaultCycle, s.defaultPeriod)
            val t = snap.today
            val number = t.cycleDay?.toString() ?: "–"
            val phaseBg = when (t.phase) {
                Phase.MENSTRUAL, Phase.LATE -> R.drawable.widget_bg_period
                Phase.FERTILE, Phase.OVULATION -> R.drawable.widget_bg_fertile
                else -> R.drawable.widget_bg_calm
            }
            val ink = context.getColor(R.color.ny_ink)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            val today = LocalDate.now()
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val infos = CycleEngine.dayInfos(snap, monday, monday.plusDays(6), today)
            ids.forEach { id ->
                val width = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val wide = width >= 250
                val views = RemoteViews(context.packageName, if (wide) R.layout.widget_period_wide else R.layout.widget_period).apply {
                    setTextViewText(R.id.widget_label, "暖月")
                    setTextViewText(R.id.widget_number, number)
                    setTextViewText(R.id.widget_unit, "天")
                    setTextViewText(R.id.widget_phase, if (snap.hasData) t.phase.title else "未记录")
                    setInt(R.id.widget_phase, "setBackgroundResource", phaseBg)
                    if (wide) {
                        val next = t.nextStart
                        val whenText = if (next == null) "下次还不确定" else "预计 ${next.monthValue}月${next.dayOfMonth}日来月经"
                        setTextViewText(R.id.widget_sub, whenText)
                        setTextViewText(R.id.widget_week_title, "本周")
                        listOf(R.id.w0, R.id.w1, R.id.w2, R.id.w3, R.id.w4, R.id.w5, R.id.w6).forEachIndexed { i, view ->
                            val d = monday.plusDays(i.toLong())
                            val type = infos[d]?.type
                            setTextViewText(view, "${d.dayOfMonth}")
                            setTextColor(view, ink)
                            val bg = when (type) {
                                DayType.PERIOD -> context.getColor(R.color.ny_period_fill)
                                DayType.FERTILE, DayType.OVULATION -> context.getColor(R.color.ny_fertile)
                                DayType.PREDICTED_PERIOD -> context.getColor(R.color.ny_rose_container)
                                else -> context.getColor(R.color.ny_surface)
                            }
                            setInt(view, "setBackgroundColor", bg)
                        }
                    } else {
                        setTextViewText(R.id.widget_sub, "距下次月经约 ")
                        setTextViewText(R.id.widget_sub_num, "${t.daysUntilNext ?: "–"}")
                        setTextViewText(R.id.widget_sub_tail, " 天")
                    }
                    setOnClickPendingIntent(R.id.widget_root, open)
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}
