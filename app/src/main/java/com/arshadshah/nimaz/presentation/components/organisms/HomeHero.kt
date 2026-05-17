package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.getArabicPrayerName
import com.arshadshah.nimaz.presentation.components.molecules.CountdownTimer
import com.arshadshah.nimaz.presentation.theme.LocalUseHijriPrimary
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Centred prayer-info hero shown at the top of the compact (phone) home
 * screen. Composes the date pair, the next-prayer label/name (English &
 * Arabic), the [CountdownTimer], and the prayer's actual clock time.
 *
 * Background gradient blends into the screen background as the user scrolls
 * past, while the dynamic top bar takes over above.
 */
@Composable
fun HomeHero(
    hijriDate: String,
    gregorianDate: String,
    nextPrayer: PrayerType?,
    nextPrayerTime: String,
    timeUntilNextPrayer: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Clip BEFORE the gradient background so the gradient is painted
            // within the rounded bounds — produces clean, anti-aliased curves
            // at any density. The LazyColumn's background shows through the
            // corner "cut-outs", making the hero read as a distinct container.
            .clip(RoundedCornerShape(bottomStart = HERO_BOTTOM_RADIUS, bottomEnd = HERO_BOTTOM_RADIUS))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        val useHijriPrimary = LocalUseHijriPrimary.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 30.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (useHijriPrimary) hijriDate.ifEmpty { "" } else gregorianDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (useHijriPrimary) gregorianDate else hijriDate.ifEmpty { "" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.next_prayer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = nextPrayer?.displayName ?: "—",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            ArabicText(
                text = getArabicPrayerName(nextPrayer),
                size = ArabicTextSize.MEDIUM,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            CountdownTimer(timeUntilNextPrayer = timeUntilNextPrayer)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.at_time, nextPrayerTime),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Bottom corner radius applied to the hero, exported so the screen layout can
 * mirror it if it ever needs to overlap the hero with another container.
 * 32.dp is wide enough to read as a deliberate "scoop" without dominating the
 * silhouette, and matches the rounded corners on the cards beneath
 * ([TodaysProgressCard] uses 20.dp; the hero gets a slightly larger radius to
 * read as the parent container of the layout).
 */
val HERO_BOTTOM_RADIUS = 32.dp

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun HomeHero_Preview() {
    NimazTheme {
        HomeHero(
            hijriDate = "7 Rajab 1446",
            gregorianDate = "Friday, January 31, 2026",
            nextPrayer = PrayerType.ASR,
            nextPrayerTime = "4:30 PM",
            timeUntilNextPrayer = "2h 15m 30s"
        )
    }
}

/**
 * Preview showing the hero on top of a tinted background so you can see the
 * curved bottom cutting against content that scrolls beneath it.
 */
@Preview(showBackground = true, widthDp = 412, heightDp = 560, name = "Hero on background")
@Composable
private fun HomeHero_WithSpacingBelow_Preview() {
    NimazTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s"
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
