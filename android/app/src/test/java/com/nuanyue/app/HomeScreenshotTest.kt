package com.nuanyue.app

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.Navigation
import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.data.PhraseEntity
import com.nuanyue.app.data.Role
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Period
import com.nuanyue.app.ui.BottomBar
import com.nuanyue.app.ui.Tab
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.home.ConfirmBody
import com.nuanyue.app.ui.home.HOME_MODE_MONTH
import com.nuanyue.app.ui.home.HOME_MODE_RING
import com.nuanyue.app.ui.home.HOME_MODE_YEAR
import com.nuanyue.app.ui.home.HomeContent
import com.nuanyue.app.ui.home.Pending
import com.nuanyue.app.ui.onboarding.OnboardingScreen
import com.nuanyue.app.ui.settings.BatteryDialog
import com.nuanyue.app.ui.settings.ConfirmDialog
import com.nuanyue.app.ui.settings.SettingsActions
import com.nuanyue.app.ui.settings.SettingsPage
import com.nuanyue.app.ui.settings.SyncDialog
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
 * 1.6.0 UI on two phones: Xiaomi 15 Pro (1440×3200, 560dpi ≈ 411dp, gesture nav)
 * and a 360×640dp phone. Layoutlib leaves system insets at 0, so the 24dp / 16dp
 * spacers stand in for the status bar and the gesture band.
 */
class HomeScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = xiaomi15Pro,
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Test fun homeToday() = both("home-today") { HomeShot(HOME_MODE_MONTH) }

    @Test fun homePastDate() = both("home-past") {
        HomeShot(HOME_MODE_MONTH, selected = LocalDate.of(2026, 10, 16))
    }

    @Test fun homeFutureDate() = both("home-future") {
        HomeShot(HOME_MODE_MONTH, selected = LocalDate.of(2026, 10, 24))
    }

    @Test fun homeEmpty() = both("home-empty") { HomeShot(HOME_MODE_MONTH, history = false, empty = true) }

    @Test fun homePartner() = both("home-partner") { HomeShot(HOME_MODE_MONTH, partner = true) }

    @Test fun year() = both("year") { HomeShot(HOME_MODE_YEAR) }

    @Test fun ring() = both("ring") { HomeShot(HOME_MODE_RING) }

    @Test fun stats() = both("stats") { Page(Tab.STATS) { StatsScreen(sampleState(history = true)) } }

    @Test fun statsEmpty() = both("stats-empty") { Page(Tab.STATS) { StatsScreen(sampleState(empty = true)) } }

    @Test fun recordStart() = both("record-start") {
        Sheet { ConfirmBody(Pending(LocalDate.of(2026, 10, 20), start = true, undo = false), {}, {}) }
    }

    @Test fun recordEnd() = both("record-end") {
        Sheet { ConfirmBody(Pending(LocalDate.of(2026, 10, 20), start = false, undo = false), {}, {}) }
    }

    @Test fun me() = both("me") { Page(Tab.ME) { SettingsPage(sampleState(), SettingsActions()) } }

    @Test fun mePartner() = both("me-partner") {
        Page(Tab.ME) { SettingsPage(sampleState(partner = true), SettingsActions()) }
    }

    @Test fun settingsSync() = both("settings-sync") {
        Page(Tab.ME) { SyncDialog(syncing = false, error = null, onDismiss = {}, onSubmit = { _, _ -> }) }
    }

    @Test fun settingsClear() = both("settings-clear") {
        Page(Tab.ME) {
            ConfirmDialog("清空所有记录？", "经期和每日记录会全部删除，无法恢复。", {}, {})
        }
    }

    @Test fun settingsLogout() = both("settings-logout") {
        Page(Tab.ME) { ConfirmDialog("退出同步？", "本机和云端的数据都会保留。", {}, {}) }
    }

    @Test fun settingsBattery() = both("settings-battery") {
        Page(Tab.ME) { BatteryDialog({}, {}) }
    }

    @Test fun updateDialog() = both("update") {
        Sheet {
            UpdatePanel(
                UpdateUi(
                    versionName = "1.9.2",
                    versionCode = 13,
                    notes = "修了未来日期不能记，首页还是熟悉的样子。",
                    phase = UpdatePhase.Available,
                    pageUrl = "https://example.com/nuanyue",
                ),
                onPrimary = {},
                onPage = {},
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp),
            )
        }
    }

    @Test fun onboarding() = both("onboarding") {
        Page(Tab.HOME, bar = false) {
            OnboardingScreen(syncing = false, onFinish = { _, _, _, _ -> }, onInvite = { _, _, _ -> })
        }
    }

    private fun both(name: String, content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(deviceConfig = xiaomi15Pro)
        paparazzi.snapshot(name = "$name-xiaomi-1440x3200") { content() }
        paparazzi.unsafeUpdateConfig(deviceConfig = smallPhone)
        paparazzi.snapshot(name = "$name-360x640") { content() }
    }
}

