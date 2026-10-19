package com.nuanyue.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.Hairline
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.StatTile
import com.nuanyue.app.ui.components.TagChip
import com.nuanyue.app.ui.components.VerticalHairline
import com.nuanyue.app.ui.theme.Palette
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private fun md(d: LocalDate) = "${d.monthValue} 月 ${d.dayOfMonth} 日"

/** Hero colors follow the phase so it can be read at a glance. */
private data class Tone(val bg: Color, val fg: Color, val soft: Color)

private fun toneFor(phase: Phase) = when (phase) {
    Phase.MENSTRUAL, Phase.LATE -> Tone(Palette.Rouge, Palette.Paper, Palette.Paper.copy(alpha = 0.78f))
    Phase.FERTILE, Phase.OVULATION -> Tone(Palette.Lemon, Palette.Ink, Palette.Ink.copy(alpha = 0.62f))
    Phase.NONE -> Tone(Palette.PaperDeep, Palette.Ink, Palette.InkSoft)
    else -> Tone(Palette.Sage, Palette.Ink, Palette.SageInk)
}

@Composable
fun HomeScreen(state: UiState, vm: AppViewModel, onEdit: (LocalDate) -> Unit, onOpenCalendar: () -> Unit) {
    val snap = state.snapshot
    val t = snap.today
    val s = state.settings
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Header(state)
        Spacer(Modifier.height(18.dp))
        HeroCard(state, vm)
        Spacer(Modifier.height(12.dp))
        WeekStrip(state, onOpenCalendar)
        if (s.loveNote.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Note(s.loveNote)
        }
        Spacer(Modifier.height(12.dp))
        TodayRecordCard(state, onEdit)
        if (snap.hasData) {
            Spacer(Modifier.height(12.dp))
            SoftCard(padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatTile("${snap.avgCycle}", "天", "平均周期", Modifier.weight(1f))
                    VerticalHairline()
                    Spacer(Modifier.width(16.dp))
                    StatTile("${snap.avgPeriod}", "天", "平均经期", Modifier.weight(1f))
                    VerticalHairline()
                    Spacer(Modifier.width(16.dp))
                    StatTile(t.nextOvulation?.let { "${it.monthValue}.${it.dayOfMonth}" } ?: "–", "", "下次排卵", Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        SectionTitle(if (t.phase == Phase.NONE) "开始之前" else "${t.phase.title}这几天")
        Spacer(Modifier.height(4.dp))
        Tips(t.phase)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Header(state: UiState) {
    val today = state.today
    val name = state.settings.nickname.takeIf { it.isNotBlank() && !state.settings.isPartner }
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                "${today.monthValue} 月 ${today.dayOfMonth} 日  ${today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(Copy.greeting() + (name?.let { "，$it" } ?: ""), style = MaterialTheme.typography.headlineMedium)
            if (state.settings.isPartner) {
                Text("只读查看 · ${state.settings.syncAccount}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.settings.syncEnabled) {
            Icon(Icons.Rounded.CloudDone, "已同步", tint = Palette.InkFaint, modifier = Modifier.padding(bottom = 4.dp).size(18.dp))
        }
    }
}

@Composable
private fun HeroCard(state: UiState, vm: AppViewModel) {
    val snap = state.snapshot
    val t = snap.today
    val tone = toneFor(t.phase)
    val h = heroText(state)

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(tone.bg).padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.label, color = tone.soft, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Numeral(h.big, if (h.big.length > 2) 64.sp else 104.sp, tone.fg)
                    if (h.unit.isNotEmpty()) {
                        Text(h.unit, color = tone.fg, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
                    }
                }
                Text(h.sub, color = tone.soft, fontSize = 13.sp)
            }
            if (snap.hasData) {
                Box(Modifier.size(108.dp)) { CycleRing(state, tone) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(tone.fg.copy(alpha = 0.15f)))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (snap.hasData) "周期第 ${t.cycleDay} 天 · 受孕几率${t.pregnancyChance}" else "还没有记录",
                color = tone.soft, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            if (!state.settings.isPartner) PeriodAction(state, vm, tone)
        }
    }
}

private data class HeroText(val label: String, val big: String, val unit: String, val sub: String)

private fun heroText(state: UiState): HeroText {
    val t = state.snapshot.today
    return when {
        !state.snapshot.hasData -> HeroText("暖月", "–", "", "来的那天，点一下右下角")
        t.phase == Phase.MENSTRUAL -> HeroText("经期第", "${t.periodDay}", "天", "预计还有 ${t.periodDaysLeft} 天")
        t.lateDays > 0 -> HeroText("已推迟", "${t.lateDays}", "天", "原本预计 ${state.snapshot.nextStart?.let(::md) ?: ""}")
        t.daysUntilNext == 0 -> HeroText("下次经期", "今天", "", "可能就是今天")
        else -> HeroText("距下次经期", "${t.daysUntilNext}", "天", "${t.nextStart?.let(::md) ?: ""}左右")
    }
}

/** Thin ring: period arc, fertile arc, and a dot for today. */
@Composable
private fun CycleRing(state: UiState, tone: Tone) {
    val snap = state.snapshot
    val cycle = snap.avgCycle.coerceAtLeast(1)
    val period = snap.avgPeriod
    // 1-based cycle day of ovulation: 14 days before the next start.
    val ovDay = cycle - 13
    val todayDay = snap.today.cycleDay ?: 0
    val anim = remember { Animatable(0f) }
    LaunchedEffect(todayDay, cycle) { anim.snapTo(0f); anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val fertileColor = if (tone.bg == Palette.Lemon) Palette.Ink else Palette.Lemon
    val periodColor = if (tone.bg == Palette.Rouge) Palette.Paper else Palette.Rouge

    Canvas(Modifier.fillMaxSize()) {
        val stroke = 6.dp.toPx()
        val r = size.minDimension / 2 - stroke
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2, r * 2)
        fun sweep(days: Int) = 360f * days / cycle
        fun startAngle(day: Int) = -90f + 360f * (day - 1) / cycle

        drawArc(tone.fg.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize, style = Stroke(1.5.dp.toPx()))
        drawArc(periodColor, startAngle(1), sweep(period) * anim.value, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(fertileColor, startAngle(ovDay - 5), sweep(10) * anim.value, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (todayDay in 1..cycle) {
            val a = Math.toRadians((startAngle(1) + 360f * (todayDay - 0.5f) / cycle * anim.value).toDouble())
            val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
            drawCircle(tone.bg, stroke * 1.15f, p)
            drawCircle(tone.fg, stroke * 0.75f, p)
        }
    }
}

@Composable
private fun PeriodAction(state: UiState, vm: AppViewModel, tone: Tone) {
    val snap = state.snapshot
    val today = state.today
    val last = snap.periods.lastOrNull { !it.start.isAfter(today) }
    val inPeriod = snap.today.phase == Phase.MENSTRUAL
    val (label, action) = when {
        last?.end == today -> "撤销「走了」" to { vm.unmarkEnd(today) }
        inPeriod && last?.start == today -> "撤销「来了」" to { vm.unmarkStart(today) }
        inPeriod -> "大姨妈走了" to { vm.markEnd(today) }
        else -> "大姨妈来了" to { vm.markStart(today) }
    }
    Text(
        label,
        color = tone.bg, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape).background(tone.fg).clickable(onClick = action)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun WeekStrip(state: UiState, onOpenCalendar: () -> Unit) {
    val today = state.today
    val infos = remember(state.snapshot, today) {
        CycleEngine.dayInfos(state.snapshot, today.minusDays(3), today.plusDays(3), today)
    }
    SoftCard(padding = PaddingValues(horizontal = 10.dp, vertical = 14.dp), onClick = onOpenCalendar) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in -3..3) {
                val d = today.plusDays(i.toLong())
                val type = infos[d]?.type ?: DayType.UNKNOWN
                val isToday = i == 0
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        if (isToday) "今天" else d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINA),
                        fontSize = 11.sp, color = if (isToday) Palette.Ink else Palette.InkFaint,
                    )
                    Spacer(Modifier.height(6.dp))
                    DayDot(d.dayOfMonth, type, isToday)
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier.size(4.dp).clip(CircleShape)
                            .background(if (state.records[d] != null) Palette.InkFaint else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
fun DayDot(day: Int, type: DayType, isToday: Boolean, size: Dp = 34.dp) {
    val (bg, fg) = when (type) {
        DayType.PERIOD -> Palette.Rouge to Palette.Paper
        DayType.OVULATION -> Palette.Lemon to Palette.Ink
        DayType.FERTILE -> Palette.LemonSoft to Palette.Ochre
        DayType.PREDICTED_PERIOD -> Color.Transparent to Palette.Rouge
        else -> Color.Transparent to Palette.Ink
    }
    Box(
        Modifier.size(size).clip(CircleShape).background(bg)
            .then(
                when {
                    type == DayType.PREDICTED_PERIOD -> Modifier.border(1.2.dp, Palette.Rouge, CircleShape)
                    isToday -> Modifier.border(1.5.dp, Palette.Ink, CircleShape)
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("$day", fontSize = 14.sp, color = fg, fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun Note(note: String) {
    SoftCard(padding = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(3.dp).height(58.dp).background(Palette.Rouge))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("留言", style = MaterialTheme.typography.bodySmall)
                Text(note, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun TodayRecordCard(state: UiState, onEdit: (LocalDate) -> Unit) {
    val r = state.records[state.today]
    val partner = state.settings.isPartner
    SoftCard(onClick = { onEdit(state.today) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("今天", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                when {
                    partner -> if (r == null) "" else "查看"
                    r == null -> "记一笔"
                    else -> "修改"
                },
                fontSize = 13.sp, color = Palette.InkSoft,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Palette.InkSoft, modifier = Modifier.size(16.dp))
        }
        if (r == null) {
            Spacer(Modifier.height(4.dp))
            Text(if (partner) "她今天还没有记录。" else "心情、症状、体温、备注。", style = MaterialTheme.typography.bodySmall)
        } else {
            Spacer(Modifier.height(12.dp))
            RecordTags(r)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordTags(r: DayRecordEntity) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (r.flow > 0) TagChip("流量 ${Copy.flows[r.flow]}", Palette.Rouge)
        if (r.pain > 0) TagChip("痛经 ${Copy.pains[r.pain]}", Palette.Rouge)
        r.moodList.forEach { TagChip(Copy.moodLabel(it), Palette.Lemon) }
        r.symptomList.forEach { TagChip(Copy.symptomLabel(it), Palette.InkFaint) }
        if (r.love > 0) TagChip("同房 · ${Copy.loves[r.love]}", Palette.InkSoft)
        r.temperature?.let { TagChip("$it ℃", Palette.Sky) }
        r.weight?.let { TagChip("$it kg", Palette.SageInk) }
    }
    if (r.note.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(r.note, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
    }
}

@Composable
private fun Tips(phase: Phase) {
    val tips = Copy.tips(phase)
    Column {
        tips.forEachIndexed { i, tip ->
            Row(Modifier.padding(vertical = 14.dp)) {
                Numeral("%02d".format(i + 1), 22.sp, Palette.InkFaint, Modifier.width(40.dp))
                Column {
                    Text(tip.title, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(3.dp))
                    Text(tip.body, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
                }
            }
            if (i != tips.lastIndex) Hairline()
        }
    }
}
