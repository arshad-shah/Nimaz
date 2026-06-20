package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Home hero: the living sky as a banner (current time + date), with the
 * next-prayer info card overlapping its curved bottom.
 *
 * The sky is driven by the clock: [timeOfDay] is recomputed each minute (the
 * sky bakes once per minute, so this is cheap) and the moon phase is derived
 * from today's date. The info card carries the prayer focus — name, time and
 * the live countdown — overlapping the sky so the two read as one unit that
 * flows into the list below.
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
    var timeOfDay by remember { mutableFloatStateOf(minuteFractionNow()) }
    var clock by remember { mutableStateOf(clockLabelNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            timeOfDay = (now.hour * 60 + now.minute) / 1440f
            clock = formatClock12(now.hour, now.minute)
            delay(30_000)
        }
    }
    val moonFraction = remember {
        MoonPhase.fractionForEpochMillis(
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
    }
    val dateLine = hijriDate.ifBlank { gregorianDate }

    Column(modifier = modifier.fillMaxWidth()) {
        // Living sky banner — current time + date overlaid (top-left).
        PrayerSkyScene(
            timeOfDay = timeOfDay,
            timeLabel = clock,
            statusLabel = dateLine,
            moonFraction = moonFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        // Next-prayer card overlapping the sky's curved bottom.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-28).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.next_prayer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = nextPrayer?.displayName ?: "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ArabicText(
                            text = getArabicPrayerName(nextPrayer),
                            size = ArabicTextSize.SMALL,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.at_time, nextPrayerTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = timeUntilNextPrayer,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun minuteFractionNow(): Float {
    val now = LocalTime.now()
    return (now.hour * 60 + now.minute) / 1440f
}

private fun clockLabelNow(): String {
    val now = LocalTime.now()
    return formatClock12(now.hour, now.minute)
}

private fun formatClock12(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour >= 12) "PM" else "AM"
    return String.format("%d:%02d %s", h, minute, amPm)
}

/**
 * Bottom corner radius kept for layouts that mirror the hero's rounding.
 */
val HERO_BOTTOM_RADIUS = 32.dp

@Preview(showBackground = true, widthDp = 412, heightDp = 320)
@Composable
private fun HomeHero_Preview() {
    NimazTheme {
        HomeHero(
            hijriDate = "7 Rajab 1446",
            gregorianDate = "Friday, January 31, 2026",
            nextPrayer = PrayerType.ASR,
            nextPrayerTime = "4:30 PM",
            timeUntilNextPrayer = "1h 12m"
        )
    }
}
