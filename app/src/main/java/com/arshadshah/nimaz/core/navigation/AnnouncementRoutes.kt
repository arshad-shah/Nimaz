package com.arshadshah.nimaz.core.navigation

/**
 * Maps an announcement payload `route` key to an in-app Route, or null if
 * unknown. Allowlist only — keys sent from the Firebase console never carry
 * serialized routes or arguments, so old app versions safely ignore keys they
 * don't recognise (the banner hides its CTA instead of navigating anywhere
 * unexpected). Mirrors [helpDeepLinkRoute].
 */
fun announcementRoute(key: String?): Route? = when (key) {
    "home" -> Route.Home
    "quran" -> Route.Quran
    "quran/search" -> Route.QuranSearch
    "quran/bookmarks" -> Route.QuranBookmarks
    "tafseer" -> Route.TafseerChapters
    "hadith" -> Route.HadithHome
    "dua" -> Route.DuaHome
    "tasbih" -> Route.TasbihHome
    "qibla" -> Route.Qibla
    "prayer/times" -> Route.PrayerTimes
    "prayer/tracker" -> Route.PrayerTracker()
    "prayer/stats" -> Route.PrayerStats
    "prayer/qada" -> Route.QadaPrayers
    "fasting" -> Route.FastingHome
    "zakat" -> Route.ZakatCalculator
    "calendar" -> Route.IslamicCalendar
    "qaida" -> Route.QaidaHome
    "khatam" -> Route.KhatamList
    "names/allah" -> Route.AsmaUlHusnaList
    "names/prophet" -> Route.AsmaUnNabiList
    "prophets" -> Route.ProphetsList
    "search" -> Route.GlobalSearch
    "search/ask" -> Route.GlobalSearch
    "search/settings" -> Route.SearchSettings
    "settings" -> Route.Settings
    "settings/notifications" -> Route.SettingsNotifications
    "settings/about" -> Route.SettingsAbout
    "settings/help" -> Route.SettingsHelp
    else -> null
}
