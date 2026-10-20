package com.nuanyue.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nuanyue.app.R
import com.nuanyue.app.ui.components.LocalMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MoonMood { HAPPY, SLEEPY, PERIOD }

/**
 * 「小月」, assembled from vector layers drawn in art/mascot. It breathes, blinks,
 * and bounces with a little burst of hearts when tapped.
 */
@Composable
fun Mascot(modifier: Modifier = Modifier, mood: MoonMood = MoonMood.HAPPY, interactive: Boolean = true, motion: Boolean = true) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val bounce = remember { Animatable(1f) }
    var blinking by remember { mutableStateOf(false) }
    var giggle by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }

    val play = motion && LocalMotion.current
    val breathe: Float
    val zFloat: Float
    if (play) {
        val idle = rememberInfiniteTransition(label = "idle")
        breathe = idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe").value
        zFloat = idle.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "z").value
    } else {
        breathe = 0f
        zFloat = 0f
    }

    LaunchedEffect(mood, play) {
        if (!play) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2600, 5200))
            if (mood == MoonMood.HAPPY) {
                blinking = true
                delay(150)
                blinking = false
            }
        }
    }

    val eyes = when {
        giggle -> R.drawable.mascot_eyes_happy
        mood == MoonMood.SLEEPY -> R.drawable.mascot_eyes_sleep
        mood == MoonMood.PERIOD -> R.drawable.mascot_eyes_happy
        blinking -> R.drawable.mascot_eyes_happy
        else -> R.drawable.mascot_eyes_open
    }
    val mouth = if (giggle || mood == MoonMood.HAPPY) R.drawable.mascot_mouth_smile else R.drawable.mascot_mouth_small

    BoxWithConstraints(
        modifier.then(
            if (interactive) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                burst++
                scope.launch {
                    giggle = true
                    bounce.snapTo(0.82f)
                    bounce.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMediumLow))
                    delay(900)
                    giggle = false
                }
            } else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) {
        val s = maxWidth
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val b = bounce.value
                // squash on press, stretch on the spring's overshoot
                scaleX = (1f + (1f - b) * 0.6f) * (1f + 0.025f * breathe)
                scaleY = b * (1f - 0.02f * breathe)
                translationY = -breathe * s.toPx() * 0.02f
            },
        ) {
            Layer(R.drawable.mascot_body)
            Layer(R.drawable.mascot_cheeks)
            Layer(eyes)
            Layer(mouth)
            if (mood == MoonMood.PERIOD) Layer(R.drawable.mascot_bottle)
        }
        if (mood == MoonMood.SLEEPY && !giggle) {
            Layer(R.drawable.mascot_zzz, Modifier.offset(y = (-zFloat * 6).dp).graphicsLayer { alpha = 0.55f + 0.45f * zFloat })
        }
        if (burst > 0) HeartBurst(burst, Modifier.size(s))
    }
}

@Composable
private fun Layer(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Image(painterResource(id), null, modifier.fillMaxSize())
}
