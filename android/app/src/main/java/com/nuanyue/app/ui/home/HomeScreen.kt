package com.nuanyue.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuanyue.app.R
import com.nuanyue.app.data.PhraseEntity
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.DayInfo
import com.nuanyue.app.domain.DayType
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.ui.AppViewModel
import com.nuanyue.app.ui.UiState
import com.nuanyue.app.ui.components.DateChip
import com.nuanyue.app.ui.components.DriftWash
import com.nuanyue.app.ui.components.LocalMotion
import com.nuanyue.app.ui.components.PetalBloom
import com.nuanyue.app.ui.components.FloatingSticker
import com.nuanyue.app.ui.components.LegendDot
import com.nuanyue.app.ui.components.Mascot
import com.nuanyue.app.ui.components.MoonMood
import com.nuanyue.app.ui.components.Numeral
import com.nuanyue.app.ui.components.PrimaryButton
import com.nuanyue.app.ui.components.SlidingSegmented
import com.nuanyue.app.ui.components.Sticker
import com.nuanyue.app.ui.components.pressScale
import com.nuanyue.app.ui.theme.FertileBrush
import com.nuanyue.app.ui.theme.Palette
import com.nuanyue.app.ui.theme.PeriodBrush
import com.nuanyue.app.ui.theme.Rounded
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val MONTHS_BACK = 120
private const val MONTHS_FORWARD = 24
internal const val HOME_MODE_MONTH = 0
internal const val HOME_MODE_YEAR = 1
internal const val HOME_MODE_RING = 2

private fun pageOf(base: YearMonth, target: YearMonth) =
    (MONTHS_BACK + ChronoUnit.MONTHS.between(base, target).toInt()).coerceIn(0, MONTHS_BACK + MONTHS_FORWARD)

@Composable
fun HomeScreen(state: UiState, vm: AppViewModel) {
    val phrase by vm.phrase.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    HomeContent(
        state = state,
        phrase = phrase,
        onNextPhrase = { scope.launch { vm.nextPhrase() } },
        onToast = vm::toast,
        onMarkStart = vm::markStart,
        onUnmarkStart = vm::unmarkStart,
        onMarkEnd = vm::markEnd,
        onUnmarkEnd = vm::unmarkEnd,
    )
}

