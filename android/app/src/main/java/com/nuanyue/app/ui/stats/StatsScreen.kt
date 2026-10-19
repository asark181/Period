package com.nuanyue.app.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.R
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.FloatingSticker
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.StatTile
import com.nuanyue.app.ui.components.pressScale
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.Rounded
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** One row of the history table. */
private data class Row3(val start: LocalDate, val periodDays: Int, val cycleDays: Int?)

@Composable
fun StatsScreen(state: UiState) {
    val snap = state.snapshot
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val rows = remember(snap, state.today) {
        snap.periods.mapIndexed { i, p ->
            val next = snap.periods.getOrNull(i + 1)
            val days = when {
                p.end != null -> CycleEngine.days(p.start, p.end) + 1
                next == null -> (CycleEngine.days(p.start, state.today) + 1).coerceIn(1, CycleEngine.MAX_PERIOD_DAYS)
                else -> snap.cycles[i].periodLength
            }
            Row3(p.start, days, next?.let { CycleEngine.days(p.start, it.start) })
        }.reversed()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFEEF2), Color.White)))
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            StatsTabs(tab) { tab = it }
            Spacer(Modifier.height(8.dp))
            Summary(rows)
            Spacer(Modifier.height(16.dp))
        }
        if (!snap.hasData) {
            Box(Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        FloatingSticker(R.drawable.st_sparkle, Modifier.offset(x = (-96).dp, y = (-70).dp).size(18.dp), rotation = -12f, periodMs = 2000)
                        Image(
                            painter = painterResource(R.drawable.illu_empty_stats),
                            contentDescription = null,
                            modifier = Modifier.size(208.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("还没有经期记录", style = MaterialTheme.typography.titleSmall)
                    Text("记满两次，这里才会算出平均周期和图表", style = MaterialTheme.typography.bodySmall)
                }
            }
            return@Column
        }
        AnimatedContent(
            tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "statsTab",
        ) { which ->
            if (which == 1) Table(rows) else Charts(state, rows)
        }
    }
}

