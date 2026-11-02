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

    @Test fun compactFits110DpSquare() = checkSize("small-square", snapshot(3), false, 110, 110)
    @Test fun compactFitsTallHost() = checkSize("tall-host", snapshot(3), false, 140, 240)
    @Test fun wideFits110DpHeight() = checkSize("short-wide", snapshot(3), true, 280, 110)

    @Test fun squareResizeConstraintDoesNotForce160DpHeight() {
        val xml = paparazzi.context.resources.getXml(R.xml.period_widget_info)
        while (xml.next() != org.xmlpull.v1.XmlPullParser.START_TAG) { }
        val attrs = paparazzi.context.resources.obtainAttributes(xml, intArrayOf(android.R.attr.minResizeHeight))
        try {
            val dp = attrs.getDimension(0, 0f) / paparazzi.context.resources.displayMetrics.density
            assertTrue("Minimum height blocks square resize: $dp dp", dp <= 110f)
        } finally { attrs.recycle(); xml.close() }
    }

    private fun checkLayouts(name: String, state: WidgetState) {
        for (wide in listOf(false, true)) {
            checkSize(name, state, wide, if (wide) 300 else 140, 160)
        }
    }

    private fun checkSize(name: String, state: WidgetState, wide: Boolean, width: Int, height: Int) {
        paparazzi.unsafeUpdateConfig(deviceConfig = config(width, height))
        val parent = FrameLayout(paparazzi.context)
        val view = PeriodWidget.createViews(paparazzi.context, state, wide, width.toFloat(), height.toFloat()).apply(paparazzi.context, parent)
        val fontUrl = javaClass.classLoader!!.getResource("fonts/DroidSansFallback.ttf")!!
        val font = Typeface.createFromFile(File(fontUrl.toURI()))
        val textIds = listOf(R.id.widget_headline, R.id.widget_detail, R.id.widget_next_date, R.id.widget_cycle_label)
        textIds.forEach { view.findViewById<TextView>(it).typeface = font }
        parent.addView(view, FrameLayout.LayoutParams(-1, -1))
        paparazzi.snapshot(parent, name = "$name-${width}x${height}")
        assertEquals(state.headline, view.findViewById<TextView>(R.id.widget_headline).text.toString())
        for (id in textIds) {
            val text = view.findViewById<TextView>(id)
            if (text.visibility != View.VISIBLE) continue
            val layout = text.layout
            assertNotNull("Text not laid out: $id", layout)
            assertTrue("Clipped text: ${text.text}", layout.getLineWidth(0) <= text.width - text.paddingLeft - text.paddingRight + 1)
            assertTrue("Clipped height ${width}x${height}: ${text.text} layout=${layout.height} view=${text.height}", layout.height <= text.height - text.paddingTop - text.paddingBottom)
        }
        val cardId = paparazzi.context.resources.getIdentifier("widget_card", "id", paparazzi.context.packageName)
        val card = view.findViewById<View>(cardId)
        assertNotNull("Background card missing", card)
        assertTrue(card.width <= parent.width && card.height <= parent.height)
        assertEquals(if (state.hasCycle) View.VISIBLE else View.INVISIBLE, view.findViewById<ProgressBar>(R.id.widget_progress).visibility)
    }

    private fun snapshot(day: Int): WidgetState {
        val start = LocalDate.of(2026, 10, 18)
        val period = Period("synthetic", start, if (day <= 5) null else start.plusDays(4))
        return WidgetState.from(CycleEngine.snapshot(listOf(period), 29, 5, start.plusDays(day.toLong() - 1)))
    }

    companion object {
        private fun config(widthDp: Int, heightDp: Int = 160) = DeviceConfig(
            screenWidth = widthDp * 3, screenHeight = heightDp * 3,
            xdpi = 480, ydpi = 480, density = Density.XXHIGH,
            orientation = if (widthDp > heightDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            softButtons = false, navigation = Navigation.NONAV,
        )
    }
}
