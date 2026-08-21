package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.core.util.formatCurrency
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.RamadanBanner
import com.arshadshah.nimaz.presentation.components.molecules.RamadanCountdownCard
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.screens.resolve
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingViewModel
import java.time.LocalDate
import java.time.YearMonth

// Colour constants shared with MakeupFastsScreen.kt, which was cut out of this file — hence
// `internal` rather than `private`. They are aliases onto the palette, not literals (rule 7).
internal val OrangeAccent = NimazColors.PrayerColors.Asr
internal val OrangeDark = NimazColors.OrangeDark
internal val GreenAccent = NimazColors.Success

/**
 * How close Ramadan has to be before the countdown card appears.
 *
 * Named because two things must agree about it — the card's gate here, and the fact that during
 * Ramadan the banner takes over instead. The failure mode of disagreement is both showing at once.
 */
private const val RamadanCardWindowDays = 30

/**
 * The fasting tracker: one scroll that reports the selected day.
 *
 * The screen this replaces was a tab row over a `LazyColumn`, with the month calendar and the
 * recommended fasts folded behind a "Go deeper" menu group. Three mechanisms — tabs, expanders and
 * a menu — for a screen with five things on it. Everything is now simply on the screen, in the
 * order someone reads it: what Ramadan is doing, which day you are looking at, that day, the
 * month around it, what is coming, and what you owe.
 *
 * Make-up fasts moved out entirely, to [Route.MakeupFasts][com.arshadshah.nimaz.core.navigation.Route.MakeupFasts].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToMakeup: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel()
) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    val makeupState by viewModel.makeupState.collectAsStateWithLifecycle()
    val ramadanState by viewModel.ramadanState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()

    // Sheet visibility is screen state, not ViewModel state: which sheet is open survives a
    // rotation and nothing else needs to know about it. What they *edit* comes from
    // `state.selectedRecord`, so the sheets never hold a second copy of the day.
    var exemptionSheetOpen by rememberSaveable { mutableStateOf(false) }
    var noteSheetOpen by rememberSaveable { mutableStateOf(false) }

    // "Today" is whatever the ViewModel is anchored to. Reading the clock here would let the rail
    // and the ViewModel disagree about the date across midnight.
    val today = remember(state.selectedDate, state.isSelectedToday) {
        if (state.isSelectedToday) state.selectedDate else LocalDate.now()
    }

    FastExemptionSheet(
        isVisible = exemptionSheetOpen,
        date = state.selectedDate,
        initialReason = state.selectedRecord?.exemptionReason,
        onSave = { reason ->
            viewModel.onEvent(FastingEvent.SaveExemption(state.selectedDate, reason))
            exemptionSheetOpen = false
        },
        onDismiss = { exemptionSheetOpen = false },
    )

    FastNoteSheet(
        isVisible = noteSheetOpen,
        date = state.selectedDate,
        initialNote = state.selectedRecord?.note.orEmpty(),
        onSave = { note ->
            viewModel.onEvent(FastingEvent.SaveNote(state.selectedDate, note))
            noteSheetOpen = false
        },
        onDismiss = { noteSheetOpen = false },
    )

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.fasting_title),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (ramadanState.isRamadan) {
                item {
                    RamadanBanner(
                        fastedDays = ramadanState.fastedDays,
                        totalDays = ramadanState.fastedDays +
                                ramadanState.missedDays + ramadanState.remainingDays,
                        currentDay = ramadanState.currentDay,
                        missedDays = ramadanState.missedDays,
                        remainingDays = ramadanState.remainingDays,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            } else if (ramadanState.daysUntilRamadan <= RamadanCardWindowDays) {
                ramadanState.ramadanStartsOn?.let { startsOn ->
                    item {
                        RamadanCountdownCard(
                            daysAway = ramadanState.daysUntilRamadan,
                            startsOn = startsOn,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }

            item {
                FastingWeekRail(
                    state = state,
                    today = today,
                    onSelectDate = { viewModel.onEvent(FastingEvent.SelectDate(it)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                FastingDayCard(
                    state = state,
                    ramadanDay = ramadanState.currentDay.takeIf { ramadanState.isRamadan },
                    onSetStatus = {
                        viewModel.onEvent(FastingEvent.SetFastStatus(state.selectedDate, it))
                    },
                    onOpenExemption = { exemptionSheetOpen = true },
                    onOpenNote = { noteSheetOpen = true },
                    onBackToToday = { viewModel.onEvent(FastingEvent.SelectDate(today)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NimazSectionHeader(
                        title = stringResource(R.string.fasting_your_month),
                        trailingText = stringResource(
                            R.string.fasting_fasted_count,
                            calendarState.records.count { it.status == FastStatus.FASTED },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FastingCalendarSection(
                        records = calendarState.records,
                        selectedDate = state.selectedDate,
                        selectedMonth = calendarState.selectedMonth,
                        selectedYear = calendarState.selectedYear,
                        onPreviousMonth = {
                            val month = if (calendarState.selectedMonth == 1) 12
                            else calendarState.selectedMonth - 1
                            val year = if (calendarState.selectedMonth == 1)
                                calendarState.selectedYear - 1 else calendarState.selectedYear
                            viewModel.onEvent(FastingEvent.SelectMonth(month, year))
                        },
                        onNextMonth = {
                            val month = if (calendarState.selectedMonth == 12) 1
                            else calendarState.selectedMonth + 1
                            val year = if (calendarState.selectedMonth == 12)
                                calendarState.selectedYear + 1 else calendarState.selectedYear
                            viewModel.onEvent(FastingEvent.SelectMonth(month, year))
                        },
                        // Selects the day rather than opening a sheet. The day card below is
                        // where a day is edited now, so a calendar tap answers "show me that
                        // day" — the question a calendar tap actually asks.
                        onSelectDate = { viewModel.onEvent(FastingEvent.SelectDate(it)) },
                        showRamadanIndicators = ramadanState.isRamadan,
                    )
                }
            }

            item {
                Column {
                    NimazSectionHeader(
                        title = stringResource(R.string.fasting_coming_up),
                        trailingText = stringResource(R.string.fasting_sunnah_virtuous),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ComingUpRow(
                        fasts = rememberComingUpFasts(
                            records = calendarState.records,
                            daysUntilAyyamAlBeed = ramadanState.daysUntilAyyamAlBeed,
                            today = today,
                        ),
                        onLogFast = { date ->
                            viewModel.onEvent(FastingEvent.SelectDate(date))
                            viewModel.onEvent(
                                FastingEvent.SetFastStatus(date, FastStatus.FASTED)
                            )
                        },
                    )
                }
            }

            item {
                MakeupFastsRow(
                    pendingCount = makeupState.pendingCount,
                    fidyaPaid = makeupState.totalFidyaPaid,
                    currency = makeupState.currency,
                    onClick = onNavigateToMakeup,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

/**
 * The way through to the make-up screen, and a report of what is waiting there.
 *
 * `NimazCard(onClick = …)` rather than a `Modifier.clickable` wrapper: a wrapping clickable paints
 * a sharp-cornered ripple that ignores the card's radius (ARCHITECTURE §8).
 */
