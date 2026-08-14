package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.model.PrayerDisplayStatus
import com.arshadshah.nimaz.presentation.model.tone

/**
 * A pill badge for a single prayer's recorded status.
 *
 * Wraps [NimazBadge] with the correct [NimazTone] and [NimazBadgeEmphasis] for each
 * [PrayerDisplayStatus], matching the prototype's `.pill` CSS and the prayer tracker's badge.
 *
 * - PRAYED  → SUCCESS/SOFT (green)
 * - LATE    → ACCENT/SOFT (blue)
 * - QADA    → PROMINENT/SOFT (purple)
 * - MISSED  → ERROR/SOFT (red)
 * - NOT_RECORDED → WARNING/OUTLINED (gold ring — absence of information)
 * - UPCOMING → MUTED/SOFT (surface-2 / secondary text)
 *
 * Use this in both the home compact prayer card and the prayer tracker day card.
 */
@Composable
fun PrayerStatusBadge(
    status: PrayerDisplayStatus,
    modifier: Modifier = Modifier,
    size: NimazBadgeSize = NimazBadgeSize.SMALL,
) {
    NimazBadge(
        text = status.label(),
        tone = status.tone(),
        size = size,
        emphasis = if (status == PrayerDisplayStatus.NOT_RECORDED)
            NimazBadgeEmphasis.OUTLINED
        else
            NimazBadgeEmphasis.SOFT,
        modifier = modifier,
    )
}

/** Localised label for a [PrayerDisplayStatus]. Composable because it calls [stringResource]. */
@Composable
fun PrayerDisplayStatus.label(): String = when (this) {
    PrayerDisplayStatus.PRAYED        -> stringResource(R.string.on_time)
    PrayerDisplayStatus.LATE          -> stringResource(R.string.late)
    PrayerDisplayStatus.QADA          -> stringResource(R.string.made_up)
    PrayerDisplayStatus.MISSED        -> stringResource(R.string.missed)
    PrayerDisplayStatus.NOT_RECORDED  -> stringResource(R.string.prayer_status_not_recorded)
    PrayerDisplayStatus.UPCOMING      -> stringResource(R.string.upcoming)
}
