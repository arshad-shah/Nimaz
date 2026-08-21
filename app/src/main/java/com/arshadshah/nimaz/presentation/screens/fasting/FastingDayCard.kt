package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.core.util.countdownOf
import com.arshadshah.nimaz.core.util.formatWeekdayDayMonth
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedOption
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotSpec
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazWindowTrack
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.components.atoms.countdownText
import com.arshadshah.nimaz.presentation.components.atoms.rememberCountdownTo
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.NimazDayRail
import com.arshadshah.nimaz.presentation.components.molecules.NimazDayRailItem
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingTrackerUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

/** Placeholder for a time the schedule has not produced yet. */
private const val NO_TIME = "--:--"

/**
 * The three cells of the status control, in order.
 *
 * `EXEMPTED` and `MAKEUP_DUE` share the third cell: both mean "this day was not fasted, and there
 * is a reason on file", and splitting them would give the reader four cells to distinguish where
 * they only ever choose between three.
 */
private val SegmentStatuses = listOf(
    FastStatus.FASTED,
    FastStatus.NOT_FASTED,
    FastStatus.EXEMPTED,
)

/**
 * The selected day: what its fasting window is, how far through it we are, and what was logged.
 *
 * Replaces the old `TodayFastSection`, whose switch could only say fasted or not-fasted and was
 * disabled outright for the two statuses that carry a reason. A day with no record at all had no
 * representation: the switch showed it as explicitly not fasting, which is a claim the user never
 * made. The three-way control has a fourth state — none selected — for exactly that.
 *
 * @param ramadanDay day number when the selected date falls in Ramadan, else `null`. It takes the
 *   slot the "back to today" button would use, because inside Ramadan the day number is the fact
 *   worth showing.
 * @param onSetStatus raised for the first two cells. The third opens [onOpenExemption] instead —
 *   an exemption without a reason is not worth recording.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FastingDayCard(
    state: FastingTrackerUiState,
    ramadanDay: Int?,
    onSetStatus: (FastStatus) -> Unit,
    onOpenExemption: () -> Unit,
    onOpenNote: () -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.NEUTRAL,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            DayHeader(
                date = state.selectedDate,
                ramadanDay = ramadanDay,
                showBackToToday = !state.isSelectedToday,
                onBackToToday = onBackToToday,
            )

            Spacer(modifier = Modifier.height(18.dp))

            FastingWindow(state = state)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.fasting_this_day).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(9.dp))

            NimazSegmentedControl(
                options = listOf(
                    NimazSegmentedOption(
                        label = stringResource(R.string.fasting_seg_fasted),
                        icon = Icons.Default.Check,
                        selectedTone = NimazTone.SUCCESS,
                    ),
                    NimazSegmentedOption(
                        label = stringResource(R.string.fasting_seg_not_fasting),
                        icon = Icons.Default.Clear,
                        selectedTone = NimazTone.NEUTRAL,
                    ),
                    NimazSegmentedOption(
                        label = stringResource(R.string.fasting_seg_exempt),
                        icon = Icons.Default.Shield,
                        selectedTone = NimazTone.WARNING,
                    ),
                ),
                selectedIndex = state.selectedRecord?.status?.segmentIndex(),
                onSelect = { index ->
                    val status = SegmentStatuses[index]
                    if (status == FastStatus.EXEMPTED) onOpenExemption() else onSetStatus(status)
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            DayFooter(
                record = state.selectedRecord,
                onOpenNote = onOpenNote,
            )
        }
    }
}

/** Which cell of the control a status lights up, or `null` for a day with no record. */
private fun FastStatus.segmentIndex(): Int = when (this) {
    FastStatus.FASTED -> 0
    FastStatus.NOT_FASTED -> 1
    FastStatus.EXEMPTED, FastStatus.MAKEUP_DUE -> 2
}

