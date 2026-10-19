package com.nuanyue.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuanyue.app.R
import com.nuanyue.app.data.PhraseEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayInfo
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.DateChip
import com.nuanyue.app.ui.components.FloatingSticker
import com.nuanyue.app.ui.components.LegendDot
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.MoonMood
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.PeriodButton
import com.nuanyue.app.ui.components.PrimaryButton
import com.nuanyue.app.ui.components.SlidingSegmented
import com.nuanyue.app.ui.components.Sticker
import com.nuanyue.app.ui.components.pressScale
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.Rounded
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val MONTHS_BACK = 120
private const val MONTHS_FORWARD = 24
private const val MODE_MONTH = 0
private const val MODE_YEAR = 1
private const val MODE_RING = 2

private fun pageOf(base: YearMonth, target: YearMonth) =
    (MONTHS_BACK + ChronoUnit.MONTHS.between(base, target).toInt()).coerceIn(0, MONTHS_BACK + MONTHS_FORWARD)

/** What the confirm sheet is asking about. */
private data class Pending(val date: LocalDate, val start: Boolean, val undo: Boolean)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: UiState, vm: AppViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(MODE_MONTH) }
    var selectedEpoch by rememberSaveable { mutableStateOf(state.today.toEpochDay()) }
    var picker by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val today = state.today
    val base = YearMonth.from(today)
    val pager = rememberPagerState(initialPage = MONTHS_BACK) { MONTHS_BACK + MONTHS_FORWARD + 1 }
    val scope = rememberCoroutineScope()
    val from = base.minusMonths(MONTHS_BACK.toLong()).atDay(1).minusDays(7)
    val to = base.plusMonths(MONTHS_FORWARD.toLong()).atEndOfMonth().plusDays(60)
    val infos = remember(state.snapshot, today) { CycleEngine.dayInfos(state.snapshot, from, to, today) }
    var yearShown by rememberSaveable { mutableIntStateOf(today.year) }

    fun jumpTo(ym: YearMonth, select: LocalDate? = null) {
        mode = MODE_MONTH
        scope.launch { pager.animateScrollToPage(pageOf(base, ym)) }
        select?.let { selectedEpoch = it.toEpochDay() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.background(Color.White)) {
            // planner doodles in the header
            FloatingSticker(R.drawable.st_star, Modifier.statusBarsPadding().padding(start = 186.dp, top = 22.dp).size(24.dp), rotation = -12f)
            FloatingSticker(R.drawable.st_sparkle, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 92.dp, top = 12.dp).size(18.dp), periodMs = 1900)
            Column(Modifier.statusBarsPadding().padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(10.dp))
                TopBar(state, mode) { mode = it }
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    mode,
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                            (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 1.04f))
                    },
                    label = "view",
                ) { m ->
                    when (m) {
                        MODE_YEAR -> YearView(state, infos, yearShown, { yearShown = it }) { ym -> jumpTo(ym) }
                        MODE_RING -> CycleRingView(state, infos, selected) { selectedEpoch = it.toEpochDay() }
                        else -> MonthView(state, infos, pager, selected, onPick = { picker = true }) { selectedEpoch = it.toEpochDay() }
                    }
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                    LegendDot(Palette.Pink, "经期")
                    LegendDot(Palette.PinkLine, "预测经期", hollow = true)
                    LegendDot(Palette.Orange, "易孕期")
                    LegendDot(Palette.Orange, "排卵日", heart = true)
                }
                Spacer(Modifier.height(22.dp))
            }
        }
        BottomPanel(state, vm, selected, infos[selected], onToday = { jumpTo(base, today) }) { pending = it }
    }

    if (picker) {
        MonthPickerSheet(
            current = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong()),
            min = base.minusMonths(MONTHS_BACK.toLong()),
            max = base.plusMonths(MONTHS_FORWARD.toLong()),
            onDismiss = { picker = false },
        ) { ym -> picker = false; jumpTo(ym) }
    }
    pending?.let { p ->
        ConfirmSheet(p, onDismiss = { pending = null }) {
            pending = null
            when {
                p.start && p.undo -> vm.unmarkStart(p.date)
                p.start -> vm.markStart(p.date)
                p.undo -> vm.unmarkEnd(p.date)
                else -> vm.markEnd(p.date)
            }
        }
    }
}

// ---------------------------------------------------------------- top

