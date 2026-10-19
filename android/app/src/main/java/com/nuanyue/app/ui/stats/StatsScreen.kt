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
import com.nuanyue.app.domain.CycleRecord
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.Pill
import com.nuanyue.app.ui.components.SectionTitle
import com.nuanyue.app.ui.components.SoftCard
import com.nuanyue.app.ui.components.StatTile
import com.nuanyue.app.ui.components.darken
import com.nuanyue.app.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

private val ymd = DateTimeFormatter.ofPattern("yyyy.M.d")

@Composable
fun StatsScreen(state: UiState) {
    val snap = state.snapshot
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("周期分析", style = MaterialTheme.typography.headlineMedium)
        Text("了解身体的节律，更好地照顾自己", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        if (!snap.hasData) {
            SoftCard(Modifier.fillMaxWidth()) {
                Text("🌱", fontSize = 40.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))
                Text("还没有数据哦", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("记录 2 次以上经期后，这里会生成专属分析", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            return@Column
        }

        Overview(state)
        Spacer(Modifier.height(16.dp))
        CycleBars(snap.cycles, snap.avgCycle)
        Spacer(Modifier.height(16.dp))
        Timeline(snap.cycles, snap.avgCycle)
        Spacer(Modifier.height(16.dp))
        FrequencyCard(state.records)
        Spacer(Modifier.height(16.dp))
        BodyCurves(state.records, state.today)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Overview(state: UiState) {
    val snap = state.snapshot
    val finished = snap.cycles.mapNotNull { it.cycleLength }
    SoftCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${snap.avgCycle}", "天", "平均周期", Palette.Coral, Modifier.weight(1f))
            StatTile("${snap.avgPeriod}", "天", "平均经期", Palette.Peach, Modifier.weight(1f))
            StatTile("${snap.periods.size}", "次", "已记录", Palette.Lavender, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("周期规律度", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                snap.regularity, color = Palette.CoralDeep, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(Palette.RoseSoft).padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        // Regularity meter: lower deviation → further right.
        val score = snap.cycleStdDev?.let { (1f - (it / 10.0).toFloat()).coerceIn(0.05f, 1f) }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Palette.Line)) {
            if (score != null) {
                Box(Modifier.fillMaxWidth(score).fillMaxHeight().clip(CircleShape).background(Palette.heroGradient))
            }
        }
        Spacer(Modifier.height(8.dp))
        val detail = buildString {
            if (finished.isNotEmpty()) append("最短 ${finished.min()} 天 · 最长 ${finished.max()} 天")
            snap.cycleStdDev?.let { append(" · 波动 ±${"%.1f".format(it)} 天") }
            if (isEmpty()) append("再记录几次经期，就能看到规律度啦")
        }
        Text(detail, style = MaterialTheme.typography.bodySmall)
        if (finished.isNotEmpty() && (finished.max() > 35 || finished.min() < 21)) {
            Spacer(Modifier.height(8.dp))
            Text(
                "💡 正常周期一般在 21–35 天。偶尔波动不用担心，如果长期不规律，建议咨询医生哦。",
                style = MaterialTheme.typography.bodySmall, color = Palette.InkSoft,
            )
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
        SectionTitle(if (mode == 0) "周期长度趋势" else "经期长度趋势")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("周期", mode == 0, { mode = 0 })
            Pill("经期", mode == 1, { mode = 1 }, color = Palette.Peach)
        }
        Spacer(Modifier.height(14.dp))
        if (data.isEmpty()) {
            Text("至少记录 2 次经期才能看到周期趋势哦", style = MaterialTheme.typography.bodySmall)
            return@SoftCard
        }
        val maxV = (data.maxOf { it.second }.coerceAtLeast(avgLine) * 1.15f)
        val anim = remember { Animatable(0f) }
        LaunchedEffect(mode, data.size) { anim.snapTo(0f); anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                data.forEach { (start, v) ->
                    val off = abs(v - avgLine)
                    val brush = when {
                        mode == 1 -> Brush.verticalGradient(listOf(Palette.Peach, Palette.Rose))
                        off <= 2 -> Palette.heroGradient
                        off <= 5 -> Brush.verticalGradient(listOf(Palette.Lemon, Palette.Peach))
                        else -> Palette.lavenderGradient
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("$v", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier.width(16.dp).height((130 * v / maxV * anim.value).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(brush),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${start.monthValue}月", fontSize = 10.sp, color = Palette.InkSoft)
                    }
                }
            }
            // Average guide line.
            Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                val chartBottom = size.height - 18.dp.toPx()
                val y = chartBottom - 130.dp.toPx() * avgLine / maxV
                drawLine(
                    Palette.CoralDeep.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("虚线为平均值 $avgLine 天", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
    }
}

@Composable
private fun Timeline(cycles: List<CycleRecord>, avg: Int) {
    SoftCard {
        SectionTitle("经期历史")
        Spacer(Modifier.height(12.dp))
        val list = cycles.reversed().take(12)
        val maxLen = (list.maxOfOrNull { it.cycleLength ?: avg } ?: avg).coerceAtLeast(avg)
        list.forEachIndexed { i, c ->
            Column(Modifier.padding(vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.start.format(ymd), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        if (c.cycleLength == null) "本周期 · 经期 ${c.periodLength} 天" else "周期 ${c.cycleLength} 天 · 经期 ${c.periodLength} 天",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(6.dp))
                val total = c.cycleLength ?: avg
                Box(Modifier.fillMaxWidth(total.toFloat() / maxLen).height(12.dp).clip(CircleShape).background(Palette.LavenderSoft)) {
                    Box(
                        Modifier.fillMaxWidth((c.periodLength.toFloat() / total).coerceIn(0.03f, 1f)).fillMaxHeight()
                            .clip(CircleShape).background(Brush.horizontalGradient(listOf(Palette.Rose, Palette.Coral))),
                    )
                    if (c.cycleLength == null) {
                        Box(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(4.dp).clip(CircleShape).background(Palette.Lavender))
                    }
                }
            }
            if (i != list.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line.copy(alpha = 0.6f)))
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
        SectionTitle("近半年身体与心情")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("症状", mode == 0, { mode = 0 }, color = Palette.Lavender)
            Pill("心情", mode == 1, { mode = 1 }, color = Palette.Lemon.darken(0.9f))
        }
        Spacer(Modifier.height(12.dp))
        if (counts.isEmpty()) {
            Text("还没有相关记录，在「今日记录」里添加吧", style = MaterialTheme.typography.bodySmall)
        } else {
            val max = counts.maxOf { it.second }
            counts.forEach { (label, n) ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                    Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(Palette.Cream)) {
                        Box(
                            Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight().clip(CircleShape)
                                .background(if (mode == 0) Palette.lavenderGradient else Palette.lemonGradient),
                        )
                    }
                    Text("${n}天", fontSize = 12.sp, color = Palette.InkSoft, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
            }
        }
        if (painDays.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            val avgPain = painDays.map { it.pain }.average()
            Text(
                "近半年有 ${painDays.size} 天记录了痛经，平均程度「${Copy.pains[avgPain.roundToInt().coerceIn(0, 4)]}」" +
                    if (avgPain >= 3) "。疼得厉害时别硬扛，必要时看看医生哦 🤍" else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BodyCurves(records: Map<LocalDate, DayRecordEntity>, today: LocalDate) {
    val since = today.minusDays(90)
    val temps = records.entries.filter { !it.key.isBefore(since) }.mapNotNull { e -> e.value.temperature?.let { e.key to it } }.sortedBy { it.first }
    val weights = records.entries.filter { !it.key.isBefore(since) }.mapNotNull { e -> e.value.weight?.let { e.key to it } }.sortedBy { it.first }
    SoftCard {
        SectionTitle("体温 & 体重（近 90 天）")
        Spacer(Modifier.height(12.dp))
        if (temps.size < 2 && weights.size < 2) {
            Text("记录 2 天以上体温或体重，就能看到变化曲线。坚持测基础体温，可以帮助判断排卵哦～", style = MaterialTheme.typography.bodySmall)
            return@SoftCard
        }
        if (temps.size >= 2) {
            LineChart("基础体温", "℃", temps, Palette.Sky, minRange = 0.8f)
            Spacer(Modifier.height(16.dp))
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
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text("最新 ${points.last().second}$unit · 区间 $minV–$maxV", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val pad = 8.dp.toPx()
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
            if (i == 0) { line.moveTo(p.x, p.y); fill.moveTo(p.x, size.height); fill.lineTo(p.x, p.y) }
            else { line.lineTo(p.x, p.y); fill.lineTo(p.x, p.y) }
        }
        fill.lineTo(pos(points.lastIndex).x, size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        points.indices.forEach { i ->
            drawCircle(Color.White, 4.dp.toPx(), pos(i))
            drawCircle(color, 2.5.dp.toPx(), pos(i))
        }
    }
}
