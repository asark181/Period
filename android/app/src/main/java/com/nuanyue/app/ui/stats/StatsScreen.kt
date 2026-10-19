package com.nuanyue.app.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.Hairline
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.StatTile
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
        Column(Modifier.background(Color.White).statusBarsPadding().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf("经期图表", "经期数据").forEachIndexed { i, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { tab = i }.padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label, fontSize = 16.sp,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                            color = if (tab == i) Palette.Pink else Palette.InkSoft,
                        )
                        Box(Modifier.padding(top = 4.dp).width(44.dp).height(2.dp).background(if (tab == i) Palette.Pink else Color.Transparent))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Summary(rows)
            Spacer(Modifier.height(16.dp))
        }
        if (!snap.hasData) {
            Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Modifier.size(90.dp))
                Spacer(Modifier.height(10.dp))
                Text("还没有数据呢", style = MaterialTheme.typography.titleSmall)
                Text("记满两次经期，小月就能帮你画出图表啦", style = MaterialTheme.typography.bodySmall)
            }
            return@Column
        }
        if (tab == 1) Table(rows) else Charts(state, rows)
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
        rows.forEach { r ->
            Hairline()
            TableRow(
                "%d-%02d-%02d".format(r.start.year, r.start.monthValue, r.start.dayOfMonth),
                "${r.periodDays}",
                r.cycleDays?.toString() ?: "当前周期",
                highlight = r.cycleDays != null && (r.cycleDays > 45 || r.cycleDays < 21),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TableRow(a: String, b: String, c: String, header: Boolean = false, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
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
        SoftCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(Modifier.size(44.dp))
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
        Spacer(Modifier.height(12.dp))
        CycleBars(rows.reversed(), snap.avgCycle)
        Spacer(Modifier.height(12.dp))
        FrequencyCard(state.records)
        Spacer(Modifier.height(12.dp))
        BodyCurves(state.records, state.today)
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
            Text("至少两次经期才有周期长度哦", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun FrequencyCard(records: Map<LocalDate, DayRecordEntity>) {
    var mode by remember { mutableIntStateOf(0) }
    val since = LocalDate.now().minusDays(180)
    val recent = records.filterKeys { !it.isBefore(since) }.values
    val counts: List<Pair<String, Int>> = if (mode == 0) {
        recent.flatMap { it.symptomList }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            .take(8).map { Copy.symptomLabel(it.key) to it.value }
    } else {
        recent.flatMap { it.moodList }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            .take(8).map { Copy.moodLabel(it.key) to it.value }
    }
    val painDays = recent.filter { it.pain > 0 }
    SoftCard(Modifier.fillMaxWidth()) {
        SectionTitle("近半年的身体和心情") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("身体", mode == 0, { mode = 0 }, color = Palette.Sky)
                Pill("心情", mode == 1, { mode = 1 }, color = Palette.Orange)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (counts.isEmpty()) {
            Text("还没有这类记录，在首页点「记录」就能添加啦", style = MaterialTheme.typography.bodySmall)
        } else {
            val max = counts.maxOf { it.second }
            counts.forEach { (label, n) ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 13.sp, modifier = Modifier.width(72.dp))
                    Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Palette.Panel)) {
                        Box(
                            Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight().clip(CircleShape)
                                .background(if (mode == 0) Palette.Sky else Palette.Orange),
                        )
                    }
                    Text("$n 天", fontSize = 12.sp, color = Palette.InkSoft, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
                }
            }
        }
        if (painDays.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val avgPain = painDays.map { it.pain }.average()
            Text(
                "有 ${painDays.size} 天记了痛经，平均「${Copy.pains[avgPain.roundToInt().coerceIn(0, 4)]}」。" +
                    if (avgPain >= 3) "经常疼得厉害的话，去医院看看会更安心。" else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BodyCurves(records: Map<LocalDate, DayRecordEntity>, today: LocalDate) {
    val since = today.minusDays(90)
    val recent = records.entries.filter { !it.key.isBefore(since) }
    val temps = recent.mapNotNull { e -> e.value.temperature?.let { e.key to it } }.sortedBy { it.first }
    val weights = recent.mapNotNull { e -> e.value.weight?.let { e.key to it } }.sortedBy { it.first }
    SoftCard(Modifier.fillMaxWidth()) {
        SectionTitle("体温和体重")
        Text("近 90 天", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        if (temps.size < 2 && weights.size < 2) {
            Text("记两天以上就能看到曲线。排卵后体温会升高一点点，坚持量能帮你看出排卵日哦。", style = MaterialTheme.typography.bodySmall)
            return@SoftCard
        }
        if (temps.size >= 2) {
            LineChart("基础体温", "℃", temps, Palette.Pink, minRange = 0.8f)
            Spacer(Modifier.height(18.dp))
        }
        if (weights.size >= 2) LineChart("体重", "kg", weights, Palette.Mint, minRange = 4f)
    }
}

@Composable
private fun LineChart(title: String, unit: String, points: List<Pair<LocalDate, Float>>, color: Color, minRange: Float) {
    val minV = points.minOf { it.second }
    val maxV = points.maxOf { it.second }
    // Keep small day-to-day noise from looking like dramatic swings.
    val range = maxOf(maxV - minV, minRange)
    val floor = (minV + maxV) / 2 - range / 2
    val firstDay = points.first().first.toEpochDay()
    val span = (points.last().first.toEpochDay() - firstDay).coerceAtLeast(1)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Numeral("${points.last().second}", 20.sp, color)
        Text(" $unit", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
    }
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val pad = 6.dp.toPx()
        fun pos(i: Int): Offset {
            val (d, v) = points[i]
            val x = pad + (size.width - 2 * pad) * (d.toEpochDay() - firstDay) / span
            val y = pad + (size.height - 2 * pad) * (1 - (v - floor) / range)
            return Offset(x, y)
        }
        val line = Path()
        val fill = Path()
        points.indices.forEach { i ->
            val p = pos(i)
            if (i == 0) {
                line.moveTo(p.x, p.y); fill.moveTo(p.x, size.height); fill.lineTo(p.x, p.y)
            } else {
                line.lineTo(p.x, p.y); fill.lineTo(p.x, p.y)
            }
        }
        fill.lineTo(pos(points.lastIndex).x, size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), Color.Transparent)))
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.White, 4.5.dp.toPx(), pos(points.lastIndex))
        drawCircle(color, 3.dp.toPx(), pos(points.lastIndex))
    }
    Text("区间 $minV – $maxV", style = MaterialTheme.typography.bodySmall)
}
