package com.nuanyue.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nuanyue.app.ui.components.LocalMotion
import com.nuanyue.app.ui.home.HOME_MODE_MONTH
import com.nuanyue.app.ui.home.HOME_MODE_RING
import com.nuanyue.app.ui.home.HOME_MODE_YEAR
import com.nuanyue.app.ui.home.HomeContent
import com.nuanyue.app.ui.settings.SettingsHooks
import com.nuanyue.app.ui.settings.SettingsPage
import com.nuanyue.app.ui.stats.StatsScreen
import com.nuanyue.app.ui.theme.NuanYueTheme
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.update.UpdatePanel
import com.nuanyue.app.update.UpdatePhase
import com.nuanyue.app.update.UpdateUi
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

    @Test fun statsOnATallPhone() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(411, 891))
        paparazzi.snapshot(name = "stats-411x891") { Page { StatsScreen(sampleState(history = true)) } }
    }

    @Test fun meWithAnUpdateCard() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(411, 891))
        paparazzi.snapshot(name = "me-411x891") {
            Page {
                SettingsPage(sampleState(), sampleUpdate(UpdatePhase.Available), SettingsHooks())
            }
        }
    }

    @Test fun updateSheet() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(411, 891))
        paparazzi.snapshot(name = "update-sheet") {
            Page {
                Box(Modifier.fillMaxSize().background(Palette.Bg), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(Color.White)
                            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp),
                    ) {
                        UpdatePanel(sampleUpdate(UpdatePhase.Downloading, received = 4_200_000, total = 12_000_000), {}, {})
                    }
                }
            }
        }
    }

    @Test fun bottomNavMidSlide() {
        paparazzi.unsafeUpdateConfig(deviceConfig = phone(411, 891))
        paparazzi.snapshot(name = "nav-mid") {
            Page {
                Box(Modifier.fillMaxSize().background(Palette.Bg), contentAlignment = Alignment.BottomCenter) {
                    BottomBar(Tab.HOME, slide = 0.45f) {}
                }
            }
        }
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
    Page {
        Column(Modifier.fillMaxSize().background(Color(0xFFFFF1EA))) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.weight(1f).background(Palette.Bg)) {
                HomeContent(
                    state = sampleState(),
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

private fun sampleState(history: Boolean = false): UiState {
    val today = LocalDate.of(2026, 10, 20)
    val periods = if (history) {
        listOf(
            Period("a", LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 6)),
            Period("b", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5)),
            Period("c", LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 4)),
            Period("d", LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 2)),
            Period("e", LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 26)),
            Period("open", LocalDate.of(2026, 10, 18), null),
        )
    } else {
        listOf(Period("open", LocalDate.of(2026, 10, 18), null))
    }
    return UiState(
        loaded = true,
        settings = AppSettings(onboarded = true, nickname = "猪猪"),
        snapshot = CycleEngine.snapshot(periods, defaultCycle = 28, defaultPeriod = 5, today = today),
        today = today,
    )
}

private fun sampleUpdate(phase: UpdatePhase, received: Long = 0, total: Long = 0) = UpdateUi(
    versionName = "1.7.0",
    versionCode = 8,
    notes = "月历改回圆角方格，更新改成在应用里下载。",
    phase = phase,
    received = received,
    total = total,
    pageUrl = "https://leafy.fxxkopenai.com/",
)

@Composable
private fun Page(content: @Composable () -> Unit) {
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
            CompositionLocalProvider(LocalMotion provides false) {
                content()
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
