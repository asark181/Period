package com.nuanyue.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Coral = Color(0xFFFF7A93)
    val CoralDeep = Color(0xFFF25C7A)
    val Rose = Color(0xFFFF6B8B)
    val RoseSoft = Color(0xFFFFE4EA)
    val RoseMist = Color(0xFFFFF1F4)
    val PredictedRose = Color(0xFFFFA3B8)
    val Lemon = Color(0xFFFFD45E)
    val LemonSoft = Color(0xFFFFF5D1)
    val Peach = Color(0xFFFFB38A)
    val PeachSoft = Color(0xFFFFEDE2)
    val Lavender = Color(0xFFB08CFF)
    val LavenderSoft = Color(0xFFF1EAFF)
    val Ovulation = Color(0xFF9466FF)
    val Mint = Color(0xFF5CC8A0)
    val MintSoft = Color(0xFFE2F7EF)
    val Sky = Color(0xFF6FB7FF)
    val Cream = Color(0xFFFFFAF6)
    val Ink = Color(0xFF4A3940)
    val InkSoft = Color(0xFF8C7A80)
    val InkFaint = Color(0xFFC4B6BB)
    val Line = Color(0xFFF3E6E9)
    val Card = Color.White

    val heroGradient = Brush.linearGradient(listOf(Color(0xFFFF8FA6), Color(0xFFFFA98F), Color(0xFFFFCB70)))
    val backgroundGradient = Brush.verticalGradient(listOf(Color(0xFFFFEEF2), Color(0xFFFFF7EE), Cream))
    val lemonGradient = Brush.linearGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFC56B)))
    val lavenderGradient = Brush.linearGradient(listOf(Color(0xFFC7A6FF), Color(0xFF9E7BFF)))
}

private val colors = lightColorScheme(
    primary = Palette.Coral,
    onPrimary = Color.White,
    primaryContainer = Palette.RoseSoft,
    onPrimaryContainer = Palette.CoralDeep,
    secondary = Palette.Lemon,
    onSecondary = Palette.Ink,
    secondaryContainer = Palette.LemonSoft,
    tertiary = Palette.Lavender,
    tertiaryContainer = Palette.LavenderSoft,
    background = Palette.Cream,
    onBackground = Palette.Ink,
    surface = Color.White,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.RoseMist,
    onSurfaceVariant = Palette.InkSoft,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Palette.RoseMist,
    outline = Palette.InkFaint,
    outlineVariant = Palette.Line,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
    bodyLarge = TextStyle(fontSize = 15.sp, color = Palette.Ink, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Palette.Ink, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, color = Palette.InkSoft, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun NuanYueTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
