// 暖月 设计 Token 提案 v0.1（2026-10-20，待确认）
// 来源：lemon-spec.md —— 学柠檬小月的克制与节奏，色彩基于 暖月 1.6.0 实测。仅浅色模式。
package com.nuanyue.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NyColors {
    // 底色
    val Bg            = Color(0xFFFFF9F8) // 页面暖白底（1.6.0 #FFF6F7 微调）
    val BgWash        = Color(0xFFFFF0F2) // 顶部 160dp 晕染起点 → Bg
    val Surface       = Color(0xFFFFFFFF) // 卡片 / 底栏 / 工具胶囊
    val SurfaceSunken = Color(0xFFFBF3F2) // 底部抽屉、分组背景（柠檬 #F6F7FC 的暖版）
    val Hairline      = Color(0xFFF3E6E8) // 分割线、1dp 描边

    // 文字
    val Ink           = Color(0xFF4A3540) // 主文字 10.8:1
    val InkSecondary  = Color(0xFF7B6571) // 次文字：Bg 5.11 / Surface 5.33 / Sunken 4.87 / BgWash 4.82 / RoseContainer 4.57 / Fertile 4.79（全部 ≥4.5:1；2026-10-20 由 #8C7682 4.0:1 加深）
    val InkTertiary   = Color(0xFFBCA9B2) // 未选图标 / 非本月日期 / 禁用（非关键文字）
    val TabUnselected = Color(0xFF876E7C) // 未选 tab 文字：Surface 4.61 ✓ / Bg 4.43（v0.5，替换 #9C8692 3.37）
    val Weekend       = Color(0xFFB64C67) // 六 / 日 表头：Bg 4.78 ✓ / Surface 4.98 / BgWash 4.50 / Sunken 4.55（v0.5，替换 #C2697F 3.59）

    // 品牌玫瑰（v0.4：拆分为 经期填充 / 主按钮 / 品牌图标 三个用途）
    val Rose          = Color(0xFFEC6482) // 品牌玫瑰：仅非文字图形——选中 tab 图标、吉祥物线稿、开关轨道、选中卡片描边。不得承载文字
    val PeriodFill    = Color(0xFFF39BAE) // 经期日色块（v0.4 方案 B，替换实心 #EC6482）；年视图/周期环经期弧/图例同色
    val OnPeriod      = Color(0xFF4A3540) // 经期日数字 = Ink，SemiBold：5.39:1 ✓（白字/#EC6482 仅 3.13 已弃用）
    val PrimaryButton = Color(0xFFD04362) // 主按钮底（月经来了 / 确认 / 继续）、日期选择器选中块：白字 4.505:1 ✓
    val OnPrimaryButton = Color(0xFFFFFFFF)
    val OnRose        = OnPrimaryButton   // 兼容旧名
    val RoseStrong    = Color(0xFFB93A5B) // 强调文字（链接、徽章、“16”、补记其他日期、立即更新）：Bg 5.28 / Surface 5.50 / Sunken 5.03 / BgWash 4.97 / RoseContainer 4.72 / Fertile 4.94（v0.3，替换 #D44A6B 4.04）
    val RoseContainer = Color(0xFFFFE8ED) // 选中气泡、徽章底、工具胶囊选中
    val RoseOutline   = Color(0xFFF4A8B8) // 通用浅玫瑰描边（描边按钮等）
    val PredictedOutline = Color(0xFFEB8399) // 预测经期：1.5dp **虚线**描边、无填充（dash 4dp / gap 3dp）；比 #F4A8B8 深一档以便与浅色经期块区分（对 Bg 2.45:1）

    // 易孕 / 排卵（刻意弱于经期）
    val Fertile       = Color(0xFFFFF1DC) // 易孕浅杏底，数字仍用 Ink
    val Ovulation     = Color(0xFFF0A23A) // 排卵 5dp 圆点

    // 今天
    val TodayRing     = Color(0xFF4A3540) // 1.5dp 外环，偏移 2dp

    val TopWash = Brush.verticalGradient(listOf(BgWash, Bg)) // 仅用于顶部 160dp
}

