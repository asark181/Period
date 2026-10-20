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
import com.nuanyue.app.domain.Phase
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
            val (label, number, unit) = when {
                !snap.hasData -> Triple("暖月", "–", "")
                t.phase == Phase.MENSTRUAL -> Triple("经期第", "${t.periodDay}", "天")
                t.lateDays > 0 -> Triple("已推迟", "${t.lateDays}", "天")
                t.daysUntilNext == 0 -> Triple("下次经期", "今天", "")
                else -> Triple("距离大姨妈", "${t.daysUntilNext}", "天")
            }
            val sub = if (snap.hasData) t.phase.title else "点我开始记录"
            val (bg, fg) = when (t.phase) {
                Phase.MENSTRUAL, Phase.LATE -> R.drawable.widget_bg_period to 0xFFFFFFFF.toInt()
                Phase.FERTILE, Phase.OVULATION -> R.drawable.widget_bg_fertile to 0xFFFFFFFF.toInt()
                Phase.NONE -> R.drawable.widget_bg_empty to context.getColor(R.color.ink)
                else -> R.drawable.widget_bg_calm to context.getColor(R.color.ink)
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_period).apply {
                    setTextViewText(R.id.widget_label, label)
                    setTextViewText(R.id.widget_number, number)
                    setTextViewText(R.id.widget_unit, unit)
                    setTextViewText(R.id.widget_sub, sub)
                    setInt(R.id.widget_root, "setBackgroundResource", bg)
                    listOf(R.id.widget_label, R.id.widget_number, R.id.widget_unit, R.id.widget_sub).forEach { setTextColor(it, fg) }
                    setOnClickPendingIntent(R.id.widget_root, open)
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}
