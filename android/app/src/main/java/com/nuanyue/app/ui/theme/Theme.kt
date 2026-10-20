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
 * Warm light theme. [NyColors] in Tokens.kt is the source of truth; these names
 * stay so existing screens keep compiling. Period fills are [RoseFill], button
 * fills are [Pink], rose text is [PinkDeep] / [RoseInk]. [NyColors.Rose] is for
 * graphics only.
 *
 * [FeedbackColors] stays the contrast contract for the toast pill. Typography
 * bakes [Ink] into text styles, so feedback must not use the default Snackbar.
 * See WarmToast. inverse surfaces stay light for that reason, which differs from
 * the spec's inverseSurface = Ink.
 */
object Palette {
    val Bg = NyColors.Bg
    val Panel = NyColors.SurfaceSunken
    val Card = NyColors.Surface
    val Line = NyColors.Hairline

    val Ink = NyColors.Ink
    val InkSoft = NyColors.InkSecondary
    val InkFaint = NyColors.InkTertiary

    val ToastBg = NyColors.Surface

    val Pink = NyColors.PrimaryButton
    val PinkDeep = NyColors.RoseStrong
    val PinkSoft = NyColors.RoseContainer
    val PinkLine = NyColors.PredictedOutline

    val Orange = NyColors.Ovulation
    val OrangeSoft = NyColors.Fertile

    val Mint = NyColors.Rose
    val MintSoft = NyColors.RoseContainer

    val Sky = NyColors.InkSecondary
    val SkySoft = NyColors.SurfaceSunken

    val Lemon = NyColors.RoseContainer
    val Blush = NyColors.RoseContainer

    /** Period cells are a light rose with ink numbers, not a solid button red. */
    val RoseFill = NyColors.PeriodFill
    val RoseDeep = NyColors.RoseStrong
    val RoseInk = NyColors.RoseStrong
    val PeachFill = NyColors.Fertile
    val PeachDeep = NyColors.Ovulation
    val PeachInk = NyColors.Ink
    val Cream = NyColors.Bg
    val Petal = NyColors.RoseOutline
    val PredictedInk = NyColors.PredictedOutline
    val Lavender = NyColors.SurfaceSunken
    val LavenderInk = NyColors.InkSecondary
    val LavenderLine = NyColors.Hairline
    val Bloom = NyColors.Ovulation
    val TodayRing = NyColors.TodayRing
    val CalendarWash = NyColors.Bg
    val Sheet = NyColors.SurfaceSunken
    val Segment = NyColors.RoseContainer
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
    primary = NyColors.PrimaryButton,
    onPrimary = NyColors.OnPrimaryButton,
    primaryContainer = NyColors.RoseContainer,
    onPrimaryContainer = NyColors.RoseStrong,
    inversePrimary = NyColors.Rose,
    secondary = NyColors.Ovulation,
    onSecondary = NyColors.Ink,
    secondaryContainer = NyColors.Fertile,
    onSecondaryContainer = NyColors.Ink,
    tertiary = NyColors.Weekend,
    onTertiary = NyColors.OnPrimaryButton,
    background = NyColors.Bg,
    onBackground = NyColors.Ink,
    surface = NyColors.Bg,
    onSurface = NyColors.Ink,
    surfaceVariant = NyColors.SurfaceSunken,
    onSurfaceVariant = NyColors.InkSecondary,
    surfaceTint = Color.Transparent,
    surfaceBright = NyColors.Surface,
    surfaceDim = NyColors.SurfaceSunken,
    surfaceContainerLowest = NyColors.Surface,
    surfaceContainerLow = NyColors.Bg,
    surfaceContainer = NyColors.SurfaceSunken,
    surfaceContainerHigh = NyColors.SurfaceSunken,
    surfaceContainerHighest = NyColors.BgWash,
    outline = NyColors.RoseOutline,
    outlineVariant = NyColors.Hairline,
    scrim = Color(0x524A3540),
    // Spec maps inverseSurface to Ink. Typography bakes Ink into body text, so a
    // Material snackbar on that dark surface is dark-on-dark. Keep the inverse
    // pair light; toasts go through WarmToast.
    inverseSurface = Color.White,
    inverseOnSurface = NyColors.Ink,
    error = Color(0xFFBA1A1A),
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
