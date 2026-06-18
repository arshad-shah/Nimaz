package com.arshadshah.nimaz.presentation.components.organisms

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.R
import androidx.compose.ui.res.stringResource

/**
 * Top bar for the home screen that smoothly morphs from a location label at
 * rest into a compact "next prayer" summary as the user scrolls. Drive it with
 * [transitionProgress]: 0f = fully at-rest (location); 1f = fully compact
 * (prayer name · time / countdown). Intermediate values render the in-between
 * state, so the bar feels like it rearranges itself in lockstep with the
 * user's drag rather than snapping.
 *
 * - Container color lerps from the hero gradient's top color
 *   (primaryContainer) to surface as content scrolls past.
 * - The location title slides up & fades out; the compact title slides up
 *   from below & fades in. They share the title slot, so at progress=0.5
 *   you see both half-overlapped — that's the morph the user feels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDynamicTopBar(
    transitionProgress: Float,
    locationName: String,
    nextPrayer: PrayerType?,
    nextPrayerTime: String,
    timeUntilNextPrayer: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = transitionProgress.coerceIn(0f, 1f)

    val containerColor = lerp(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.surface,
        progress
    )

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
        ),
        title = {
            Box {
                Box(
                    modifier = Modifier
                        .alpha(1f - progress)
                        .graphicsLayer {
                            translationY = -SLIDE_DISTANCE_DP.toPx() * progress
                        }
                ) {
                    LocationTitle(locationName = locationName)
                }

                Box(
                    modifier = Modifier
                        .alpha(progress)
                        .graphicsLayer {
                            translationY = SLIDE_DISTANCE_DP.toPx() * (1f - progress)
                        }
                ) {
                    CompactPrayerTitle(
                        nextPrayer = nextPrayer,
                        nextPrayerTime = nextPrayerTime,
                        timeUntilNextPrayer = timeUntilNextPrayer
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private val SLIDE_DISTANCE_DP = 12.dp

@Composable
private fun LocationTitle(locationName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = locationName.ifEmpty { "Set location" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompactPrayerTitle(
    nextPrayer: PrayerType?,
    nextPrayerTime: String,
    timeUntilNextPrayer: String,
) {
    val color = getPrayerColor(nextPrayer)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrayerColorDot(color = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = nextPrayer?.displayName ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (nextPrayerTime.isNotEmpty()) {
                Text(
                    text = "  ·  $nextPrayerTime",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "in $timeUntilNextPrayer",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 18.dp)
        )
    }
}

@Composable
private fun PrayerColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, name = "1. At rest (progress = 0)")
@Composable
private fun TopBar_AtRest_Preview() {
    NimazTheme {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            HomeDynamicTopBar(
                transitionProgress = 0f,
                locationName = "Dublin, Ireland",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m",
                onSettingsClick = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "2. Fully compact (progress = 1)")
@Composable
private fun TopBar_FullyCompact_Preview() {
    NimazTheme {
        HomeDynamicTopBar(
            transitionProgress = 1f,
            locationName = "Dublin, Ireland",
            nextPrayer = PrayerType.ASR,
            nextPrayerTime = "4:30 PM",
            timeUntilNextPrayer = "2h 15m",
            onSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 480, name = "3. Transition snapshots")
@Composable
private fun TopBar_TransitionSnapshots_Preview() {
    NimazTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                LabeledTopBar("progress = $p") {
                    HomeDynamicTopBar(
                        transitionProgress = p,
                        locationName = "Dublin, Ireland",
                        nextPrayer = PrayerType.ASR,
                        nextPrayerTime = "4:30 PM",
                        timeUntilNextPrayer = "2h 15m",
                        onSettingsClick = {},
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 480, name = "4. Each prayer (compact)")
@Composable
private fun TopBar_EachPrayer_Preview() {
    NimazTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PrayerType.FAJR to ("5:23 AM" to "30m"),
                PrayerType.SUNRISE to ("6:45 AM" to "1h 52m"),
                PrayerType.DHUHR to ("1:15 PM" to "5h 14m"),
                PrayerType.ASR to ("4:30 PM" to "2h 15m"),
                PrayerType.MAGHRIB to ("6:12 PM" to "1h 04m"),
                PrayerType.ISHA to ("7:45 PM" to "3h 30m"),
            ).forEach { (prayer, info) ->
                HomeDynamicTopBar(
                    transitionProgress = 1f,
                    locationName = "",
                    nextPrayer = prayer,
                    nextPrayerTime = info.first,
                    timeUntilNextPrayer = info.second,
                    onSettingsClick = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 320, name = "5. Countdown durations")
@Composable
private fun TopBar_CountdownDurations_Preview() {
    NimazTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledTopBar("Far away (8h 45m)") {
                HomeDynamicTopBar(
                    transitionProgress = 1f,
                    locationName = "",
                    nextPrayer = PrayerType.FAJR,
                    nextPrayerTime = "5:23 AM",
                    timeUntilNextPrayer = "8h 45m",
                    onSettingsClick = {},
                )
            }
            LabeledTopBar("Close (12m 30s)") {
                HomeDynamicTopBar(
                    transitionProgress = 1f,
                    locationName = "",
                    nextPrayer = PrayerType.MAGHRIB,
                    nextPrayerTime = "6:12 PM",
                    timeUntilNextPrayer = "12m 30s",
                    onSettingsClick = {},
                )
            }
            LabeledTopBar("Imminent (45s)") {
                HomeDynamicTopBar(
                    transitionProgress = 1f,
                    locationName = "",
                    nextPrayer = PrayerType.ASR,
                    nextPrayerTime = "4:30 PM",
                    timeUntilNextPrayer = "45s",
                    onSettingsClick = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "6. Empty state — no location")
@Composable
private fun TopBar_NoLocation_Preview() {
    NimazTheme {
        HomeDynamicTopBar(
            transitionProgress = 0f,
            locationName = "",
            nextPrayer = null,
            nextPrayerTime = "",
            timeUntilNextPrayer = "—",
            onSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "7. Long location name")
@Composable
private fun TopBar_LongLocation_Preview() {
    NimazTheme {
        HomeDynamicTopBar(
            transitionProgress = 0f,
            locationName = "Kingstown upon Hull, East Riding of Yorkshire",
            nextPrayer = PrayerType.DHUHR,
            nextPrayerTime = "1:15 PM",
            timeUntilNextPrayer = "5h 14m",
            onSettingsClick = {},
        )
    }
}

@Composable
private fun LabeledTopBar(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
            letterSpacing = 1.sp
        )
        content()
        Spacer(modifier = Modifier.height(4.dp))
    }
}
