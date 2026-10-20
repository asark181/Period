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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.nuanyue.app.ui.theme.Palette

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("暖月", Icons.Rounded.Brightness3),
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

private data class BubbleStyle(val wash: Color, val ink: Color, val outline: ImageVector, val filled: ImageVector)

private val bubbles = listOf(
    BubbleStyle(Color(0xFFFFD9E4), Palette.Pink, Icons.Outlined.Brightness3, Icons.Rounded.Brightness3),
    BubbleStyle(Color(0xFFD9E6FF), Color(0xFF5B8DEF), Icons.Outlined.BarChart, Icons.Rounded.BarChart),
    BubbleStyle(Color(0xFFFFE4C8), Palette.Orange, Icons.Outlined.Person, Icons.Rounded.Person),
)

/**
 * White bar. The selected tab is a filled icon inside a pastel circle that sits
 * up over the top edge. [slide] is only for a still mid-frame: the current tab
 * is rising by that amount and the previous tab is sinking by the rest.
 */
@Composable
internal fun BottomBar(current: Tab, slide: Float? = null, onSelect: (Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val motion = LocalMotion.current && slide == null
    // The circle is laid out inside this box, so the top half can sit above the
    // white bar without drawing outside the bar's own bounds (those get clipped).
    Box(Modifier.fillMaxWidth().navigationBarsPadding().height(86.dp)) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(54.dp)
                .shadow(6.dp, RectangleShape, clip = false, ambientColor = Color(0x102B3040), spotColor = Color(0x182B3040))
                .background(Color.White),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Tab.entries.forEachIndexed { i, t ->
                val style = bubbles[i]
                val target = when {
                    slide != null && i == current.ordinal -> slide
                    slide != null && current.ordinal > 0 && i == current.ordinal - 1 -> 1f - slide
                    slide != null -> 0f
                    i == current.ordinal -> 1f
                    else -> 0f
                }
                val pop by animateFloatAsState(
                    target,
                    if (motion) spring(dampingRatio = 0.28f, stiffness = Spring.StiffnessMedium) else tween(0),
                    label = "pop",
                )
                val risen = pop > 0.5f
                Column(
                    Modifier.weight(1f)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            if (t != current) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(t)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(52.dp)) {
                        Box(
                            Modifier.align(Alignment.TopCenter).size(44.dp).graphicsLayer {
                                val s = 0.8f + 0.2f * pop
                                scaleX = s
                                scaleY = s
                                translationY = (1f - pop) * 12f * density
                                alpha = pop.coerceIn(0f, 1f)
                            }
                                .shadow(5.dp, CircleShape, clip = false, ambientColor = Color(0x182B3040), spotColor = Color(0x262B3040))
                                .clip(CircleShape)
                                .background(style.wash),
                        )
                        Icon(
                            if (risen) style.filled else style.outline,
                            t.label,
                            tint = if (risen) style.ink else Color(0xFFC5CAD6),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(22.dp)
                                .graphicsLayer {
                                    val s = if (pop > 0.02f) 0.8f + 0.2f * pop else 1f
                                    scaleX = s
                                    scaleY = s
                                    translationY = -17f * pop * density
                                },
                        )
                    }
                    Text(
                        t.label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (risen) FontWeight.Bold else FontWeight.Normal,
                        color = if (risen) Palette.Ink else Color(0xFF9AA3B2),
                    )
                }
            }
        }
    }
}
