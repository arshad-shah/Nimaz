package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Header for the home screen's prayer-times list. Replaces the previous
 * "Settings" text link (which was a 24pt bodySmall tap target buried in the
 * corner) with a 40dp [FilledTonalIconButton] showing a tune/settings icon.
 *
 * Two-line title: bold "Prayer Times" on top, smaller "X passed · Y to go"
 * underneath so the user gets a glanceable progress hint right at the
 * section header — no need to count cards manually.
 *
 * The settings button uses the [Icons.Default.Tune] glyph (the slider/adjust
 * icon) rather than the generic gear because "tune" reads as "configure the
 * specifics of this thing" while the gear has been overloaded to mean
 * "global settings."
 */
@Composable
fun PrayerTimesSectionHeader(
    passedCount: Int,
    upcomingCount: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.prayer_times),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (passedCount + upcomingCount > 0) {
                Text(
                    text = buildSubtitle(passedCount, upcomingCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FilledTonalIconButton(
            onClick = onSettingsClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun buildSubtitle(passed: Int, upcoming: Int): String = when {
    passed == 0 -> "$upcoming to go"
    upcoming == 0 -> "all done for today"
    else -> "$passed passed · $upcoming to go"
}

@Preview(showBackground = true, widthDp = 400, name = "Mid-day (2 passed, 3 to go)")
@Composable
private fun PrayerTimesSectionHeader_MidDay_Preview() {
    NimazTheme {
        PrayerTimesSectionHeader(
            passedCount = 2,
            upcomingCount = 3,
            onSettingsClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Early morning (0 passed)")
@Composable
private fun PrayerTimesSectionHeader_Morning_Preview() {
    NimazTheme {
        PrayerTimesSectionHeader(
            passedCount = 0,
            upcomingCount = 5,
            onSettingsClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Late evening (all done)")
@Composable
private fun PrayerTimesSectionHeader_AllDone_Preview() {
    NimazTheme {
        PrayerTimesSectionHeader(
            passedCount = 5,
            upcomingCount = 0,
            onSettingsClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
