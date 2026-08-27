package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.domain.model.PrayerName

/**
 * Accent colour for a [PrayerName], drawn from [NimazColors.PrayerColors].
 *
 * Centralises the `when (prayerName) { … }` block that was duplicated in
 * `PrayerStatsChart` (`:feature:tracker`) and `NimazQadaPrayerItem`.
 */
fun PrayerName.color(): Color = when (this) {
    PrayerName.FAJR -> NimazColors.PrayerColors.Fajr
    PrayerName.SUNRISE -> NimazColors.PrayerColors.Sunrise
    PrayerName.DHUHR -> NimazColors.PrayerColors.Dhuhr
    PrayerName.ASR -> NimazColors.PrayerColors.Asr
    PrayerName.MAGHRIB -> NimazColors.PrayerColors.Maghrib
    PrayerName.ISHA -> NimazColors.PrayerColors.Isha
}