@Composable
private fun DayHeader(
    date: LocalDate,
    ramadanDay: Int?,
    showBackToToday: Boolean,
    onBackToToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.formatWeekdayDayMonth(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // A fast is a Hijri-calendar act: "13 Sha'ban" is what says *which* recommended fast
            // a day is, and the Gregorian line above cannot.
            val hijri = remember(date) { HijriDateCalculator.toHijri(date) }
            Text(
                text = hijri.formatted(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            ramadanDay != null -> NimazBadge(
                text = stringResource(R.string.fasting_ramadan_day, ramadanDay),
                tone = NimazTone.ACCENT,
                size = NimazBadgeSize.SMALL,
            )

            showBackToToday -> NimazButton(
                text = stringResource(R.string.fasting_back_to_today),
                onClick = onBackToToday,
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
            )
        }
    }
}

/**
 * The suhoor→iftar band, with a lede saying what the reader actually wants: how long is left.
 *
 * `now` comes from [rememberNow] rather than a clock read at composition, so the countdown moves
 * and a screen left open across midnight does not keep answering yesterday's question.
 */
@Composable
private fun FastingWindow(state: FastingTrackerUiState) {
    val now by rememberNow(TickResolution.MINUTES)
    val suhoorAt = state.selectedSuhoorAt
    val iftarAt = state.selectedIftarAt

    // Only today gets a marker. On any other day "now" is not inside that day's window, so a
    // marker would be pointing at nothing.
    val progress = if (state.isSelectedToday && suhoorAt != null && iftarAt != null) {
        val span = (iftarAt - suhoorAt).inWholeSeconds.toFloat()
        if (span <= 0f) null else ((now - suhoorAt).inWholeSeconds.toFloat() / span)
    } else {
        null
    }

    // The sentence and the duration inside it, kept apart so only the duration is accented —
    // "Iftar in **3h 18m**". Highlighting the whole line makes the words compete with the number,
    // and the number is the only part that changes.
    val duration: String? = when {
        suhoorAt == null || iftarAt == null -> null
        !state.isSelectedToday -> countdownText(
            countdownOf(iftarAt - suhoorAt),
            showSeconds = false
        )

        now < suhoorAt -> countdownText(rememberCountdownTo(suhoorAt).value, showSeconds = false)
        now < iftarAt -> countdownText(rememberCountdownTo(iftarAt).value, showSeconds = false)
        else -> null
    }

    val lede = when {
        suhoorAt == null || iftarAt == null || duration == null ->
            stringResource(R.string.fasting_window_closed)

        !state.isSelectedToday -> stringResource(R.string.fasting_window_length, duration)
        now < suhoorAt -> stringResource(R.string.fasting_window_suhoor_in, duration)
        else -> stringResource(R.string.fasting_window_iftar_in, duration)
    }

    // Located by search rather than by assuming the placeholder is last: "Iftar in %1$s" and
    // "Sahur bitişine %1$s" put it in different places, and a hardcoded span would accent the
    // wrong words in half the shipped locales.
    val accentColor = MaterialTheme.colorScheme.primary
    val ledeText = remember(lede, duration, accentColor) {
        buildAnnotatedString {
            append(lede)
            val start = duration?.let { lede.lastIndexOf(it) } ?: -1
            if (duration != null && start >= 0) {
                addStyle(
                    SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold),
                    start,
                    start + duration.length,
                )
            }
        }
    }

    val suhoorText = suhoorAt?.let { clockTimeText(it) } ?: NO_TIME
    val iftarText = iftarAt?.let { clockTimeText(it) } ?: NO_TIME

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NimazIcon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                size = NimazIconSize.SMALL,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = ledeText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        NimazWindowTrack(
            startLabel = stringResource(R.string.fasting_suhoor_ends),
            startValue = suhoorText,
            endLabel = stringResource(R.string.fasting_iftar),
            endValue = iftarText,
            progress = progress,
            contentDescription = stringResource(
                R.string.fasting_window_a11y,
                suhoorText,
                iftarText,
            ),
        )
    }
}

/** The chips under the control: what is on file for this day, and the way to add to it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayFooter(
    record: FastRecord?,
    onOpenNote: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            record == null -> NimazChip(
                text = stringResource(R.string.fasting_not_logged_yet),
                onClick = {},
                variant = NimazChipVariant.ASSIST,
                enabled = false,
            )

            else -> {
                record.exemptionReason?.let { reason ->
                    NimazChip(
                        text = reason.displayName(),
                        onClick = {},
                        variant = NimazChipVariant.ASSIST,
                        enabled = false,
                    )
                }
                if (record.status == FastStatus.MAKEUP_DUE) {
                    NimazChip(
                        text = stringResource(R.string.fasting_owed_make_up_later),
                        onClick = {},
                        variant = NimazChipVariant.ASSIST,
                        enabled = false,
                    )
                }
            }
        }

        NimazButton(
            text = stringResource(R.string.fasting_add_note),
            onClick = onOpenNote,
            variant = NimazButtonVariant.TEXT,
            size = NimazButtonSize.SMALL,
        )
    }
}

/**
 * The week rail above the day card.
 *
 * Reads [FastingTrackerUiState.weekRecords] rather than the calendar month's records, because a
 * week that straddles the first of a month is half missing from a single-month query.
 *
 * @param today used to decide which cells are in the future; supplied rather than read from the
 *   clock so the rail cannot disagree with the ViewModel about what day it is.
 */
