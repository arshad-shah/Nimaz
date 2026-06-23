package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.domain.model.PrayerType
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Presentation-layer visual helpers for [PrayerType]. Used by every component
 * that renders a prayer accent (cards, dots, top-bar). Keep behaviour here so
 * the colours/icons/Arabic names stay consistent across the app.
 */

@Composable
fun getPrayerColor(prayerType: PrayerType?): Color {
    return when (prayerType) {
        PrayerType.FAJR -> Color(0xFF6366F1)      // Indigo
        PrayerType.SUNRISE -> Color(0xFFF59E0B)    // Amber
        PrayerType.DHUHR -> Color(0xFFEAB308)       // Yellow
        PrayerType.ASR -> Color(0xFFF97316)          // Orange
        PrayerType.MAGHRIB -> Color(0xFFEF4444)    // Red
        PrayerType.ISHA -> Color(0xFF8B5CF6)        // Violet
        else -> MaterialTheme.colorScheme.primary
    }
}

fun getPrayerIcon(prayerType: PrayerType?): ImageVector {
    return when (prayerType) {
        PrayerType.FAJR -> PrayerIconFajr
        PrayerType.SUNRISE -> PrayerIconSunrise
        PrayerType.DHUHR -> PrayerIconDhuhr
        PrayerType.ASR -> PrayerIconAsr
        PrayerType.MAGHRIB -> PrayerIconMaghrib
        PrayerType.ISHA -> PrayerIconIsha
        else -> PrayerIconDhuhr
    }
}

fun getArabicPrayerName(prayerType: PrayerType?): String {
    return when (prayerType) {
        PrayerType.FAJR -> "الفجر"
        PrayerType.SUNRISE -> "الشروق"
        PrayerType.DHUHR -> "الظهر"
        PrayerType.ASR -> "العصر"
        PrayerType.MAGHRIB -> "المغرب"
        PrayerType.ISHA -> "العشاء"
        else -> ""
    }
}


// ==================== PREVIEWS ====================

/**
 * Renders the colour swatch + icon + Arabic name produced by the helpers for
 * every [PrayerType], so the per-prayer accent/icon/name mapping is visible.
 */
@Composable
private fun PrayerVisualsShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrayerType.entries.forEach { prayer ->
            val color = getPrayerColor(prayer)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Icon(
                    imageVector = getPrayerIcon(prayer),
                    contentDescription = prayer.displayName,
                    modifier = Modifier.size(28.dp),
                    tint = color
                )
                Text(
                    text = prayer.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = getArabicPrayerName(prayer),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Prayer Visuals — Light")
@Composable
private fun PrayerVisualsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        PrayerVisualsShowcase()
    }
}

@Preview(showBackground = true, name = "Prayer Visuals — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PrayerVisualsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        PrayerVisualsShowcase()
    }
}
