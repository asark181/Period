package com.nuanyue.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.ui.theme.Palette

/** False when the system animator scale is 0, or a screenshot asks for a still frame. */
val LocalMotion = compositionLocalOf { true }

/**
 * Two pale discs that drift a few pixels. Still when motion is off, so screenshots
 * and reduced-motion stay on the first frame.
 */
@Composable
fun DriftWash(modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val shift = if (motion) {
        val t = rememberInfiniteTransition(label = "wash")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "washX").value
    } else {
        0.35f
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0x33F6D5DE), w * 0.42f, Offset(w * (0.15f + 0.08f * shift), h * 0.2f))
        drawCircle(Color(0x22E4D9F5), w * 0.36f, Offset(w * (0.82f - 0.06f * shift), h * 0.85f))
    }
}

/** Scale that dips while pressed and springs back with a little wobble. */
@Composable
fun pressScale(source: MutableInteractionSource, pressed: Float = 0.9f): Float {
    val isPressed by source.collectIsPressedAsState()
    val motion = LocalMotion.current
    val s by animateFloatAsState(
        if (isPressed) pressed else 1f,
        if (motion) spring(dampingRatio = if (isPressed) 1f else 0.35f, stiffness = Spring.StiffnessMedium) else tween(0),
        label = "press",
    )
    return s
}

/**
 * The big 「月经开始 / 月经结束」 button. Turning on: the fill grows from the centre like ink,
 * the icon spins into a check mark and a burst of hearts flies out.
 */
@Composable
fun PeriodButton(icon: ImageVector, label: String, active: Boolean, tint: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.86f)
    val fill = remember { Animatable(if (active) 1f else 0f) }
    var burst by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(active) {
        if (first) { first = false; fill.snapTo(if (active) 1f else 0f); return@LaunchedEffect }
        if (active) burst++
        fill.animateTo(if (active) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    val labelColor by animateColorAsState(if (active) tint.darken(0.9f) else Palette.InkSoft, label = "label")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
        // fixed-size host so the burst can overflow without affecting layout
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            HeartBurst(burst, Modifier.requiredSize(160.dp))
            Box(
                Modifier.size(64.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow((2 + 8 * fill.value).dp, RoundedCornerShape(22.dp), spotColor = tint.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(2.dp, if (fill.value > 0.01f) tint else Palette.Line, RoundedCornerShape(22.dp))
                    .clickable(source, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                // ink-fill reveal
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.maxDimension * 0.75f * fill.value
                    if (r > 0f) drawCircle(tint, r, center)
                }
                AnimatedContent(
                    active,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), initialScale = 0.3f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.3f) + fadeOut())
                    },
                    label = "icon",
                ) { on ->
                    val spin = remember { Animatable(if (on) -120f else 0f) }
                    LaunchedEffect(Unit) { spin.animateTo(0f, spring(dampingRatio = 0.5f)) }
                    Icon(
                        if (on) Icons.Rounded.Check else icon, label,
                        tint = if (on) Color.White else Palette.InkFaint,
                        modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = spin.value },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, color = labelColor, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/**
 * Segmented control whose highlight slides between options with a spring.
 */
@Composable
fun SlidingSegmented(
    options: List<ImageVector>,
    selected: Int,
    onSelect: (Int) -> Unit,
    itemSize: Dp = 40.dp,
    descriptions: List<String> = emptyList(),
) {
    val haptic = LocalHapticFeedback.current
    val offset by animateDpAsState(itemSize * selected, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "seg")
    Box(
        Modifier.shadow(8.dp, CircleShape, ambientColor = Color(0x1AE59AAB), spotColor = Color(0x28E59AAB)).clip(CircleShape).background(Color.White).padding(5.dp),
    ) {
        Box(Modifier.offset(x = offset).size(itemSize).clip(CircleShape).background(Palette.OrangeSoft))
        Row {
            options.forEachIndexed { i, icon ->
                val tint by animateColorAsState(if (i == selected) Palette.Orange else Palette.InkSoft, label = "segTint")
                val pop by animateFloatAsState(if (i == selected) 1.12f else 1f, spring(dampingRatio = 0.4f), label = "segPop")
                Box(
                    Modifier.size(itemSize).clip(CircleShape)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            if (i != selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, descriptions.getOrNull(i), tint = tint, modifier = Modifier.size(21.dp).graphicsLayer { scaleX = pop; scaleY = pop })
                }
            }
        }
    }
}

/** A planner sticker from res/drawable (st_*). */
@Composable
fun Sticker(@DrawableRes id: Int, modifier: Modifier = Modifier, rotation: Float = 0f) {
    Image(painterResource(id), null, modifier.graphicsLayer { rotationZ = rotation })
}

/** Floats a sticker gently up and down, for decoration. */
@Composable
fun FloatingSticker(@DrawableRes id: Int, modifier: Modifier = Modifier, rotation: Float = 0f, amplitude: Dp = 3.dp, periodMs: Int = 2400, motion: Boolean = true) {
    if (!motion || !LocalMotion.current) {
        Sticker(id, modifier, rotation)
        return
    }
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Image(painterResource(id), null, modifier.graphicsLayer { translationY = y * amplitude.toPx(); rotationZ = rotation + y * 3f })
}

/** Horizontal track with a pill that slides to the selected tab. Used by the bottom bar. */
@Composable
fun SlidingIndicator(count: Int, selected: Int, modifier: Modifier = Modifier, color: Color = Palette.PinkSoft, pillWidth: Dp = 56.dp, pillHeight: Dp = 34.dp) {
    BoxWithConstraints(modifier) {
        val slot = maxWidth / count
        val x by animateDpAsState(slot * selected + (slot - pillWidth) / 2, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "ind")
        Box(Modifier.offset(x = x).width(pillWidth).height(pillHeight).clip(CircleShape).background(color))
    }
}

/** A short bloom of petals after a period is recorded. Stays still when motion is off. */
@Composable
fun PetalBloom(tick: Int, modifier: Modifier = Modifier) {
    if (!LocalMotion.current) return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(680, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    if (t <= 0f || t >= 1f) return
    Canvas(modifier) {
        val colors = listOf(Color(0xFFE7A3B5), Color(0xFFF3C7A8), Color(0xFFD9C6EE))
        repeat(7) { i ->
            val ang = -PI / 2 + i * PI * 2 / 7
            val dist = size.minDimension * 0.42f * t
            val r = size.minDimension * 0.055f * (1f - t * 0.4f)
            drawCircle(
                colors[i % 3].copy(alpha = 1f - t),
                r,
                Offset(center.x + (cos(ang) * dist).toFloat(), center.y + (sin(ang) * dist).toFloat()),
            )
        }
        drawCircle(Color(0xFFF6D5DE).copy(alpha = (1f - t) * 0.55f), size.minDimension * 0.55f * t, style = Stroke(width = 3.dp.toPx()))
    }
}
