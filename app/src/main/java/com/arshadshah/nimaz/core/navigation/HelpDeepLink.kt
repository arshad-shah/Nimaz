package com.arshadshah.nimaz.core.navigation

/** Maps a help.json step `deeplink` key to an in-app Route, or null if unknown. */
fun helpDeepLinkRoute(key: String?): Route? = when (key) {
    "prayer_settings" -> Route.SettingsPrayerCalculation
    "notifications" -> Route.SettingsNotifications
    "location" -> Route.SettingsLocation
    "qibla" -> Route.Qibla
    "quran_settings" -> Route.SettingsQuran
    "language" -> Route.SettingsLanguage
    "appearance" -> Route.SettingsAppearance
    "calendar" -> Route.IslamicCalendar
    "fasting" -> Route.FastingTracker
    "tasbih" -> Route.TasbihHome
    "hadith" -> Route.HadithHome
    "settings" -> Route.Settings
    "home" -> Route.Home
    else -> null
}
