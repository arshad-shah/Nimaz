package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.domain.model.PrayerType

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
