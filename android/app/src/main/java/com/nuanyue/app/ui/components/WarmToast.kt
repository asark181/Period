package com.nuanyue.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.ui.theme.FeedbackColors
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.toastHoldMs
import kotlinx.coroutines.delay

private val toastShape = RoundedCornerShape(28.dp)

/**
 * Every transient line in the app (sync, copy, updates, calendar hints) comes through
 * [com.nuanyue.app.ui.AppViewModel.toast] and lands here. Do not swap this back to
 * Material [androidx.compose.material3.Snackbar]: its container is inverseSurface
 * (near-black unless the theme overrides it) and its text uses bodyMedium, whose
 * color is Palette.Ink, so the line was dark on dark.
 */
@Composable
fun WarmMessageHost(
    message: String?,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var display by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        display = text
        delay(toastHoldMs(text))
        onConsumed()
    }
    Box(modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(220)) +
                slideInVertically(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) { it / 2 } +
                scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.92f),
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it / 3 },
        ) {
            WarmToast(message ?: display)
        }
    }
}

@Composable
private fun WarmToast(text: String) {
    Row(
        Modifier
            .widthIn(max = 340.dp)
            .shadow(14.dp, toastShape, ambientColor = Color(0x33F2708A), spotColor = Color(0x40F2708A))
            .clip(toastShape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFF1F4), Color(FeedbackColors.BACKGROUND.toInt()))))
            .border(1.dp, Palette.PinkLine, toastShape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Palette.PinkSoft), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.Pink))
        }
        Spacer(Modifier.width(10.dp))
        AnimatedContent(
            text,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "toast",
        ) { line ->
            Text(
                line,
                color = Color(FeedbackColors.INK.toInt()),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            )
        }
    }
}
