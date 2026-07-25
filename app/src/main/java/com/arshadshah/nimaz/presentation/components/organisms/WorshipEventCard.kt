package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Pre-resolved display data for the Home "Next Worship" card. All strings are localized/formatted
 * by the caller (HomeViewModel) so the composable stays pure. Carried on [EventCardUi.worship].
 */
data class WorshipCardUi(
    val type: WorshipReminderType,
    val name: String,
    val arabic: String,
    val body: String,
    val eventTime: String,
    val timeLabel: String,
    val countdown: String,
    val countdownLabel: String
)

/** Per-type visual treatment for the worship card, mirroring [eventCardVisualsFor]. */
private data class WorshipVisuals(
    val accent: Color,
    val icon: ImageVector,
    val ornament: EventOrnament
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
 * Home "Next Worship" card (Direction A). The single nearest upcoming *enabled* worship reminder,
 * built on [EventCard] so it shares the Jumu'ah card's anatomy: accented icon well, name eyebrow +
 * Arabic, one body line, a trailing event time, and a countdown highlight. All strings are
 * pre-resolved by the caller (localized), so this is a pure, preview-friendly composable.
 */
@Composable
fun WorshipEventCard(
    type: WorshipReminderType,
    name: String,
    arabic: String,
    body: String,
    eventTime: String,
    timeLabel: String,
    countdown: String,
    countdownLabel: String,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    val v = worshipVisualsFor(type)
    EventCard(
        accent = v.accent,
        containerAccent = v.accent,
        icon = v.icon,
        ornament = v.ornament,
        eyebrow = name,
        arabic = arabic,
        body = body,
        fillHeight = fillHeight,
        trailing = if (eventTime.isNotEmpty()) {
            {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = eventTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (timeLabel.isNotEmpty()) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else null,
        highlight = if (countdown.isNotEmpty()) {
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countdownLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = v.accent
                    )
                }
            }
        } else null,
        modifier = modifier,
    )
}

// ── Previews (the "so I can see it" deliverable — one per representative type) ──

@Composable
private fun sample(type: WorshipReminderType, name: String, arabic: String, body: String) {
    WorshipEventCard(
        type = type, name = name, arabic = arabic, body = body,
        eventTime = "2:48 AM", timeLabel = "Begins",
        countdown = "4h 12m", countdownLabel = "Begins in",
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Tahajjud — light")
@Composable
private fun WorshipCard_Tahajjud_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        sample(WorshipReminderType.TAHAJJUD, "Tahajjud", "تَهَجُّد", "A blessed time for du'a has begun.")
    }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Tahajjud — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun WorshipCard_Tahajjud_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        sample(WorshipReminderType.TAHAJJUD, "Tahajjud", "تَهَجُّد", "A blessed time for du'a has begun.")
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Iftar — light")
@Composable
private fun WorshipCard_Iftar_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        WorshipEventCard(
            type = WorshipReminderType.IFTAR, name = "Iftar", arabic = "إفْطار",
            body = "Maghrib has entered. Break your fast.",
            eventTime = "6:41 PM", timeLabel = "Maghrib",
            countdown = "22m", countdownLabel = "Iftar in",
        )
    }
}

@Preview(showBackground = true, widthDp = 400, fontScale = 2f, name = "Suhoor — 200% font")
@Composable
private fun WorshipCard_Suhoor_LargeFont() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        WorshipEventCard(
            type = WorshipReminderType.SUHOOR, name = "Suhoor", arabic = "سُحُور",
            body = "Fajr is approaching. Finish your suhoor.",
            eventTime = "4:52 AM", timeLabel = "Fajr",
            countdown = "30m", countdownLabel = "Ends in",
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Laylatul Qadr — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun WorshipCard_Qadr_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        WorshipEventCard(
            type = WorshipReminderType.LAYLATUL_QADR, name = "Laylatul Qadr", arabic = "لَيْلَة القَدْر",
            body = "An odd night of the last ten. Seek the Night of Decree.",
            eventTime = "Night 27", timeLabel = "Last ten",
            countdown = "Tonight", countdownLabel = "Seek",
        )
    }
}
