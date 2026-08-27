package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.MushafScript

/**
 * Maps an announcement payload `route` key to an in-app Route, or null if
 * unknown. Allowlist only — keys sent from the Firebase console never carry
 * serialized routes or arguments, so old app versions safely ignore keys they
 * don't recognise (the banner hides its CTA instead of navigating anywhere
 * unexpected). Mirrors [helpDeepLinkRoute].
 */
fun announcementRoute(key: String?): Route? {
    val k = key?.trim()?.trim('/')?.takeIf { it.isNotEmpty() } ?: return null
    staticAnnouncementRoute(k)?.let { return it }
    return parameterisedAnnouncementRoute(k)
}

private fun staticAnnouncementRoute(key: String): Route? = when (key) {
    "home" -> Route.Home
    "quran" -> Route.Quran
    "quran/search" -> Route.QuranSearch
    // Bookmarks are inside Saved now; the published key keeps a destination.
    "quran/bookmarks" -> Route.QuranSaved
    "quran/browse" -> Route.QuranBrowse()
    "tafseer" -> Route.TafseerChapters
    "hadith" -> Route.HadithHome
    "dua" -> Route.DuaHome
    "tasbih" -> Route.TasbihHome
    "qibla" -> Route.Qibla
    "prayer/times" -> Route.PrayerTimes
    "prayer/tracker" -> Route.PrayerTracker
    "prayer/stats" -> Route.PrayerStats
    "prayer/qada" -> Route.QadaPrayers
    "fasting" -> Route.FastingHome
    "zakat" -> Route.ZakatCalculator
    "calendar" -> Route.IslamicCalendar
    "qaida" -> Route.QaidaHome
    "khatam" -> Route.KhatamList
    // The three catalogues are one tabbed screen now, so these three keys keep working by
    // selecting a tab rather than by reaching three destinations. Announcements already sent
    // still land where they meant to.
    "names" -> Route.Names()
    "names/allah" -> Route.Names(NamesTab.ASMA_UL_HUSNA.ordinal)
    "names/prophet" -> Route.Names(NamesTab.ASMA_UN_NABI.ordinal)
    "prophets" -> Route.Names(NamesTab.PROPHETS.ordinal)
    "favourites" -> Route.Favourites
    "search" -> Route.GlobalSearch
    "search/ask" -> Route.GlobalSearch
    "search/settings" -> Route.SearchSettings
    "settings" -> Route.Settings
    "settings/notifications" -> Route.SettingsNotifications
    "settings/notifications/worship", "settings/worship" -> Route.SettingsWorshipReminders
    "settings/notifications/prayers" -> Route.SettingsNotificationsPrayers
    "settings/notifications/weekly" -> Route.SettingsNotificationsWeekly
    "settings/notifications/sound" -> Route.SettingsNotificationsSound
    // The screen was renamed to Diagnostics. The old key stays because it is published — an
    // announcement already sent, or composed against the old name, must still land somewhere.
    "settings/notifications/diagnostics", "settings/notifications/troubleshooting" -> Route.SettingsNotificationsDiagnostics
    "settings/about" -> Route.SettingsAbout
    "settings/help" -> Route.SettingsHelp
    "bookmarks" -> Route.AllBookmarks
    "fasting/tracker" -> Route.FastingTracker
    "fasting/stats" -> Route.FastingStats
    "prayer/monthly" -> Route.MonthlyPrayerTimes
    "zakat/history" -> Route.ZakatHistory
    "tasbih/presets" -> Route.TasbihPresets
    "tasbih/stats" -> Route.TasbihStats
    "tasbih/history" -> Route.TasbihHistory
    "hadith/search" -> Route.HadithSearch
    "hadith/bookmarks" -> Route.HadithBookmarks
    "dua/favorites" -> Route.DuaFavorites
    "dua/search" -> Route.DuaSearch
    "settings/appearance" -> Route.SettingsAppearance
    "settings/location" -> Route.SettingsLocation
    "settings/language" -> Route.SettingsLanguage
    "settings/prayer-calculation" -> Route.SettingsPrayerCalculation
    "settings/widgets" -> Route.SettingsWidgets
    "settings/sync" -> Route.SettingsSync
    "qaida/letters" -> Route.QaidaLetters
    else -> null
}