/**
 * Home tab that fits one screen: header, the current view, and the day actions
 * are measured so the calendar (or year / ring) is the only part that shrinks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeContent(
    state: UiState,
    phrase: PhraseEntity?,
    onNextPhrase: () -> Unit,
    onToast: (String) -> Unit,
    onMarkStart: (LocalDate) -> Unit,
    onUnmarkStart: (LocalDate) -> Unit,
    onMarkEnd: (LocalDate) -> Unit,
    onUnmarkEnd: (LocalDate) -> Unit,
    now: LocalTime = LocalTime.now(),
    motion: Boolean = true,
    initialMode: Int = HOME_MODE_MONTH,
) {
    var mode by rememberSaveable { mutableIntStateOf(initialMode) }
    var selectedEpoch by rememberSaveable { mutableStateOf(state.today.toEpochDay()) }
    var picker by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val today = state.today
    val base = YearMonth.from(today)
    val pager = rememberPagerState(initialPage = MONTHS_BACK) { MONTHS_BACK + MONTHS_FORWARD + 1 }
    val scope = rememberCoroutineScope()
    val from = base.minusMonths(MONTHS_BACK.toLong()).atDay(1).minusDays(7)
    val to = base.plusMonths(MONTHS_FORWARD.toLong()).atEndOfMonth().plusDays(60)
    val infos = remember(state.snapshot, today) { CycleEngine.dayInfos(state.snapshot, from, to, today) }
    var yearShown by rememberSaveable { mutableIntStateOf(today.year) }

    fun jumpTo(ym: YearMonth, select: LocalDate? = null) {
        mode = HOME_MODE_MONTH
        scope.launch { pager.animateScrollToPage(pageOf(base, ym)) }
        select?.let { selectedEpoch = it.toEpochDay() }
    }

    var bloom by remember { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val roomy = maxHeight >= 700.dp
    OneScreen(
        header = { CompactHeader(state, mode, now, motion, roomy) { mode = it } },
        body = {
            AnimatedContent(
                mode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.96f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 1.02f))
                },
                label = "view",
            ) { m ->
                when (m) {
                    HOME_MODE_YEAR -> YearView(Modifier.fillMaxSize(), state, infos, yearShown, { yearShown = it }) { ym -> jumpTo(ym) }
                    HOME_MODE_RING -> CycleRingView(Modifier.fillMaxSize(), state, infos, selected, motion) { selectedEpoch = it.toEpochDay() }
                    else -> MonthView(Modifier.fillMaxSize(), state, infos, pager, selected, onPick = { picker = true }, roomy = roomy) {
                        selectedEpoch = it.toEpochDay()
                    }
                }
            }
        },
        dock = {
            DayDock(
                state, phrase, selected, infos[selected], bloom, roomy,
                onToday = { jumpTo(base, today) },
                onNextPhrase = onNextPhrase,
                onToast = onToast,
                onAsk = { pending = it },
            )
        },
    )
    }

    if (picker) {
        MonthPickerSheet(
            current = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong()),
            min = base.minusMonths(MONTHS_BACK.toLong()),
            max = base.plusMonths(MONTHS_FORWARD.toLong()),
            onDismiss = { picker = false },
        ) { ym -> picker = false; jumpTo(ym) }
    }
    pending?.let { p ->
        ConfirmSheet(p, onDismiss = { pending = null }) {
            pending = null
            when {
                p.start && p.undo -> onUnmarkStart(p.date)
                p.start -> { bloom++; onMarkStart(p.date) }
                p.undo -> onUnmarkEnd(p.date)
                else -> { bloom++; onMarkEnd(p.date) }
            }
        }
    }
}

/** Header and dock keep their height. Whatever is left goes to the calendar, year, or ring. */
@Composable
private fun OneScreen(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    dock: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(Modifier.layoutId("header").fillMaxWidth()) { header() }
            Box(Modifier.layoutId("body").fillMaxSize()) { body() }
            Box(Modifier.layoutId("dock").fillMaxWidth()) { dock() }
        },
        modifier = Modifier.fillMaxSize().testTag("home-root"),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxWidth = width)
        val headerPlaceable = measurables.first { it.layoutId == "header" }.measure(loose)
        val dockPlaceable = measurables.first { it.layoutId == "dock" }.measure(loose)
        val bodyHeight = (constraints.maxHeight - headerPlaceable.height - dockPlaceable.height).coerceAtLeast(0)
        val bodyPlaceable = measurables.first { it.layoutId == "body" }.measure(
            constraints.copy(minWidth = width, maxWidth = width, minHeight = bodyHeight, maxHeight = bodyHeight),
        )
        layout(width, constraints.maxHeight) {
            headerPlaceable.placeRelative(0, 0)
            bodyPlaceable.placeRelative(0, headerPlaceable.height)
            dockPlaceable.placeRelative(0, (constraints.maxHeight - dockPlaceable.height).coerceAtLeast(0))
        }
    }
}

// ---------------------------------------------------------------- header

