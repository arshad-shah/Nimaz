package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.EventProximity
import com.arshadshah.nimaz.core.util.progressToward
import com.arshadshah.nimaz.core.util.proximityOf
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.atoms.NimazClockText
import com.arshadshah.nimaz.presentation.components.atoms.NimazCountdownText
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.theme.LocalAnimationsEnabled
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Display model for the Home "Next Worship" card.
 *
 * ## What changed
 *
 * The previous `WorshipCardUi` carried pre-formatted strings built in
 * `HomeViewModel.renderWorshipCard()` with `context.getString`, refreshed by a
 * 60 s loop. That is the root of the card feeling dead:
 *
 *  - the countdown was minute-resolution and refreshed once a minute, so it sat
 *    at "0m" for up to sixty seconds and jumped in 60 s steps directly below a
 *    prayer countdown ticking every second;
 *  - after toggling the 12/24-hour preference the card's time stayed in the old
 *    format until the next 60 s tick;
 *  - nothing could animate or respond to how close the event was, because the
 *    card only ever received a finished string.
 *
 * Now the card receives **instants** and derives everything itself.
 *
 * @param eventAt the instant the event happens (Maghrib for Iftar, the start of
 *   the last third for Tahajjud, …).
 * @param windowStart the anchor the progress arc measures from — normally the
 *   previous prayer. Null hides the arc rather than inventing a span.
 * @param windowEnd when the event's window closes; between [eventAt] and this the
 *   card is [EventProximity.ACTIVE]. Null means it flips straight to passed.
 */
data class WorshipCardUi(
    val type: WorshipReminderType,
    val name: String,
    val arabic: String,
    val body: String,
    val eventAt: Instant,
    val windowStart: Instant? = null,
    val windowEnd: Instant? = null,
    val subKey: String? = null,
)

/** Per-type visual treatment for the worship card, mirroring [eventCardVisualsFor]. */
private data class WorshipVisuals(
    val accent: Color,
    val icon: ImageVector,
    val ornament: EventOrnament,
)

private fun worshipVisualsFor(type: WorshipReminderType): WorshipVisuals = when (type) {
    WorshipReminderType.TAHAJJUD ->
        WorshipVisuals(NimazPalette.MatPurple, Icons.Filled.NightsStay, EventOrnament.Pattern(NimazPatternStyle.STAR_FIELD))
    WorshipReminderType.WITR ->
        WorshipVisuals(NimazPalette.Teal700, Icons.Filled.Bedtime, EventOrnament.Divider)
    WorshipReminderType.SUHOOR ->
        WorshipVisuals(NimazPalette.Amber700, Icons.Filled.WbTwilight, EventOrnament.Divider)
    WorshipReminderType.IFTAR ->
        WorshipVisuals(NimazPalette.GoldDark, Icons.Filled.LocalDining, EventOrnament.Divider)
    WorshipReminderType.TARAWEEH ->
        WorshipVisuals(NimazPalette.MatPurple, Icons.Filled.Mosque, EventOrnament.Pattern(NimazPatternStyle.LATTICE))
    WorshipReminderType.LAYLATUL_QADR ->
        WorshipVisuals(NimazPalette.MatPurple, Icons.Filled.AutoAwesome, EventOrnament.Pattern(NimazPatternStyle.STAR_FIELD))
    WorshipReminderType.ADHKAR_MORNING ->
        WorshipVisuals(NimazPalette.Amber700, Icons.Filled.WbSunny, EventOrnament.Divider)
    WorshipReminderType.ADHKAR_EVENING ->
        WorshipVisuals(NimazPalette.Teal700, Icons.Filled.SelfImprovement, EventOrnament.Divider)
    WorshipReminderType.MONDAY_THURSDAY_FAST ->
        WorshipVisuals(NimazPalette.Teal700, Icons.Filled.CalendarMonth, EventOrnament.Divider)
    WorshipReminderType.WHITE_DAYS_FAST ->
        WorshipVisuals(NimazPalette.Teal700, Icons.Outlined.WaterDrop, EventOrnament.Divider)
    WorshipReminderType.ARAFAH_ASHURA_FAST ->
        WorshipVisuals(NimazPalette.Teal700, Icons.Outlined.Terrain, EventOrnament.Divider)
}

/**
 * The worship card.
 *
 * Driven by [EventProximity] rather than a number alone, so it changes *weight*
 * as the event approaches instead of only changing digits:
 *
 * | Proximity     | Treatment                                                  |
 * |---------------|------------------------------------------------------------|
 * | `DISTANT`     | Quiet. Hairline accent, time as a small trailing label.    |
 * | `APPROACHING` | Countdown becomes the focal element; accent fill ramps in. |
 * | `IMMINENT`    | Seconds appear; accent saturates; the arc completes.       |
 * | `ACTIVE`      | Flips from a countdown to a "now" state with its action.    |
 * | `PASSED`      | Renders nothing — the resolver should have moved on.        |
 *
 * All motion honours `LocalAnimationsEnabled`.
 */
