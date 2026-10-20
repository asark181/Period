package com.nuanyue.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.R

/**
 * Cool and quiet, in the direction of a clean period calendar: light grey page,
 * white cards, slate text, rose only for period, saturated orange for fertile.
 *
 * [FeedbackColors] stays the contrast contract for tests. Screen text uses [Ink],
 * which is a dark slate on the light page. Typography bakes that color in, so
 * feedback must not use the default Snackbar. See WarmToast.
 */
object Palette {
    val Bg = Color(0xFFF5F6FA)
    val Panel = Color(0xFFF3F4F8)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFE6E8F0)

    val Ink = Color(0xFF2B3040)
    val InkSoft = Color(0xFF8B93A7)
    val InkFaint = Color(0xFFC5CAD6)

    val ToastBg = Color(0xFFFFFFFF)

    val Pink = Color(0xFFF0708A)
    val PinkDeep = Color(0xFFE25B78)
    val PinkSoft = Color(0xFFFFF0F3)
    val PinkLine = Color(0xFFF0708A)

    val Orange = Color(0xFFF5A623)
    val OrangeSoft = Color(0xFFFFE8C2)

    val Mint = Color(0xFF3DB8A6)
    val MintSoft = Color(0xFFE5F7F4)

    val Sky = Color(0xFF8E9BB5)
    val SkySoft = Color(0xFFEEF2F8)

    val Lemon = Color(0xFFFFD56A)
    val Blush = Color(0xFFFFD9E4)

    /** Flat calendar fills. Numbers on a fill are white. */
    val RoseFill = Color(0xFFF0708A)
    val RoseDeep = Color(0xFFE25B78)
    val RoseInk = Color(0xFFE25B78)
    val PeachFill = Color(0xFFF5A623)
    val PeachDeep = Color(0xFFE09010)
    val PeachInk = Color(0xFFC47A12)
    val Cream = Color(0xFFF5F6FA)
    val Petal = Color(0xFFF0708A)
    val PredictedInk = Color(0xFFF0708A)
    val Lavender = Color(0xFFEEF0F6)
    val LavenderInk = Color(0xFF5C6B8A)
    val LavenderLine = Color(0xFFD5D9E6)
    val Bloom = Color(0xFFF0708A)
    val TodayRing = Color(0xFFF0708A)
    val CalendarWash = Color(0xFFF5F6FA)
    val Sheet = Color(0xFFF3F4F8)
    val Segment = Color(0xFFFFE3B3)
}

/** Flat fills, kept so older call sites do not paint a gradient. */
val PeriodBrush = Brush.linearGradient(listOf(Palette.RoseFill, Palette.RoseFill))
val FertileBrush = Brush.linearGradient(listOf(Palette.PeachFill, Palette.PeachFill))

/** Rounded numerals (Nunito, variable font). Chinese text stays in the system sans. */
@OptIn(ExperimentalTextApi::class)
val Rounded = FontFamily(
    Font(R.font.nunito, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val colors = lightColorScheme(
    primary = Palette.Pink,
    onPrimary = Color.White,
    primaryContainer = Palette.PinkSoft,
    onPrimaryContainer = Palette.PinkDeep,
    secondary = Palette.Orange,
    onSecondary = Color.White,
    secondaryContainer = Palette.OrangeSoft,
    tertiary = Palette.Mint,
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Palette.Card,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Panel,
    onSurfaceVariant = Palette.InkSoft,
    surfaceContainerLow = Palette.Card,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.Card,
    surfaceContainerHighest = Palette.Panel,
    outline = Palette.InkFaint,
    outlineVariant = Palette.Line,
    // Snackbar and other "inverse" surfaces default to near-black. Keep them light
    // even if a future screen forgets to use WarmToast.
    inverseSurface = Color.White,
    inverseOnSurface = Palette.Ink,
    inversePrimary = Palette.PinkDeep,
    surfaceTint = Color.Transparent,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    bodyLarge = TextStyle(fontSize = 15.sp, color = Palette.Ink, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Palette.Ink, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, color = Palette.InkSoft, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

private val shapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun NuanYueTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
