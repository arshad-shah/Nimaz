package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimeCard
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay

private fun isDone(status: PrayerStatus): Boolean =
    status == PrayerStatus.PRAYED || status == PrayerStatus.QADA || status == PrayerStatus.LATE

/**
 * Prayer section for the home compact layout.
 *
 * Shows a [NimazSectionHeader] (title + done-count badge + settings icon) above a [NimazCard]
 * that contains all [PrayerTimeCard] rows and an "Open the tracker" text button at the bottom.
 */
@Composable
fun HomePrayerCard(
    prayers: List<PrayerTimeDisplay>,
    onSettingsClick: () -> Unit,
    onTrackerClick: () -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneCount = prayers.count { isDone(it.prayerStatus) }

    Column(modifier = modifier.fillMaxWidth()) {
        NimazSectionHeader(
            title = stringResource(R.string.home_todays_prayers),
            trailingText = "$doneCount of 5",
            trailingContent = {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Text(
                        text = "$doneCount of 5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    NimazIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick,
                        contentDescription = stringResource(R.string.home_prayer_settings_cd),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        NimazCard(
            style = NimazCardStyle.FILLED,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            prayers.forEach { prayer ->
                PrayerTimeCard(
                    prayer = prayer,
                    isActive = prayer.isNext,
                    onClick = onTrackerClick,
                    onToggle = { onTogglePrayer(prayer.type) },
                )
            }
            NimazButton(
                text = stringResource(R.string.home_open_tracker),
                onClick = onTrackerClick,
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