@Composable
fun WorshipEventCard(
    card: WorshipCardUi,
    modifier: Modifier = Modifier,
    onAction: ((WorshipReminderType) -> Unit)? = null,
    fillHeight: Boolean = false,
) {
    val v = worshipVisualsFor(card.type)
    val animationsEnabled = LocalAnimationsEnabled.current

    // A minute-resolution read is enough to classify proximity; the countdown text
    // below independently escalates itself to seconds when it needs to.
    val now by rememberNow(TickResolution.MINUTES)
    val proximity = proximityOf(card.eventAt, now, card.windowEnd)
    if (proximity == EventProximity.PASSED) return

    // Accent presence ramps with proximity — the glanceable signal.
    val accentWeight = when (proximity) {
        EventProximity.DISTANT -> 0.35f
        EventProximity.APPROACHING -> 0.7f
        EventProximity.IMMINENT, EventProximity.ACTIVE -> 1f
        EventProximity.PASSED -> 0f
    }
    val accent by animateColorAsState(
        targetValue = v.accent.copy(alpha = accentWeight),
        animationSpec = tween(if (animationsEnabled) 600 else 0),
        label = "worship_accent",
    )

    val progress = card.windowStart?.let { progressToward(card.eventAt, it, now) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(if (animationsEnabled) 600 else 0),
        label = "worship_progress",
    )

    EventCard(
        accent = accent,
        containerAccent = v.accent,
        icon = v.icon,
        ornament = v.ornament,
        eyebrow = card.name,
        arabic = card.arabic,
        body = card.body,
        fillHeight = fillHeight,
        modifier = if (progress != null) {
            modifier.drawBehind { drawProgressArc(animatedProgress, v.accent) }
        } else {
            modifier
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                NimazClockText(
                    instant = card.eventAt,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                timeLabelFor(card.type)?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        highlight = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (proximity == EventProximity.ACTIVE) R.string.worship_card_now
                        else R.string.worship_card_in
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (proximity != EventProximity.ACTIVE) {
                    NimazCountdownText(
                        target = card.eventAt,
                        // Seconds only in the final approach — a card four hours out has no
                        // business recomposing at 1 Hz.
                        showSeconds = proximity == EventProximity.IMMINENT,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = v.accent,
                    )
                }
            }
        },
        primaryAction = onAction?.let { handler ->
            actionLabelFor(card.type)?.let { label ->
                EventAction(label = label) { handler(card.type) }
            }
        },
    )
}

/**
 * The arc around the card showing where "now" sits between [WorshipCardUi.windowStart]
 * and the event.
 *
 * ⚠️ The `inset`/`diameter` below hardcode where [EventCard] currently draws its icon
 * well (12.dp padding, ~40.dp well). A padding change in [EventCard] silently misaligns
 * this. Tracked as follow-up: give [EventCard] an optional icon-decoration slot so the
 * arc is laid out *by* the well rather than guessed relative to the card.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressArc(
    progress: Float,
    accent: Color,
) {
    val stroke = 3.dp.toPx()
    val diameter = 40.dp.toPx()
    val inset = 12.dp.toPx()
    val arcSize = Size(diameter, diameter)
    drawArc(
        color = accent.copy(alpha = 0.18f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(width = stroke),
    )
    drawArc(
        color = accent,
        startAngle = -90f,
        sweepAngle = 360f * progress.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(width = stroke),
    )
}

/**
 * The obvious next step for each reminder — each is a screen that already exists.
 */
@Composable
private fun actionLabelFor(type: WorshipReminderType): String? = when (type) {
    WorshipReminderType.TAHAJJUD -> stringResource(R.string.worship_action_duas)
    WorshipReminderType.WITR -> stringResource(R.string.worship_action_duas)
    WorshipReminderType.SUHOOR -> stringResource(R.string.worship_action_fast_tracker)
    WorshipReminderType.IFTAR -> stringResource(R.string.worship_action_iftar_dua)
    WorshipReminderType.TARAWEEH -> stringResource(R.string.worship_action_quran)
    WorshipReminderType.LAYLATUL_QADR -> stringResource(R.string.worship_action_duas)
    WorshipReminderType.ADHKAR_MORNING,
    WorshipReminderType.ADHKAR_EVENING -> stringResource(R.string.worship_action_adhkar)
    WorshipReminderType.MONDAY_THURSDAY_FAST,
    WorshipReminderType.WHITE_DAYS_FAST,
    WorshipReminderType.ARAFAH_ASHURA_FAST -> stringResource(R.string.worship_action_fast_tracker)
}

/** Shown under the time, only where it adds meaning. */
@Composable
private fun timeLabelFor(type: WorshipReminderType): String? = when (type) {
    WorshipReminderType.TAHAJJUD -> stringResource(R.string.worship_card_begins)
    WorshipReminderType.IFTAR -> stringResource(R.string.prayer_maghrib)
    WorshipReminderType.SUHOOR -> stringResource(R.string.prayer_fajr)
    else -> null
}

// ── Previews ──

@Composable
private fun sample(type: WorshipReminderType, name: String, arabic: String, body: String, inHours: Long) {
    val now = Clock.System.now()
    WorshipEventCard(
        card = WorshipCardUi(
            type = type, name = name, arabic = arabic, body = body,
            eventAt = now + inHours.hours,
            windowStart = now - 1.hours,
            windowEnd = now + (inHours + 4).hours,
        ),
        onAction = {},
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Tahajjud — light")
@Composable
private fun WorshipCard_Tahajjud_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        sample(WorshipReminderType.TAHAJJUD, "Tahajjud", "تَهَجُّد", "A blessed time for du'a.", inHours = 4)
    }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Iftar — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun WorshipCard_Iftar_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        sample(WorshipReminderType.IFTAR, "Iftar", "إفْطار", "Maghrib has entered. Break your fast.", inHours = 1)
    }
}
