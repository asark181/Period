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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayInfo
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.CuteSwitch
import com.nuanyue.app.ui.components.Hairline
import com.nuanyue.app.ui.components.LegendDot
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.home.RecordTags
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral("${month.monthValue}", 56.sp)
            Column(Modifier.padding(start = 8.dp, bottom = 8.dp).weight(1f)) {
                Text("月", style = MaterialTheme.typography.titleMedium)
                Text("${month.year}", style = MaterialTheme.typography.bodySmall)
            }
            if (pager.currentPage != MONTHS_BACK) {
                Text(
                    "回到今天", fontSize = 13.sp, color = Palette.InkSoft,
                    modifier = Modifier.padding(bottom = 12.dp).clip(CircleShape).clickable {
                        scope.launch { pager.animateScrollToPage(MONTHS_BACK) }
                        selectedEpoch = state.today.toEpochDay()
                    }.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "上个月", tint = Palette.Ink)
            }
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "下个月", tint = Palette.Ink)
            }
        }
        Spacer(Modifier.height(8.dp))
        SoftCard(padding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = Palette.InkFaint)
                }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalPager(pager, verticalAlignment = Alignment.Top) { page ->
                val ym = base.plusMonths((page - MONTHS_BACK).toLong())
                MonthGrid(ym, infos, state.records, state.today, selected) { selectedEpoch = it.toEpochDay() }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            ) {
                LegendDot(Palette.Rouge, "经期")
                LegendDot(Palette.Rouge, "预测", hollow = true)
                LegendDot(Palette.LemonSoft, "易孕期")
                LegendDot(Palette.Lemon, "排卵日")
                LegendDot(Palette.InkFaint, "有记录")
            }
        }
        Spacer(Modifier.height(12.dp))
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
    val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val rows = (lead + ym.lengthOfMonth() + 6) / 7
    Column {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    Box(Modifier.weight(1f).aspectRatio(0.95f), contentAlignment = Alignment.Center) {
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
    val (bg, fg) = when (type) {
        DayType.PERIOD -> Palette.Rouge to Palette.Paper
        DayType.OVULATION -> Palette.Lemon to Palette.Ink
        DayType.FERTILE -> Palette.LemonSoft to Palette.Ochre
        DayType.PREDICTED_PERIOD -> Color.Transparent to Palette.Rouge
        else -> Color.Transparent to (if (isFuture) Palette.InkSoft else Palette.Ink)
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.padding(3.dp).fillMaxSize().clip(shape)
            .background(bg)
            .then(if (isSelected) Modifier.border(1.5.dp, Palette.Ink, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (type == DayType.PREDICTED_PERIOD && !isSelected) {
            Canvas(Modifier.padding(1.dp).fillMaxSize()) {
                drawRoundRect(
                    Palette.Rouge, cornerRadius = CornerRadius(11.dp.toPx()),
                    style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
            }
        }
        Text(
            "${date.dayOfMonth}", color = fg, fontSize = 15.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (isToday) Box(Modifier.width(10.dp).height(2.dp).background(fg))
            else if (hasRecord) Box(Modifier.size(4.dp).clip(CircleShape).background(fg.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun DayDetail(date: LocalDate, info: DayInfo?, state: UiState, vm: AppViewModel, onEdit: (LocalDate) -> Unit) {
    val snap = state.snapshot
    val isFuture = date.isAfter(state.today)
    val partner = state.settings.isPartner
    val startHere = snap.periods.any { it.start == date }
    val endHere = snap.periods.any { it.end == date }
    val canEnd = snap.periods.any { !it.start.isAfter(date) && CycleEngine.days(it.start, date) < CycleEngine.MAX_PERIOD_DAYS }
    val record = state.records[date]

    SoftCard {
        Text(
            "${date.monthValue} 月 ${date.dayOfMonth} 日  ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(describe(info), style = MaterialTheme.typography.bodySmall)
        if (isFuture) {
            Spacer(Modifier.height(10.dp))
            Text("这一天还没到，显示的是预测。", style = MaterialTheme.typography.bodySmall)
        } else if (!partner) {
            Spacer(Modifier.height(6.dp))
            ToggleRow("这天来了", startHere) { if (it) vm.markStart(date) else vm.unmarkStart(date) }
            if (canEnd || endHere) {
                ToggleRow("这天走了", endHere) { if (it) vm.markEnd(date) else vm.unmarkEnd(date) }
            }
        }
        if (!isFuture) {
            Spacer(Modifier.height(8.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("记录", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (!partner || record != null) {
                    Text(
                        if (partner) "查看" else if (record == null) "添加" else "修改",
                        color = Palette.InkSoft, fontSize = 13.sp,
                        modifier = Modifier.clip(CircleShape).clickable { onEdit(date) }.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (record == null) Text("没有记录", style = MaterialTheme.typography.bodySmall)
            else RecordTags(record)
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        CuteSwitch(checked, onChange = onChange)
    }
}

private fun describe(info: DayInfo?): String {
    if (info == null || info.type == DayType.UNKNOWN) return "没有周期数据"
    val label = when (info.type) {
        DayType.PERIOD -> "经期"
        DayType.PREDICTED_PERIOD -> "预测经期"
        DayType.FERTILE -> "易孕期"
        DayType.OVULATION -> "排卵日"
        else -> "安全期"
    }
    return "$label · 周期第 ${info.cycleDay} 天"
}
