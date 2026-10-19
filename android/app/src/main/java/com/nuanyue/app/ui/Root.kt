package com.nuanyue.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.ui.home.HomeScreen
import com.nuanyue.app.ui.onboarding.OnboardingScreen
import com.nuanyue.app.ui.record.RecordSheet
import com.nuanyue.app.ui.settings.SettingsScreen
import com.nuanyue.app.ui.stats.StatsScreen
import com.nuanyue.app.ui.theme.Palette
import java.time.LocalDate

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("暖月", Icons.Rounded.Brightness3),
    STATS("统计", Icons.Rounded.BarChart),
    ME("我", Icons.Rounded.PersonOutline),
}

@Composable
fun NuanYueRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var editing by remember { mutableStateOf<LocalDate?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    if (!state.loaded) {
        Box(Modifier.fillMaxSize().background(Palette.Bg))
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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BottomBar(tab) { tab = it } },
        modifier = Modifier.background(Palette.Bg),
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                when (t) {
                    Tab.HOME -> HomeScreen(state, vm, onEdit = { editing = it })
                    Tab.STATS -> StatsScreen(state)
                    Tab.ME -> SettingsScreen(state, vm)
                }
            }
        }
    }

    editing?.let { day ->
        RecordSheet(
            date = day,
            initial = state.records[day] ?: DayRecordEntity(day = day.toEpochDay()),
            readOnly = state.settings.isPartner,
            onDismiss = { editing = null },
            onSave = { vm.saveRecord(it); editing = null },
        )
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val selected = t == current
                Column(
                    Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t) }
                        .padding(horizontal = 22.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (selected) Palette.PinkSoft else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(t.icon, t.label, tint = if (selected) Palette.Pink else Palette.InkFaint, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        t.label, fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Palette.Ink else Palette.InkFaint,
                    )
                }
            }
        }
    }
}