/*
 * ── Material3 ColorScheme 映射（直接替换现有 ui/theme/Color.kt + Theme.kt 中 lightColorScheme(...) 的取值，
 *    不要另建平行主题；仅浅色模式，darkColorScheme 不提供） ──
 *
 *  lightColorScheme(
 *      primary              = NyColors.PrimaryButton,  // #D04362 主按钮（Button 默认 containerColor），白字 4.505:1
 *      onPrimary            = NyColors.OnPrimaryButton,// #FFFFFF
 *      // 选中 tab 图标 / 吉祥物 / 开关轨道：显式使用 NyColors.Rose #EC6482（非文字图形）；经期色块显式使用 NyColors.PeriodFill，均不走 primary
 *      primaryContainer     = NyColors.RoseContainer,  // #FFE8ED 选中气泡/凸起圆、徽章底、工具胶囊选中
 *      onPrimaryContainer   = NyColors.RoseStrong,     // #B93A5B 徽章文字、链接、强调文字（≥4.5:1）
 *      inversePrimary       = NyColors.Rose,           // #EC6482
 *      secondary            = NyColors.Ovulation,      // #F0A23A 排卵点（仅状态用，不用于按钮）
 *      onSecondary          = NyColors.Ink,            // #4A3540
 *      secondaryContainer   = NyColors.Fertile,        // #FFF1DC 易孕浅底
 *      onSecondaryContainer = NyColors.Ink,            // #4A3540 易孕日数字
 *      tertiary             = NyColors.Weekend,        // #B64C67 六/日 表头
 *      onTertiary           = NyColors.OnRose,         // #FFFFFF
 *      background           = NyColors.Bg,             // #FFF9F8
 *      onBackground         = NyColors.Ink,            // #4A3540
 *      surface              = NyColors.Bg,             // #FFF9F8（Scaffold/页面）；卡片用 surfaceContainerLowest
 *      onSurface            = NyColors.Ink,            // #4A3540
 *      surfaceVariant       = NyColors.SurfaceSunken,  // #FBF3F2
 *      onSurfaceVariant     = NyColors.InkSecondary,   // #7B6571 次文字（≥4.5:1）
 *      surfaceTint          = Color.Transparent,       // 关闭 M3 tonal elevation 着色，保持暖白
 *      surfaceBright        = NyColors.Surface,        // #FFFFFF
 *      surfaceDim           = NyColors.SurfaceSunken,  // #FBF3F2
 *      surfaceContainerLowest  = NyColors.Surface,     // #FFFFFF 卡片、底栏、工具胶囊
 *      surfaceContainerLow     = NyColors.Bg,          // #FFF9F8
 *      surfaceContainer        = NyColors.SurfaceSunken, // #FBF3F2 底部抽屉、分组底
 *      surfaceContainerHigh    = NyColors.SurfaceSunken, // #FBF3F2
 *      surfaceContainerHighest = NyColors.BgWash,      // #FFF0F2
 *      outline              = NyColors.RoseOutline,    // #F4A8B8 预测描边、描边按钮
 *      outlineVariant       = NyColors.Hairline,       // #F3E6E8 分割线、卡片 1dp 发丝线
 *      scrim                = Color(0x524A3540),       // 暖墨 32% 遮罩（底部抽屉）
 *      inverseSurface       = NyColors.Ink,            // #4A3540 Snackbar
 *      inverseOnSurface     = NyColors.Bg,             // #FFF9F8
 *      error                = Color(0xFFBA1A1A),       // 保持 M3 默认
 *  )
 *
 *  不进 ColorScheme、直接引用 NyColors 的：InkTertiary #BCA9B2（未选 tab 图标/禁用/非本月日期，非正文）、
 *  TabUnselected #876E7C、TodayRing #4A3540、TopWash 渐变。
 *
 *  对比度（WCAG 2.x）：Ink 10.75/Bg · 11.20/Surface · 10.25/Sunken · 9.61/RoseContainer · 10.07/Fertile；
 *  InkSecondary #7B6571 5.11/Bg · 5.33/Surface · 4.87/Sunken · 4.82/BgWash · 4.57/RoseContainer · 4.79/Fertile；
 *  RoseStrong #B93A5B 5.28/Bg · 5.50/Surface · 5.03/Sunken · 4.97/BgWash · 4.72/RoseContainer · 4.94/Fertile；Weekend #B64C67 4.78/Bg · 4.98/Surface · 4.50/BgWash；
 *  TabUnselected #876E7C 4.61/Surface · 4.43/Bg（仅用于白色底栏）；InkTertiary 2.13/Bg（仅装饰/禁用）。
 *  v0.4 已定（负责人选方案 B）：经期日 = PeriodFill #F39BAE + Ink 字 5.39:1；主按钮 = #D04362 + 白字 4.505:1；
 *    预测经期 = PredictedOutline #EB8399 1.5dp 虚线、无填充。
 *  规则：#EC6482 不承载任何文字；玫瑰色文字一律 RoseStrong #B93A5B；含文字的玫瑰色块用 PeriodFill(+Ink) 或 PrimaryButton(+白)。
 *  v0.5：Weekend → #B64C67、TabUnselected → #876E7C，所有正文文字对其实际底色均 ≥4.5:1（InkTertiary 仅用于图标/禁用/非本月日期）。
 *  ⚠ 桌面小组件（RemoteViews）无法读取本文件，使用 spec/colors.xml（ny_* 资源）；两者必须保持同值，改一处须同步另一处。
 */

