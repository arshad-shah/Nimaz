package com.arshadshah.nimaz.core.navigation

/** Maps a help.json step `deeplink` key to an in-app Route, or null if unknown. */
fun helpDeepLinkRoute(key: String?): Route? = when (key) {
    "prayer_settings" -> Route.SettingsPrayerCalculation
    "notifications" -> Route.SettingsNotifications
    "worship_reminders" -> Route.SettingsWorshipReminders
    "location" -> Route.SettingsLocation
    "qibla" -> Route.Qibla
    "quran_settings" -> Route.SettingsQuran
    "language" -> Route.SettingsLanguage
    "appearance" -> Route.SettingsAppearance
    "calendar" -> Route.IslamicCalendar
    "fasting" -> Route.FastingTracker
    "tasbih" -> Route.TasbihHome
    "hadith" -> Route.HadithHome
    "zakat" -> Route.ZakatCalculator
    "prayer_tracker" -> Route.PrayerTracker
    "qada" -> Route.QadaPrayers
    "qaida" -> Route.QaidaHome
    "dua" -> Route.DuaHome
    "tafseer" -> Route.TafseerChapters
    "khatam" -> Route.KhatamList
    "widgets" -> Route.SettingsWidgets
    "settings" -> Route.Settings
    "home" -> Route.Home
    else -> null
}
