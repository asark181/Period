package com.nuanyue.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuanyue.app.R

/**
 * Two accents on warm paper: rouge for the period, lemon for the fertile window.
 * Everything is flat — no gradients, no glow.
 */
object Palette {
    val Paper = Color(0xFFFAF6F0)
    val PaperDeep = Color(0xFFF1EAE0)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFEAE2D8)

    val Ink = Color(0xFF2A2421)
    val InkSoft = Color(0xFF7A6E68)
    val InkFaint = Color(0xFFBDB2AA)

    val Rouge = Color(0xFFD9475A)
    val RougeSoft = Color(0xFFF8E1E1)
    val Lemon = Color(0xFFF3C84B)
    val LemonSoft = Color(0xFFFBF0C8)
    val Ochre = Color(0xFF9A6B12)
    val Sage = Color(0xFFDCE5D3)
    val SageInk = Color(0xFF4F6346)
    val Sky = Color(0xFF5C86A8)
}

/** Serif used only for numerals and the wordmark. Chinese text stays in the system sans. */
val Serif = FontFamily(Font(R.font.instrument_serif))

private val colors = lightColorScheme(
    primary = Palette.Ink,
    onPrimary = Palette.Paper,
    primaryContainer = Palette.RougeSoft,
    onPrimaryContainer = Palette.Rouge,
    secondary = Palette.Lemon,
    onSecondary = Palette.Ink,
    secondaryContainer = Palette.LemonSoft,
    tertiary = Palette.Rouge,
    background = Palette.Paper,
    onBackground = Palette.Ink,
    surface = Palette.Card,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.PaperDeep,
    onSurfaceVariant = Palette.InkSoft,
    surfaceContainerLow = Palette.Card,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.Card,
    surfaceContainerHighest = Palette.PaperDeep,
    outline = Palette.InkFaint,
    outlineVariant = Palette.Line,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    bodyLarge = TextStyle(fontSize = 15.sp, color = Palette.Ink, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Palette.Ink, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, color = Palette.InkSoft, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun NuanYueTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
