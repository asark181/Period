package com.nuanyue.app

import android.graphics.Typeface
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.Navigation
import com.android.resources.ScreenOrientation
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Period
import com.nuanyue.app.widget.PeriodWidget
import com.nuanyue.app.widget.WidgetState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** 用真正的 RemoteViews 膨胀布局，校验桌面宿主支持的控件及最小尺寸的文字边界。 */
class WidgetLayoutTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = config(140), theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Test fun compactAndWideShowAllPeriodInformation() = checkLayouts("period", snapshot(3))
    @Test fun compactAndWideShowCountdown() = checkLayouts("countdown", snapshot(8))
    @Test fun compactAndWideShowFertilePhase() = checkLayouts("fertile", snapshot(12))
    @Test fun compactAndWideShowLateAndOriginalDate() = checkLayouts("late", snapshot(34))
    @Test fun emptyHidesPredictionAndProgress() = checkLayouts(
        "empty", WidgetState.from(CycleEngine.snapshot(emptyList(), 29, 5, LocalDate.of(2026, 10, 20))),
    )

    private fun checkLayouts(name: String, state: WidgetState) {
        for (wide in listOf(false, true)) {
            paparazzi.unsafeUpdateConfig(deviceConfig = config(if (wide) 300 else 140))
            val parent = FrameLayout(paparazzi.context)
            val view = PeriodWidget.createViews(paparazzi.context, state, wide).apply(paparazzi.context, parent)
            val fontUrl = javaClass.classLoader!!.getResource("fonts/DroidSansFallback.ttf")!!
            val font = Typeface.createFromFile(File(fontUrl.toURI()))
            val textIds = listOf(R.id.widget_headline, R.id.widget_detail, R.id.widget_next_date, R.id.widget_cycle_label)
            textIds.forEach { view.findViewById<TextView>(it).typeface = font }
            parent.addView(view, FrameLayout.LayoutParams(-1, -1))
            paparazzi.snapshot(parent, name = "$name-${if (wide) "4x2" else "2x2"}")
            assertEquals(state.headline, view.findViewById<TextView>(R.id.widget_headline).text.toString())
            for (id in textIds) {
                val text = view.findViewById<TextView>(id)
                if (text.visibility != View.VISIBLE) continue
                val layout = text.layout
                assertNotNull("Text not laid out: $id", layout)
                assertTrue("Clipped text: ${text.text}", layout.getLineWidth(0) <= text.width - text.paddingLeft - text.paddingRight + 1)
                assertTrue("Clipped height wide=$wide: ${text.text} layout=${layout.height} view=${text.height} padding=${text.paddingTop + text.paddingBottom} lines=${layout.lineCount}", layout.height <= text.height - text.paddingTop - text.paddingBottom)
            }
            assertEquals(if (state.hasCycle) View.VISIBLE else View.INVISIBLE, view.findViewById<ProgressBar>(R.id.widget_progress).visibility)
        }
    }

    private fun snapshot(day: Int): WidgetState {
        val start = LocalDate.of(2026, 10, 18)
        val period = Period("synthetic", start, if (day <= 5) null else start.plusDays(4))
        return WidgetState.from(CycleEngine.snapshot(listOf(period), 29, 5, start.plusDays(day.toLong() - 1)))
    }

    companion object {
        private fun config(widthDp: Int) = DeviceConfig(
            screenWidth = widthDp * 3, screenHeight = 160 * 3,
            xdpi = 480, ydpi = 480, density = Density.XXHIGH,
            orientation = if (widthDp > 160) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            softButtons = false, navigation = Navigation.NONAV,
        )
    }
}
