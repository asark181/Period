package com.nuanyue.app.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayInfo
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.CuteSwitch
import com.nuanyue.app.ui.components.LegendDot
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.TagChip
import com.nuanyue.app.ui.components.color
import com.nuanyue.app.ui.components.darken
import com.nuanyue.app.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val MONTHS_BACK = 36
private const val MONTHS_FORWARD = 12

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(state: UiState, vm: AppViewModel, onEdit: (LocalDate) -> Unit) {
    val base = YearMonth.from(state.today)
    val pager = rememberPagerState(initialPage = MONTHS_BACK) { MONTHS_BACK + MONTHS_FORWARD + 1 }
    val scope = rememberCoroutineScope()
    var selectedEpoch by rememberSaveable { mutableStateOf(state.today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val month = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong())

    val from = base.minusMonths(MONTHS_BACK.toLong()).atDay(1).minusDays(7)
    val to = base.plusMonths(MONTHS_FORWARD.toLong()).atEndOfMonth().plusDays(7)
    val infos = remember(state.snapshot, state.today) { CycleEngine.dayInfos(state.snapshot, from, to, state.today) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "上个月", tint = Palette.Coral)
            }
            if (pager.currentPage != MONTHS_BACK) {
                TextButton(onClick = {
                    scope.launch { pager.animateScrollToPage(MONTHS_BACK) }
                    selectedEpoch = state.today.toEpochDay()
                }) { Text("今天", color = Palette.Coral, fontWeight = FontWeight.Bold) }
            }
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "下个月", tint = Palette.Coral)
            }
        }
        Spacer(Modifier.height(10.dp))
        SoftCard(padding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, color = Palette.InkSoft)
                }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalPager(pager, verticalAlignment = Alignment.Top) { page ->
                val ym = base.plusMonths((page - MONTHS_BACK).toLong())
                MonthGrid(ym, infos, state.records, state.today, selected) { selectedEpoch = it.toEpochDay() }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                LegendDot(Palette.Rose, "月经期")
                LegendDot(Palette.PredictedRose, "预测经期", hollow = true)
                LegendDot(Palette.Lavender, "易孕期")
                LegendDot(Palette.Ovulation, "排卵日")
                LegendDot(Palette.Lemon.darken(0.95f), "有记录")
            }
        }
        Spacer(Modifier.height(16.dp))
        DayDetail(selected, infos[selected], state, vm, onEdit)
        Spacer(Modifier.height(24.dp))
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
    val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value)
    val cells = lead + ym.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    Box(Modifier.weight(1f).aspectRatio(0.92f), contentAlignment = Alignment.Center) {
                        if (idx in 0 until ym.lengthOfMonth()) {
                            val d = first.plusDays(idx.toLong())
                            DayCell(d, infos[d], records[d] != null, d == today, d.isAfter(today), d == selected) { onSelect(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, info: DayInfo?, hasRecord: Boolean, isToday: Boolean, isFuture: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val type = info?.type ?: DayType.UNKNOWN
    val c = type.color()
    val bg = when (type) {
        DayType.PERIOD -> Palette.Rose
        DayType.OVULATION -> Palette.Ovulation
        DayType.FERTILE -> Palette.LavenderSoft
        else -> Color.Transparent
    }
    val fg = when (type) {
        DayType.PERIOD, DayType.OVULATION -> Color.White
        DayType.FERTILE -> Palette.Ovulation
        DayType.PREDICTED_PERIOD -> Palette.Rose
        else -> if (isFuture) Palette.InkSoft else Palette.Ink
    }
    Box(
        Modifier.padding(3.dp).fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .then(if (isSelected) Modifier.border(2.dp, Palette.Lemon.darken(0.92f), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(3.dp).fillMaxSize().clip(RoundedCornerShape(12.dp)).background(bg))
        if (type == DayType.PREDICTED_PERIOD) {
            Canvas(Modifier.padding(4.dp).fillMaxSize()) {
                drawRoundRect(
                    c, cornerRadius = CornerRadius(11.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isToday) "今" else "${date.dayOfMonth}",
                color = fg, fontSize = 15.sp,
                fontWeight = if (isToday || type == DayType.PERIOD || type == DayType.OVULATION) FontWeight.Bold else FontWeight.Medium,
            )
            val marker = when {
                info?.isPeriodStart == true -> "始"
                info?.isPeriodEnd == true -> "终"
                type == DayType.OVULATION -> "排卵"
                else -> null
            }
            if (marker != null) Text(marker, fontSize = 8.sp, color = fg.copy(alpha = 0.9f), lineHeight = 9.sp)
        }
        if (hasRecord) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(5.dp).clip(CircleShape)
                    .background(if (type == DayType.PERIOD || type == DayType.OVULATION) Color.White else Palette.Lemon.darken(0.95f)),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayDetail(date: LocalDate, info: DayInfo?, state: UiState, vm: AppViewModel, onEdit: (LocalDate) -> Unit) {
    val snap = state.snapshot
    val isFuture = date.isAfter(state.today)
    val partner = state.settings.isPartner
    val startHere = snap.periods.any { it.start == date }
    val endHere = snap.periods.any { it.end == date }
    val recent = snap.periods.lastOrNull { !it.start.isAfter(date) && CycleEngine.days(it.start, date) < CycleEngine.MAX_PERIOD_DAYS }
    val canEnd = recent != null
    val record = state.records[date]

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(describe(info), style = MaterialTheme.typography.bodySmall)
            }
            info?.type?.let { if (it != DayType.UNKNOWN) TagChip(label(it), it.color()) }
        }
        if (isFuture) {
            Spacer(Modifier.height(10.dp))
            Text("未来的日子还不能记录哦，这里展示的是预测～", style = MaterialTheme.typography.bodySmall)
        } else if (!partner) {
            Spacer(Modifier.height(6.dp))
            ToggleRow("大姨妈来了", "标记为本次经期第一天", startHere) { if (it) vm.markStart(date) else vm.unmarkStart(date) }
            if (canEnd || endHere) {
                ToggleRow("大姨妈走了", "标记为本次经期最后一天", endHere) { if (it) vm.markEnd(date) else vm.unmarkEnd(date) }
            }
        }
        if (!isFuture) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当天记录", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (!partner || record != null) {
                    Row(
                        Modifier.clip(CircleShape).background(Palette.RoseSoft).clickable { onEdit(date) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Edit, null, tint = Palette.CoralDeep, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (partner) "查看" else if (record == null) "去记录" else "修改", color = Palette.CoralDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (record == null) {
                Text("这一天还没有记录", style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (record.flow > 0) TagChip("流量 ${Copy.flows[record.flow]}", Palette.Rose)
                    if (record.pain > 0) TagChip("痛经 ${Copy.pains[record.pain]}", Palette.Peach)
                    record.moodList.forEach { TagChip(Copy.moodLabel(it), Palette.Lemon.darken(0.9f)) }
                    record.symptomList.forEach { TagChip(Copy.symptomLabel(it), Palette.Lavender) }
                    if (record.love > 0) TagChip("爱爱 · ${Copy.loves[record.love]}", Palette.Coral)
                    record.temperature?.let { TagChip("体温 $it℃", Palette.Sky) }
                    record.weight?.let { TagChip("体重 ${it}kg", Palette.Mint) }
                }
                if (record.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("“${record.note}”", style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.Rose))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        CuteSwitch(checked, onChange = onChange)
    }
}

private fun label(t: DayType) = when (t) {
    DayType.PERIOD -> "月经期"
    DayType.PREDICTED_PERIOD -> "预测经期"
    DayType.FERTILE -> "易孕期"
    DayType.OVULATION -> "排卵日"
    DayType.SAFE -> "安全期"
    DayType.UNKNOWN -> ""
}

private fun describe(info: DayInfo?): String {
    if (info == null || info.type == DayType.UNKNOWN) return "暂无周期数据"
    val chance = when (info.type) {
        DayType.OVULATION -> "怀孕几率极高"
        DayType.FERTILE -> "怀孕几率较高"
        else -> "怀孕几率低"
    }
    return "周期第 ${info.cycleDay} 天 · $chance"
}
