package com.nuanyue.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.StatTile
import com.nuanyue.app.ui.components.TagChip
import com.nuanyue.app.ui.components.color
import com.nuanyue.app.ui.components.darken
import com.nuanyue.app.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val md = DateTimeFormatter.ofPattern("M月d日")

@Composable
fun HomeScreen(state: UiState, vm: AppViewModel, onEdit: (LocalDate) -> Unit, onOpenCalendar: () -> Unit) {
    val snap = state.snapshot
    val t = snap.today
    val s = state.settings
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Header(state)
        Spacer(Modifier.height(16.dp))
        HeroCard(state, vm)
        Spacer(Modifier.height(16.dp))
        WeekStrip(state, onOpenCalendar)
        if (s.loveNote.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            LoveNote(s.loveNote, s.isPartner)
        }
        Spacer(Modifier.height(16.dp))
        TodayRecordCard(state, onEdit)
        Spacer(Modifier.height(16.dp))
        if (snap.hasData) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${snap.avgCycle}", "天", "平均周期", Palette.Coral, Modifier.weight(1f))
                StatTile("${snap.avgPeriod}", "天", "平均经期", Palette.Peach, Modifier.weight(1f))
                StatTile(t.nextOvulation?.let { "${it.monthValue}.${it.dayOfMonth}" } ?: "--", "", "下次排卵日", Palette.Lavender, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }
        SectionTitle(if (t.phase == Phase.NONE) "新手小贴士" else "${t.phase.title}贴心小贴士")
        Spacer(Modifier.height(10.dp))
        Tips(t.phase)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(state: UiState) {
    val today = state.today
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                Copy.greeting(if (state.settings.isPartner) "守护者" else state.settings.nickname),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${today.monthValue}月${today.dayOfMonth}日 ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)} · " +
                    if (state.settings.isPartner) "正在守护 TA 的周期" else Copy.subGreeting(state.snapshot.today.phase),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.settings.syncEnabled) {
            Icon(Icons.Rounded.CloudDone, "已同步", tint = Palette.Mint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HeroCard(state: UiState, vm: AppViewModel) {
    val snap = state.snapshot
    val t = snap.today
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier.fillMaxWidth()
            .shadow(18.dp, shape, spotColor = Palette.Coral.copy(alpha = 0.45f))
            .clip(shape)
            .background(Palette.heroGradient),
    ) {
        // Decorative bubbles.
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * 0.35f, center = Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.22f, center = Offset(size.width * 0.02f, size.height * 0.95f))
            drawCircle(Color.White.copy(alpha = 0.18f), radius = 6.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.22f), radius = 4.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.78f))
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                CycleRing(state)
                HeroCenter(state)
            }
            Spacer(Modifier.height(12.dp))
            if (snap.hasData) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip("怀孕几率 ${t.pregnancyChance}")
                    t.cycleDay?.let { HeroChip("周期第 $it 天") }
                    t.nextStart?.let { if (t.phase != Phase.MENSTRUAL) HeroChip("预计 ${it.format(md)}") }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (!state.settings.isPartner) PeriodActionButton(state, vm)
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun HeroCenter(state: UiState) {
    val t = state.snapshot.today
    val partner = state.settings.isPartner
    val (top, big, unit, bottom) = when {
        !state.snapshot.hasData -> Quad("欢迎来到暖月", "♡", "", "记录第一次经期吧")
        t.phase == Phase.MENSTRUAL -> Quad("月经期 · 第", "${t.periodDay}", "天", "预计还有 ${t.periodDaysLeft} 天结束")
        t.lateDays > 0 -> Quad("经期已推迟", "${t.lateDays}", "天", "放轻松，它可能就快来啦")
        t.daysUntilNext == 0 -> Quad("大姨妈", "今天", "", "可能到访，做好准备哦")
        else -> Quad(
            "距离下次大姨妈", "${t.daysUntilNext}", "天",
            when {
                partner -> "提前准备好红糖水哦"
                t.phase == Phase.OVULATION -> "排卵日 · 注意防护哦"
                t.phase == Phase.FERTILE -> "易孕期 · 注意防护哦"
                else -> "安全期 · 元气满满"
            },
        )
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(1f, 1.04f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(top, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                big, color = Color.White, fontWeight = FontWeight.ExtraBold,
                fontSize = if (big.length > 2) 46.sp else 64.sp,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            )
            if (unit.isNotEmpty()) Text(unit, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp, start = 2.dp))
        }
        Text(bottom, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

private data class Quad(val a: String, val b: String, val c: String, val d: String)


/** A ring that maps the whole cycle: period arc, fertile arc, ovulation dot, and a marker for today. */
@Composable
private fun CycleRing(state: UiState) {
    val snap = state.snapshot
    val cycle = snap.avgCycle.coerceAtLeast(1)
    val period = snap.avgPeriod
    // 1-based cycle day of ovulation: 14 days before the next start.
    val ovDay = cycle - 13
    val todayDay = snap.today.cycleDay ?: 0
    val anim = remember { Animatable(0f) }
    LaunchedEffect(todayDay, cycle) { anim.snapTo(0f); anim.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    Canvas(Modifier.fillMaxSize()) {
        val stroke = 16.dp.toPx()
        val r = size.minDimension / 2 - stroke
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2, r * 2)
        fun sweep(days: Int) = 360f * days / cycle
        fun startAngle(day: Int) = -90f + 360f * (day - 1) / cycle

        drawArc(Color.White.copy(alpha = 0.28f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        if (snap.hasData) {
            drawArc(Color.White.copy(alpha = 0.95f), startAngle(1), sweep(period) * anim.value, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(Color(0xFFE9DDFF), startAngle(ovDay - 5), sweep(10) * anim.value, false, topLeft, arcSize, style = Stroke(stroke * 0.55f, cap = StrokeCap.Round))
            val ovAngle = Math.toRadians((startAngle(ovDay) + sweep(1) / 2).toDouble())
            drawCircle(Palette.Ovulation, stroke * 0.42f, Offset(center.x + r * cos(ovAngle).toFloat(), center.y + r * sin(ovAngle).toFloat()))
            if (todayDay in 1..cycle) {
                val a = Math.toRadians((startAngle(1) + 360f * (todayDay - 0.5f) / cycle * anim.value).toDouble())
                val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
                drawCircle(Color.White, stroke * 0.85f, p)
                drawCircle(Palette.CoralDeep, stroke * 0.45f, p)
            }
        }
    }
}

@Composable
private fun PeriodActionButton(state: UiState, vm: AppViewModel) {
    val snap = state.snapshot
    val today = state.today
    val last = snap.periods.lastOrNull { !it.start.isAfter(today) }
    val inPeriod = snap.today.phase == Phase.MENSTRUAL
    val endedToday = last?.end == today
    val (label, action) = when {
        endedToday -> "大姨妈今天走了 · 撤销" to { vm.unmarkEnd(today) }
        inPeriod && last != null && last.start == today -> "大姨妈今天来了 · 撤销" to { vm.unmarkStart(today) }
        inPeriod -> "大姨妈走了" to { vm.markEnd(today) }
        else -> "大姨妈来了" to { vm.markStart(today) }
    }
    Row(
        Modifier
            .shadow(6.dp, CircleShape, spotColor = Color(0x55C0405F))
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = action)
            .padding(horizontal = 26.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WaterDrop, null, tint = Palette.Rose, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Palette.CoralDeep, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun WeekStrip(state: UiState, onOpenCalendar: () -> Unit) {
    val today = state.today
    val infos = remember(state.snapshot, today) {
        CycleEngine.dayInfos(state.snapshot, today.minusDays(3), today.plusDays(3), today)
    }
    SoftCard(padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp), onClick = onOpenCalendar) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in -3..3) {
                val d = today.plusDays(i.toLong())
                val info = infos[d]
                val type = info?.type ?: DayType.UNKNOWN
                val isToday = i == 0
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        if (isToday) "今天" else d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINA),
                        fontSize = 11.sp, color = if (isToday) Palette.CoralDeep else Palette.InkSoft,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    val c = type.color()
                    val filled = type == DayType.PERIOD || type == DayType.OVULATION
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(
                                when {
                                    filled -> c
                                    type == DayType.FERTILE -> Palette.LavenderSoft
                                    isToday -> Palette.RoseMist
                                    else -> Color.Transparent
                                },
                            )
                            .then(
                                if (type == DayType.PREDICTED_PERIOD) Modifier.border(1.5.dp, c, CircleShape)
                                else if (isToday && !filled) Modifier.border(1.5.dp, Palette.Coral, CircleShape) else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${d.dayOfMonth}",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = when {
                                filled -> Color.White
                                type == DayType.FERTILE -> Palette.Ovulation
                                type == DayType.PREDICTED_PERIOD -> Palette.Rose
                                else -> Palette.Ink
                            },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.size(4.dp).clip(CircleShape)
                            .background(if (state.records[d] != null) Palette.Lemon.darken(0.95f) else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoveNote(note: String, partner: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Palette.lemonGradient).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Favorite, null, tint = Palette.CoralDeep, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(if (partner) "我留给 TA 的小纸条" else "TA 留给你的小纸条", fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.7f))
            Spacer(Modifier.height(2.dp))
            Text(note, style = MaterialTheme.typography.titleSmall, color = Palette.Ink)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayRecordCard(state: UiState, onEdit: (LocalDate) -> Unit) {
    val r = state.records[state.today]
    SoftCard(onClick = { onEdit(state.today) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日记录", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        state.settings.isPartner -> if (r == null) "TA 今天还没有记录" else "TA 今天的记录，点击查看"
                        r == null -> "心情、症状、体温…花 10 秒记一下吧"
                        else -> "已记录，点击可以修改"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Palette.heroGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (r == null) Icons.Rounded.Add else Icons.Rounded.Edit, "记录", tint = Color.White)
            }
        }
        if (r != null) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.flow > 0) TagChip("流量 ${Copy.flows[r.flow]}", Palette.Rose)
                if (r.pain > 0) TagChip("痛经 ${Copy.pains[r.pain]}", Palette.Peach)
                r.moodList.forEach { TagChip(Copy.moodLabel(it), Palette.Lemon.darken(0.9f)) }
                r.symptomList.forEach { TagChip(Copy.symptomLabel(it), Palette.Lavender) }
                if (r.love > 0) TagChip("爱爱 · ${Copy.loves[r.love]}", Palette.Coral)
                r.temperature?.let { TagChip("体温 $it℃", Palette.Sky) }
                r.weight?.let { TagChip("体重 ${it}kg", Palette.Mint) }
            }
            if (r.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("“${r.note}”", style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
            }
        }
    }
}

@Composable
private fun Tips(phase: Phase) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
        items(Copy.tips(phase)) { tip ->
            SoftCard(Modifier.width(210.dp).heightIn(min = 136.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tip.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(tip.title, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(tip.body, style = MaterialTheme.typography.bodySmall, color = Palette.InkSoft)
            }
        }
    }
}