@Composable
fun FastingWeekRail(
    state: FastingTrackerUiState,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekStart = remember(state.selectedDate) {
        state.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val statusByDate = remember(state.weekRecords) {
        state.weekRecords.associate { record ->
            LocalDate.ofEpochDay(record.date / MILLIS_PER_DAY) to record.status
        }
    }
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }

    // Not `Locale.getDefault()`: that reads no observable state, so the rail's weekday initials
    // would keep the old language after a locale change until something else recomposed it.
    val locale = LocalLocale.current.platformLocale

    NimazDayRail(
        days = days.map { date ->
            NimazDayRailItem(
                weekdayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                dayLabel = date.dayOfMonth.toString(),
                marker = statusByDate[date]?.dotSpec(),
                isToday = date == today,
                // A day that has not happened cannot be logged, so it is not selectable either.
                enabled = !date.isAfter(today),
                contentDescription = date.formatWeekdayDayMonth(),
            )
        },
        selectedIndex = days.indexOf(state.selectedDate).takeIf { it >= 0 },
        onSelect = { onSelectDate(days[it]) },
        modifier = modifier,
    )
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * How a status reads as a rail marker.
 *
 * `NOT_FASTED` is a **ring**: an absent dot already means "no record", and the two are different
 * facts. That distinction is the reason [NimazStatusDotStyle] exists.
 */
private fun FastStatus.dotSpec(): NimazStatusDotSpec = when (this) {
    FastStatus.FASTED -> NimazStatusDotSpec(NimazTone.SUCCESS)
    FastStatus.NOT_FASTED ->
        NimazStatusDotSpec(NimazTone.NEUTRAL, NimazStatusDotStyle.OUTLINED)

    FastStatus.EXEMPTED -> NimazStatusDotSpec(NimazTone.MUTED)
    FastStatus.MAKEUP_DUE -> NimazStatusDotSpec(NimazTone.WARNING)
}

// ==================== PREVIEWS ====================

private fun previewRecord(status: FastStatus) = FastRecord(
    id = 1,
    date = 0,
    hijriDate = null,
    hijriMonth = null,
    hijriYear = null,
    fastType = FastType.VOLUNTARY,
    status = status,
    exemptionReason = null,
    suhoorTime = null,
    iftarTime = null,
    note = null,
    createdAt = 0,
    updatedAt = 0,
)

@Composable
private fun FastingDayCardShowcase() {
    val today = LocalDate.of(2026, 8, 13)
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FastingDayCard(
            state = FastingTrackerUiState(
                selectedDate = today,
                isSelectedToday = true,
                isLoading = false,
            ),
            ramadanDay = null,
            onSetStatus = {},
            onOpenExemption = {},
            onOpenNote = {},
            onBackToToday = {},
        )
        FastingDayCard(
            state = FastingTrackerUiState(
                selectedDate = LocalDate.of(2026, 8, 11),
                selectedRecord = previewRecord(FastStatus.FASTED),
                isSelectedToday = false,
                isLoading = false,
            ),
            ramadanDay = null,
            onSetStatus = {},
            onOpenExemption = {},
            onOpenNote = {},
            onBackToToday = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "FastingDayCard — Light")
@Composable
private fun FastingDayCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { FastingDayCardShowcase() }
}

@Preview(
    showBackground = true, widthDp = 400, name = "FastingDayCard — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun FastingDayCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { FastingDayCardShowcase() }
}

@Preview(showBackground = true, widthDp = 400, name = "FastingWeekRail — Light")
@Composable
private fun FastingWeekRailPreview() {
    val today = LocalDate.of(2026, 8, 13)
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        FastingWeekRail(
            state = FastingTrackerUiState(
                selectedDate = today,
                isSelectedToday = true,
                isLoading = false,
            ),
            today = today,
            onSelectDate = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
