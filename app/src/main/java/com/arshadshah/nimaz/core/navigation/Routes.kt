package com.arshadshah.nimaz.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    // Main screens (Bottom navigation)
    @Serializable
    data object Home : Route

    @Serializable
    data object Quran : Route

    @Serializable
    data object Tasbih : Route

    @Serializable
    data object QiblaNav : Route

    @Serializable
    data object More : Route

    // Quran screens
    @Serializable
    data class QuranReader(val surahNumber: Int, val ayahNumber: Int = 1) : Route

    @Serializable
    data class QuranPage(val pageNumber: Int) : Route

    @Serializable
    data class QuranJuz(val juzNumber: Int) : Route

    @Serializable
    data object QuranSearch : Route

    /**
     * The merged browse surface: every surah in mushaf order, under juz headers, with one field
     * that also understands "juz 15" and "page 299".
     *
     * Replaces the Surah/Juz/Page sub-tabs that used to live inside [Quran]. Page stops being a
     * browse tab here because it was never really an index — it was the door to a different
     * *reading mode*, which now lives in the reader.
     *
     * [infoForSurah] raises the surah-info sheet on arrival. It exists so the published
     * announcement key `quran/surah/{n}/info` keeps a destination after `SurahInfo` retired as
     * a screen: an announcement already on a user's device must still land where it meant to.
     */
    @Serializable
    data class QuranBrowse(val infoForSurah: Int? = null) : Route

    /**
     * Everything the user has marked, whatever it is about — Qur'an, Hadith or Dua, bookmarked,
     * favourited or annotated.
     *
     * App-wide rather than Qur'an-scoped, because the store always was: `bookmarks` is one table
     * keyed by `(kind, target_id)`, and scoping this screen to the Qur'an would strand a
     * reader's existing hadith and dua marks with nowhere to see them.
     */
    @Serializable
    data object QuranSaved : Route

    // Hadith screens
    @Serializable
    data object HadithHome : Route

    @Serializable
    data class HadithBook(val bookId: String) : Route

    @Serializable
    data class HadithChapter(val bookId: String, val chapterId: String) : Route

    @Serializable
    data class HadithReader(val hadithId: String) : Route

    /**
     * A hadith addressed the way a reader cites one — book plus the number printed on it —
     * rather than by database id.
     *
     * Separate from [HadithReader] because they are genuinely different addresses, and
     * collapsing them is what broke the bookmarks: a `HadithBookmark` stores `bookId` and
     * `hadithNumber` and no id, so the bookmark screen had to pass `hadithNumber.toString()`
     * into [HadithReader]'s `hadithId` slot. That resolves to the row whose **primary key**
     * equals the number — a real hadith, from an arbitrary book, opened with no error.
     */
    @Serializable
    data class HadithByNumber(val bookId: String, val hadithNumber: Int) : Route

    /**
     * Every hadith in the collection carrying one authenticity grade, read as one list.
     *
     * [grade] is the [com.arshadshah.nimaz.domain.model.HadithGrade] name, because a route
     * argument has to survive being written into a URL — the reader parses it back.
     */
    @Serializable
    data class HadithByGrade(val grade: String) : Route

    @Serializable
    data object HadithSearch : Route

    @Serializable
    data object HadithBookmarks : Route

    @Serializable
    data object HadithSettings : Route

    // Dua screens
    @Serializable
    data object DuaHome : Route

    @Serializable
    data class DuaCategory(val categoryId: String) : Route

    /**
     * Every dua filed under one occasion, across the curated categories.
     *
     * [occasion] is the [com.arshadshah.nimaz.domain.model.DuaOccasion] name; the screen parses
     * it back, and an unknown value falls back to `GENERAL` rather than opening an empty list.
     */
    @Serializable
    data class DuaOccasion(val occasion: String) : Route

    @Serializable
    data class DuaReader(val duaId: String) : Route

    @Serializable
    data object DuaFavorites : Route

    @Serializable
    data object DuaSearch : Route

    @Serializable
    data object DuaSettings : Route

    // Prayer screens
    @Serializable
    data object PrayerTimes : Route

    @Serializable
    data object PrayerTracker : Route

    @Serializable
    data object PrayerStats : Route

    @Serializable
    data object QadaPrayers : Route

    @Serializable
    data object MonthlyPrayerTimes : Route

    // Fasting screens
    @Serializable
    data object FastingHome : Route

    @Serializable
    data object FastingTracker : Route

    @Serializable
    data object FastingStats : Route

    /**
     * Make-up fasts — what is owed, and what has been settled by fasting or fidya.
     *
     * This route existed once, was deleted when make-up became a tab inside the fast tracker,
     * and is back because the tab went away again in the 2026-08 redesign. The tab row it lived
     * in was the only thing standing between the tracker and one uninterrupted scroll, and a
     * two-tab row is a poor trade for that. Recorded rather than quietly re-added: the pendulum
     * has swung twice, and the next person deserves to know why it is where it is.
     */
    @Serializable
    data object MakeupFasts : Route

    /**
     * Night worship (Tahajjud / Witr) — the destination for those two Home worship cards.
     *
     * Exists because those reminders had nowhere useful to go: the app can tell you the last
     * third of the night has begun, but had no screen answering "what now". Everything else a
     * worship card links to (dua categories, the fast tracker) already existed.
     */
    @Serializable
    data object NightWorship : Route

    // Tasbih screens
    @Serializable
    data object TasbihHome : Route

    @Serializable
    data class TasbihCounter(val presetId: Long? = null) : Route

    @Serializable
    data object TasbihPresets : Route

    @Serializable
    data object TasbihStats : Route

    @Serializable
    data object TasbihHistory : Route

    /**
     * The custom-dhikr form. With [presetId] null it creates one; with an id it edits that one
     * — the same fields either way, so the same destination rather than a near-copy of it.
     */
    @Serializable
    data class TasbihAddPreset(val presetId: Long? = null) : Route

    // Zakat screens
    @Serializable
    data object ZakatCalculator : Route

    @Serializable
    data object ZakatHistory : Route

    /**
     * The nisab basis, the metal prices and the display currency. Named `SettingsZakat`
     * rather than `ZakatSettings` so it sorts with the other `Settings*` destinations —
     * it is reachable from the Settings hub as well as from the calculator's top bar.
     */
    @Serializable
    data object SettingsZakat : Route

    // Qibla screen
    @Serializable
    data object Qibla : Route

    // Qaida (children's Arabic reader) screens
    @Serializable
    data object QaidaHome : Route

    @Serializable
    data class QaidaReader(val lessonId: Int) : Route

    @Serializable
    data object QaidaLetters : Route

    // Islamic Calendar screens
    @Serializable
    data object IslamicCalendar : Route

    @Serializable
    data class IslamicMonth(val month: Int, val year: Int) : Route

    // Settings screens
    @Serializable
    data object Settings : Route

    @Serializable
    data object SettingsPrayerCalculation : Route

    @Serializable
    data object SettingsNotifications : Route

    /** Extended worship & fasting reminders (Tahajjud, Suhoor, Iftar, adhkar …). */
    @Serializable
    data object SettingsWorshipReminders : Route

    // Notification hub subscreens (#301).
    @Serializable
    data object SettingsNotificationsPrayers : Route

    @Serializable
    data object SettingsNotificationsWeekly : Route

    @Serializable
    data object SettingsNotificationsSound : Route

    @Serializable
    data object SettingsNotificationsDiagnostics : Route

    @Serializable
    data object SettingsAppearance : Route

    @Serializable
    data object SettingsLanguage : Route

    @Serializable
    data object SettingsLocation : Route

    @Serializable
    data object SettingsAbout : Route

    @Serializable
    data object SettingsQuran : Route

    @Serializable
    data object SettingsWidgets : Route

    @Serializable
    data object SettingsHelp : Route

    @Serializable
    data class HelpTopicDetail(val topicId: String) : Route

    @Serializable
    data class HelpGuide(val guideId: String) : Route

    @Serializable
    data object SettingsSync : Route

    // Search & AI settings (Ask with Proof)
    @Serializable
    data object SearchSettings : Route

    // Onboarding
    @Serializable
    data object Onboarding : Route

    // All Bookmarks
    @Serializable
    data object AllBookmarks : Route

    // Global Search
    @Serializable
    data object GlobalSearch : Route

    // Tafseer
    @Serializable
    data object TafseerChapters : Route

    @Serializable
    data class Tafseer(val surahNumber: Int, val ayahNumber: Int = 1) : Route

    /**
     * A surah's long-form background — the source's own sections, read continuously.
     *
     * Its own destination rather than a block on the surah-info sheet, because the longest of
     * them is 47 KB of prose and burying that under an info surface's first fold made the info
     * surface a document instead of an answer.
     */
    @Serializable
    data class SurahBackground(val surahNumber: Int) : Route

    /**
     * A surah's passage outline — its table of contents.
     *
     * [currentAyah] is the verse the reader is on when this is opened *from the reader*, so the
     * passage containing it can be marked and scrolled to. Null when it is opened from surah
     * info, where there is no such place to be.
     */
    @Serializable
    data class SurahPassages(val surahNumber: Int, val currentAyah: Int? = null) : Route

    /**
     * The Qur'an's subject browser. One route for the whole descent, not one per level —
     * the ontology goes five deep, and a route per level would mean five back-stack entries
     * for one act of browsing. `QuranTopicsViewModel` holds the path.
     */
    @Serializable
    data object QuranTopics : Route

    /**
     * The subjects one surah speaks about, weightiest here first.
     *
     * Its own destination rather than [QuranTopics] opened with an argument, because it is a
     * different question. [QuranTopics] browses a hierarchy from its roots down; this is a flat
     * list of what *these* verses are cited under, and it exists because "Subjects in this
     * surah" used to open the global tree at the top — the same twenty roots whichever surah
     * you came from, with nothing carrying the surah you were holding.
     */
    @Serializable
    data class SurahSubjects(val surahNumber: Int) : Route

    /**
     * One subject, with its citations.
     *
     * [tree] is a [com.arshadshah.nimaz.domain.model.TopicTree] `wire` value rather than the
     * enum, because a route argument has to have a `NavType` and a String always does. It is
     * only used to pick which hierarchy the breadcrumb and the child list are drawn from; an
     * unrecognised value falls back to the thematic tree rather than failing to open.
     *
     * [fromSurah] is the surah the reader arrived from, when they arrived from one. It does not
     * filter anything — a subject's citations are the whole of it — but that surah's verses are
     * pinned to the top of the list and named, so opening "Patience" from Al-Baqarah does not
     * land on Al-Fatiha's citation with Al-Baqarah's forty somewhere below the fold.
     */
    @Serializable
    data class QuranTopicDetail(
        val topicId: Int,
        val tree: String = "thematic",
        val fromSurah: Int? = null,
    ) : Route

    // Select Reciter
    @Serializable
    data object SelectReciter : Route

    // Select Quran translation
    @Serializable
    data object SelectTranslation : Route

    // Licenses
    @Serializable
    data object Licenses : Route

    @Serializable
    data class LicenseDetail(val libraryHashCode: Int) : Route

    /**
     * The three name catalogues, as one tabbed screen.
     *
     * They were `AsmaUlHusnaList`, `AsmaUnNabiList` and `ProphetsList` — three destinations,
     * three More entries, three search boxes and three favourites filters for what a reader
     * thinks of as one place. [tab] says which tab opens; it is an ordinal rather than the
     * enum because a `Route` has to serialise into a deep link.
     */
    @Serializable
    data class Names(val tab: Int = 0) : Route

    /**
     * Everything the user has starred, in one place.
     *
     * Reached from the Names top bar. Sectioned by what the item is, so consolidating the
     * rest of the app's favourites here later adds a section rather than a screen.
     */
    @Serializable
    data object Favourites : Route

    @Serializable
    data class AsmaUlHusnaDetail(val nameId: Int) : Route

    @Serializable
    data class AsmaUnNabiDetail(val nameId: Int) : Route

    @Serializable
    data class ProphetDetail(val prophetId: Int) : Route

    // Khatam
    @Serializable
    data object KhatamList : Route

    @Serializable
    data class KhatamDetail(val khatamId: Long) : Route

    @Serializable
    data object KhatamCreate : Route

    @Serializable
    data class KhatamEdit(val khatamId: Long) : Route
}

// Navigation destinations for bottom navigation
enum class BottomNavDestination(val route: Route, val title: String, val icon: String) {
    HOME(Route.Home, "Home", "home"),
    QURAN(Route.Quran, "Quran", "menu_book"),
    TASBIH(Route.Tasbih, "Tasbih", "counter"),
    QIBLA(Route.QiblaNav, "Qibla", "compass"),
    MORE(Route.More, "More", "more_horiz")
}