@Composable
private fun StatsTabs(tab: Int, onTab: (Int) -> Unit) {
    val labels = listOf("经期图表", "经期数据")
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        val slot = maxWidth / labels.size
        val x by animateDpAsState(slot * tab, spring(dampingRatio = 0.7f, stiffness = 380f), label = "statsTab")
        Box(Modifier.offset(x = x).width(slot).height(36.dp).clip(CircleShape).background(Palette.PinkSoft))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val selected = tab == i
                val color by animateColorAsState(if (selected) Palette.PinkDeep else Palette.InkSoft, label = "tabColor")
                val source = remember(i) { MutableInteractionSource() }
                val scale = pressScale(source, 0.96f)
                Box(
                    Modifier.weight(1f).height(36.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .clickable(source, indication = null) { onTab(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = color, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun Summary(rows: List<Row3>) {
    val cycles = rows.mapNotNull { it.cycleDays }
    // The ongoing period is still running, so leave it out of period-length stats.
    val periods = rows.filter { it.cycleDays != null }.map { it.periodDays }.ifEmpty { rows.map { it.periodDays } }
    fun avg(l: List<Int>) = if (l.isEmpty()) "-" else "${l.average().roundToInt()}"
    Column {
        Row {
            StatTile(avg(cycles), "天", "平均周期", Modifier.weight(1f))
            StatTile(cycles.maxOrNull()?.toString() ?: "-", "天", "最长周期", Modifier.weight(1f))
            StatTile(cycles.minOrNull()?.toString() ?: "-", "天", "最短周期", Modifier.weight(1f))
        }
        Row {
            StatTile(avg(periods), "天", "平均经期", Modifier.weight(1f))
            StatTile(periods.maxOrNull()?.toString() ?: "-", "天", "最长经期", Modifier.weight(1f))
            StatTile(periods.minOrNull()?.toString() ?: "-", "天", "最短经期", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Table(rows: List<Row3>) {
    Column(Modifier.background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(Palette.Bg))
        TableRow("开始日期", "经期天数", "周期天数", header = true)
        rows.forEachIndexed { index, r ->
            TableRow(
                "%d-%02d-%02d".format(r.start.year, r.start.monthValue, r.start.dayOfMonth),
                "${r.periodDays}",
                r.cycleDays?.toString() ?: "当前周期",
                highlight = r.cycleDays != null && (r.cycleDays > 45 || r.cycleDays < 21),
                zebra = index % 2 == 1,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TableRow(a: String, b: String, c: String, header: Boolean = false, highlight: Boolean = false, zebra: Boolean = false) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (zebra) Palette.PinkSoft.copy(alpha = 0.45f) else Color.Transparent)
            .padding(vertical = 12.dp),
    ) {
        listOf(a, b, c).forEachIndexed { i, s ->
            Text(
                s, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                fontFamily = if (header) null else Rounded,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = if (header) 15.sp else 16.sp,
                color = if (highlight && i == 2) Palette.Orange else Palette.Ink,
            )
        }
    }
}

@Composable
private fun Charts(state: UiState, rows: List<Row3>) {
    val snap = state.snapshot
    Column(Modifier.padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(4.dp))
        Box {
        SoftCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(Modifier.size(52.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("小月这样帮你预测", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "按最近 6 次周期加权算出 ${snap.avgCycle} 天，经期按 ${snap.avgPeriod} 天算。" +
                            "偶尔特别长的一次（比如漏记了）不会算进去。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("规律程度", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    snap.regularity, color = Palette.PinkDeep, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.clip(CircleShape).background(Palette.PinkSoft).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            snap.cycleStdDev?.let {
                Text(
                    "最近几次前后相差约 ±${"%.0f".format(it)} 天。" + if (it > 5) "波动有点大，预测日期前后几天来都很正常哦。" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        com.nuanyue.app.ui.components.Sticker(com.nuanyue.app.R.drawable.st_washi_yellow, Modifier.align(Alignment.TopEnd).padding(end = 28.dp).size(width = 84.dp, height = 20.dp), rotation = 4f)
        com.nuanyue.app.ui.components.FloatingSticker(com.nuanyue.app.R.drawable.st_star, Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 6.dp).size(26.dp), rotation = 10f)
        }
        Spacer(Modifier.height(12.dp))
        CycleBars(rows.reversed(), snap.avgCycle)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CycleBars(rows: List<Row3>, avg: Int) {
    var mode by remember { mutableIntStateOf(0) }
    val data = if (mode == 0) rows.mapNotNull { r -> r.cycleDays?.let { r.start to it } }.takeLast(12)
    else rows.filter { it.cycleDays != null }.map { it.start to it.periodDays }.takeLast(12)
    val avgLine = if (mode == 0) avg else (data.map { it.second }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0)
    SoftCard(Modifier.fillMaxWidth()) {
        SectionTitle(if (mode == 0) "周期长度" else "经期长度") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("周期", mode == 0, { mode = 0 })
                Pill("经期", mode == 1, { mode = 1 })
            }
        }
        Spacer(Modifier.height(16.dp))
        if (data.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.illu_empty_stats),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("至少两次经期才有周期长度哦", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            return@SoftCard
        }
        val maxV = data.maxOf { it.second }.coerceAtLeast(avgLine) * 1.12f
        val anim = remember { Animatable(0f) }
        LaunchedEffect(mode, data.size) { anim.snapTo(0f); anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                data.forEach { (start, v) ->
                    val off = mode == 0 && abs(v - avgLine) > 6
                    val brush = if (off) Brush.verticalGradient(listOf(Color(0xFFFFD27A), Palette.Orange))
                    else Brush.verticalGradient(listOf(Palette.PinkLine, Palette.Pink))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            "$v", fontSize = 11.sp, fontFamily = Rounded, fontWeight = FontWeight.Bold,
                            color = if (off) Palette.Orange else Palette.InkSoft,
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier.width(14.dp).height((140 * v / maxV * anim.value).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(7.dp)).background(brush),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${start.monthValue}月", fontSize = 10.sp, color = Palette.InkFaint)
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val chartBottom = size.height - 18.dp.toPx()
                val y = chartBottom - 140.dp.toPx() * avgLine / maxV
                drawLine(
                    Palette.Sky.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (mode == 0) "虚线是预测用的 $avgLine 天，橙色是和它差得比较多的几次" else "虚线是平均 $avgLine 天",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
