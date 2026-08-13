package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.formatClock
import com.arshadshah.nimaz.core.util.formatFullDate
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedOption
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotSpec
import com.arshadshah.nimaz.presentation.components.atoms.NimazTimelineNode
import com.arshadshah.nimaz.presentation.components.atoms.NimazTimelineTrack
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordionStyle
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** The four assertions a user can make, in picker order. */
private val PICKER_STATUSES = listOf(
    PrayerStatus.PRAYED,
    PrayerStatus.LATE,
    PrayerStatus.MISSED,
    PrayerStatus.QADA,
)

/**
 * The selected day: what its schedule was, how far through it you are, and what you logged.
 *
 * @param expandedPrayer the one open row, or `null`. Hoisted so exactly one can be open — see
 *   [NimazAccordion]'s hoisted overload.
 */
@Composable
fun PrayerTrackerDayCard(
    selectedDate: LocalDate,
    statuses: Map<PrayerName, PrayerDisplayStatus>,
    times: PrayerTimes?,
    now: LocalDateTime,
    streak: Int,
    expandedPrayer: PrayerName?,
    onExpandedChange: (PrayerName?) -> Unit,
    onSetStatus: (PrayerName, PrayerStatus?) -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val use24Hour = LocalUse24HourFormat.current
    val isToday = selectedDate == now.toLocalDate()
    val doneCount = TRACKED_PRAYERS.count { statuses[it]?.isDone() == true }

    NimazCard(style = NimazCardStyle.FILLED, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(top = 18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedDate.formatFullDate(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (streak > 0) {
                            NimazBadge(
                                text = stringResource(R.string.prayer_streak_format, streak),
                                icon = Icons.Default.LocalFireDepartment,
                                size = NimazBadgeSize.SMALL,
                                tone = NimazTone.WARNING,
                                emphasis = NimazBadgeEmphasis.SOFT,
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.prayer_recorded_count_format,
                                doneCount,
                                TRACKED_PRAYERS.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!isToday) {
                    NimazButton(
                        text = stringResource(R.string.back_to_today),
                        onClick = onBackToToday,
                        variant = NimazButtonVariant.TEXT,
                        size = NimazButtonSize.SMALL,
                    )
                }
            }

            DayTimeline(
                statuses = statuses,
                times = times,
                now = now,
                isToday = isToday,
                use24Hour = use24Hour,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp),
            )

            NimazDivider(modifier = Modifier.padding(top = 16.dp))

            TRACKED_PRAYERS.forEachIndexed { index, prayer ->
                if (index > 0) NimazDivider()
                val status = statuses[prayer] ?: PrayerDisplayStatus.UPCOMING
                PrayerRow(
                    prayer = prayer,
                    status = status,
                    time = times?.timeFor(prayer)?.formatClock(use24Hour),
                    canBeMadeUp = status != PrayerDisplayStatus.UPCOMING,
                    expanded = expandedPrayer == prayer,
                    onExpandedChange = { open -> onExpandedChange(if (open) prayer else null) },
                    onSetStatus = { newStatus -> onSetStatus(prayer, newStatus) },
                )
            }
        }
    }
}

@Composable
private fun DayTimeline(
    statuses: Map<PrayerName, PrayerDisplayStatus>,
    times: PrayerTimes?,
    now: LocalDateTime,
    isToday: Boolean,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    if (times == null) return

    val start = times.fajr
    val end = times.isha
    val span = Duration.between(start, end).toMinutes().toFloat()

    // A day whose Isha is not after its Fajr is not a day this can draw. It happens at extreme
    // latitudes and after a bad location fix, and the atom would clamp every node onto the same
    // point -- five dots in a stack reads as one dot, which is worse than no timeline.
    if (span <= 0f) return

    fun positionOf(at: LocalDateTime) =
        Duration.between(start, at).toMinutes().toFloat() / span

    val nodes = TRACKED_PRAYERS.mapNotNull { prayer ->
        val at = times.timeFor(prayer) ?: return@mapNotNull null
        val status = statuses[prayer] ?: PrayerDisplayStatus.UPCOMING
        NimazTimelineNode(
            position = positionOf(at),
            spec = NimazStatusDotSpec(status.tone(), status.dotStyle()),
            label = prayer.displayName(),
        )
    }

    NimazTimelineTrack(
        nodes = nodes,
        startLabel = "${PrayerName.FAJR.displayName()} ${start.formatClock(use24Hour)}",
        endLabel = "${PrayerName.ISHA.displayName()} ${end.formatClock(use24Hour)}",
        progress = if (isToday) positionOf(now) else null,
        modifier = modifier,
    )
}

