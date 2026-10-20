package com.nuanyue.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.nuanyue.app.MainActivity
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.R
import com.nuanyue.app.domain.CycleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeriodWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

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
            val state = WidgetState.from(snap)
            ids.forEach { id ->
                val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(mapOf(
                        SizeF(140f, 160f) to createViews(context, state, wide = false),
                        SizeF(300f, 160f) to createViews(context, state, wide = true),
                    ))
                } else {
                    val width = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    createViews(context, state, wide = width >= 300)
                }
                manager.updateAppWidget(id, views)
            }
        }

        internal fun createViews(context: Context, state: WidgetState, wide: Boolean): RemoteViews {
            val bg = when (state.tone) {
                WidgetTone.PERIOD -> R.drawable.widget_bg_period
                WidgetTone.FERTILE -> R.drawable.widget_bg_fertile
                WidgetTone.EMPTY -> R.drawable.widget_bg_empty
                WidgetTone.CALM -> R.drawable.widget_bg_calm
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            return RemoteViews(context.packageName, if (wide) R.layout.widget_period_wide else R.layout.widget_period).apply {
                setTextViewText(R.id.widget_headline, state.headline)
                setTextViewText(R.id.widget_detail, state.detail)
                setTextViewText(R.id.widget_next_date, state.nextDate)
                setViewVisibility(R.id.widget_next_date, if (state.nextDate.isEmpty()) View.GONE else View.VISIBLE)
                setTextViewText(R.id.widget_cycle_label, state.cycleLabel)
                setProgressBar(R.id.widget_progress, 100, state.progressPercent, false)
                setViewVisibility(R.id.widget_progress, if (state.hasCycle) View.VISIBLE else View.INVISIBLE)
                setInt(R.id.widget_root, "setBackgroundResource", bg)
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }
    }
}
