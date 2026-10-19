package com.nuanyue.app.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.data.DayRecordEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleRecord
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.Hairline
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.StatTile
import com.nuanyue.app.ui.components.VerticalHairline
import com.nuanyue.app.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

private val ymd = DateTimeFormatter.ofPattern("yyyy.M.d")

@Composable
fun StatsScreen(state: UiState) {
    val snap = state.snapshot
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("分析", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (snap.hasData) "基于 ${snap.periods.size} 次经期、${state.records.size} 天记录" else "还没有记录",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))

        if (!snap.hasData) {
            SoftCard(Modifier.fillMaxWidth()) {
                Text("记满两次经期后，这里会出现周期长度、规律程度和症状统计。", style = MaterialTheme.typography.bodyMedium, color = Palette.InkSoft)
            }
            return@Column
        }

        Overview(state)
        Spacer(Modifier.height(12.dp))
        CycleBars(snap.cycles, snap.avgCycle)
        Spacer(Modifier.height(12.dp))
        Timeline(snap.cycles, snap.avgCycle)
        Spacer(Modifier.height(12.dp))
        FrequencyCard(state.records)
        Spacer(Modifier.height(12.dp))
        BodyCurves(state.records, state.today)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Overview(state: UiState) {
    val snap = state.snapshot
    val finished = snap.cycles.mapNotNull { it.cycleLength }
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatTile("${snap.avgCycle}", "天", "平均周期", Modifier.weight(1f))
            VerticalHairline()
            Spacer(Modifier.width(16.dp))
            StatTile("${snap.avgPeriod}", "天", "平均经期", Modifier.weight(1f))
            VerticalHairline()
            Spacer(Modifier.width(16.dp))
            StatTile(snap.cycleStdDev?.let { "±%.1f".format(it) } ?: "–", "天", "周期波动", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Hairline()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("规律程度", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(snap.regularity, style = MaterialTheme.typography.titleSmall)
        }
        if (finished.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("最短 ${finished.min()} 天，最长 ${finished.max()} 天。", style = MaterialTheme.typography.bodySmall)
            if (finished.max() > 35 || finished.min() < 21) {
                Text("一般 21–35 天都算正常。长期不规律，可以去妇科查一下。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CycleBars(cycles: List<CycleRecord>, avg: Int) {
    var mode by remember { mutableIntStateOf(0) }
    val data = cycles.takeLast(12).let { list ->
        if (mode == 0) list.mapNotNull { c -> c.cycleLength?.let { c.start to it } }
        else list.map { it.start to it.periodLength }
    }
    val avgLine = if (mode == 0) avg else (data.map { it.second }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0)
    SoftCard {
        SectionTitle("长度变化") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("周期", mode == 0, { mode = 0 })
                Pill("经期", mode == 1, { mode = 1 })
            }
        }
        Spacer(Modifier.height(16.dp))
        if (data.isEmpty()) {
            Text("至少两次经期才有周期长度。", style = MaterialTheme.typography.bodySmall)
            return@SoftCard
        }
        val maxV = data.maxOf { it.second }.coerceAtLeast(avgLine) * 1.15f
        val anim = remember { Animatable(0f) }
        LaunchedEffect(mode, data.size) { anim.snapTo(0f); anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                data.forEach { (start, v) ->
                    // Bars far from the average stand out in lemon.
                    val color = if (mode == 0 && abs(v - avgLine) > 4) Palette.Lemon else Palette.Rouge
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("$v", fontSize = 11.sp, color = Palette.InkSoft)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.width(10.dp).height((130 * v / maxV * anim.value).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(color),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${start.monthValue}月", fontSize = 10.sp, color = Palette.InkFaint)
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                val chartBottom = size.height - 18.dp.toPx()
                val y = chartBottom - 130.dp.toPx() * avgLine / maxV
                drawLine(
                    Palette.Ink.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("虚线：平均 $avgLine 天", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
    }
}

@Composable
private fun Timeline(cycles: List<CycleRecord>, avg: Int) {
    SoftCard {
        SectionTitle("历次经期")
        Spacer(Modifier.height(8.dp))
        val list = cycles.reversed().take(12)
        val maxLen = (list.maxOfOrNull { it.cycleLength ?: avg } ?: avg).coerceAtLeast(avg)
        list.forEachIndexed { i, c ->
            Column(Modifier.padding(vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(c.start.format(ymd), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (c.cycleLength == null) "本周期 · 经期 ${c.periodLength} 天" else "周期 ${c.cycleLength} 天 · 经期 ${c.periodLength} 天",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val total = c.cycleLength ?: avg
                Box(Modifier.fillMaxWidth(total.toFloat() / maxLen).height(6.dp).clip(CircleShape).background(Palette.PaperDeep)) {
                    Box(
                        Modifier.fillMaxWidth((c.periodLength.toFloat() / total).coerceIn(0.03f, 1f)).fillMaxHeight()
                            .clip(CircleShape).background(Palette.Rouge),
                    )
                }
            }
            if (i != list.lastIndex) Hairline()
        }
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
    SoftCard {
        SectionTitle("近半年") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("身体", mode == 0, { mode = 0 })
                Pill("心情", mode == 1, { mode = 1 })
            }
        }
        Spacer(Modifier.height(12.dp))
        if (counts.isEmpty()) {
            Text("还没有这类记录。", style = MaterialTheme.typography.bodySmall)
        } else {
            val max = counts.maxOf { it.second }
            counts.forEach { (label, n) ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 13.sp, modifier = Modifier.width(76.dp))
                    Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Palette.PaperDeep)) {
                        Box(
                            Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight().clip(CircleShape)
                                .background(if (mode == 0) Palette.Ink else Palette.Lemon),
                        )
                    }
                    Text("$n 天", fontSize = 12.sp, color = Palette.InkSoft, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                }
            }
        }
        if (painDays.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val avgPain = painDays.map { it.pain }.average()
            Text(
                "有 ${painDays.size} 天记了痛经，平均「${Copy.pains[avgPain.roundToInt().coerceIn(0, 4)]}」。" +
                    if (avgPain >= 3) "经常疼得厉害，建议去医院看看。" else "",
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
    SoftCard {
        SectionTitle("体温与体重")
        Text("近 90 天", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        if (temps.size < 2 && weights.size < 2) {
            Text("记两天以上就能看到曲线。基础体温在排卵后会升高，坚持量能帮你看出排卵日。", style = MaterialTheme.typography.bodySmall)
            return@SoftCard
        }
        if (temps.size >= 2) {
            LineChart("基础体温", "℃", temps, Palette.Rouge, minRange = 0.8f)
            Spacer(Modifier.height(18.dp))
        }
        if (weights.size >= 2) LineChart("体重", "kg", weights, Palette.Ink, minRange = 4f)
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
        Numeral("${points.last().second}", 22.sp)
        Text(" $unit", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 3.dp))
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
        points.indices.forEach { i -> val p = pos(i); if (i == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y) }
        drawLine(Palette.Line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        drawPath(line, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, 3.dp.toPx(), pos(points.lastIndex))
    }
    Row {
        Text("${points.first().first.monthValue}.${points.first().first.dayOfMonth}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text("区间 $minV–$maxV", style = MaterialTheme.typography.bodySmall)
    }
}
