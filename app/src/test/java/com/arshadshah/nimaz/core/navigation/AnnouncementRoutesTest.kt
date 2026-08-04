package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.MushafScript
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementRoutesTest {

    @Test
    fun `known static keys resolve to routes`() {
        assertThat(announcementRoute("home")).isEqualTo(Route.Home)
        assertThat(announcementRoute("quran")).isEqualTo(Route.Quran)
        assertThat(announcementRoute("search/ask")).isEqualTo(Route.GlobalSearch)
        assertThat(announcementRoute("search/settings")).isEqualTo(Route.SearchSettings)
        assertThat(announcementRoute("prayer/tracker")).isEqualTo(Route.PrayerTracker())
        assertThat(announcementRoute("settings/about")).isEqualTo(Route.SettingsAbout)
        assertThat(announcementRoute("khatam")).isEqualTo(Route.KhatamList)
        // Existing notifications deep-link still resolves to the hub (unchanged) …
        assertThat(announcementRoute("settings/notifications")).isEqualTo(Route.SettingsNotifications)
        // … and the new worship-reminders subscreen keys resolve.
        assertThat(announcementRoute("settings/notifications/worship")).isEqualTo(Route.SettingsWorshipReminders)
        assertThat(announcementRoute("settings/worship")).isEqualTo(Route.SettingsWorshipReminders)
        // Notification hub subscreens (#301).
        assertThat(announcementRoute("settings/notifications/prayers")).isEqualTo(Route.SettingsNotificationsPrayers)
        assertThat(announcementRoute("settings/notifications/weekly")).isEqualTo(Route.SettingsNotificationsWeekly)
        assertThat(announcementRoute("settings/notifications/sound")).isEqualTo(Route.SettingsNotificationsSound)
        assertThat(announcementRoute("settings/notifications/diagnostics")).isEqualTo(Route.SettingsNotificationsDiagnostics)
        // The screen was renamed from Troubleshooting to Diagnostics. This key is already
        // published, so an announcement composed against the old name must still land.
        assertThat(announcementRoute("settings/notifications/troubleshooting")).isEqualTo(Route.SettingsNotificationsDiagnostics)
    }

    @Test
    fun `unknown key resolves to null`() {
        assertThat(announcementRoute("brand/new/feature")).isNull()
        assertThat(announcementRoute("")).isNull()
        assertThat(announcementRoute(null)).isNull()
    }

    @Test
    fun `urls are not feature keys`() {
        assertThat(announcementRoute("https://nimaz.arshadshah.com/privacy")).isNull()
    }

    @Test
    fun `parameterised quran keys resolve`() {
        assertThat(announcementRoute("quran/surah/18")).isEqualTo(Route.QuranReader(18))
        assertThat(announcementRoute("quran/surah/18/ayah/10"))
            .isEqualTo(Route.QuranReader(18, 10))
        assertThat(announcementRoute("quran/surah/18/info")).isEqualTo(Route.SurahInfo(18))
        assertThat(announcementRoute("quran/page/300")).isEqualTo(Route.QuranPage(300))
        assertThat(announcementRoute("quran/juz/30")).isEqualTo(Route.QuranJuz(30))
    }

    @Test
    fun `parameterised tafseer keys resolve`() {
        assertThat(announcementRoute("tafseer/2")).isEqualTo(Route.Tafseer(2))
        assertThat(announcementRoute("tafseer/2/ayah/255")).isEqualTo(Route.Tafseer(2, 255))
    }

    @Test
    fun `parameterised string-id keys resolve`() {
        assertThat(announcementRoute("dua/category/morning"))
            .isEqualTo(Route.DuaCategory("morning"))
        assertThat(announcementRoute("dua/reader/istikhara"))
            .isEqualTo(Route.DuaReader("istikhara"))
        assertThat(announcementRoute("hadith/book/bukhari"))
            .isEqualTo(Route.HadithBook("bukhari"))
        assertThat(announcementRoute("hadith/book/bukhari/chapter/1"))
            .isEqualTo(Route.HadithChapter("bukhari", "1"))
        assertThat(announcementRoute("hadith/12345"))
            .isEqualTo(Route.HadithReader("12345"))
    }

    @Test
    fun `parameterised numeric detail keys resolve`() {
        assertThat(announcementRoute("names/allah/40")).isEqualTo(Route.AsmaUlHusnaDetail(40))
        assertThat(announcementRoute("names/prophet/5")).isEqualTo(Route.AsmaUnNabiDetail(5))
        assertThat(announcementRoute("prophets/3")).isEqualTo(Route.ProphetDetail(3))
        assertThat(announcementRoute("qaida/lesson/7")).isEqualTo(Route.QaidaReader(7))
        assertThat(announcementRoute("prayer/tracker/2")).isEqualTo(Route.PrayerTracker(2))
        assertThat(announcementRoute("calendar/9/1447")).isEqualTo(Route.IslamicMonth(9, 1447))
    }

    @Test
    fun `parameterised long-id keys resolve`() {
        assertThat(announcementRoute("khatam/7")).isEqualTo(Route.KhatamDetail(7L))
        assertThat(announcementRoute("tasbih/counter")).isEqualTo(Route.TasbihCounter(null))
        assertThat(announcementRoute("tasbih/counter/42")).isEqualTo(Route.TasbihCounter(42L))
    }

    @Test
    fun `new static object keys resolve`() {
        assertThat(announcementRoute("bookmarks")).isEqualTo(Route.AllBookmarks)
        assertThat(announcementRoute("fasting/tracker")).isEqualTo(Route.FastingTracker)
        assertThat(announcementRoute("fasting/stats")).isEqualTo(Route.FastingStats)
        assertThat(announcementRoute("prayer/monthly")).isEqualTo(Route.MonthlyPrayerTimes)
        assertThat(announcementRoute("zakat/history")).isEqualTo(Route.ZakatHistory)
        assertThat(announcementRoute("tasbih/presets")).isEqualTo(Route.TasbihPresets)
        assertThat(announcementRoute("tasbih/stats")).isEqualTo(Route.TasbihStats)
        assertThat(announcementRoute("tasbih/history")).isEqualTo(Route.TasbihHistory)
    }

    @Test
    fun `candidate static object keys resolve`() {
        assertThat(announcementRoute("hadith/search")).isEqualTo(Route.HadithSearch)
        assertThat(announcementRoute("hadith/bookmarks")).isEqualTo(Route.HadithBookmarks)
        assertThat(announcementRoute("dua/favorites")).isEqualTo(Route.DuaFavorites)
        assertThat(announcementRoute("dua/search")).isEqualTo(Route.DuaSearch)
        assertThat(announcementRoute("settings/appearance")).isEqualTo(Route.SettingsAppearance)
        assertThat(announcementRoute("settings/location")).isEqualTo(Route.SettingsLocation)
        assertThat(announcementRoute("settings/language")).isEqualTo(Route.SettingsLanguage)
        assertThat(announcementRoute("settings/prayer-calculation"))
            .isEqualTo(Route.SettingsPrayerCalculation)
        assertThat(announcementRoute("settings/widgets")).isEqualTo(Route.SettingsWidgets)
        assertThat(announcementRoute("settings/sync")).isEqualTo(Route.SettingsSync)
        assertThat(announcementRoute("qaida/letters")).isEqualTo(Route.QaidaLetters)
    }

    @Test
    fun `integer args are range checked`() {
        assertThat(announcementRoute("quran/surah/0")).isNull()
        assertThat(announcementRoute("quran/surah/1")).isEqualTo(Route.QuranReader(1))
        assertThat(announcementRoute("quran/surah/114")).isEqualTo(Route.QuranReader(114))
        assertThat(announcementRoute("quran/surah/115")).isNull()
        assertThat(announcementRoute("quran/page/0")).isNull()
        // Page links are bounded by the largest edition, not by Madani's 604 — the target
        // edition isn't known at resolve time, and the reader clamps once it is.
        assertThat(announcementRoute("quran/page/604")).isEqualTo(Route.QuranPage(604))
        val maxPage = MushafScript.MAX_TOTAL_PAGES
        assertThat(announcementRoute("quran/page/$maxPage")).isEqualTo(Route.QuranPage(maxPage))
        assertThat(announcementRoute("quran/page/${maxPage + 1}")).isNull()
        assertThat(announcementRoute("quran/juz/31")).isNull()
        assertThat(announcementRoute("names/allah/99")).isEqualTo(Route.AsmaUlHusnaDetail(99))
        assertThat(announcementRoute("names/allah/100")).isNull()
    }

    @Test
    fun `malformed keys resolve to null`() {
        assertThat(announcementRoute("quran/surah/")).isNull()
        assertThat(announcementRoute("quran/surah/abc")).isNull()
        assertThat(announcementRoute("quran//18")).isNull()
        assertThat(announcementRoute("quran/surah/18/ayah")).isNull()
        assertThat(announcementRoute("khatam/notanumber")).isNull()
    }

    @Test
    fun `leading and trailing slashes are tolerated`() {
        assertThat(announcementRoute("/home/")).isEqualTo(Route.Home)
        assertThat(announcementRoute("/quran/surah/18/")).isEqualTo(Route.QuranReader(18))
    }

    @Test
    fun `hadith slash id does not swallow reserved segments`() {
        // "hadith/book" is a prefix, not a hadith id — must not become HadithReader("book")
        assertThat(announcementRoute("hadith/book")).isNull()
    }

    @Test
    fun `non-https urls are not feature keys`() {
        assertThat(announcementRoute("http://example.com")).isNull()
        assertThat(announcementRoute("javascript:alert(1)")).isNull()
    }
}
