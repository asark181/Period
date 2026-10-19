package com.nuanyue.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.ui.components.BrandMark
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.PrimaryButton
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

    Box(Modifier.fillMaxSize().background(Palette.Paper)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(18.dp))
            if (step > 0) Text("$step / 3", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.weight(1f)) {
                AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { s ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (s) {
                            0 -> Welcome()
                            1 -> NameStep(nickname) { nickname = it }
                            2 -> {
                                StepTitle("上一次月经是哪天开始的？", "记不清可以跳过，下次来的那天再点。")
                                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Palette.Card).border(1.dp, Palette.Line, RoundedCornerShape(18.dp))) {
                                    DatePicker(
                                        dateState, title = null, headline = null, showModeToggle = false,
                                        colors = DatePickerDefaults.colors(
                                            containerColor = Palette.Card,
                                            selectedDayContainerColor = Palette.Rouge,
                                            todayDateBorderColor = Palette.Ink,
                                            todayContentColor = Palette.Ink,
                                        ),
                                    )
                                }
                            }
                            else -> {
                                StepTitle("一般多久来一次？", "大概就行，记录多了会自动修正。")
                                NumberPicker("每次持续", periodLen, 2..10) { periodLen = it }
                                Spacer(Modifier.height(12.dp))
                                NumberPicker("两次之间隔", cycleLen, 20..45) { cycleLen = it }
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
                PrimaryButton(
                    when (step) { 0 -> "开始"; 3 -> "完成"; else -> "下一步" },
                    onClick = { if (step < 3) step++ else finish() },
                    enabled = step != 2 || dateState.selectedDateMillis != null,
                )
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxWidth().padding(top = 96.dp)) {
        BrandMark(Modifier.size(72.dp))
        Spacer(Modifier.height(36.dp))
        Text("暖月", fontSize = 44.sp, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(14.dp))
        Text("记下每次经期，\n算出下一次什么时候来，\n快到的时候提醒你。", style = MaterialTheme.typography.bodyLarge, color = Palette.InkSoft, lineHeight = 26.sp)
        Spacer(Modifier.height(40.dp))
        Text("设置只要三步。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StepTitle(title: String, sub: String) {
    Spacer(Modifier.height(40.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(sub, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun NameStep(value: String, onChange: (String) -> Unit) {
    StepTitle("怎么称呼你？", "首页问候时会用到，可以留空。")
    OutlinedTextField(
        value, { onChange(it.take(10)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.titleLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Ink, unfocusedBorderColor = Palette.Line,
            focusedContainerColor = Palette.Card, unfocusedContainerColor = Palette.Card, cursorColor = Palette.Ink,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberPicker(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Palette.Card)
            .border(1.dp, Palette.Line, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RoundBtn("−", value > range.first) { onChange(value - 1) }
        Row(Modifier.width(84.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Numeral("$value", 40.sp)
            Text(" 天", fontSize = 13.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 6.dp))
        }
        RoundBtn("+", value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun RoundBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).border(1.dp, Palette.Line, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 20.sp, color = if (enabled) Palette.Ink else Palette.InkFaint) }
}
