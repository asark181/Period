package com.nuanyue.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.Rounded

private val cardShape = RoundedCornerShape(26.dp)

/** White rounded card with a soft rose shadow. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    brush: Brush = SolidColor(Palette.Card),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val scale = if (onClick != null) pressScale(source, 0.985f) else 1f
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(10.dp, cardShape, ambientColor = Color(0x1AE59AAB), spotColor = Color(0x28E59AAB))
            .clip(cardShape)
            .background(brush)
            .then(
                if (onClick != null) Modifier.clickable(source, indication = null, onClick = onClick) else Modifier,
            )
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 8.dp).size(width = 4.dp, height = 14.dp).clip(CircleShape).background(Palette.Pink))
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Rounded-font numeral. */
@Composable
fun Numeral(
    text: String,
    size: TextUnit,
    color: Color = Palette.Ink,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold,
) {
    Text(text, fontFamily = Rounded, fontWeight = weight, fontSize = size, color = color, lineHeight = size * 1.1f, modifier = modifier)
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Palette.Pink) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.94f)
    val bg by animateColorAsState(if (selected) color.copy(alpha = 0.16f) else Palette.Card, label = "pill")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) color else Palette.Line, CircleShape)
            .clickable(source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = 13.sp, color = if (selected) color.darken(0.85f) else Palette.Ink,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
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
    container: Color = Palette.Pink,
    content: Color = Color.White,
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.95f)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(height)
            .clip(CircleShape)
            .background(if (enabled) container else Palette.Line)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) content else Palette.InkFaint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = Palette.Pink, bg: Color = Palette.PinkSoft, size: Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Big rounded-square action like 「月经开始」 with a label underneath. */
@Composable
fun ActionTile(icon: ImageVector, label: String, tint: Color, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier.size(60.dp)
                .shadow(if (active) 6.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = tint.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Card)
                .border(2.dp, if (active) tint else Palette.Line, RoundedCornerShape(20.dp))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (active) tint else Palette.InkFaint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label, fontSize = 13.sp, color = if (active) tint.darken(0.9f) else Palette.InkSoft,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun StatTile(value: String, unit: String, label: String, modifier: Modifier = Modifier, accent: Color = Palette.Pink) {
    Column(modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral(value, 22.sp, accent)
            if (unit.isNotEmpty()) Text(unit, fontSize = 12.sp, color = accent, modifier = Modifier.padding(start = 1.dp, bottom = 3.dp))
        }
    }
}

@Composable
fun VerticalHairline(height: Dp = 36.dp) {
    Box(Modifier.width(1.dp).height(height).background(Palette.Line))
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
}

@Composable
fun LegendDot(color: Color, label: String, hollow: Boolean = false, heart: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (heart) {
            Icon(if (hollow) Icons.Rounded.FavoriteBorder else Icons.Rounded.Favorite, null, tint = color, modifier = Modifier.size(12.dp))
        } else {
            Box(
                Modifier.size(10.dp).clip(RoundedCornerShape(3.dp))
                    .then(if (hollow) Modifier.border(1.2.dp, color, RoundedCornerShape(3.dp)) else Modifier.background(color)),
            )
        }
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
            checkedTrackColor = Palette.Pink,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = Palette.Line,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun TagChip(text: String, color: Color) {
    Text(
        text, color = color.darken(0.8f), fontSize = 12.sp,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

fun Color.darken(f: Float = 0.82f) = Color(red * f, green * f, blue * f, alpha)

fun DayType.color(): Color = when (this) {
    DayType.PERIOD -> Palette.Pink
    DayType.PREDICTED_PERIOD -> Palette.PinkLine
    DayType.FERTILE -> Palette.Orange
    DayType.OVULATION -> Palette.Orange
    DayType.SAFE -> Palette.Mint
    DayType.UNKNOWN -> Palette.InkFaint
}

/**
 * One date chip: filled rounded square for period and fertile days, pink outline for a
 * predicted period, a heart for ovulation, and a double pink ring for today.
 */
@Composable
fun DateChip(
    label: String,
    type: DayType,
    isToday: Boolean,
    isSelected: Boolean,
    size: Dp,
    hasRecord: Boolean = false,
    muted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(size * 0.3f)
    val filled = type == DayType.PERIOD || type == DayType.FERTILE || type == DayType.OVULATION
    val bg = when (type) {
        DayType.PERIOD -> Palette.Pink
        DayType.FERTILE, DayType.OVULATION -> Palette.Orange
        DayType.PREDICTED_PERIOD -> Color.White
        else -> Color.Transparent
    }
    val fg = if (filled) Color.White else if (muted) Palette.InkSoft else Palette.Ink
    Box(
        Modifier.size(size)
            .then(if (isSelected) Modifier.border(2.dp, Palette.Sky, RoundedCornerShape(size * 0.34f)) else Modifier)
            .padding(if (isSelected) 3.dp else 0.dp)
            .clip(shape)
            .background(bg)
            .then(if (type == DayType.PREDICTED_PERIOD || (isToday && !filled)) Modifier.border(1.5.dp, Palette.PinkLine, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (isToday && !filled) {
            Box(Modifier.fillMaxSize().padding(3.dp).border(1.dp, Palette.PinkLine, RoundedCornerShape(size * 0.22f)))
        }
        if (type == DayType.OVULATION && !isToday) {
            Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
            Text(label, fontFamily = Rounded, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.26f).sp, color = Palette.Orange)
        } else {
            Text(
                if (isToday) "今" else label,
                fontFamily = Rounded, fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp, color = fg,
            )
        }
        if (hasRecord) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = size * 0.07f).size(4.dp).clip(CircleShape)
                    .background(if (filled) Color.White else Palette.Mint),
            )
        }
    }
}
