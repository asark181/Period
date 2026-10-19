package com.nuanyue.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.SlidingIndicator
import com.nuanyue.app.ui.components.WarmMessageHost
import com.nuanyue.app.ui.home.HomeScreen
import com.nuanyue.app.ui.onboarding.OnboardingScreen
import com.nuanyue.app.ui.settings.SettingsScreen
import com.nuanyue.app.ui.stats.StatsScreen
import com.nuanyue.app.ui.theme.Palette

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("暖月", Icons.Rounded.Brightness3),
    STATS("统计", Icons.Rounded.BarChart),
    ME("我", Icons.Rounded.PersonOutline),
}

@Composable
fun NuanYueRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
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
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            AnimatedContent(
                tab,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(320)) { it / 5 * dir } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(260)) { -it / 5 * dir } + fadeOut(tween(180)))
                },
                label = "tab",
            ) { t ->
                when (t) {
                    Tab.HOME -> HomeScreen(state, vm)
                    Tab.STATS -> StatsScreen(state)
                    Tab.ME -> SettingsScreen(state, vm)
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        Modifier.fillMaxSize().background(Palette.Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(Modifier.size(108.dp), interactive = false)
            Spacer(Modifier.height(14.dp))
            Text("正在打开记录", color = Palette.InkSoft, fontSize = 14.sp)
        }
    }
}

private val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxWidth()
            .shadow(18.dp, barShape, clip = false, ambientColor = Color(0x18E59AAB), spotColor = Color(0x28E59AAB))
            .clip(barShape)
            .background(Color.White),
    ) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            SlidingIndicator(
                Tab.entries.size, current.ordinal,
                Modifier.fillMaxWidth().padding(top = 6.dp),
                color = Palette.PinkSoft,
                pillWidth = 64.dp,
                pillHeight = 36.dp,
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Tab.entries.forEach { t ->
                    val selected = t == current
                    val pop by animateFloatAsState(
                        if (selected) 1.16f else 1f,
                        spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow),
                        label = "pop",
                    )
                    val lift by animateFloatAsState(if (selected) -1.5f else 0f, spring(dampingRatio = 0.55f), label = "lift")
                    val tint by animateColorAsState(if (selected) Palette.Pink else Palette.InkFaint, label = "tint")
                    val text by animateColorAsState(if (selected) Palette.PinkDeep else Palette.InkFaint, label = "text")
                    val icon = if (selected && t == Tab.ME) Icons.Rounded.Person else t.icon
                    Column(
                        Modifier.weight(1f).fillMaxSize()
                            .clickable(remember { MutableInteractionSource() }, indication = null) {
                                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(t)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.padding(top = 11.dp).size(24.dp).graphicsLayer { scaleX = pop; scaleY = pop; translationY = lift * density }) {
                            Icon(icon, t.label, tint = tint, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            t.label, fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = text, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
