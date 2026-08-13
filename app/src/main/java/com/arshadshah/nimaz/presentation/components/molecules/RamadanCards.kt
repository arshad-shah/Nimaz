package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.formatLongDate
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.LocalDate

/**
 * The three Ramadan cards.
 *
 * They were private composables inside `FastTrackerScreen.kt` (issue #492). A Ramadan
 * countdown is not fasting-tab-specific — the home screen and the calendar have as much claim
 * on one — so the only thing keeping them there was the file they happened to be written in.
 *
 * **They were also not pure, and moving them is what made that matter.** The countdown called
 * `HijriDateCalculator.daysUntilNextRamadan()` and `today()` at composition, and the tracker
 * called `LocalDate.now()` and did its own arithmetic. A composable that reads the clock is a
 * screen open across midnight showing yesterday's answer; shared, it is that defect in every
 * future caller. So the values arrive as parameters now, from
 * `GetRamadanCountdownUseCase` and `CountUnloggedRamadanDaysUseCase`, which take the
 * `TodayProvider` seam and can be tested.
 */

/** Progress through Ramadan: which day it is, and how many of them have been fasted. */
@Composable
fun RamadanBanner(
    fastedDays: Int,
    totalDays: Int,
    currentDay: Int,
    modifier: Modifier = Modifier,
) {
    GradientCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        gradientColors = listOf(
            NimazColors.FastingColors.Ramadan,
            NimazColors.FastingColors.Ramadan.copy(alpha = 0.85f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.fasting_current),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.fasting_ramadan_day, currentDay),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { if (totalDays > 0) fastedDays.toFloat() / totalDays else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Text(
                    text = "$fastedDays/$totalDays",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * How long until Ramadan begins.
 *
 * @param daysAway from `GetRamadanCountdownUseCase`, not from the clock at composition.
 */
@Composable
fun RamadanCountdownCard(
    daysAway: Int,
    startsOn: LocalDate,
    modifier: Modifier = Modifier,
) {
    GradientCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        gradientColors = listOf(
            NimazColors.FastingColors.Ramadan,
            NimazColors.FastingColors.Ramadan.copy(alpha = 0.8f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.fasting_ramadan_starts_in),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$daysAway",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = if (daysAway == 1) {
                    stringResource(R.string.fasting_day)
                } else {
                    stringResource(R.string.fasting_days)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = startsOn.formatLongDate(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * A nudge about days of Ramadan that have gone by with nothing recorded against them.
 *
 * Renders nothing when there are none, so a caller can place it unconditionally.
 *
 * @param unloggedDays from `CountUnloggedRamadanDaysUseCase`.
 */
@Composable
fun RamadanMissedFastsTracker(
    unloggedDays: Int,
    modifier: Modifier = Modifier,
) {
    if (unloggedDays <= 0) return

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(14.dp),
        colors = NimazCardDefaults.colors(
            container = NimazColors.PrayerColors.Maghrib.copy(alpha = 0.1f),
            border = NimazColors.PrayerColors.Maghrib.copy(alpha = 0.3f),
            borderWidth = 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NimazColors.PrayerColors.Maghrib.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$unloggedDays",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.PrayerColors.Maghrib
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (unloggedDays == 1) {
                        stringResource(R.string.fasting_unlogged_day)
                    } else {
                        stringResource(R.string.fasting_unlogged_days)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.fasting_log_calendar_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RamadanBannerPreview() {
    NimazTheme {
        RamadanBanner(fastedDays = 15, totalDays = 30, currentDay = 16)
    }
}

@Preview(showBackground = true)
@Composable
private fun RamadanCountdownCardPreview() {
    NimazTheme {
        // A fixed date, because a preview that reads the clock renders differently every day.
        RamadanCountdownCard(daysAway = 42, startsOn = LocalDate.of(2027, 2, 8))
    }
}

@Preview(showBackground = true)
@Composable
private fun RamadanMissedFastsTrackerPreview() {
    NimazTheme {
        RamadanMissedFastsTracker(unloggedDays = 3)
    }
}
