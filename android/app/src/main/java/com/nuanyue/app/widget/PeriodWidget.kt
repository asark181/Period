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
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
import com.nuanyue.app.MainActivity
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.R
import com.nuanyue.app.domain.CycleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

/** 保留旧组件名，让已添加的小组件在覆盖安装后继续更新。 */
open class PeriodWidget : AppWidgetProvider() {
    protected open val wideLayout: Boolean = false

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { render(context, manager, ids, wideLayout) } finally { pending.finish() }
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val targets = listOf(PeriodWidget::class.java to false, WidePeriodWidget::class.java to true)
                .map { (provider, wide) -> manager.getAppWidgetIds(ComponentName(context, provider)) to wide }
                .filter { (ids, _) -> ids.isNotEmpty() }
            if (targets.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                targets.forEach { (ids, wide) -> render(context, manager, ids, wide) }
            }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray, wide: Boolean) {
            val app = context.applicationContext as NuanYueApp
            val s = app.settings.current()
            val state = WidgetState.from(CycleEngine.snapshot(app.repository.periodsNow(), s.defaultCycle, s.defaultPeriod))
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: if (wide) 250 else 110
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: 110
                val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth).coerceAtLeast(minWidth)
                val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight).coerceAtLeast(minHeight)
                val sizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                        .orEmpty().filter { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 }
                        .distinct().take(16)
                } else emptyList()
                val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sizes.isNotEmpty()) {
                    RemoteViews(sizes.associateWith { size -> createViews(context, state, wide, size.width, size.height) })
                } else {
                    // 部分桌面不提供 OPTION_APPWIDGET_SIZES，继续按横竖屏的可用范围适配。
                    RemoteViews(
                        createViews(context, state, wide, maxWidth.toFloat(), minHeight.toFloat()),
                        createViews(context, state, wide, minWidth.toFloat(), maxHeight.toFloat()),
                    )
                }
                manager.updateAppWidget(id, views)
            }
        }

        internal fun createViews(
            context: Context, state: WidgetState, wide: Boolean,
            widthDp: Float = if (wide) 300f else 140f, heightDp: Float = 140f,
        ): RemoteViews {
            val bg = when (state.tone) {
                WidgetTone.PERIOD -> R.drawable.widget_bg_period
                WidgetTone.FERTILE -> R.drawable.widget_bg_fertile
                WidgetTone.EMPTY -> R.drawable.widget_bg_empty
                WidgetTone.CALM -> R.drawable.widget_bg_calm
            }
            val headlineColor = when (state.tone) {
                WidgetTone.PERIOD -> 0xFFE25373.toInt()
                WidgetTone.FERTILE -> 0xFFD67B27.toInt()
                WidgetTone.CALM -> 0xFF4A3540.toInt()
                WidgetTone.EMPTY -> 0xFF4A3540.toInt()
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            return RemoteViews(context.packageName, if (wide) R.layout.widget_period_wide else R.layout.widget_period).apply {
                setTextViewText(R.id.widget_headline, state.headline)
                setTextColor(R.id.widget_headline, headlineColor)
                setTextViewText(R.id.widget_detail, state.detail)
                setTextViewText(R.id.widget_next_date, state.nextDate)
                // 空数据也保留这一行的空间，避免按比例分配的文字区域突然变形。
                setViewVisibility(R.id.widget_next_date, if (state.nextDate.isEmpty()) View.INVISIBLE else View.VISIBLE)
                setTextViewText(R.id.widget_cycle_label, state.cycleLabel)
                setProgressBar(R.id.widget_progress, 100, state.progressPercent, false)
                setViewVisibility(R.id.widget_progress, if (state.hasCycle) View.VISIBLE else View.INVISIBLE)
                setInt(R.id.widget_card, "setBackgroundResource", bg)
                setOnClickPendingIntent(R.id.widget_card, open)
            }
        }
    }
}

/** 独立入口使桌面直接按 4×2 添加，而非要求用户先拉宽方形卡片。 */
class WidePeriodWidget : PeriodWidget() {
    override val wideLayout = true
}