private fun moodFor(state: UiState, now: LocalTime): MoonMood = when {
    state.snapshot.today.phase == Phase.MENSTRUAL -> MoonMood.PERIOD
    now.hour >= 23 || now.hour < 6 -> MoonMood.SLEEPY
    else -> MoonMood.HAPPY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactHeader(state: UiState, mode: Int, now: LocalTime, motion: Boolean, roomy: Boolean, onMode: (Int) -> Unit) {
    val t = state.snapshot.today
    val name = state.settings.nickname.takeIf { it.isNotBlank() && !state.settings.isPartner }
    val hello = Copy.greeting(now) + (name?.let { "，$it" } ?: "")
    val mood = if (state.settings.isPartner) "正在陪她一起看着日子" else Copy.mood(t.phase)
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF1EA), Color(0xFFFFF6F7)))))
        DriftWash(Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = if (roomy) 2.dp else 0.dp, bottom = if (roomy) 6.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(Modifier.weight(1f)) {
            SlidingSegmented(
                listOf(Icons.Rounded.CalendarMonth, Icons.Rounded.CalendarViewMonth, Icons.Rounded.DonutLarge),
                mode, onMode, itemSize = if (roomy) 34.dp else 30.dp, descriptions = listOf("月历", "整年", "周期环"),
            )
            Spacer(Modifier.height(if (roomy) 4.dp else 2.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hello,
                    style = if (roomy) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(state)
            }
            Text(mood, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.padding(start = 6.dp).size(if (roomy) 48.dp else 40.dp), contentAlignment = Alignment.Center) {
            FloatingSticker(R.drawable.st_star, Modifier.align(Alignment.TopEnd).size(14.dp), rotation = 12f, amplitude = 2.dp, periodMs = 2200, motion = motion)
            Mascot(Modifier.size(if (roomy) 42.dp else 34.dp), moodFor(state, now), motion = motion)
        }
        }
    }
}

