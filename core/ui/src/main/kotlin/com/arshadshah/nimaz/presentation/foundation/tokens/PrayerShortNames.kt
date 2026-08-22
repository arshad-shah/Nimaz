package com.arshadshah.nimaz.presentation.foundation.tokens

import android.content.Context
import com.arshadshah.nimaz.core.ui.R

/*
 * Moved out of `:feature:widget`'s `WidgetUi.kt` in PR 21 of #551: `WidgetsScreen`'s in-app
 * preview needs exactly this lookup, and it left for `:feature:settings`. Two feature modules,
 * so it goes down to `:core:ui` — which is where the strings it resolves already live.
 */

/**
 * The translated short name for a prayer, or null when the string is not one of the five daily
 * prayers (or Sunrise).
 *
 * Widgets render outside the app's own composition, so nothing else catches an English literal
 * here: `PrayerTimesWidget` and `PrayerTrackerWidget` built their rows from `"Fajr"`, `"Dhuhr"`,
 * … while `WidgetsScreen`'s in-app preview of the very same widgets resolved the translated
 * resources. The strings were already translated into all five shipped locales; only the lookup
 * was missing.
 *
 * Null rather than a default prayer: labelling a row "Dhuhr" because the name was unrecognised
 * is worse than showing the raw string.
 */
fun prayerShortNameRes(prayerName: String): Int? = when (prayerName.trim().lowercase()) {
    "fajr" -> R.string.widget_prayer_short_fajr
    "sunrise" -> R.string.widget_prayer_short_sunrise
    "dhuhr", "zuhr" -> R.string.widget_prayer_short_dhuhr
    "asr" -> R.string.widget_prayer_short_asr
    "maghrib" -> R.string.widget_prayer_short_maghrib
    "isha" -> R.string.widget_prayer_short_isha
    else -> null
}

/** The translated short name, falling back to [prayerName] itself when it is not a prayer. */
fun Context.prayerShortName(prayerName: String): String =
    prayerShortNameRes(prayerName)?.let { getString(it) } ?: prayerName
