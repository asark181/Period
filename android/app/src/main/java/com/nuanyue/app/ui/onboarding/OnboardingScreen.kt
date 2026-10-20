package com.nuanyue.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.R
import com.nuanyue.app.ui.components.FloatingSticker
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.PrimaryButton
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.Rounded
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val INVITE = -1

@Composable
fun OnboardingScreen(
    syncing: Boolean,
    onFinish: (nickname: String, lastStart: LocalDate?, periodLen: Int, cycleLen: Int) -> Unit,
    onInvite: (code: String, asPartner: Boolean, onError: (String) -> Unit) -> Unit,
) {
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
    fun askNotify() { if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }

    fun finish() {
        val start = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        askNotify()
        onFinish(nickname.trim(), start, periodLen, cycleLen)
    }

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(18.dp))
            if (step > 0) StepDots(step)
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    step,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(340)) { it / 5 * dir } + fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(240)) { -it / 6 * dir } + fadeOut(tween(160)))
                    },
                    label = "step",
                ) { s ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (s) {
                            0 -> Welcome()
                            INVITE -> InviteStep(syncing) { code, partner, onError -> askNotify(); onInvite(code, partner, onError) }
                            1 -> NameStep(nickname) { nickname = it }
                            2 -> {
                                StepTitle("上一次大姨妈是哪天来的？", "记不清也没关系，可以跳过，下次来的那天再点就好。")
                                Box(Modifier.clip(RoundedCornerShape(22.dp)).background(Color.White)) {
                                    DatePicker(
                                        dateState, title = null, headline = null, showModeToggle = false,
                                        colors = DatePickerDefaults.colors(
                                            containerColor = Color.White,
                                            selectedDayContainerColor = Palette.Pink,
                                            todayDateBorderColor = Palette.Pink,
                                            todayContentColor = Palette.Pink,
                                        ),
                                    )
                                }
                            }
                            else -> {
                                StepTitle("一般多久来一次呀？", "大概就行，记录多了小月会自己算准。")
                                NumberPicker("每次持续", periodLen, 2..10) { periodLen = it }
                                Spacer(Modifier.height(12.dp))
                                NumberPicker("两次之间隔", cycleLen, 20..45) { cycleLen = it }
                            }
                        }
                    }
                }
            }
            if (step != INVITE) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        step == 0 -> Text(
                            "我有邀请码", color = Palette.Pink, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(CircleShape).clickable { step = INVITE }.padding(12.dp),
                        )
                        else -> Text("上一步", color = Palette.InkSoft, modifier = Modifier.clip(CircleShape).clickable { step-- }.padding(12.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    if (step == 2 && dateState.selectedDateMillis == null) {
                        Text("跳过", color = Palette.InkSoft, modifier = Modifier.clip(CircleShape).clickable { step++ }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    PrimaryButton(
                        when (step) { 0 -> "开始使用"; 3 -> "完成"; else -> "下一步" },
                        onClick = { if (step < 3) step++ else finish() },
                        enabled = step != 2 || dateState.selectedDateMillis != null,
                    )
                }
            } else {
                Text(
                    "返回", color = Palette.InkSoft,
                    modifier = Modifier.padding(vertical = 16.dp).clip(CircleShape).clickable { step = 0 }.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..3).forEach { i ->
            val w by animateDpAsState(if (i == step) 22.dp else 6.dp, spring(dampingRatio = 0.7f), label = "dot")
            Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (i <= step) Palette.Pink else Palette.Line))
        }
    }
}

@Composable
private fun Welcome() {
    val pop = remember { Animatable(0.86f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Square art with wide empty margins. A 16:10 crop keeps 小月 and the notebook
        // and leaves room for the title underneath.
        Box(contentAlignment = Alignment.Center) {
            FloatingSticker(R.drawable.st_cloud, Modifier.offset(x = (-128).dp, y = (-92).dp).size(34.dp), rotation = -8f, periodMs = 2800)
            Image(
                painter = painterResource(R.drawable.illu_welcome),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 380.dp)
                    .aspectRatio(1.6f)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("你好呀，我是小月", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "帮你记下每次月经开始和结束，\n算出下一次大概哪天来，\n快到的时候提醒你。",
            style = MaterialTheme.typography.bodyLarge, color = Palette.InkSoft, textAlign = TextAlign.Center, lineHeight = 26.sp,
        )
    }
}

@Composable
private fun StepTitle(title: String, sub: String) {
    Spacer(Modifier.height(32.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(sub, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NameStep(value: String, onChange: (String) -> Unit) {
    StepTitle("小月该怎么叫你呢？", "打招呼的时候会用到，不想填也可以空着。")
    OutlinedTextField(
        value, { onChange(it.take(10)) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        textStyle = MaterialTheme.typography.titleLarge,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InviteStep(syncing: Boolean, onSubmit: (String, Boolean, (String) -> Unit) -> Unit) {
    var code by remember { mutableStateOf("") }
    var partner by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(40.dp))
        Mascot(Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text("输入邀请码", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("以前的记录会一起接过来，不用重新记啦", style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            code, { code = it.take(20); error = null },
            singleLine = true,
            placeholder = { Text("NY-XXXX-XXXX-XXXX", color = Palette.InkFaint, fontFamily = Rounded) },
            textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = Rounded, textAlign = TextAlign.Center, letterSpacing = 1.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("我是本人", !partner, { partner = false })
            Pill("我是伴侣（只看）", partner, { partner = true }, color = Palette.Sky)
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = Palette.PinkDeep, fontSize = 13.sp)
        }
        Spacer(Modifier.height(28.dp))
        if (syncing) {
            CircularProgressIndicator(color = Palette.Pink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        } else {
            PrimaryButton("连接", onClick = { onSubmit(code, partner) { error = it } }, modifier = Modifier.fillMaxWidth(), enabled = code.isNotBlank())
        }
    }
}

@Composable
private fun NumberPicker(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x1AE59AAB), spotColor = Color(0x22E59AAB))
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RoundBtn("−", value > range.first) { onChange(value - 1) }
        Row(Modifier.width(84.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Numeral("$value", 34.sp, Palette.Pink)
            Text(" 天", fontSize = 13.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 6.dp))
        }
        RoundBtn("+", value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun RoundBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(if (enabled) Palette.PinkSoft else Palette.Panel)
            .border(0.dp, Color.Transparent, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 20.sp, color = if (enabled) Palette.Pink else Palette.InkFaint) }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Pink, unfocusedBorderColor = Palette.Line,
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, cursorColor = Palette.Pink,
)