private fun parameterisedAnnouncementRoute(key: String): Route? {
    val s = key.split('/')
    fun int(i: Int, range: IntRange): Int? =
        s.getOrNull(i)?.toIntOrNull()?.takeIf { it in range }
    fun str(i: Int): String? = s.getOrNull(i)?.takeIf { it.isNotBlank() }
    fun long(i: Int): Long? = s.getOrNull(i)?.toLongOrNull()

    return when {
        s.size == 3 && s[0] == "quran" && s[1] == "surah" ->
            int(2, 1..114)?.let { Route.QuranReader(it) }

        s.size == 5 && s[0] == "quran" && s[1] == "surah" && s[3] == "ayah" ->
            int(2, 1..114)?.let { su -> int(4, 1..300)?.let { Route.QuranReader(su, it) } }

        // Surah info is a sheet now, not a screen. The key resolves to Browse with the sheet
        // raised rather than being dropped: an announcement already sent to a device must still
        // land where it meant to.
        s.size == 4 && s[0] == "quran" && s[1] == "surah" && s[3] == "info" ->
            int(2, 1..114)?.let { Route.QuranBrowse(infoForSurah = it) }

        // Validate against the largest edition (604) so a page deep-link resolves regardless
        // of the user's active Mushaf script; the reader clamps to the active edition's page
        // count (548 for IndoPak-16) once the preference is read. See MushafScript / #270.
        s.size == 3 && s[0] == "quran" && s[1] == "page" ->
            int(2, 1..MushafScript.MAX_TOTAL_PAGES)?.let { Route.QuranPage(it) }

        s.size == 3 && s[0] == "quran" && s[1] == "juz" ->
            int(2, 1..30)?.let { Route.QuranJuz(it) }

        s.size == 2 && s[0] == "tafseer" ->
            int(1, 1..114)?.let { Route.Tafseer(it) }

        s.size == 4 && s[0] == "tafseer" && s[2] == "ayah" ->
            int(1, 1..114)?.let { su -> int(3, 1..300)?.let { Route.Tafseer(su, it) } }

        s.size == 3 && s[0] == "dua" && s[1] == "category" ->
            str(2)?.let { Route.DuaCategory(it) }

        s.size == 3 && s[0] == "dua" && s[1] == "reader" ->
            str(2)?.let { Route.DuaReader(it) }

        s.size == 5 && s[0] == "hadith" && s[1] == "book" && s[3] == "chapter" ->
            str(2)?.let { b -> str(4)?.let { Route.HadithChapter(b, it) } }

        s.size == 3 && s[0] == "hadith" && s[1] == "book" ->
            str(2)?.let { Route.HadithBook(it) }

        s.size == 2 && s[0] == "hadith" && s[1] !in RESERVED_HADITH_SEGMENTS ->
            str(1)?.let { Route.HadithReader(it) }

        s.size == 2 && s[0] == "tasbih" && s[1] == "counter" ->
            Route.TasbihCounter(null)

        s.size == 3 && s[0] == "tasbih" && s[1] == "counter" ->
            long(2)?.let { Route.TasbihCounter(it) }

        s.size == 3 && s[0] == "prayer" && s[1] == "tracker" ->
            // The `{tab}` segment predates the tab row's removal. Shipped announcements still
            // carry it, so it keeps resolving: 1 meant the qada tab and is now its own screen.
            int(2, 0..10)?.let { tab ->
                if (tab == 1) Route.QadaPrayers else Route.PrayerTracker
            }

        s.size == 3 && s[0] == "qaida" && s[1] == "lesson" ->
            int(2, 1..Int.MAX_VALUE)?.let { Route.QaidaReader(it) }

        s.size == 3 && s[0] == "calendar" ->
            int(1, 1..12)?.let { m ->
                s.getOrNull(2)?.toIntOrNull()?.let { y -> Route.IslamicMonth(m, y) }
            }

        s.size == 3 && s[0] == "names" && s[1] == "allah" ->
            int(2, 1..99)?.let { Route.AsmaUlHusnaDetail(it) }

        s.size == 3 && s[0] == "names" && s[1] == "prophet" ->
            int(2, 1..99)?.let { Route.AsmaUnNabiDetail(it) }

        s.size == 2 && s[0] == "prophets" ->
            int(1, 1..99)?.let { Route.ProphetDetail(it) }

        s.size == 2 && s[0] == "khatam" ->
            long(1)?.let { Route.KhatamDetail(it) }

        else -> null
    }
}

private val RESERVED_HADITH_SEGMENTS = setOf("book", "search", "bookmarks")
