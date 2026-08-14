package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazQadaPrayerItem
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.tracker.QadaPrayersUiState

/**
 * The make-up prayers a user owes: a summary, then the list, grouped by month.
 *
 * Reachable two ways — as its own screen ([QadaPrayersScreen]) and as a tab inside
 * [PrayerTrackerScreen] — and written twice for it, 46 lines of list plus a 30-line summary
 * card each (audit §1.4). Only the surrounding chrome differs: the standalone screen has a top
 * bar and its own padding, the tab has neither.
 *
 * The two copies had already drifted, which is the argument for this file existing: the tab's
 * summary card still hand-rolled its icon container out of a `Box` + `clip` + `background`
 * while the screen's used `NimazIcon(type = CONTAINED)`. The design-system one is what
 * survives here.
 */
@Composable
fun QadaPrayerList(
    state: QadaPrayersUiState,
    onMarkCompleted: (PrayerRecord) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            QadaSummaryCard(totalMissed = state.totalMissed)
        }

        if (state.missedPrayers.isEmpty() && !state.isLoading) {
            item {
                NimazEmptyState(
                    title = stringResource(R.string.all_caught_up),
                    message = stringResource(R.string.all_caught_up_message)
                )
            }
        }

        state.groupedByMonth.forEach { (monthYear, prayers) ->
            item {
                Text(
                    text = monthYear,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(prayers, key = { it.id }) { prayer ->
                NimazQadaPrayerItem(
                    prayer = prayer,
                    actionText = stringResource(R.string.qada_mark_made_up),
                    onMarkCompleted = { onMarkCompleted(prayer) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** How many prayers are outstanding, in one card. */
@Composable
private fun QadaSummaryCard(totalMissed: Int) {
    val warningOrange = NimazColors.PrayerColors.Asr
    val warningOrangeDark = NimazColors.OrangeDark

    GradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradientColors = listOf(warningOrange, warningOrangeDark)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NimazIcon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = Color.White,
                containerColor = Color.Black.copy(alpha = 0.15f),
                containerSize = 48.dp,
                iconSize = 28.dp,
                cornerRadius = 12.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.prayers_to_make_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (totalMissed == 0) {
                        stringResource(R.string.all_caught_up_short)
                    } else {
                        pluralStringResource(
                            R.plurals.missed_prayers_pending,
                            totalMissed,
                            totalMissed
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Text(
                text = "$totalMissed",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
