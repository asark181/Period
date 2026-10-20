package com.nuanyue.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.data.PhraseEntity
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Period
import com.nuanyue.app.ui.BottomBar
import com.nuanyue.app.ui.Tab
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.home.HOME_MODE_MONTH
import com.nuanyue.app.ui.home.HOME_MODE_RING
import com.nuanyue.app.ui.home.HOME_MODE_YEAR
import com.nuanyue.app.ui.home.HomeContent
import com.nuanyue.app.ui.theme.NuanYueTheme
import com.nuanyue.app.ui.theme.Palette
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Home tab at phone sizes. October 2026, nickname 猪猪, period open since the 18th,
 * today the 20th (day 3). Motion is off so the frame settles. The 24dp / 16dp
 * padding stands in for the status bar and gesture inset, which layoutlib leaves at 0.
 */
class HomeScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = phone(411, 891),
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Test fun monthOnATallPhone() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(411, 891))
        paparazzi.snapshot(name = "home-411x891") { HomeShot(HOME_MODE_MONTH) }
    }

    @Test fun monthOnASmallPhone() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, 640))
        paparazzi.snapshot(name = "home-360x640") { HomeShot(HOME_MODE_MONTH) }
    }

    @Test fun monthOnASmallPhoneWithLargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, 640, fontScale = 1.3f))
        paparazzi.snapshot(name = "home-360x640-font") { HomeShot(HOME_MODE_MONTH) }
    }

    @Test fun yearOnASmallPhone() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, 640))
        paparazzi.snapshot(name = "home-year-360x640") { HomeShot(HOME_MODE_YEAR) }
    }

    @Test fun ringOnASmallPhone() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, 640))
        paparazzi.snapshot(name = "home-ring-360x640") { HomeShot(HOME_MODE_RING) }
    }
}

private fun phone(widthDp: Int, heightDp: Int, fontScale: Float = 1f) = DeviceConfig(
    screenWidth = widthDp * 2,
    screenHeight = heightDp * 2,
    xdpi = 320,
    ydpi = 320,
    density = Density.XHIGH,
    fontScale = fontScale,
    softButtons = false,
)

@Composable
private fun HomeShot(mode: Int) {
    val today = LocalDate.of(2026, 10, 20)
    val state = UiState(
        loaded = true,
        settings = AppSettings(onboarded = true, nickname = "猪猪"),
        snapshot = CycleEngine.snapshot(
            listOf(Period("open", LocalDate.of(2026, 10, 18), null)),
            defaultCycle = 28,
            defaultPeriod = 5,
            today = today,
        ),
        today = today,
    )
    val cjk = remember {
        FontFamily(
            Font(cjkFontFile(), FontWeight.Normal),
            Font(cjkFontFile(), FontWeight.Medium),
            Font(cjkFontFile(), FontWeight.SemiBold),
            Font(cjkFontFile(), FontWeight.Bold),
        )
    }
    NuanYueTheme {
        val colors = MaterialTheme.colorScheme
        val type = MaterialTheme.typography
        MaterialTheme(colorScheme = colors, typography = type.withFont(cjk)) {
            Column(Modifier.fillMaxSize().background(Color(0xFFFFEEF2))) {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.weight(1f).background(Palette.Bg)) {
                    HomeContent(
                        state = state,
                        phrase = PhraseEntity(id = 1, category = "P", text = "给自己点一份热乎乎的砂锅粥。"),
                        onNextPhrase = {},
                        onToast = {},
                        onMarkStart = {},
                        onUnmarkStart = {},
                        onMarkEnd = {},
                        onUnmarkEnd = {},
                        now = LocalTime.of(12, 0),
                        motion = false,
                        initialMode = mode,
                    )
                }
                Column(Modifier.fillMaxWidth().background(Color.White)) {
                    BottomBar(Tab.HOME) {}
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun cjkFontFile(): File {
    val url = HomeScreenshotTest::class.java.classLoader!!.getResource("fonts/DroidSansFallback.ttf")
        ?: error("test font fonts/DroidSansFallback.ttf is missing")
    if (url.protocol == "file") return File(url.toURI())
    val tmp = File.createTempFile("cjk", ".ttf")
    url.openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
    return tmp
}

private fun TextStyle.withFont(font: FontFamily) = copy(fontFamily = font)

private fun androidx.compose.material3.Typography.withFont(font: FontFamily) = copy(
    displayLarge = displayLarge.withFont(font),
    displayMedium = displayMedium.withFont(font),
    displaySmall = displaySmall.withFont(font),
    headlineLarge = headlineLarge.withFont(font),
    headlineMedium = headlineMedium.withFont(font),
    headlineSmall = headlineSmall.withFont(font),
    titleLarge = titleLarge.withFont(font),
    titleMedium = titleMedium.withFont(font),
    titleSmall = titleSmall.withFont(font),
    bodyLarge = bodyLarge.withFont(font),
    bodyMedium = bodyMedium.withFont(font),
    bodySmall = bodySmall.withFont(font),
    labelLarge = labelLarge.withFont(font),
    labelMedium = labelMedium.withFont(font),
    labelSmall = labelSmall.withFont(font),
)
