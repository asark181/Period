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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
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

private val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

@Composable
internal fun BottomBar(current: Tab, slide: Float? = null, onSelect: (Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val motion = LocalMotion.current && slide == null
    val target = slide ?: current.ordinal.toFloat()
    val animated by animateFloatAsState(
        target,
        if (motion) spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow) else tween(0),
        label = "blob",
    )
    val index = if (slide != null) slide else animated
    val stretch = sin((index - floor(index)) * PI).toFloat().let { abs(it) }
    Column(
        Modifier.fillMaxWidth()
            .shadow(16.dp, barShape, clip = false, ambientColor = Color(0x14C47D92), spotColor = Color(0x22E7A3B5))
            .clip(barShape)
            .background(Color.White),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 68.dp)) {
            val count = Tab.entries.size
            val slot = maxWidth / count
            val blobW = 72.dp + 34.dp * stretch
            val x = slot * index + (slot - blobW) / 2
            Box(
                Modifier.offset(x = x, y = 8.dp)
                    .width(blobW)
                    .height(40.dp)
                    .shadow(8.dp, CircleShape, clip = false, ambientColor = Color(0x22E7A3B5), spotColor = Color(0x33F6D5DE))
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Palette.RoseFill, Palette.Lavender))),
            )
            Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                Tab.entries.forEachIndexed { i, t ->
                    val influence = (1f - abs(index - i)).coerceIn(0f, 1f)
                    val tint by animateColorAsState(if (influence > 0.5f) Palette.RoseInk else Palette.InkFaint, label = "tint")
                    val icon = if (influence > 0.6f && t == Tab.ME) Icons.Rounded.Person else t.icon
                    Column(
                        Modifier.weight(1f).padding(top = 6.dp, bottom = 6.dp)
                            .clickable(remember { MutableInteractionSource() }, indication = null) {
                                if (t != current) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(t)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(26.dp).graphicsLayer {
                                val squash = influence
                                scaleX = 1f + 0.18f * squash
                                scaleY = 1f - 0.08f * stretch * (if (squash > 0.4f) 1f else 0f) + 0.16f * squash
                                translationY = -6f * squash * density
                            },
                        ) {
                            Icon(icon, t.label, tint = tint, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            t.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (influence > 0.5f) FontWeight.Bold else FontWeight.Medium,
                            color = Palette.RoseInk.copy(alpha = 0.35f + 0.65f * influence),
                            modifier = Modifier.padding(top = 2.dp).graphicsLayer { alpha = 0.35f + 0.65f * influence },
                        )
                    }
                }
            }
        }
    }
}