/** Xiaomi 15 Pro: 1440×3200, 560dpi → about 411×914dp. No soft keys (gesture navigation). */
private val xiaomi15Pro = DeviceConfig(
    screenWidth = 1440,
    screenHeight = 3200,
    xdpi = 560,
    ydpi = 560,
    density = Density.create(560),
    softButtons = false,
    navigation = Navigation.NONAV,
)

private val smallPhone = DeviceConfig(
    screenWidth = 720,
    screenHeight = 1280,
    xdpi = 320,
    ydpi = 320,
    density = Density.XHIGH,
    softButtons = false,
    navigation = Navigation.NONAV,
)

@Composable
private fun HomeShot(
    mode: Int,
    selected: LocalDate? = null,
    history: Boolean = true,
    empty: Boolean = false,
    partner: Boolean = false,
) {
    Page(Tab.HOME) {
        HomeContent(
            state = sampleState(history = history && !empty, empty = empty, partner = partner),
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
            initialSelected = selected,
        )
    }
}

@Composable
private fun Page(tab: Tab, bar: Boolean = true, content: @Composable () -> Unit) {
    val cjk = remember {
        FontFamily(
            Font(cjkFontFile(), FontWeight.Normal),
            Font(cjkFontFile(), FontWeight.Medium),
            Font(cjkFontFile(), FontWeight.SemiBold),
            Font(cjkFontFile(), FontWeight.Bold),
        )
    }
    val registryOwner = remember {
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: androidx.core.app.ActivityOptionsCompat?,
                ) = Unit
            }
        }
    }
    NuanYueTheme {
        val colors = MaterialTheme.colorScheme
        val type = MaterialTheme.typography
        MaterialTheme(colorScheme = colors, typography = type.withFont(cjk)) {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
            Column(Modifier.fillMaxSize().background(Palette.Bg)) {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.weight(1f)) { content() }
                if (bar) {
                    Column(Modifier.fillMaxWidth().background(Color.White)) {
                        BottomBar(tab) {}
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    Page(Tab.HOME, bar = false) {
        Box(Modifier.fillMaxSize().background(Color(0x524A3540)), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.height(4.dp).fillMaxWidth(0.12f).clip(RoundedCornerShape(2.dp)).background(Palette.PinkLine))
                }
                content()
            }
        }
    }
}

private fun sampleState(history: Boolean = true, empty: Boolean = false, partner: Boolean = false): UiState {
    val today = LocalDate.of(2026, 10, 20)
    val periods = when {
        empty -> emptyList()
        history -> listOf(
            Period("a", LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 2)),
            Period("b", LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 26)),
            Period("open", LocalDate.of(2026, 10, 18), null),
        )
        else -> listOf(Period("open", LocalDate.of(2026, 10, 18), null))
    }
    return UiState(
        loaded = true,
        settings = AppSettings(
            onboarded = true,
            nickname = "猪猪",
            role = if (partner) Role.PARTNER else Role.SELF,
        ),
        snapshot = CycleEngine.snapshot(periods, defaultCycle = 28, defaultPeriod = 5, today = today),
        today = today,
    )
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
