package com.arshadshah.nimaz.support

import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.R

/**
 * Single source of truth for every UI selector used by the instrumented tests.
 *
 * Goal: keep magic strings out of the test bodies. Tests reference [NavLabel],
 * [Tag], or [str] / [strId] instead of repeating literals. When a label moves to a
 * resource, or a `testTag` is added to the app, only this file changes.
 *
 * Three kinds of selector live here:
 *  - [NavLabel] — the bottom-navigation labels. These are hard-coded literals in
 *    `NavGraph.bottomNavItems` (used for both the visible Text and the icon's
 *    contentDescription), so matching them is stable and locale-independent.
 *  - [Tag] — the handful of real `Modifier.testTag` values that exist in the app
 *    today (the Qaida learning flow). Add new app tags here as they appear.
 *  - [strId] / [str] — string-resource lookups, for screen content that is
 *    correctly externalized to res/values/strings.xml. Resolving at runtime keeps
 *    tests honest against the shipped copy rather than duplicating it.
 */
object Selectors {

    /** Bottom-navigation destination labels (literal strings in NavGraph). */
    object NavLabel {
        const val HOME = "Home"
        const val QURAN = "Quran"
        const val TASBIH = "Tasbih"
        const val QIBLA = "Qibla"
        const val MORE = "More"

        val all = listOf(HOME, QURAN, TASBIH, QIBLA, MORE)
    }

    /** Real `Modifier.testTag` values present in the production UI. */
    object Tag {
        const val QAIDA_STAR = "qaida_star"
        const val QAIDA_DOT = "qaida_dot"
        const val QAIDA_CONTINUE = "qaida_continue"
    }

    /**
     * String-resource IDs for stable, user-visible content. Grouped by area so a
     * test reads `Selectors.Common.back` rather than a bare `R.string.*`.
     */
    object Common {
        @StringRes val back = R.string.cd_back
        @StringRes val settings = R.string.cd_settings
        @StringRes val today = R.string.today
    }

    object More {
        @StringRes val title = R.string.more
        @StringRes val settings = R.string.settings
        // Menu-item titles (each navigates to a feature landing screen).
        @StringRes val prayerTracker = R.string.prayer_tracker
        @StringRes val fasting = R.string.fasting
        @StringRes val khatam = R.string.khatam_quran
        @StringRes val qaida = R.string.qaida
        // One row for what used to be three: the catalogues are tabs of `Route.Names` now.
        @StringRes val names = R.string.names_title
        @StringRes val hadith = R.string.hadith
        @StringRes val duas = R.string.duas
        @StringRes val tafseer = R.string.tafseer
        @StringRes val calendar = R.string.calendar
        @StringRes val prayerTimes = R.string.prayer_times
        @StringRes val monthlyPrayerTimes = R.string.monthly_prayer_times
        @StringRes val zakat = R.string.zakat
        @StringRes val aboutNimaz = R.string.about_nimaz
        @StringRes val helpSupport = R.string.help_support
    }

    object Prayer {
        @StringRes val trackerTitle = R.string.prayer_tracker_title
        @StringRes val times = R.string.prayer_times
    }

    /** Row labels on the Settings hub (each opens a settings sub-screen). */
    object Settings {
        @StringRes val calculationMethod = R.string.calculation_method
        @StringRes val location = R.string.location
        @StringRes val notifications = R.string.notifications
        @StringRes val quranSettings = R.string.quran_settings
        @StringRes val appearance = R.string.appearance
        @StringRes val language = R.string.language
        @StringRes val widgets = R.string.widgets
        @StringRes val syncData = R.string.sync_data
    }

    object Qibla {
        @StringRes val compass = R.string.compass
    }

    object Quran {
        @StringRes val homeTitle = R.string.quran_home_title
        @StringRes val browseTab = R.string.quran_home_tab_browse
        @StringRes val favoritesTab = R.string.quran_home_tab_favorites
    }

    object Tasbih {
        @StringRes val beads = R.string.tasbih_mode_beads
        @StringRes val classic = R.string.tasbih_mode_classic
    }

    object Home {
        @StringRes val nextPrayer = R.string.next_prayer
    }

    /** Resolve a string resource using the instrumentation target (app) context. */
    fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    /** Resolve a formatted string resource. */
    fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)
}
