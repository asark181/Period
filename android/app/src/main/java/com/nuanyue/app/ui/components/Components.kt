package com.nuanyue.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.Serif

/** White card with a hairline edge. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Big serif numeral, e.g. "14". */
@Composable
fun Numeral(text: String, size: TextUnit, color: Color = Palette.Ink, modifier: Modifier = Modifier) {
    Text(text, fontFamily = Serif, fontSize = size, color = color, lineHeight = size, modifier = modifier)
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(if (selected) Palette.Ink else Palette.Card, label = "pill")
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Palette.Ink else Palette.Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) Palette.Paper else Palette.Ink)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    container: Color = Palette.Ink,
    content: Color = Palette.Paper,
) {
    Row(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(if (enabled) container else Palette.Line)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) content else Palette.InkFaint, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = Palette.Ink, bg: Color = Palette.PaperDeep, size: Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** A numeral over a small label; used in rows separated by hairlines. */
@Composable
fun StatTile(value: String, unit: String, label: String, modifier: Modifier = Modifier, accent: Color = Palette.Ink) {
    Column(modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral(value, 34.sp, accent)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(unit, fontSize = 12.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun VerticalHairline(height: Dp = 40.dp) {
    Box(Modifier.width(1.dp).height(height).background(Palette.Line))
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
}

@Composable
fun LegendDot(color: Color, label: String, hollow: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(9.dp).clip(CircleShape)
                .then(if (hollow) Modifier.border(1.2.dp, color, CircleShape) else Modifier.background(color)),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) Palette.Ink else Palette.InkFaint)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        CuteSwitch(checked, enabled, onChange)
    }
}

@Composable
fun CuteSwitch(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Palette.Ink,
            checkedThumbColor = Palette.Paper,
            uncheckedTrackColor = Palette.PaperDeep,
            uncheckedThumbColor = Palette.Card,
            uncheckedBorderColor = Palette.Line,
        ),
    )
}

/** Outlined tag with a small colored dot. */
@Composable
fun TagChip(text: String, color: Color) {
    Row(
        Modifier.clip(CircleShape).border(1.dp, Palette.Line, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Palette.Ink, fontSize = 12.sp)
    }
}

fun Color.darken(f: Float = 0.82f) = Color(red * f, green * f, blue * f, alpha)

fun DayType.color(): Color = when (this) {
    DayType.PERIOD -> Palette.Rouge
    DayType.PREDICTED_PERIOD -> Palette.Rouge
    DayType.FERTILE -> Palette.Lemon
    DayType.OVULATION -> Palette.Ochre
    DayType.SAFE -> Palette.Sage
    DayType.UNKNOWN -> Palette.InkFaint
}

/** The brand mark: an open ring (the cycle) with a lemon dot sitting in the gap (today). */
@Composable
fun BrandMark(modifier: Modifier = Modifier, ring: Color = Palette.Rouge, dot: Color = Palette.Lemon) {
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = size.minDimension * 0.11f
        val r = size.minDimension / 2 - stroke
        val topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r)
        drawArc(
            ring, startAngle = -40f, sweepAngle = 310f, useCenter = false, topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        val a = Math.toRadians(-65.0)
        drawCircle(
            dot, stroke * 0.95f,
            androidx.compose.ui.geometry.Offset(center.x + r * kotlin.math.cos(a).toFloat(), center.y + r * kotlin.math.sin(a).toFloat()),
        )
    }
}