@Composable
private fun PrayerRow(
    prayer: PrayerName,
    status: PrayerDisplayStatus,
    time: String?,
    canBeMadeUp: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSetStatus: (PrayerStatus?) -> Unit,
) {
    val options = PICKER_STATUSES
        .filter { canBeMadeUp || it != PrayerStatus.QADA }

    NimazAccordion(
        title = prayer.displayName(),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        subtitle = time,
        style = NimazAccordionStyle.FLAT,
        trailing = {
            NimazBadge(
                text = status.label(),
                size = NimazBadgeSize.LARGE,
                tone = status.tone(),
                emphasis = if (status == PrayerDisplayStatus.NOT_RECORDED) {
                    NimazBadgeEmphasis.OUTLINED
                } else {
                    NimazBadgeEmphasis.SOFT
                },
            )
        },
    ) {
        NimazSegmentedControl(
            options = options.map { candidate ->
                NimazSegmentedOption(
                    label = candidate.pickerLabel(),
                    selectedTone = candidate.displayed().tone(),
                )
            },
            selectedIndex = options.indexOfFirst { it.displayed() == status }.takeIf { it >= 0 },
            // The control reports a tap even on the selected cell, which is how tap-to-clear
            // reaches us: choosing what you already chose withdraws the assertion.
            onSelect = { index ->
                val chosen = options[index]
                onSetStatus(if (chosen.displayed() == status) null else chosen)
            },
        )
        if (status == PrayerDisplayStatus.NOT_RECORDED) {
            Text(
                text = stringResource(R.string.prayer_not_recorded_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** The display status a stored status maps to, for matching the picker against the row. */
private fun PrayerStatus.displayed(): PrayerDisplayStatus = when (this) {
    PrayerStatus.PRAYED -> PrayerDisplayStatus.PRAYED
    PrayerStatus.LATE -> PrayerDisplayStatus.LATE
    PrayerStatus.MISSED -> PrayerDisplayStatus.MISSED
    PrayerStatus.QADA -> PrayerDisplayStatus.QADA
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> PrayerDisplayStatus.NOT_RECORDED
}

@Composable
private fun PrayerStatus.pickerLabel(): String = when (this) {
    PrayerStatus.PRAYED -> stringResource(R.string.on_time)
    PrayerStatus.LATE -> stringResource(R.string.late)
    PrayerStatus.MISSED -> stringResource(R.string.missed)
    PrayerStatus.QADA -> stringResource(R.string.made_up)
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> stringResource(R.string.prayer_status_not_recorded)
}

@Composable
private fun PrayerDisplayStatus.label(): String = when (this) {
    PrayerDisplayStatus.PRAYED -> stringResource(R.string.on_time)
    PrayerDisplayStatus.LATE -> stringResource(R.string.late)
    PrayerDisplayStatus.QADA -> stringResource(R.string.made_up)
    PrayerDisplayStatus.MISSED -> stringResource(R.string.missed)
    PrayerDisplayStatus.NOT_RECORDED -> stringResource(R.string.prayer_status_not_recorded)
    PrayerDisplayStatus.UPCOMING -> stringResource(R.string.upcoming)
}
