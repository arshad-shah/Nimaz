package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Occasion behind an [EventCard] — selects accent, icon, and background ornament. */
enum class EventOccasion {
    EID_AL_FITR, EID_AL_ADHA, RAMADAN, LAYLAT_AL_QADR, ARAFAH,
    ASHURA, MAWLID, HIJRI_NEW_YEAR, JUMUAH, GENERIC
}

/** Resolved visual treatment for an occasion. */
data class EventCardVisuals(
    val accent: Color,
    val containerAccent: Color,
    val icon: ImageVector,
    val ornament: EventOrnament,
)

/**
 * Maps an occasion to its house-style accent/icon/ornament (spec §3.3).
 * Accents are text-safe on white; gold is structural (well/border only), so Eid
 * uses GoldDark for the icon tint but Gold500 for the well container.
 */
fun eventCardVisualsFor(occasion: EventOccasion): EventCardVisuals = when (occasion) {
    EventOccasion.EID_AL_FITR -> EventCardVisuals(
        accent = NimazPalette.GoldDark,
        containerAccent = NimazPalette.Gold500,
        icon = Icons.Filled.Celebration,
        ornament = EventOrnament.Burst(play = true),
    )
    EventOccasion.EID_AL_ADHA -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Filled.Mosque,
        ornament = EventOrnament.Pattern(NimazPatternStyle.CORNER_MEDALLION),
    )
    EventOccasion.RAMADAN -> EventCardVisuals(
        accent = NimazPalette.MatPurple,
        containerAccent = NimazPalette.MatPurple,
        icon = Icons.Filled.NightsStay,
        ornament = EventOrnament.Pattern(NimazPatternStyle.LATTICE),
    )
    EventOccasion.LAYLAT_AL_QADR -> EventCardVisuals(
        accent = NimazPalette.MatPurple,
        containerAccent = NimazPalette.MatPurple,
        icon = Icons.Filled.AutoAwesome,
        ornament = EventOrnament.Pattern(NimazPatternStyle.STAR_FIELD),
    )
    EventOccasion.ARAFAH -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Outlined.Terrain,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.ASHURA -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Outlined.WaterDrop,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.MAWLID -> EventCardVisuals(
        accent = NimazPalette.Amber700,
        containerAccent = NimazPalette.Amber700,
        icon = Icons.Filled.Star,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.HIJRI_NEW_YEAR -> EventCardVisuals(
        accent = NimazPalette.Amber700,
        containerAccent = NimazPalette.Amber700,
        icon = Icons.Filled.CalendarMonth,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.JUMUAH -> EventCardVisuals(
        accent = NimazPalette.GreenDeep,
        containerAccent = NimazPalette.GreenDeep,
        icon = Icons.Filled.Mosque,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.GENERIC -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Filled.Event,
        ornament = EventOrnament.Divider,
    )
}

// ---- Preview matrix (the "so I can see it" deliverable) ----

@Composable
private fun EventCardOccasionSample(occasion: EventOccasion) {
    val v = eventCardVisualsFor(occasion)
    EventCard(
        accent = v.accent,
        containerAccent = v.containerAccent,
        icon = v.icon,
        ornament = v.ornament,
        eyebrow = occasion.name.lowercase().replaceFirstChar { it.uppercase() },
        arabic = "عيد مبارك",
        headline = "Blessed occasion",
        body = "A short, warm line about the day and what it means.",
        transliteration = "taqabbal Allāhu minnā wa minkum",
        proof = "Al-Baqarah 2:185" to "…that you may complete the count and glorify God.",
        primaryAction = EventAction("Learn more") {},
        secondaryAction = EventAction("Later") {},
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Eid al-Fitr — light")
@Composable
private fun EventCard_EidFitr_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.EID_AL_FITR) }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Eid al-Fitr — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun EventCard_EidFitr_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) { EventCardOccasionSample(EventOccasion.EID_AL_FITR) }
}

@Preview(showBackground = true, widthDp = 400, fontScale = 2f, name = "Eid — 200% font")
@Composable
private fun EventCard_EidFitr_LargeFont() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.EID_AL_FITR) }
}

@Preview(showBackground = true, widthDp = 400, name = "Eid al-Adha — light")
@Composable
private fun EventCard_EidAdha_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.EID_AL_ADHA) }
}

@Preview(showBackground = true, widthDp = 400, name = "Ramadan — light")
@Composable
private fun EventCard_Ramadan_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.RAMADAN) }
}

@Preview(showBackground = true, widthDp = 400, name = "Laylat al-Qadr — light")
@Composable
private fun EventCard_Qadr_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.LAYLAT_AL_QADR) }
}

@Preview(showBackground = true, widthDp = 400, name = "Arafah — light")
@Composable
private fun EventCard_Arafah_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.ARAFAH) }
}

@Preview(showBackground = true, widthDp = 400, name = "Ashura — light")
@Composable
private fun EventCard_Ashura_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.ASHURA) }
}

@Preview(showBackground = true, widthDp = 400, name = "Mawlid — light")
@Composable
private fun EventCard_Mawlid_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.MAWLID) }
}

@Preview(showBackground = true, widthDp = 400, name = "Hijri new year — light")
@Composable
private fun EventCard_Hijri_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.HIJRI_NEW_YEAR) }
}

@Preview(showBackground = true, widthDp = 400, name = "Generic — light")
@Composable
private fun EventCard_Generic_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.GENERIC) }
}

@Preview(showBackground = true, widthDp = 400, name = "Generic — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun EventCard_Generic_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) { EventCardOccasionSample(EventOccasion.GENERIC) }
}
