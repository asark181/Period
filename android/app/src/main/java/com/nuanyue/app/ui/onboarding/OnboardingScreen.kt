package com.nuanyue.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.ui.components.GradientButton
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.theme.Palette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun OnboardingScreen(onFinish: (nickname: String, lastStart: LocalDate?, periodLen: Int, cycleLen: Int) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var nickname by rememberSaveable { mutableStateOf("") }
    var periodLen by rememberSaveable { mutableIntStateOf(5) }
    var cycleLen by rememberSaveable { mutableIntStateOf(28) }
    val today = LocalDate.now()
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = null,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() &&
                    utcTimeMillis >= today.minusDays(90).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        },
    )
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun finish() {
        val start = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        onFinish(nickname.trim(), start, periodLen, cycleLen)
    }

    Box(Modifier.fillMaxSize().background(Palette.backgroundGradient)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(Modifier.height(5.dp).width(if (i == step) 28.dp else 12.dp).clip(CircleShape).background(if (i <= step) Palette.Coral else Palette.Line))
                }
            }
            Box(Modifier.weight(1f)) {
                AnimatedContent(step, transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                }, label = "step") { s ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (s) {
                            0 -> Welcome()
                            1 -> NameStep(nickname) { nickname = it }
                            2 -> {
                                StepTitle("🩸", "上次大姨妈是哪天来的？", "不记得也没关系，可以跳过，之后在首页记录")
                                SoftCard {
                                    DatePicker(
                                        dateState, title = null, headline = null, showModeToggle = false,
                                        colors = DatePickerDefaults.colors(
                                            containerColor = Color.White,
                                            selectedDayContainerColor = Palette.Coral,
                                            todayDateBorderColor = Palette.Coral,
                                            todayContentColor = Palette.CoralDeep,
                                        ),
                                    )
                                }
                            }
                            else -> {
                                StepTitle("⏳", "你的周期大概多长？", "大概就好，记录越多，暖月会算得越准")
                                NumberPicker("经期一般持续", periodLen, 2..10) { periodLen = it }
                                Spacer(Modifier.height(16.dp))
                                NumberPicker("两次月经间隔", cycleLen, 20..45) { cycleLen = it }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    Text("上一步", color = Palette.InkSoft, modifier = Modifier.clip(CircleShape).clickable { step-- }.padding(12.dp))
                }
                Spacer(Modifier.weight(1f))
                if (step == 2 && dateState.selectedDateMillis == null) {
                    Text("跳过", color = Palette.InkSoft, modifier = Modifier.clip(CircleShape).clickable { step++ }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                }
                GradientButton(
                    when (step) { 0 -> "开始吧"; 3 -> "开启暖月"; else -> "下一步" },
                    onClick = { if (step < 3) step++ else finish() },
                    enabled = step != 2 || dateState.selectedDateMillis != null,
                )
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(Palette.heroGradient), contentAlignment = Alignment.Center) {
            Text("🌙", fontSize = 72.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text("你好呀，欢迎来到暖月", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "记录经期 · 预测周期 · 贴心提醒\n每个月的那几天，都有人温柔地陪着你",
            style = MaterialTheme.typography.bodyLarge, color = Palette.InkSoft, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Feature("📅", "智能预测")
            Feature("🔔", "提前提醒")
            Feature("📊", "周期分析")
            Feature("🔒", "加密同步")
        }
    }
}

@Composable
private fun Feature(emoji: String, label: String) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Palette.InkSoft)
    }
}

@Composable
private fun StepTitle(emoji: String, title: String, sub: String) {
    Spacer(Modifier.height(36.dp))
    Text(emoji, fontSize = 40.sp)
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(6.dp))
    Text(sub, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NameStep(value: String, onChange: (String) -> Unit) {
    StepTitle("🌸", "我该怎么称呼你呢？", "暖月会用这个名字和你打招呼")
    OutlinedTextField(
        value, { onChange(it.take(10)) },
        placeholder = { Text("宝贝", color = Palette.InkFaint) },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        textStyle = MaterialTheme.typography.titleLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Coral, unfocusedBorderColor = Palette.Line,
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, cursorColor = Palette.Coral,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("宝贝", "小可爱", "公主", "小仙女").forEach { n ->
            Text(
                n, color = Palette.CoralDeep, fontSize = 13.sp,
                modifier = Modifier.clip(CircleShape).background(Palette.RoseSoft).clickable { onChange(n) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun NumberPicker(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    SoftCard {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.InkSoft)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RoundBtn("−", value > range.first) { onChange(value - 1) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text("$value", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Palette.CoralDeep)
                Text(" 天", fontSize = 16.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 10.dp))
            }
            RoundBtn("+", value < range.last) { onChange(value + 1) }
        }
    }
}

@Composable
private fun RoundBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(if (enabled) Palette.RoseSoft else Palette.Line).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 24.sp, color = if (enabled) Palette.CoralDeep else Palette.InkFaint, fontWeight = FontWeight.Bold) }
}
