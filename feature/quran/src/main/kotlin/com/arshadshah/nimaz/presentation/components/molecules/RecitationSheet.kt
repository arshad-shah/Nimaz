package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.atoms.asSegments

/**
 * How the recitation is played, rather than what is played.
 *
 * The reader could start audio and stop it, and nothing else. This is the half of a recitation
 * player that makes it useful for **memorisation**: repeat a verse until it sticks, loop the
 * passage you are working on, slow the reciter down enough to follow the madd, and have the
 * page turn itself so you can keep your eyes on the text.
 *
 * Both selectors are the house segmented control — the same one the whole app uses — which is
 * why there are two of them on one sheet and neither shouts: the lift carries the selection, not
 * the brand colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationSheet(
    reciterName: String,
    repeat: RecitationRepeat,
    speed: RecitationSpeed,
    followAlong: Boolean,
    ayahCount: Int,
    onOpenReciters: () -> Unit,
    onRepeatChange: (RecitationRepeat) -> Unit,
    onSpeedChange: (RecitationSpeed) -> Unit,
    onFollowAlongChange: (Boolean) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.recitation_settings),
        icon = Icons.Default.GraphicEq,
        onClose = onDismiss,
        footer = {
            NimazSheetFooterButtons(
                primaryText = stringResource(R.string.recitation_done),
                onPrimary = onDismiss,
                secondaryText = stringResource(R.string.recitation_stop),
                onSecondary = onStop,
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            NimazMenuItem(
                title = reciterName,
                subtitle = stringResource(R.string.reciter),
                icon = Icons.Default.RecordVoiceOver,
                onClick = onOpenReciters,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NimazSheetSectionLabel(text = stringResource(R.string.recitation_repeat))
                NimazSegmentedControl(
                    options = listOf(
                        stringResource(R.string.recitation_repeat_off),
                        stringResource(R.string.recitation_repeat_ayah),
                        stringResource(R.string.recitation_repeat_range),
                        stringResource(R.string.recitation_repeat_surah),
                    ).asSegments(),
                    selectedIndex = repeat.selectedIndex(),
                    onSelect = { index ->
                        onRepeatChange(repeat.forIndex(index, ayahCount))
                    },
                    size = NimazSegmentedSize.SMALL,
                    purpose = NimazSegmentedPurpose.VALUE,
                )

                // The count and the range only exist for the mode that has them. Showing a
                // greyed stepper under "Off" would advertise a control that does nothing.
                when (repeat) {
                    is RecitationRepeat.Ayah -> NimazNumberStepper(
                        label = stringResource(R.string.recitation_repeat_times),
                        value = repeat.times,
                        onValueChange = { onRepeatChange(RecitationRepeat.ayahClamped(it)) },
                        // The domain type floors at 2 — one play is not a repeat — so the
                        // stepper floors there too rather than letting the guard throw.
                        minValue = RecitationRepeat.MIN_TIMES,
                        maxValue = MAX_TIMES,
                    )

                    // Stacked, not side by side. Two steppers sharing a phone's width leaves
                    // each about 160dp for a label, a minus, a number and a plus — the label
                    // ellipsises and the buttons crowd the value. A row each costs one line of
                    // sheet and gives both their full width.
                    is RecitationRepeat.Range -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NimazNumberStepper(
                            label = stringResource(R.string.recitation_repeat_from),
                            value = repeat.fromAyah,
                            onValueChange = {
                                onRepeatChange(
                                    RecitationRepeat.Range(
                                        fromAyah = it,
                                        toAyah = maxOf(it, repeat.toAyah),
                                    )
                                )
                            },
                            minValue = 1,
                            maxValue = maxOf(1, ayahCount),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        NimazNumberStepper(
                            label = stringResource(R.string.recitation_repeat_to),
                            value = repeat.toAyah,
                            onValueChange = {
                                onRepeatChange(
                                    RecitationRepeat.Range(
                                        fromAyah = minOf(repeat.fromAyah, it),
                                        toAyah = it,
                                    )
                                )
                            },
                            minValue = 1,
                            maxValue = maxOf(1, ayahCount),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    RecitationRepeat.Off, RecitationRepeat.Surah -> Unit
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NimazSheetSectionLabel(text = stringResource(R.string.recitation_speed))
                NimazSegmentedControl(
                    options = RecitationSpeed.entries
                        .map { stringResource(R.string.recitation_speed_label, it.label()) }
                        .asSegments(),
                    selectedIndex = RecitationSpeed.entries.indexOf(speed),
                    onSelect = { onSpeedChange(RecitationSpeed.entries[it]) },
                    size = NimazSegmentedSize.SMALL,
                    purpose = NimazSegmentedPurpose.VALUE,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.recitation_follow_along),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.recitation_follow_along_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NimazSwitch(checked = followAlong, onCheckedChange = onFollowAlongChange)
            }
        }
    }
}

/** Beyond this a reader wants a range, not a count. */
private const val MAX_TIMES = 20

/** Which cell of the repeat control this mode is. */
private fun RecitationRepeat.selectedIndex(): Int = when (this) {
    RecitationRepeat.Off -> 0
    is RecitationRepeat.Ayah -> 1
    is RecitationRepeat.Range -> 2
    RecitationRepeat.Surah -> 3
}

/**
 * The mode a tapped cell means, keeping whatever the reader already chose inside it.
 *
 * Switching to Range and back to Verse should not forget the range, so each branch reuses the
 * current value where it can and falls back to a sensible default where it cannot.
 */
private fun RecitationRepeat.forIndex(index: Int, ayahCount: Int): RecitationRepeat = when (index) {
    1 -> RecitationRepeat.ayahClamped(
        (this as? RecitationRepeat.Ayah)?.times ?: RecitationRepeat.DEFAULT_TIMES
    )

    2 -> (this as? RecitationRepeat.Range)
        ?: RecitationRepeat.Range(fromAyah = 1, toAyah = maxOf(1, ayahCount))

    3 -> RecitationRepeat.Surah
    else -> RecitationRepeat.Off
}

/** `0.75`, `1`, `1.25` — without the trailing zero a Float's own toString insists on. */
private fun RecitationSpeed.label(): String =
    if (multiplier == multiplier.toInt().toFloat()) {
        multiplier.toInt().toString()
    } else {
        multiplier.toString().trimEnd('0').trimEnd('.')
    }