@Composable
private fun MakeupFastsRow(
    pendingCount: Int,
    fidyaPaid: Double,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.NEUTRAL,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NimazIcon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = OrangeAccent,
                containerColor = OrangeAccent.copy(alpha = 0.14f),
                containerSize = 44.dp,
                iconSize = 22.dp,
                cornerRadius = 14.dp,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.fasting_row_makeup),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Only the paid figure, and only once nothing is outstanding — the mapper decides
                // which of the two facts the row leads with, and returns null when neither is
                // worth saying. A row with nothing true to report gets no second line at all
                // rather than a dash.
                FastingSubtitles.makeup(
                    pending = pendingCount,
                    fidyaPaid = fidyaPaid.takeIf { it > 0.0 }
                        ?.let { formatCurrency(it, currency) },
                ).resolve()?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (pendingCount > 0) {
                NimazBadge(
                    text = pendingCount.toString(),
                    tone = NimazTone.WARNING,
                    size = NimazBadgeSize.SMALL,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            NimazIcon(
                imageVector = NimazIcons.Forward,
                contentDescription = null,
                size = NimazIconSize.SMALL,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The month grid, with a dot per logged day.
 *
 * `NOT_FASTED` draws a **ring**: an absent dot already means "no record", so a filled dot for
 * "recorded as not fasted" would make the two indistinguishable.
 */
@Composable
private fun FastingCalendarSection(
    records: List<FastRecord>,
    selectedDate: LocalDate,
    selectedMonth: Int,
    selectedYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    showRamadanIndicators: Boolean = true,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val displayedMonth = remember(selectedMonth, selectedYear) {
        YearMonth.of(selectedYear, selectedMonth)
    }
    val daysInMonth = displayedMonth.lengthOfMonth()

    val recordMap = remember(records) {
        records.associateBy { LocalDate.ofEpochDay(it.date / MILLIS_PER_DAY) }
    }

    val ramadanDaysInMonth = remember(selectedMonth, selectedYear, showRamadanIndicators) {
        if (showRamadanIndicators) {
            (1..daysInMonth).filter { day ->
                HijriDateCalculator.isRamadan(LocalDate.of(selectedYear, selectedMonth, day))
            }.toSet()
        } else emptySet()
    }

    val futureTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val notFastedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val fastedLabel = stringResource(R.string.fasting_fasted)
    val notFastingLabel = stringResource(R.string.fasting_seg_not_fasting)
    val exemptLabel = stringResource(R.string.fasting_seg_exempt)
    val owedLabel = stringResource(R.string.fasting_makeup_owed)
    val legendItems = remember(fastedLabel, notFastingLabel, exemptLabel, owedLabel) {
        listOf(
            CalendarLegendItem(NimazColors.FastingColors.Fasted, fastedLabel),
            CalendarLegendItem(
                notFastedColor, notFastingLabel, NimazStatusDotStyle.OUTLINED
            ),
            CalendarLegendItem(NimazColors.FastingColors.Exempted, exemptLabel),
            CalendarLegendItem(NimazColors.FastingColors.Makeup, owedLabel),
        )
    }

    NimazCalendar(
        displayedMonth = displayedMonth,
        selectedDate = selectedDate,
        onDateSelected = onSelectDate,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        modifier = modifier,
        dayStateProvider = { date ->
            if (date.monthValue == selectedMonth && date.year == selectedYear) {
                val record = recordMap[date]
                val isFuture = date.isAfter(today)
                val isRamadanDay = date.dayOfMonth in ramadanDaysInMonth
                CalendarDayState(
                    indicatorColor = when (record?.status) {
                        FastStatus.FASTED -> NimazColors.FastingColors.Fasted
                        FastStatus.NOT_FASTED -> notFastedColor
                        FastStatus.EXEMPTED -> NimazColors.FastingColors.Exempted
                        FastStatus.MAKEUP_DUE -> NimazColors.FastingColors.Makeup
                        null -> null
                    },
                    indicatorStyle = if (record?.status == FastStatus.NOT_FASTED) {
                        NimazStatusDotStyle.OUTLINED
                    } else {
                        NimazStatusDotStyle.FILLED
                    },
                    backgroundColor = if (isRamadanDay) {
                        NimazColors.FastingColors.Ramadan.copy(alpha = 0.15f)
                    } else null,
                    textColor = when {
                        isRamadanDay && !isFuture -> NimazColors.FastingColors.Ramadan
                        isFuture -> futureTextColor
                        else -> null
                    },
                    fontWeight = if (isRamadanDay) FontWeight.SemiBold else null
                )
            } else CalendarDayState()
        },
        legendItems = legendItems
    )
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

// ==================== PREVIEWS ====================

@Composable
private fun MakeupRowShowcase() {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MakeupFastsRow(pendingCount = 3, fidyaPaid = 24.0, currency = "GBP", onClick = {})
        MakeupFastsRow(pendingCount = 0, fidyaPaid = 24.0, currency = "GBP", onClick = {})
        MakeupFastsRow(pendingCount = 0, fidyaPaid = 0.0, currency = "GBP", onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Makeup row — Light")
@Composable
private fun MakeupFastsRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { MakeupRowShowcase() }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Makeup row — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun MakeupFastsRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { MakeupRowShowcase() }
}
