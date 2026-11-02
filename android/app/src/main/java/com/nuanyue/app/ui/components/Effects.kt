package com.nuanyue.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.nuanyue.app.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(val angle: Float, val distance: Float, val size: Float, val spin: Float, val color: Color, val heart: Boolean)

/**
 * A one-shot burst of little hearts and dots flying outward. Changing [trigger] fires it again.
 * Drawn on a Canvas so it costs nothing when idle.
 */
@Composable
fun HeartBurst(trigger: Int, modifier: Modifier = Modifier, colors: List<Color> = listOf(Palette.Pink, Palette.Orange, Palette.PinkLine, Palette.Lemon)) {
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val rnd = Random(trigger * 7919)
        List(14) { i ->
            Particle(
                angle = (i / 14f) * 360f + rnd.nextFloat() * 18f,
                distance = 0.55f + rnd.nextFloat() * 0.45f,
                size = 0.06f + rnd.nextFloat() * 0.05f,
                spin = rnd.nextFloat() * 90f - 45f,
                color = colors[i % colors.size],
                heart = i % 2 == 0,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val r = size.minDimension / 2
        particles.forEach { pt ->
            val a = Math.toRadians(pt.angle.toDouble())
            val d = r * pt.distance * p
            val c = Offset(center.x + (d * cos(a)).toFloat(), center.y + (d * sin(a)).toFloat() - r * 0.25f * p * p)
            val alpha = (1f - p) * (1f - p)
            val s = r * pt.size * (1f - 0.4f * p)
            if (pt.heart) rotate(pt.spin * p, c) { heart(c, s, pt.color.copy(alpha = alpha)) }
            else drawCircle(pt.color.copy(alpha = alpha), s * 0.55f, c)
        }
    }
}

private fun DrawScope.heart(c: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y + s * 0.9f)
        cubicTo(c.x - s * 1.4f, c.y - s * 0.1f, c.x - s * 0.7f, c.y - s * 1.2f, c.x, c.y - s * 0.45f)
        cubicTo(c.x + s * 0.7f, c.y - s * 1.2f, c.x + s * 1.4f, c.y - s * 0.1f, c.x, c.y + s * 0.9f)
        close()
    }
    drawPath(path, color)
}