object NySpace {
    val unit     = 4.dp
    val xs       = 4.dp
    val s        = 8.dp
    val m        = 12.dp
    val l        = 16.dp
    val xl       = 24.dp
    val xxl      = 32.dp
    val xxxl     = 48.dp

    val shell    = 16.dp // 外壳边距：工具胶囊、问候区、主按钮
    val content  = 24.dp // 内容对齐线：标题/日期行/卡片文字统一左缘
    val grid     = 12.dp // 日历左右边距；列宽 = (屏宽 - 24dp) / 7

    val dayChip        = 36.dp // 日期色块（方形）
    val dayChipCompact = 32.dp // 360×640dp 等紧凑屏
    val dayRow         = 48.dp // 日历行高
    val dayRowCompact  = 40.dp
    val todayRingWidth = 1.5.dp
    val todayRingGap   = 2.dp
    val predictedStroke = 1.5.dp // 虚线
    val predictedDash   = 4.dp
    val predictedGap    = 3.dp
    val ovulationDot   = 5.dp

    val listRow      = 56.dp
    val cardPadH     = 16.dp
    val cardPadV     = 14.dp
    val cardGap      = 16.dp
    val toolbarPill  = 48.dp // 工具胶囊高度
    val toolbarDot   = 40.dp // 胶囊内选中圆
    val buttonHeight = 52.dp // 主按钮
    val bottomBar    = 64.dp // + 系统手势区 inset
    val tabBubbleW   = 64.dp
    val tabBubbleH   = 32.dp
    val icon         = 22.dp
    val mascot       = 40.dp // 小月
    val hairline     = 1.dp
}

object NyRadius {
    val chip       = RoundedCornerShape(12.dp)
    val iconButton = RoundedCornerShape(14.dp)
    val card       = RoundedCornerShape(20.dp)
    val sheetTop   = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val pill       = RoundedCornerShape(percent = 50) // 主按钮、工具胶囊、徽章、tab 气泡
}

object NyType {
    // 系统中文字体（Noto Sans SC / 思源黑体）
    val displayMonth  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp) // “10”；“月”用 monthUnit
    val monthUnit     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp) // 颜色 InkSecondary
    val titleGreeting = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium,   lineHeight = 28.sp)
    val bodyCta       = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium,   lineHeight = 22.sp)
    val body          = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal,   lineHeight = 22.sp)
    val dayNumber     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium,   lineHeight = 20.sp) // 经期日用 SemiBold + OnPeriod(Ink)
    val label         = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal,   lineHeight = 18.sp)
    val caption       = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp) // 星期表头、徽章
    val micro         = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal,   lineHeight = 14.sp) // 图例、tab（选中只加粗不放大）
}

/**
 * ⚠ 职责边界：DayKind 只是把 CycleEngine 的计算结果映射为视觉。
 *   UI 层不得自行推算经期 / 预测 / 易孕 / 排卵状态（不做日期加减、不复制周期算法），
 *   一律以 CycleEngine 输出为唯一事实来源（source of truth）；dayVisual() 只做 kind → 颜色/字重 的查表。
 */
/** 日历格子状态 → 视觉（强弱：经期实心 > 易孕浅底 > 预测描边；今天外环可叠加任意状态） */
enum class DayKind { Normal, Period, Predicted, Fertile, Ovulation, OutOfMonth }

data class DayVisual(val fill: Color?, val stroke: Color?, val text: Color, val weight: FontWeight, val dot: Color? = null, val dashed: Boolean = false)

fun dayVisual(kind: DayKind): DayVisual = when (kind) {
    DayKind.Normal     -> DayVisual(null, null, NyColors.Ink, FontWeight.Medium)
    DayKind.Period     -> DayVisual(NyColors.PeriodFill, null, NyColors.OnPeriod, FontWeight.SemiBold)
    DayKind.Predicted  -> DayVisual(null, NyColors.PredictedOutline, NyColors.Ink, FontWeight.Medium, dashed = true) // 1.5dp 虚线：PathEffect.dashPathEffect(floatArrayOf(4dp, 3dp))
    DayKind.Fertile    -> DayVisual(NyColors.Fertile, null, NyColors.Ink, FontWeight.Medium)
    DayKind.Ovulation  -> DayVisual(NyColors.Fertile, null, NyColors.Ink, FontWeight.Medium, dot = NyColors.Ovulation)
    DayKind.OutOfMonth -> DayVisual(null, null, NyColors.InkTertiary, FontWeight.Normal)
}