@Composable
private fun StatusPill(state: UiState) {
    val snap = state.snapshot
    val t = snap.today
    val (prefix, num, suffix) = when {
        !snap.hasData -> Triple("还没有记录", "", "")
        t.phase == Phase.MENSTRUAL -> Triple("经期第", "${t.periodDay}", "天")
        t.lateDays > 0 -> Triple("已推迟", "${t.lateDays}", "天")
        t.daysUntilNext == 0 -> Triple("可能今天来", "", "")
        else -> Triple("还有", "${t.daysUntilNext}", "天")
    }
    Row(
        Modifier.clip(CircleShape).background(pillWash(state)).border(1.dp, Palette.Petal, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = pillInk(state)
        Text(prefix, color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (num.isNotEmpty()) {
            AnimatedContent(
                num,
                transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() },
                label = "num",
            ) { n ->
                Numeral(n, 15.sp, ink, weight = FontWeight.ExtraBold)
            }
            Text(suffix, color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- month

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthView(
    modifier: Modifier,
    state: UiState,
    infos: Map<LocalDate, DayInfo>,
    pager: PagerState,
    selected: LocalDate,
    onPick: () -> Unit,
    roomy: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val base = YearMonth.from(state.today)
    val scope = rememberCoroutineScope()
    val month = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong())
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.94f)
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(12.dp))
                    .clickable(src, indication = null, onClick = onPick).padding(end = 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                AnimatedContent(month, transitionSpec = { monthSlide(targetState > initialState) }, label = "title") { m ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Numeral("${m.year}", 13.sp, Palette.InkSoft, Modifier.padding(bottom = 3.dp, end = 4.dp))
                        Numeral("${m.monthValue}", if (roomy) 26.sp else 22.sp)
                        Text("月", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                Icon(Icons.Rounded.KeyboardArrowDown, "选择年月", tint = Palette.InkSoft, modifier = Modifier.padding(bottom = 3.dp).size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            if (pager.currentPage != MONTHS_BACK) {
                Text(
                    "回今天", fontSize = 12.sp, color = Palette.Pink, maxLines = 1,
                    modifier = Modifier.clip(CircleShape).background(Palette.PinkSoft).clickable {
                        scope.launch { pager.animateScrollToPage(MONTHS_BACK) }
                        onSelect(state.today)
                    }.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
            ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, d ->
                Text(
                    d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 12.sp, maxLines = 1, color = if (i >= 5) Palette.Pink else Palette.Sky,
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val ym = base.plusMonths((pager.currentPage - MONTHS_BACK).toLong())
            val rows = weekRows(ym).coerceAtLeast(1)
            val gap = 4.dp
            val fontScale = LocalDensity.current.fontScale
            val legendBlock = (18.dp * fontScale).coerceIn(18.dp, 30.dp)
            // One unclipped title line. Anything taller is leftover the card grows into.
            val cardFloor = (20.dp + 24.dp * fontScale).coerceIn(44.dp, 68.dp)
            val cellFromW = ((maxWidth - gap * 6) / 7).coerceAtLeast(0.dp)
            val gridBudget = (maxHeight - legendBlock - cardFloor).coerceAtLeast(0.dp)
            val cellFromH = ((gridBudget - gap * (rows - 1)) / rows).coerceAtLeast(0.dp)
            val cell = minOf(cellFromW, cellFromH)
            val gridH = (cell * rows + gap * (rows - 1)).coerceAtLeast(1.dp)
            Column(Modifier.fillMaxSize()) {
                HorizontalPager(
                    pager,
                    modifier = Modifier.height(gridH).fillMaxWidth().clipToBounds().testTag("home-calendar"),
                    verticalAlignment = Alignment.Top,
                    beyondViewportPageCount = 1,
                ) { page ->
                    val pageYm = base.plusMonths((page - MONTHS_BACK).toLong())
                    val drift = if (LocalMotion.current) pager.currentPageOffsetFraction else 0f
                    MonthGrid(pageYm, cell, gap, infos, state.today, selected, drift, onSelect)
                }
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LegendDot(Palette.RoseDeep, "经期", brush = PeriodBrush)
                    LegendDot(Palette.RoseDeep, "预测", dashed = true)
                    LegendDot(Palette.PeachDeep, "易孕", brush = FertileBrush)
                    LegendDot(Palette.PeachDeep, "排卵", brush = FertileBrush, marker = true)
                }
                DayStoryCard(
                    Modifier.weight(1f).padding(top = 6.dp),
                    dayBrief(selected, state.today, infos[selected], state.snapshot),
                )
            }
        }
    }
}

private fun monthSlide(forward: Boolean): ContentTransform =
    (slideInVertically { if (forward) it / 2 else -it / 2 } + fadeIn()) togetherWith (slideOutVertically { if (forward) -it / 2 else it / 2 } + fadeOut())

@Composable
private fun ArrowBtn(icon: ImageVector, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.8f)
    Box(
        Modifier.size(32.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).clickable(src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Palette.InkSoft) }
}

@Composable
private fun MonthGrid(
    ym: YearMonth,
    cell: Dp,
    gap: Dp,
    infos: Map<LocalDate, DayInfo>,
    today: LocalDate,
    selected: LocalDate,
    drift: Float,
    onSelect: (LocalDate) -> Unit,
) {
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value - java.time.DayOfWeek.MONDAY.value
    val rows = weekRows(ym)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until rows) {
            Row(
                Modifier.fillMaxWidth().height(cell),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            ) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    Box(
                        Modifier.size(cell).graphicsLayer {
                            translationY = drift * (c - 3) * 6f
                            alpha = 1f - kotlin.math.abs(drift) * 0.15f
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx in 0 until ym.lengthOfMonth()) {
                            val d = first.plusDays(idx.toLong())
                            DateChip(
                                "${d.dayOfMonth}",
                                infos[d]?.type ?: DayType.UNKNOWN,
                                d == today,
                                d == selected,
                                size = cell,
                                muted = d.isAfter(today),
                            ) { onSelect(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStoryCard(modifier: Modifier, brief: DayBrief) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Palette.Line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        val titleH = 24.dp * scale
        val lineH = 18.dp * scale
        val stripH = 16.dp
        val lines = when {
            maxHeight >= titleH + lineH * 2 + stripH -> 2
            maxHeight >= titleH + lineH * 2 -> 2
            maxHeight >= titleH + lineH -> 1
            else -> 0
        }
        val showStrip = lines > 0 && maxHeight >= titleH + lineH * lines + stripH
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(
                brief.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 20.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (lines > 0) {
                Text(
                    brief.detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    maxLines = lines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (showStrip) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Palette.PinkSoft),
                ) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(brief.progress.coerceIn(0f, 1f)).background(PeriodBrush),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- year

@Composable
private fun YearView(
    modifier: Modifier,
    state: UiState,
    infos: Map<LocalDate, DayInfo>,
    year: Int,
    onYear: (Int) -> Unit,
    onMonth: (YearMonth) -> Unit,
) {
    val today = state.today
    val minYear = today.minusMonths(MONTHS_BACK.toLong()).year
    val maxYear = today.plusMonths(MONTHS_FORWARD.toLong()).year
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { if (year > minYear) onYear(year - 1) }
                AnimatedContent(year, transitionSpec = { monthSlide(targetState > initialState) }, label = "year") { y ->
                    Numeral("$y", 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight) { if (year < maxYear) onYear(year + 1) }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, null, tint = Palette.InkFaint, modifier = Modifier.size(14.dp))
                Text(" 点月份看详情", fontSize = 12.sp, color = Palette.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AnimatedContent(
            year,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "yearGrid",
        ) { y ->
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 4) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 3) {
                            val ym = YearMonth.of(y, row * 3 + col + 1)
                            MiniMonth(ym, infos, today, Modifier.weight(1f).fillMaxHeight()) { onMonth(ym) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(ym: YearMonth, infos: Map<LocalDate, DayInfo>, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src, 0.96f)
    val isNow = ym == YearMonth.from(today)
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val rows = weekRows(ym)
    Column(
        modifier.graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(12.dp))
            .background(if (isNow) Palette.PinkSoft.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(src, indication = null, onClick = onClick).padding(horizontal = 3.dp, vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral("${ym.monthValue}", 14.sp, if (isNow) Palette.PinkDeep else Palette.Ink)
            Text("月", fontSize = 10.sp, color = Palette.InkSoft, maxLines = 1, modifier = Modifier.padding(bottom = 1.dp))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val rowH = if (rows == 0) maxHeight else maxHeight / rows
            val font = (rowH.value * 0.62f).coerceIn(6f, 9f)
            val showNumbers = rowH >= 8.dp
            Column(Modifier.fillMaxSize()) {
                for (r in 0 until rows) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c - lead
                            Box(Modifier.weight(1f).fillMaxHeight().padding(0.5.dp), contentAlignment = Alignment.Center) {
                                if (idx in 0 until ym.lengthOfMonth()) {
                                    val d = first.plusDays(idx.toLong())
                                    val type = infos[d]?.type ?: DayType.UNKNOWN
                                    val bg = when (type) {
                                        DayType.PERIOD -> Palette.RoseFill
                                        DayType.FERTILE, DayType.OVULATION -> Palette.PeachFill
                                        else -> Color.Transparent
                                    }
                                    val ink = when (type) {
                                        DayType.PERIOD -> Palette.RoseInk
                                        DayType.FERTILE, DayType.OVULATION -> Palette.PeachInk
                                        DayType.PREDICTED_PERIOD -> Palette.PredictedInk
                                        else -> Palette.Ink.copy(alpha = 0.75f)
                                    }
                                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        val side = minOf(maxWidth, maxHeight)
                                        Box(
                                            Modifier.size(side).clip(CircleShape).background(bg)
                                                .then(
                                                    when {
                                                        d == today -> Modifier.border(1.dp, Palette.LavenderLine, CircleShape)
                                                        type == DayType.PREDICTED_PERIOD -> Modifier.border(1.dp, Palette.RoseDeep, CircleShape)
                                                        else -> Modifier
                                                    },
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (showNumbers) {
                                                Text(
                                                    "${d.dayOfMonth}",
                                                    fontFamily = Rounded,
                                                    fontSize = font.sp,
                                                    lineHeight = font.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    color = ink,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthPickerSheet(current: YearMonth, min: YearMonth, max: YearMonth, onDismiss: () -> Unit, onPick: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(current.year) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(true),
        containerColor = Palette.Card,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Palette.PinkLine) },
    ) {
        Column(Modifier.padding(horizontal = 22.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("跳到", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { if (year > min.year) year-- }
                AnimatedContent(year, transitionSpec = { monthSlide(targetState > initialState) }, label = "pickYear") { y ->
                    Numeral("$y", 24.sp, modifier = Modifier.padding(horizontal = 10.dp))
                }
                ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight) { if (year < max.year) year++ }
            }
            Spacer(Modifier.height(12.dp))
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 4) {
                        val ym = YearMonth.of(year, row * 4 + col + 1)
                        val enabled = !ym.isBefore(min) && !ym.isAfter(max)
                        val sel = ym == current
                        val src = remember(ym) { MutableInteractionSource() }
                        val scale = pressScale(src, 0.88f)
                        Box(
                            Modifier.weight(1f).height(44.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel) Palette.Pink else Palette.Bg)
                                .clickable(src, indication = null, enabled = enabled) { onPick(ym) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${ym.monthValue}月", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                color = when { sel -> Color.White; enabled -> Palette.Ink; else -> Palette.InkFaint },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton("回到这个月", { onPick(YearMonth.now()) }, Modifier.fillMaxWidth(), container = Palette.PinkSoft, content = Palette.PinkDeep)
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------------------------------------------------------------- ring

@Composable
private fun CycleRingView(
    modifier: Modifier,
    state: UiState,
    infos: Map<LocalDate, DayInfo>,
    selected: LocalDate,
    motion: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val snap = state.snapshot
    if (!snap.hasData) {
        Box(modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Modifier.size(72.dp), MoonMood.HAPPY, motion = motion)
                Spacer(Modifier.height(8.dp))
                Text(
                    "记下第一次月经开始，这里会按周期把日子排成一圈",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }
    var which by remember { mutableIntStateOf(1) }
    val today = state.today
    val starts = snap.periods.map { it.start }.filter { !it.isAfter(today) }
    val curStart = starts.last()
    val curLen = maxOf(snap.avgCycle, CycleEngine.days(curStart, today) + 1)
    val (start, len) = when (which) {
        0 -> if (starts.size >= 2) starts[starts.size - 2] to CycleEngine.days(starts[starts.size - 2], curStart) else curStart to curLen
        2 -> curStart.plusDays(curLen.toLong()) to snap.avgCycle
        else -> curStart to curLen
    }
    val days = (0 until len.coerceIn(15, 60)).map { start.plusDays(it.toLong()) }
    val play = motion && LocalMotion.current
    val appear = remember(which, play) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(which, play) {
        if (!play) {
            appear.snapTo(1f)
            return@LaunchedEffect
        }
        appear.snapTo(0f)
        appear.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Column(modifier.padding(horizontal = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(start = 8.dp)) {
            listOf("上次", "本次", "下次").forEachIndexed { i, label ->
                val dot by animateFloatAsState(if (which == i) 1f else 0f, spring(dampingRatio = 0.4f), label = "dot")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { which = i }.padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(label, fontSize = 15.sp, maxLines = 1, fontWeight = if (which == i) FontWeight.Bold else FontWeight.Normal, color = if (which == i) Palette.Ink else Palette.InkFaint)
                    Box(Modifier.padding(top = 2.dp).size(5.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(Palette.Pink))
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side)) {
                val chip = min(26f, (side.value * 0.42f * 2 * PI.toFloat() / days.size) * 0.86f).dp
                val radius = side / 2 - chip / 2 - 4.dp
                Box(Modifier.align(Alignment.Center).size(radius * 2 + chip).clip(CircleShape).background(Palette.Bg))
                Box(Modifier.align(Alignment.Center).size(radius * 2 - chip).clip(CircleShape).background(Color.White))
                days.forEachIndexed { i, d ->
                    val local = ((appear.value * days.size - i * 0.6f) / 3f).coerceIn(0f, 1f)
                    val angle = -PI / 2 + 2 * PI * i / days.size
                    Box(
                        Modifier.align(Alignment.Center)
                            .offset((radius.value * cos(angle)).dp, (radius.value * sin(angle)).dp)
                            .graphicsLayer { alpha = local; scaleX = 0.5f + 0.5f * local; scaleY = 0.5f + 0.5f * local },
                    ) {
                        DateChip("${d.dayOfMonth}", infos[d]?.type ?: DayType.UNKNOWN, d == today, d == selected, chip, muted = d.isAfter(today)) { onSelect(d) }
                    }
                    if (d.dayOfMonth == 1 || i == 0) {
                        val lr = radius - chip
                        Text(
                            "${d.monthValue}月", fontSize = 10.sp, color = Palette.InkSoft, maxLines = 1,
                            modifier = Modifier.align(Alignment.Center).offset((lr.value * cos(angle)).dp, (lr.value * sin(angle)).dp),
                        )
                    }
                }
                val info = infos[selected]
                val centre = (radius * 2 - chip * 2 - 28.dp).coerceAtLeast(48.dp)
                val big = if (side < 240.dp) 22.sp else 32.sp
                Column(
                    Modifier.align(Alignment.Center).size(centre).clip(CircleShape).background(Palette.SkySoft).padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(phaseLabel(info?.type), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("第", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
                        AnimatedContent(info?.cycleDay ?: 0, transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() }, label = "cd") { n ->
                            Numeral(if (n == 0) "-" else "$n", big)
                        }
                        Text("天", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Text(
                        "%d-%02d-%02d".format(selected.year, selected.monthValue, selected.dayOfMonth),
                        fontFamily = Rounded, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Palette.Sky, maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun phaseLabel(t: DayType?) = when (t) {
    DayType.PERIOD -> "经期"
    DayType.PREDICTED_PERIOD -> "预测经期"
    DayType.FERTILE -> "易孕期"
    DayType.OVULATION -> "排卵日"
    DayType.SAFE -> "安全期"
    else -> "周期"
}

// ---------------------------------------------------------------- dock

@Composable
private fun pillWash(state: UiState): Color = when (state.snapshot.today.phase) {
    Phase.MENSTRUAL -> Palette.RoseFill
    Phase.FERTILE, Phase.OVULATION -> Palette.PeachFill
    else -> Palette.Lavender
}

private fun pillInk(state: UiState): Color = when (state.snapshot.today.phase) {
    Phase.MENSTRUAL -> Palette.RoseInk
    Phase.FERTILE, Phase.OVULATION -> Palette.PeachInk
    else -> Palette.LavenderInk
}

@Composable
private fun DayDock(
    state: UiState,
    phrase: PhraseEntity?,
    date: LocalDate,
    info: DayInfo?,
    bloom: Int,
    roomy: Boolean,
    onToday: () -> Unit,
    onNextPhrase: () -> Unit,
    onToast: (String) -> Unit,
    onAsk: (Pending) -> Unit,
) {
    val actions = remember(date, state.today, state.snapshot.periods) {
        dayActions(date, state.today, state.snapshot.periods)
    }
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF7F8), Palette.Bg)))
            .padding(horizontal = 16.dp, vertical = if (roomy) 6.dp else 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(date, transitionSpec = { monthSlide(targetState > initialState) }, label = "date") { d ->
                Text(
                    "${d.monthValue}月${d.dayOfMonth}日 ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            info?.type?.takeIf { it != DayType.UNKNOWN }?.let {
                Text(
                    "  " + phaseLabel(it) + (info.cycleDay?.let { day -> " · 第${day}天" } ?: ""),
                    fontSize = 12.sp, color = Palette.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } ?: Spacer(Modifier.weight(1f))
            if (date != state.today) {
                Text(
                    "回到今天", fontSize = 12.sp, color = Palette.Sky, maxLines = 1,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onToday).padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(if (roomy) 6.dp else 2.dp))
        if (state.settings.isPartner) {
            Text("这是她的记录，只能查看", style = MaterialTheme.typography.bodySmall, maxLines = 1)
        } else {
            val primary = actions.primary
            val undo = primary.pending.undo
            val blocked = actions.blockedReason != null
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PrimaryButton(
                text = primary.label,
                onClick = {
                    val reason = actions.blockedReason
                    if (reason != null) onToast(reason) else onAsk(primary.pending)
                },
                modifier = Modifier.fillMaxWidth().testTag("home-primary"),
                icon = when {
                    undo -> null
                    primary.pending.start -> Icons.Rounded.PlayArrow
                    else -> Icons.Rounded.Stop
                },
                height = if (roomy) 46.dp else 40.dp,
                container = when {
                    blocked -> Palette.Line
                    undo -> Palette.PinkSoft
                    else -> Palette.Pink
                },
                content = when {
                    blocked -> Palette.InkFaint
                    undo -> Palette.PinkDeep
                    else -> Color.White
                },
            )
            PetalBloom(bloom, Modifier.size(150.dp))
            }
            actions.secondary?.let { secondary ->
                val src = remember { MutableInteractionSource() }
                val scale = pressScale(src, 0.97f)
                Text(
                    secondary.label,
                    color = Palette.PinkDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .clickable(src, indication = null) {
                            val reason = actions.blockedReason
                            if (reason != null) onToast(reason) else onAsk(secondary.pending)
                        }
                        .padding(vertical = if (roomy) 6.dp else 2.dp)
                        .testTag("home-secondary"),
                )
            }
        }
        Spacer(Modifier.height(if (roomy) 4.dp else 2.dp))
        PhraseLine(phrase, onNextPhrase, roomy)
    }
}

@Composable
private fun PhraseLine(phrase: PhraseEntity?, onNext: () -> Unit, roomy: Boolean) {
    var shown by remember { mutableStateOf(phrase) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val press = pressScale(src, 0.98f)
    val motion = LocalMotion.current
    LaunchedEffect(phrase, motion) {
        if (phrase == null || phrase.id == shown?.id) return@LaunchedEffect
        if (shown == null || !motion) {
            shown = phrase
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        rotation.animateTo(90f, tween(140, easing = FastOutSlowInEasing))
        shown = phrase
        rotation.snapTo(-90f)
        rotation.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    Row(
        Modifier.fillMaxWidth().testTag("home-phrase")
            .graphicsLayer { rotationY = rotation.value; cameraDistance = 12f * density; scaleX = press; scaleY = press }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .clickable(src, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch { onNext() }
            }
            .padding(horizontal = 12.dp, vertical = if (roomy) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Sticker(R.drawable.st_heart, Modifier.size(14.dp))
        Text(
            shown?.text ?: "正在挑今天的一句",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text("换一句", color = Palette.Pink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ConfirmSheet(p: Pending, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val md = "${p.date.monthValue}月${p.date.dayOfMonth}日"
    val (title, body, sticker) = when {
        p.start && p.undo -> Triple("取消 $md 的开始？", "这次经期会删掉，预测会重算。", R.drawable.st_cloud)
        p.start -> Triple("$md 月经来了吗？", "这天会记成这次经期的第一天，之后的预测跟着改。", R.drawable.st_drop)
        p.undo -> Triple("取消 $md 的结束？", "会改回这次经期还没结束。", R.drawable.st_cloud)
        else -> Triple("$md 月经走了吗？", "这天会记成这次经期的最后一天。", R.drawable.st_bottle)
    }
    val wobble = remember { Animatable(-8f) }
    LaunchedEffect(Unit) { wobble.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Palette.Card,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Palette.PinkLine) },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Sticker(sticker, Modifier.size(40.dp).graphicsLayer { rotationZ = wobble.value })
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Palette.InkSoft, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("再想想", { scope.launch { sheet.hide(); onDismiss() } }, Modifier.weight(1f), height = 44.dp, container = Palette.Panel, content = Palette.InkSoft)
                PrimaryButton(if (p.undo) "确认取消" else "确认", { scope.launch { sheet.hide(); onConfirm() } }, Modifier.weight(1f), height = 44.dp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
