package com.nuanyue.app.ui.theme

/**
 * Colors for the in-app feedback pill. Kept as plain ARGB longs so unit tests can
 * lock contrast without starting Android.
 *
 * Palette.Ink / Palette.ToastBg must stay the same values. The 1.3.0 snackbar drew
 * Material's default inverseSurface (near #313033) and then painted Palette.Ink on
 * top, because typography bakes a color into bodyMedium and Text prefers that over
 * the snackbar content color. Dark-on-dark. These two numbers are the fix's contract.
 */
object FeedbackColors {
    const val BACKGROUND = 0xFFFFF7F8L
    const val INK = 0xFF4A3540L
    const val ACCENT = 0xFFF2708AL

    /** What Material lightColorScheme uses for inverseSurface when we don't override it. */
    const val MATERIAL_INVERSE_SURFACE = 0xFF313033L
}

fun relativeLuminance(argb: Long): Double {
    fun channel(byte: Long): Double {
        val c = byte / 255.0
        return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    val r = channel((argb shr 16) and 0xFF)
    val g = channel((argb shr 8) and 0xFF)
    val b = channel(argb and 0xFF)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

fun contrastRatio(a: Long, b: Long): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    val hi = maxOf(l1, l2)
    val lo = minOf(l1, l2)
    return (hi + 0.05) / (lo + 0.05)
}

/** Short lines stay long enough to read; long errors get a little more, then stop. */
fun toastHoldMs(text: String): Long = (1800L + text.length * 50L).coerceIn(2400L, 4200L)
