package com.nuanyue.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.ui.components.LocalMotion
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.WarmMessageHost
import com.nuanyue.app.ui.home.HomeScreen
import com.nuanyue.app.ui.onboarding.OnboardingScreen
import com.nuanyue.app.ui.settings.SettingsScreen
import com.nuanyue.app.ui.update.UpdateSheet
import com.nuanyue.app.ui.stats.StatsScreen
import com.nuanyue.app.ui.theme.NyColors
import com.nuanyue.app.ui.theme.Palette

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Rounded.Brightness3),
    STATS("统计", Icons.Rounded.BarChart),
    ME("我", Icons.Rounded.PersonOutline),
}

@Composable
fun NuanYueRoot(vm: AppViewModel = viewModel()) {
    val ctx = LocalContext.current
    val motion = remember {
        android.provider.Settings.Global.getFloat(
            ctx.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    CompositionLocalProvider(LocalMotion provides motion) {
        NuanYueScaffold(vm)
    }
}

@Composable
private fun NuanYueScaffold(vm: AppViewModel) {
    val ctx = LocalContext.current
    val motion = LocalMotion.current
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    if (!state.loaded) {
        LoadingScreen()
        return
    }
    if (!state.settings.onboarded) {
        val syncing by vm.syncing.collectAsStateWithLifecycle()
        OnboardingScreen(
            syncing = syncing,
            onFinish = vm::finishOnboarding,
            onInvite = { code, partner, onError -> vm.joinInvite(code, partner) { ok, err -> if (!ok) onError(err ?: "连接失败") } },
        )
        return
    }

    Scaffold(
        containerColor = Palette.Bg,
        snackbarHost = { WarmMessageHost(message, vm::consumeMessage) },
        bottomBar = { BottomBar(tab) { tab = it } },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            AnimatedContent(
                tab,
                transitionSpec = {
                    if (!motion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(320)) { it / 5 * dir } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 5 * dir } + fadeOut(tween(180)))
                    }
                },
                label = "tab",
            ) { t ->
                when (t) {
                    Tab.HOME -> HomeScreen(state, vm)
                    Tab.STATS -> StatsScreen(state)
                    Tab.ME -> SettingsScreen(state, vm, update)
                }
            }
        }
    }
    update?.takeIf { it.sheet }?.let { offer ->
        UpdateSheet(
            offer,
            onPrimary = { vm.updatePrimary(ctx) },
            onDismiss = vm::dismissUpdate,
            onPage = { vm.openUpdatePage(ctx) },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        Modifier.fillMaxSize().background(Palette.Bg).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(Modifier.size(108.dp), interactive = false)
            Spacer(Modifier.height(14.dp))
            Text("正在打开记录", color = Palette.InkSoft, fontSize = 14.sp)
        }
    }
}

private val tabOutline = listOf(
    Icons.Outlined.Brightness3,
    Icons.Outlined.BarChart,
    Icons.Outlined.Person,
)
private val tabFilled = listOf(
    Icons.Rounded.Brightness3,
    Icons.Rounded.BarChart,
    Icons.Rounded.Person,
)

/**
 * Full-width white bar, flush to the bottom, with a hairline on top. The selected
 * tab is a rose icon inside a 56dp circle that sits above the bar. Only that
 * circle moves. The 64dp label row and the raised circle stay one piece; system
 * navigation insets pad that piece up and the white fill continues underneath.
 * [slide] is a still mid-frame: 0 is the previous tab, 1 is [current].
 */
@Composable
internal fun BottomBar(current: Tab, slide: Float? = null, onSelect: (Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val motion = LocalMotion.current && slide == null
    val target = if (slide == null) current.ordinal.toFloat() else (current.ordinal - 1 + slide).coerceIn(0f, 2f)
    val index by animateFloatAsState(
        target,
        if (motion) spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow) else tween(0),
        label = "bubble",
    )
    val shown = if (slide != null || !motion) target else index
    val riding = shown.roundToNearest()
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val barTop = 22.dp.toPx()
            val cx = (shown + 0.5f) / 3f * size.width
            val radius = 34.dp.toPx()
            val path = Path().apply {
                moveTo(0f, barTop)
                val left = (cx - radius - 18.dp.toPx()).coerceAtLeast(0f)
                val right = (cx + radius + 18.dp.toPx()).coerceAtMost(size.width)
                lineTo(left, barTop)
                cubicTo(cx - radius * 0.72f, barTop, cx - radius * 0.92f, 0f, cx, 0f)
                cubicTo(cx + radius * 0.92f, 0f, cx + radius * 0.72f, barTop, right, barTop)
                lineTo(size.width, barTop)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, Color.White)
            drawPath(path, NyColors.Hairline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
        }
        Box(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(86.dp),
        ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(68.dp)) {
            val x = maxWidth * (shown + 0.5f) / 3f - 34.dp
            Box(
                Modifier.padding(start = x).size(68.dp).border(6.dp, Color.White, CircleShape).padding(6.dp)
                    .clip(CircleShape).background(NyColors.RoseContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tabFilled[riding], Tab.entries[riding].label, tint = NyColors.Rose, modifier = Modifier.size(26.dp))
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp), verticalAlignment = Alignment.Bottom) {
            Tab.entries.forEachIndexed { i, t ->
                val selected = i == current.ordinal && slide == null
                Column(
                    Modifier.weight(1f).padding(bottom = 8.dp)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            if (t != current) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(t)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!selected) {
                        Icon(tabOutline[i], t.label, tint = NyColors.InkTertiary, modifier = Modifier.size(22.dp))
                    } else {
                        Spacer(Modifier.height(22.dp))
                    }
                    Text(
                        t.label,
                        fontSize = if (selected) 14.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) NyColors.Ink else NyColors.TabUnselected,
                    )
                }
            }
        }
        }
    }
}

private fun Float.roundToNearest(): Int = kotlin.math.round(this).toInt().coerceIn(0, 2)
