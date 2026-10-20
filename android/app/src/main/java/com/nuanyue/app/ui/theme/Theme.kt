package com.nuanyue.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
 * Soft and warm, in the 柠檬小月 direction: blush paper, coral pink for the period,
 * warm orange for the fertile window, mint for sync, pale sky for weekday labels.
 *
 * [Ink] and [ToastBg] match [FeedbackColors]. Text styles below bake a color in on
 * purpose (captions stay softer). Compose Text then ignores LocalContentColor, which
 * is why feedback must not use the default Snackbar. See WarmToast.
 */
object Palette {
    val Bg = Color(0xFFFFF6F7)
    val Panel = Color(0xFFFFF0F3)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFF3E3E8)

    val Ink = Color(FeedbackColors.INK.toInt())
    val InkSoft = Color(0xFF9A8490)
    val InkFaint = Color(0xFFD5C4CC)

    val ToastBg = Color(FeedbackColors.BACKGROUND.toInt())

    val Pink = Color(FeedbackColors.ACCENT.toInt())
    val PinkDeep = Color(0xFFE25373)
    val PinkSoft = Color(0xFFFFE8ED)
    val PinkLine = Color(0xFFF0B8C3)

    val Orange = Color(0xFFF0B46A)
    val OrangeSoft = Color(0xFFFFF1D9)

    val Mint = Color(0xFF4FC6B0)
    val MintSoft = Color(0xFFDDF5F0)

    val Sky = Color(0xFF6E8CC6)
    val SkySoft = Color(0xFFE9F0FC)

    val Lemon = Color(0xFFFFD86B)
    val Blush = Color(0xFFFFB3C1)
}

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
    inverseSurface = Palette.ToastBg,
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
