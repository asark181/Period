package com.nuanyue.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayInfo
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.ActionTile
import com.nuanyue.app.ui.components.DateChip
import com.nuanyue.app.ui.components.LegendDot
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.MoonMood
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.TagChip
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.Rounded
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val MONTHS_BACK = 40
private const val MONTHS_FORWARD = 12

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: UiState, vm: AppViewModel, onEdit: (LocalDate) -> Unit) {
    var ringMode by rememberSaveable { mutableStateOf(false) }
    var selectedEpoch by rememberSaveable { mutableStateOf(state.today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val today = state.today
    val base = YearMonth.from(today)
    val from = base.minusMonths(MONTHS_BACK.toLong()).atDay(1).minusDays(7)
    val to = base.plusMonths(MONTHS_FORWARD.toLong()).atEndOfMonth().plusDays(60)
    val infos = remember(state.snapshot, today) { CycleEngine.dayInfos(state.snapshot, from, to, today) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.background(Color.White).statusBarsPadding().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            TopBar(state, ringMode) { ringMode = it }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(ringMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "view") { ring ->
                if (ring) CycleRingView(state, infos, selected) { selectedEpoch = it.toEpochDay() }
                else MonthView(state, infos, selected) { selectedEpoch = it.toEpochDay() }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            ) {
                LegendDot(Palette.Pink, "经期")
                LegendDot(Palette.PinkLine, "预测经期", hollow = true)
                LegendDot(Palette.Orange, "易孕期")
                LegendDot(Palette.Orange, "排卵日", heart = true)
            }
            Spacer(Modifier.height(22.dp))
        }
        BottomPanel(state, vm, selected, infos[selected], onEdit) { selectedEpoch = today.toEpochDay() }
    }
}

// ---------------------------------------------------------------- top

@Composable
private fun TopBar(state: UiState, ringMode: Boolean, onMode: (Boolean) -> Unit) {
    val t = state.snapshot.today
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.shadow(4.dp, CircleShape, spotColor = Color(0x22404A70)).clip(CircleShape).background(Color.White)
                .padding(5.dp),
        ) {
            ModeIcon(Icons.Rounded.CalendarMonth, "月历", !ringMode) { onMode(false) }
            ModeIcon(Icons.Rounded.DonutLarge, "周期环", ringMode) { onMode(true) }
        }
        Spacer(Modifier.weight(1f))
        StatusPill(state)
        Spacer(Modifier.width(8.dp))
        Mascot(Modifier.size(46.dp), if (t.phase == Phase.MENSTRUAL) MoonMood.SLEEPY else MoonMood.HAPPY)
    }
    Spacer(Modifier.height(14.dp))
    val name = state.settings.nickname.takeIf { it.isNotBlank() && !state.settings.isPartner }
    Text(Copy.greeting() + (name?.let { "，$it" } ?: ""), style = MaterialTheme.typography.headlineMedium)
    Text(
        if (state.settings.isPartner) "正在陪她一起看着日子" else Copy.mood(t.phase),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ModeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(if (selected) Palette.OrangeSoft else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (selected) Palette.Orange else Palette.InkSoft, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StatusPill(state: UiState) {
    val snap = state.snapshot
    val t = snap.today
    val text = when {
        !snap.hasData -> "还没有记录"
        t.phase == Phase.MENSTRUAL -> "经期第 ${t.periodDay} 天"
        t.lateDays > 0 -> "已推迟 ${t.lateDays} 天"
        t.daysUntilNext == 0 -> "可能今天来"
        t.phase == Phase.OVULATION -> "今天排卵日"
        else -> "还有 ${t.daysUntilNext} 天"
    }
    Text(
        text, color = Palette.PinkDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape).background(Palette.PinkSoft).border(1.dp, Palette.PinkLine, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ---------------------------------------------------------------- month view

@Composable
private fun MonthView(state: UiState, infos: Map<LocalDate, DayInfo>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val base = YearMonth.from(state.today)
    val pager = rememberPagerState(initialPage = MONTHS_BACK) { MONTHS_BACK + MONTHS_FORWARD + 1 }
    val scope = rememberCoroutineScope()
    val month = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong())
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${month.year}", fontFamily = Rounded, fontSize = 14.sp, color = Palette.InkSoft)
                Row(verticalAlignment = Alignment.Bottom) {
                    Numeral("${month.monthValue}", 30.sp)
                    Text(" 月", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
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
                Text(
                    d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp,
                    color = if (i >= 5) Palette.Pink else Palette.Sky,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalPager(pager, verticalAlignment = Alignment.Top) { page ->
            val ym = base.plusMonths((page - MONTHS_BACK).toLong())
            MonthGrid(ym, infos, state.records, state.today, selected, onSelect)
        }
    }
}

@Composable
private fun ArrowBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Palette.InkSoft)
    }
}

@Composable
private fun MonthGrid(
    ym: YearMonth,
    infos: Map<LocalDate, DayInfo>,
    records: Map<LocalDate, DayRecordEntity>,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
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
                            DateChip(
                                "${d.dayOfMonth}", infos[d]?.type ?: DayType.UNKNOWN, d == today, d == selected, 40.dp,
                                hasRecord = records[d] != null, muted = d.isAfter(today),
                            ) { onSelect(d) }
                        }
                    }
                }
            }
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
                Mascot(Modifier.size(90.dp))
                Spacer(Modifier.height(10.dp))
                Text("记录第一次经期后，这里会画出你的周期", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }
    // 0 = previous, 1 = current, 2 = next
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

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("上次", "本次", "下次").forEachIndexed { i, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { which = i }.padding(4.dp),
                ) {
                    Text(
                        label, fontSize = 16.sp,
                        fontWeight = if (which == i) FontWeight.Bold else FontWeight.Normal,
                        color = if (which == i) Palette.Ink else Palette.InkFaint,
                    )
                    Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(if (which == i) Palette.Pink else Color.Transparent))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val w = maxWidth
            val chip = min(30f, (w.value * 0.42f * 2 * PI.toFloat() / days.size) * 0.86f).dp
            val radius = w / 2 - chip / 2 - 6.dp
            // track
            Box(
                Modifier.align(Alignment.Center).size(radius * 2 + chip + 6.dp).clip(CircleShape)
                    .background(Palette.Bg),
            )
            Box(Modifier.align(Alignment.Center).size(radius * 2 - chip - 6.dp).clip(CircleShape).background(Color.White))
            days.forEachIndexed { i, d ->
                val angle = -PI / 2 + 2 * PI * i / days.size
                val x = (radius.value * cos(angle)).dp
                val y = (radius.value * sin(angle)).dp
                Box(Modifier.align(Alignment.Center).offset(x, y)) {
                    DateChip(
                        "${d.dayOfMonth}", infos[d]?.type ?: DayType.UNKNOWN, d == today, d == selected, chip,
                        hasRecord = false, muted = d.isAfter(today),
                    ) { onSelect(d) }
                }
                if (d.dayOfMonth == 1 || i == 0) {
                    val lr = radius - chip
                    Text(
                        "${d.monthValue}月", fontSize = 11.sp, color = Palette.InkSoft,
                        modifier = Modifier.align(Alignment.Center)
                            .offset((lr.value * cos(angle)).dp, (lr.value * sin(angle)).dp),
                    )
                }
            }
            // centre
            val info = infos[selected]
            val centre = radius * 2 - chip * 2 - 40.dp
            Column(
                Modifier.align(Alignment.Center).size(centre).clip(CircleShape).background(Palette.SkySoft),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(phaseLabel(info?.type), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("第", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Numeral("${info?.cycleDay ?: "-"}", 40.sp)
                    Text("天", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(2.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomPanel(
    state: UiState,
    vm: AppViewModel,
    date: LocalDate,
    info: DayInfo?,
    onEdit: (LocalDate) -> Unit,
    onToday: () -> Unit,
) {
    val snap = state.snapshot
    val partner = state.settings.isPartner
    val isFuture = date.isAfter(state.today)
    val startHere = snap.periods.any { it.start == date }
    val endHere = snap.periods.any { it.end == date }
    val canEnd = snap.periods.any { !it.start.isAfter(date) && CycleEngine.days(it.start, date) < CycleEngine.MAX_PERIOD_DAYS }
    val record = state.records[date]

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Palette.Panel)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(32.dp).height(4.dp).clip(CircleShape).background(Palette.InkFaint))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(8.dp))
            info?.type?.takeIf { it != DayType.UNKNOWN }?.let {
                Text(
                    phaseLabel(it) + (info.cycleDay?.let { d -> " · 第${d}天" } ?: ""),
                    fontSize = 12.sp, color = Palette.InkSoft,
                )
            }
            Spacer(Modifier.weight(1f))
            if (date != state.today) {
                Text("今天", fontSize = 12.sp, color = Palette.Sky, modifier = Modifier.clip(CircleShape).clickable(onClick = onToday).padding(6.dp))
            }
        }
        Spacer(Modifier.height(14.dp))

        if (partner) {
            Text("这是她的记录，只能查看哦", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionTile(Icons.Rounded.PlayArrow, "月经开始", Palette.Pink, active = startHere, enabled = true) {
                    if (isFuture) vm.toast("这一天还没到哦") else if (startHere) vm.unmarkStart(date) else vm.markStart(date)
                }
                ActionTile(Icons.Rounded.Stop, "月经结束", Palette.Pink, active = endHere, enabled = true) {
                    when {
                        isFuture -> vm.toast("这一天还没到哦")
                        endHere -> vm.unmarkEnd(date)
                        canEnd -> vm.markEnd(date)
                        else -> vm.toast("先标记月经开始，再标记结束哦")
                    }
                }
                Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(44.dp).background(Palette.InkFaint.copy(alpha = 0.5f)))
                ActionTile(if (record == null) Icons.Rounded.Add else Icons.Rounded.Edit, "记录", Palette.Mint, active = true) {
                    if (isFuture) vm.toast("这一天还没到哦") else onEdit(date)
                }
            }
        }

        if (record != null) {
            Spacer(Modifier.height(14.dp))
            SoftCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp), onClick = { onEdit(date) }) {
                RecordTags(record)
            }
        }

        if (state.settings.loveNote.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            SoftCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, null, tint = Palette.Pink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.settings.loveNote, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("给你的小提醒", style = MaterialTheme.typography.titleSmall, color = Palette.InkSoft)
        Spacer(Modifier.height(10.dp))
        Copy.tips(state.snapshot.today.phase).forEach { tip ->
            SoftCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = PaddingValues(14.dp)) {
                Text(tip.title, style = MaterialTheme.typography.titleSmall, color = Palette.PinkDeep)
                Spacer(Modifier.height(4.dp))
                Text(tip.body, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordTags(r: DayRecordEntity) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (r.flow > 0) TagChip("流量 ${Copy.flows[r.flow]}", Palette.Pink)
        if (r.pain > 0) TagChip("痛经 ${Copy.pains[r.pain]}", Palette.PinkDeep)
        r.moodList.forEach { TagChip(Copy.moodLabel(it), Palette.Orange) }
        r.symptomList.forEach { TagChip(Copy.symptomLabel(it), Palette.Sky) }
        if (r.love > 0) TagChip("同房 · ${Copy.loves[r.love]}", Palette.Pink)
        r.temperature?.let { TagChip("$it ℃", Palette.Sky) }
        r.weight?.let { TagChip("$it kg", Palette.Mint) }
    }
    if (r.note.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(r.note, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
    }
}
