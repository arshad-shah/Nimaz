package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.formatClockTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.GlassPill
import com.arshadshah.nimaz.presentation.components.atoms.GlassPillTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.getArabicPrayerName
import com.arshadshah.nimaz.presentation.components.atoms.glassBackdropSource
import com.arshadshah.nimaz.presentation.components.atoms.rememberGlassBackdrop
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import com.arshadshah.nimaz.presentation.theme.LocalUseHijriPrimary
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds
import com.arshadshah.nimaz.presentation.components.atoms.NimazCountdownText
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import kotlin.time.Instant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Home hero: a living-sky banner (current time + date) at the original hero
 * height and corners (square top, rounded bottom), with the next-prayer card
 * overlapping its curved bottom.
 *
 * Location + settings live in the dynamic top bar above, so the sky stays
 * content-light and the sun arc has room to read. The card carries the prayer
 * focus — name, time and the live countdown — and overlaps the sky so the two
 * read as one unit that flows into the list below.
 */
@Composable
fun HomeHero(
    hijriDate: String,
    gregorianDate: String,
    nextPrayer: PrayerType?,
    nextPrayerAt: Instant?,
    modifier: Modifier = Modifier,
    sunriseFraction: Float = 0.27f,
    sunsetFraction: Float = 0.80f,
) {
    val use24Hour = LocalUse24HourFormat.current
    // One shared minute-resolution read replaces the old private 30s loop, so the
    // displayed minute now flips on the real minute boundary instead of up to half a
    // minute late — and it costs nothing when the app is backgrounded.
    val nowInstant by rememberNow(TickResolution.MINUTES)
    val nowLocalTime = remember(nowInstant) {
        java.time.Instant.ofEpochMilli(nowInstant.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault()).toLocalTime()
    }
    val timeOfDay = (nowLocalTime.hour * 60 + nowLocalTime.minute) / 1440f
    val clock = formatClockTime(nowLocalTime.hour, nowLocalTime.minute, use24Hour)

    val backdrop = rememberGlassBackdrop()
    val moonFraction = remember {
        MoonPhase.fractionForEpochMillis(
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
    }
    val useHijriPrimary = LocalUseHijriPrimary.current
    val shadow =
        Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 1f), blurRadius = 7f)

    // The screen is edge-to-edge, so grow the sky by the status-bar inset to
    // leave a clean band of empty sky behind the status bar at the top.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(modifier = modifier.fillMaxWidth()) {
        // Living-sky banner — original hero height plus the status-bar band.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_SKY_HEIGHT + statusBarTop)
        ) {
            SkyBackground(
                timeOfDay = timeOfDay,
                moonFraction = moonFraction,
                modifier = Modifier.matchParentSize().glassBackdropSource(backdrop),
                shape = RoundedCornerShape(
                    bottomStart = HERO_BOTTOM_RADIUS,
                    bottomEnd = HERO_BOTTOM_RADIUS
                ),
                sunriseFraction = sunriseFraction,
                sunsetFraction = sunsetFraction,
            )
            Column(
                // Centre within the *visible* sky (below the status-bar band),
                // not the full box — otherwise the content reads as too high.
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = statusBarTop),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPill(
                    text = clock,
                    tone = GlassPillTone.Ghost,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = shadow
                    ),
                    backdrop = backdrop,
                )

                GlassPill(
                    text = if (useHijriPrimary) hijriDate.ifEmpty { gregorianDate } else gregorianDate,
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                    backdrop = backdrop,
                )
                if (useHijriPrimary && gregorianDate.isNotEmpty()) {
                    GlassPill(
                        text = gregorianDate,
                        style = MaterialTheme.typography.bodySmall.copy(shadow = shadow),
                        backdrop = backdrop,
                    )


                }
            }
        }

        // Next-prayer card overlapping the sky's curved bottom.
        NimazCard(
            style = NimazCardStyle.FILLED,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-10).dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 4.dp,
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
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
//                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                        )
                    }
                    if (nextPrayerAt != null) {
                        Text(
                            text = stringResource(R.string.at_time, clockTimeText(nextPrayerAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (nextPrayerAt != null) {
                    // Ticks itself off the shared clock — seconds only in the final approach.
                    NimazCountdownText(
                        target = nextPrayerAt,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun minuteFractionNow(): Float {
    val now = LocalTime.now()
    return (now.hour * 60 + now.minute) / 1440f
}

/** Sky-banner height — matches the original hero's height. */
val HERO_SKY_HEIGHT = 280.dp

/** Bottom corner radius applied to the hero. */
val HERO_BOTTOM_RADIUS = 32.dp

@Preview(showBackground = true, widthDp = 412, heightDp = 380)
@Composable
private fun HomeHero_Preview() {
    NimazTheme(
        useHijriPrimary = true
    ) {
        HomeHero(
            hijriDate = "7 Rajab 1446",
            gregorianDate = "Friday, January 31, 2026",
            nextPrayer = PrayerType.MAGHRIB,
            nextPrayerAt = Clock.System.now() + 1.hours,

            )
    }
}
