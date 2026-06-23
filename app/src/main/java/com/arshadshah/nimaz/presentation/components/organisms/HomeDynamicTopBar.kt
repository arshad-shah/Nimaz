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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.GlassIconButton
import com.arshadshah.nimaz.presentation.components.atoms.GlassPill
import com.arshadshah.nimaz.presentation.components.atoms.GlassPillTone
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.components.atoms.rememberGlassBackdrop
import com.arshadshah.nimaz.presentation.theme.MiscArtColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Home top bar that overlays the living-sky hero and morphs with scroll.
 *
 * Drive it with [transitionProgress]: 0f = at rest, 1f = fully scrolled.
 *
 * - At rest it is *transparent* and blends into the sky: a translucent
 *   location pill on the left and a scrim-backed settings button on the right,
 *   floating over the sky. A soft top gradient keeps the status-bar icons and
 *   pill legible over a bright midday sky.
 * - As the user scrolls, the bar fills in with [MaterialTheme.colorScheme.surface],
 *   the scrim fades out, a hairline divider fades in, and the location cross-fades
 *   into a compact "next prayer · time / countdown" summary.
 *
 * This is meant to be placed as an OVERLAY (a sibling in a Box on top of the
 * scrolling content), not in a Scaffold's topBar slot — so the hero renders
 * behind it (including behind the status bar) and the list scrolls underneath.
 * It carries its own statusBarsPadding.
 */
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
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val backdrop = rememberGlassBackdrop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = progress))
            .drawBehind {
                // At-rest scrim keeps the status-bar icons + pill legible over a
                // bright sky; fades out as the bar solidifies.
                if (progress < 1f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.22f),
                            1f to Color.Transparent,
                        ),
                        alpha = 1f - progress,
                    )
                }
                // Hairline divider once the bar is solid.
                if (progress > 0f) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                        alpha = progress,
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title slot — pill and prayer summary share it, cross-fading.
            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .alpha(1f - progress)
                        .graphicsLayer { translationY = -SLIDE_DISTANCE_DP.toPx() * progress }
                ) {
                    GlassPill(
                        text = locationName.ifEmpty { stringResource(R.string.location_set_prompt) },
                        leadingIcon = Icons.Default.LocationOn,
                        tone = GlassPillTone.Solid,
                        backdrop = backdrop,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .alpha(progress)
                        .graphicsLayer { translationY = SLIDE_DISTANCE_DP.toPx() * (1f - progress) }
                ) {
                    CompactPrayerTitle(
                        nextPrayer = nextPrayer,
                        nextPrayerTime = nextPrayerTime,
                        timeUntilNextPrayer = timeUntilNextPrayer,
                    )
                }
            }

            SettingsButton(progress = progress, onClick = onSettingsClick)
        }
    }
}

private val SLIDE_DISTANCE_DP = 12.dp


@Composable
private fun SettingsButton(progress: Float, onClick: () -> Unit) {
    // The glass cross-fades with the bar: bright white over the sky, settling to
    // a surface tint as the bar solidifies — no separate scrim needed.
    val tint = lerp(Color.White, MaterialTheme.colorScheme.onSurfaceVariant, progress)
    GlassIconButton(
        icon = Icons.Default.Settings,
        contentDescription = stringResource(R.string.cd_settings),
        onClick = onClick,
        tint = tint,
    )
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

@Preview(showBackground = true, widthDp = 412, name = "1. At rest (over sky)")
@Composable
private fun TopBar_AtRest_Preview() {
    NimazTheme {
        Box(
            modifier = Modifier.background(
                MiscArtColors.TopBarBlue
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

@Preview(showBackground = true, widthDp = 412, heightDp = 520, name = "3. Transition snapshots")
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

@Preview(showBackground = true, widthDp = 412, name = "4. Long location name")
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