private fun moodFor(state: UiState): MoonMood {
    val h = LocalTime.now().hour
    return when {
        state.snapshot.today.phase == Phase.MENSTRUAL -> MoonMood.PERIOD
        h >= 23 || h < 6 -> MoonMood.SLEEPY
        else -> MoonMood.HAPPY
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopBar(state: UiState, mode: Int, onMode: (Int) -> Unit) {
    val t = state.snapshot.today
    Row(verticalAlignment = Alignment.CenterVertically) {
        SlidingSegmented(
            listOf(Icons.Rounded.CalendarMonth, Icons.Rounded.CalendarViewMonth, Icons.Rounded.DonutLarge),
            mode, onMode, descriptions = listOf("月历", "整年", "周期环"),
        )
        Spacer(Modifier.weight(1f))
        Mascot(Modifier.size(64.dp), moodFor(state))
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val name = state.settings.nickname.takeIf { it.isNotBlank() && !state.settings.isPartner }
            Text(Copy.greeting() + (name?.let { "，$it" } ?: ""), style = MaterialTheme.typography.headlineMedium)
            Text(if (state.settings.isPartner) "正在陪她一起看着日子" else Copy.mood(t.phase), style = MaterialTheme.typography.bodySmall)
        }
        StatusPill(state)
    }
}

@Composable
private fun StatusPill(state: UiState) {
    val snap = state.snapshot
    val t = snap.today
    val (prefix, num, suffix) = when {
        !snap.hasData -> Triple("还没有记录", "", "")
        t.phase == Phase.MENSTRUAL -> Triple("经期第", "${t.periodDay}", "天")
        t.lateDays > 0 -> Triple("已推迟", "${t.lateDays}", "天")
        t.daysUntilNext == 0 -> Triple("可能今天来", "", "")
        else -> Triple("还有", "${t.daysUntilNext}", "天")
    }
    Row(
        Modifier.clip(CircleShape).background(Palette.PinkSoft).border(1.dp, Palette.PinkLine, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prefix, color = Palette.PinkDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (num.isNotEmpty()) {
            // the number rolls when it changes
            AnimatedContent(num, transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() }, label = "num") { n ->
                Numeral(" $n ", 17.sp, Palette.PinkDeep, weight = FontWeight.ExtraBold)
            }
            Text(suffix, color = Palette.PinkDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------- month view

@Composable
private fun MonthView(
    state: UiState,
    infos: Map<LocalDate, DayInfo>,
    pager: PagerState,
    selected: LocalDate,
    onPick: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val base = YearMonth.from(state.today)
    val scope = rememberCoroutineScope()
    val month = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong())
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.94f)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(14.dp))
                    .clickable(src, indication = null, onClick = onPick).padding(end = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                AnimatedContent(month, transitionSpec = { monthSlide(targetState > initialState) }, label = "title") { m ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Numeral("${m.year}", 16.sp, Palette.InkSoft, Modifier.padding(bottom = 5.dp, end = 6.dp))
                        Numeral("${m.monthValue}", 32.sp)
                        Text(" 月", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Icon(Icons.Rounded.KeyboardArrowDown, "选择年月", tint = Palette.InkSoft, modifier = Modifier.padding(bottom = 5.dp))
            }
            Spacer(Modifier.weight(1f))
            if (pager.currentPage != MONTHS_BACK) {
                Text(
                    "回今天", fontSize = 12.sp, color = Palette.Pink,
                    modifier = Modifier.clip(CircleShape).background(Palette.PinkSoft).clickable {
                        scope.launch { pager.animateScrollToPage(MONTHS_BACK) }
                        onSelect(state.today)
                    }.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
            ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, color = if (i >= 5) Palette.Pink else Palette.Sky)
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalPager(pager, verticalAlignment = Alignment.Top, beyondViewportPageCount = 1) { page ->
            val ym = base.plusMonths((page - MONTHS_BACK).toLong())
            MonthGrid(ym, infos, state.today, selected, onSelect)
        }
    }
}

private fun monthSlide(forward: Boolean): ContentTransform =
    (slideInVertically { if (forward) it / 2 else -it / 2 } + fadeIn()) togetherWith (slideOutVertically { if (forward) -it / 2 else it / 2 } + fadeOut())

@Composable
private fun ArrowBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.8f)
    Box(
        Modifier.size(36.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).clickable(src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Palette.InkSoft) }
}

@Composable
private fun MonthGrid(ym: YearMonth, infos: Map<LocalDate, DayInfo>, today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val rows = (lead + ym.lengthOfMonth() + 6) / 7
    Column {
        for (r in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (r < rows && idx in 0 until ym.lengthOfMonth()) {
                            val d = first.plusDays(idx.toLong())
                            DateChip("${d.dayOfMonth}", infos[d]?.type ?: DayType.UNKNOWN, d == today, d == selected, 40.dp, muted = d.isAfter(today)) { onSelect(d) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- year view

@Composable
private fun YearView(state: UiState, infos: Map<LocalDate, DayInfo>, year: Int, onYear: (Int) -> Unit, onMonth: (YearMonth) -> Unit) {
    val today = state.today
    val minYear = today.minusMonths(MONTHS_BACK.toLong()).year
    val maxYear = today.plusMonths(MONTHS_FORWARD.toLong()).year
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(CircleShape).background(Palette.Bg).padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { if (year > minYear) onYear(year - 1) }
                AnimatedContent(year, transitionSpec = { monthSlide(targetState > initialState) }, label = "year") { y ->
                    Numeral("$y", 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
                }
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight) { if (year < maxYear) onYear(year + 1) }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, null, tint = Palette.InkFaint, modifier = Modifier.size(14.dp))
                Text(" 点月份看详情", fontSize = 12.sp, color = Palette.InkFaint)
            }
        }
        Spacer(Modifier.height(12.dp))
        AnimatedContent(year, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "yearGrid") { y ->
            Column {
                for (row in 0 until 4) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (col in 0 until 3) {
                            val ym = YearMonth.of(y, row * 3 + col + 1)
                            MiniMonth(ym, infos, today, Modifier.weight(1f)) { onMonth(ym) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(ym: YearMonth, infos: Map<LocalDate, DayInfo>, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.93f)
    val isNow = ym == YearMonth.from(today)
    Column(
        modifier.graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(14.dp))
            .background(if (isNow) Palette.PinkSoft.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(src, indication = null, onClick = onClick).padding(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral("${ym.monthValue}", 18.sp, if (isNow) Palette.PinkDeep else Palette.Ink)
            Text(" 月", fontSize = 11.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 2.dp))
        }
        Spacer(Modifier.height(4.dp))
        val first = ym.atDay(1)
        val lead = first.dayOfWeek.value - 1
        val rows = (lead + ym.lengthOfMonth() + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (idx in 0 until ym.lengthOfMonth()) {
                            val d = first.plusDays(idx.toLong())
                            val type = infos[d]?.type ?: DayType.UNKNOWN
                            val bg = when (type) {
                                DayType.PERIOD -> Palette.Pink
                                DayType.FERTILE, DayType.OVULATION -> Palette.Orange
                                else -> Color.Transparent
                            }
                            Box(
                                Modifier.fillMaxSize().padding(0.5.dp).clip(CircleShape).background(bg)
                                    .then(
                                        when {
                                            d == today -> Modifier.border(1.2.dp, Palette.PinkDeep, CircleShape)
                                            type == DayType.PREDICTED_PERIOD -> Modifier.border(1.dp, Palette.PinkLine, CircleShape)
                                            else -> Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${d.dayOfMonth}", fontFamily = Rounded, fontSize = 8.sp, lineHeight = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (bg != Color.Transparent) Color.White else Palette.Ink.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthPickerSheet(current: YearMonth, min: YearMonth, max: YearMonth, onDismiss: () -> Unit, onPick: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(current.year) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 22.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("跳到", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { if (year > min.year) year-- }
                AnimatedContent(year, transitionSpec = { monthSlide(targetState > initialState) }, label = "pickYear") { y ->
                    Numeral("$y", 24.sp, modifier = Modifier.padding(horizontal = 10.dp))
                }
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight) { if (year < max.year) year++ }
            }
            Spacer(Modifier.height(16.dp))
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 4) {
                        val ym = YearMonth.of(year, row * 4 + col + 1)
                        val enabled = !ym.isBefore(min) && !ym.isAfter(max)
                        val sel = ym == current
                        val src = remember(ym) { MutableInteractionSource() }
                        val s = pressScale(src, 0.88f)
                        Box(
                            Modifier.weight(1f).height(48.dp).graphicsLayer { scaleX = s; scaleY = s }
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel) Palette.Pink else Palette.Bg)
                                .clickable(src, indication = null, enabled = enabled) { onPick(ym) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${ym.monthValue}月", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = when { sel -> Color.White; enabled -> Palette.Ink; else -> Palette.InkFaint },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(6.dp))
            PrimaryButton("回到这个月", { onPick(YearMonth.now()) }, Modifier.fillMaxWidth(), container = Palette.PinkSoft, content = Palette.PinkDeep)
            Spacer(Modifier.height(18.dp))
        }
    }
}

// ---------------------------------------------------------------- cycle ring view

@Composable
private fun CycleRingView(state: UiState, infos: Map<LocalDate, DayInfo>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val snap = state.snapshot
    if (!snap.hasData) {
        Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Modifier.size(110.dp), MoonMood.HAPPY)
                Spacer(Modifier.height(10.dp))
                Text("记录第一次经期后，这里会画出你的周期", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }
    var which by remember { mutableIntStateOf(1) }
    val today = state.today
    val starts = snap.periods.map { it.start }.filter { !it.isAfter(today) }
    val curStart = starts.last()
    val curLen = maxOf(snap.avgCycle, CycleEngine.days(curStart, today) + 1)
    val (start, len) = when (which) {
        0 -> if (starts.size >= 2) starts[starts.size - 2] to CycleEngine.days(starts[starts.size - 2], curStart) else curStart to curLen
        2 -> curStart.plusDays(curLen.toLong()) to snap.avgCycle
        else -> curStart to curLen
    }
    val days = (0 until len.coerceIn(15, 60)).map { start.plusDays(it.toLong()) }
    val appear = remember(which) { Animatable(0f) }
    LaunchedEffect(which) { appear.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("上次", "本次", "下次").forEachIndexed { i, label ->
                val dot by animateFloatAsState(if (which == i) 1f else 0f, spring(dampingRatio = 0.4f), label = "dot")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { which = i }.padding(4.dp),
                ) {
                    Text(label, fontSize = 16.sp, fontWeight = if (which == i) FontWeight.Bold else FontWeight.Normal, color = if (which == i) Palette.Ink else Palette.InkFaint)
                    Box(Modifier.padding(top = 3.dp).size(5.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(Palette.Pink))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val w = maxWidth
            val chip = min(30f, (w.value * 0.42f * 2 * PI.toFloat() / days.size) * 0.86f).dp
            val radius = w / 2 - chip / 2 - 6.dp
            Box(Modifier.align(Alignment.Center).size(radius * 2 + chip + 6.dp).clip(CircleShape).background(Palette.Bg))
            Box(Modifier.align(Alignment.Center).size(radius * 2 - chip - 6.dp).clip(CircleShape).background(Color.White))
            days.forEachIndexed { i, d ->
                // dates fan out around the circle one after another
                val local = ((appear.value * days.size - i * 0.6f) / 3f).coerceIn(0f, 1f)
                val angle = -PI / 2 + 2 * PI * i / days.size
                Box(
                    Modifier.align(Alignment.Center)
                        .offset((radius.value * cos(angle)).dp, (radius.value * sin(angle)).dp)
                        .graphicsLayer { alpha = local; scaleX = 0.5f + 0.5f * local; scaleY = 0.5f + 0.5f * local },
                ) {
                    DateChip("${d.dayOfMonth}", infos[d]?.type ?: DayType.UNKNOWN, d == today, d == selected, chip, muted = d.isAfter(today)) { onSelect(d) }
                }
                if (d.dayOfMonth == 1 || i == 0) {
                    val lr = radius - chip
                    Text(
                        "${d.monthValue}月", fontSize = 11.sp, color = Palette.InkSoft,
                        modifier = Modifier.align(Alignment.Center).offset((lr.value * cos(angle)).dp, (lr.value * sin(angle)).dp),
                    )
                }
            }
            val info = infos[selected]
            val centre = radius * 2 - chip * 2 - 40.dp
            Column(
                Modifier.align(Alignment.Center).size(centre).clip(CircleShape).background(Palette.SkySoft),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Sticker(R.drawable.st_moon, Modifier.size(28.dp), rotation = -10f)
                Text(phaseLabel(info?.type), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("第", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    AnimatedContent(info?.cycleDay ?: 0, transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() }, label = "cd") { n ->
                        Numeral(if (n == 0) "-" else "$n", 40.sp)
                    }
                    Text("天", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    "%d-%02d-%02d".format(selected.year, selected.monthValue, selected.dayOfMonth),
                    fontFamily = Rounded, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Palette.Sky,
                )
            }
        }
    }
}

private fun phaseLabel(t: DayType?) = when (t) {
    DayType.PERIOD -> "经期"
    DayType.PREDICTED_PERIOD -> "预测经期"
    DayType.FERTILE -> "易孕期"
    DayType.OVULATION -> "排卵日"
    DayType.SAFE -> "安全期"
    else -> "周期"
}

// ---------------------------------------------------------------- bottom panel

@Composable
private fun BottomPanel(
    state: UiState,
    vm: AppViewModel,
    date: LocalDate,
    info: DayInfo?,
    onToday: () -> Unit,
    onAsk: (Pending) -> Unit,
) {
    val snap = state.snapshot
    val partner = state.settings.isPartner
    val isFuture = date.isAfter(state.today)
    val startHere = snap.periods.any { it.start == date }
    val endHere = snap.periods.any { it.end == date }
    val canEnd = snap.periods.any { !it.start.isAfter(date) && CycleEngine.days(it.start, date) < CycleEngine.MAX_PERIOD_DAYS }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Palette.Panel)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(32.dp).height(4.dp).clip(CircleShape).background(Palette.InkFaint))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(date, transitionSpec = { monthSlide(targetState > initialState) }, label = "date") { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${d.monthValue}月${d.dayOfMonth}日 ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(8.dp))
            info?.type?.takeIf { it != DayType.UNKNOWN }?.let {
                Text(phaseLabel(it) + (info.cycleDay?.let { d -> " · 第${d}天" } ?: ""), fontSize = 12.sp, color = Palette.InkSoft)
            }
            Spacer(Modifier.weight(1f))
            if (date != state.today) {
                Text("今天", fontSize = 12.sp, color = Palette.Sky, modifier = Modifier.clip(CircleShape).clickable(onClick = onToday).padding(6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (partner) {
            Text("这是她的记录，只能查看哦", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PeriodButton(Icons.Rounded.PlayArrow, "月经开始", startHere, Palette.Pink) {
                    if (isFuture) vm.toast("这一天还没到哦") else onAsk(Pending(date, start = true, undo = startHere))
                }
                PeriodButton(Icons.Rounded.Stop, "月经结束", endHere, Palette.Pink) {
                    when {
                        isFuture -> vm.toast("这一天还没到哦")
                        endHere -> onAsk(Pending(date, start = false, undo = true))
                        canEnd -> onAsk(Pending(date, start = false, undo = false))
                        else -> vm.toast("先标记月经开始，再标记结束哦")
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        PhraseCard(vm)
        Spacer(Modifier.height(24.dp))
    }
}

/** A planner-style note with a washi tape; tap it and it flips over to a new line. */
@Composable
private fun PhraseCard(vm: AppViewModel) {
    val phrase by vm.phrase.collectAsStateWithLifecycle()
    var shown by remember { mutableStateOf<PhraseEntity?>(null) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val press = pressScale(src, 0.96f)
    LaunchedEffect(phrase) {
        if (phrase == null || phrase?.id == shown?.id) return@LaunchedEffect
        if (shown == null) { shown = phrase; return@LaunchedEffect }
        rotation.animateTo(90f, tween(170, easing = FastOutSlowInEasing))
        shown = phrase
        rotation.snapTo(-90f)
        rotation.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation.value
                    cameraDistance = 14f * density
                    scaleX = press; scaleY = press
                }
                .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x22404A70))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable(src, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { vm.nextPhrase() }
                }
                .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 16.dp),
        ) {
            Text(shown?.text ?: "小月正在想今天要对你说什么……", style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sticker(R.drawable.st_heart, Modifier.size(14.dp))
                Text(" 小月的悄悄话 · 点一下换一句", fontSize = 11.sp, color = Palette.InkFaint)
            }
        }
        Sticker(R.drawable.st_washi_pink, Modifier.align(Alignment.TopCenter).offset(y = (-10).dp).width(96.dp).height(22.dp), rotation = -3f)
        Sticker(R.drawable.st_flower, Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 8.dp).size(30.dp), rotation = 12f)
    }
}

@Composable
private fun ConfirmSheet(p: Pending, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val md = "${p.date.monthValue}月${p.date.dayOfMonth}日"
    val (title, body, sticker) = when {
        p.start && p.undo -> Triple("取消 $md 的「月经开始」？", "这次经期的记录会被删掉，预测也会重新计算。", R.drawable.st_cloud)
        p.start -> Triple("$md 大姨妈来了吗？", "确认后会把这天记为本次经期第一天，之后的预测会跟着更新。", R.drawable.st_drop)
        p.undo -> Triple("取消 $md 的「月经结束」？", "会改回经期还没结束的状态。", R.drawable.st_cloud)
        else -> Triple("$md 大姨妈走了吗？", "确认后这天会记为本次经期最后一天。辛苦啦，这几天好好休息过了吗？", R.drawable.st_bottle)
    }
    val wobble = remember { Animatable(-14f) }
    LaunchedEffect(Unit) { wobble.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow)) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Sticker(sticker, Modifier.size(72.dp).graphicsLayer { rotationZ = wobble.value })
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("再想想", { scope.launch { sheet.hide(); onDismiss() } }, Modifier.weight(1f), container = Palette.Panel, content = Palette.InkSoft)
                PrimaryButton(if (p.undo) "确认取消" else "确认", { scope.launch { sheet.hide(); onConfirm() } }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
