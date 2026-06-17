package com.arshadshah.nimaz.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.HiltTestActivity
import com.arshadshah.nimaz.core.navigation.NavGraph
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive "visit every screen like a user" crawl.
 *
 * Hosts the real [NavGraph] (driven by a captured [NavHostController]) inside a
 * Hilt activity, then navigates to every destination in the app and asserts the
 * screen composes without crashing — a composition failure throws at
 * `waitForIdle()` and fails the owning test, pinpointing the broken screen.
 *
 * NOT run in CI (no emulator). Run locally in Android Studio across devices
 * before a release: `./gradlew connectedDebugAndroidTest` or right-click → Run.
 *
 * Caveats to expect on first local run:
 *  - Detail screens are visited with sample ids (e.g. ProphetDetail(1)); adjust
 *    to ids present in your seeded data if a screen legitimately needs them.
 *  - A screen with an indefinite animation/sensor loop (e.g. Qibla compass) can
 *    keep Compose "busy"; if `waitForIdle()` stalls there, drive that screen
 *    with `mainClock.autoAdvance = false` or move it to its own test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationCrawlTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.setContent {
            navController = rememberNavController()
            NimazTheme {
                NavGraph(navController = navController)
            }
        }
        // Wait until onboarding state resolves and the NavHost (with its start
        // destination) is actually composed before driving navigation.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            navController.currentDestination != null
        }
    }

    /** Navigate to [route] and assert the destination composed without crashing. */
    private fun visit(route: Route) {
        composeRule.runOnUiThread { navController.navigate(route) }
        composeRule.waitForIdle()
        assertThat(navController.currentDestination).isNotNull()
    }

    private fun visitAll(vararg routes: Route) = routes.forEach { visit(it) }

    // ── Bottom-nav sections ─────────────────────────────────────────
    @Test
    fun crawl_mainSections() = visitAll(
        Route.Home, Route.Quran, Route.Tasbih, Route.QiblaNav, Route.More
    )

    // ── Quran ───────────────────────────────────────────────────────
    @Test
    fun crawl_quran() = visitAll(
        Route.QuranReader(surahNumber = 1),
        Route.QuranPage(pageNumber = 1),
        Route.QuranJuz(juzNumber = 1),
        Route.SurahInfo(surahNumber = 1),
        Route.Tafseer(surahNumber = 1, ayahNumber = 1),
        Route.QuranSearch,
        Route.QuranBookmarks,
        Route.SelectReciter
    )

    // ── Hadith ──────────────────────────────────────────────────────
    @Test
    fun crawl_hadith() = visitAll(
        Route.HadithHome,
        Route.HadithBook(bookId = "1"),
        Route.HadithChapter(bookId = "1", chapterId = "1"),
        Route.HadithReader(hadithId = "1"),
        Route.HadithSearch,
        Route.HadithBookmarks
    )

    // ── Dua ─────────────────────────────────────────────────────────
    @Test
    fun crawl_dua() = visitAll(
        Route.DuaHome,
        Route.DuaCategory(categoryId = "1"),
        Route.DuaReader(duaId = "1"),
        Route.DuaFavorites,
        Route.DuaSearch
    )

    // ── Prayer ──────────────────────────────────────────────────────
    @Test
    fun crawl_prayer() = visitAll(
        Route.PrayerTimes,
        Route.PrayerTracker(),
        Route.PrayerStats,
        Route.QadaPrayers,
        Route.MonthlyPrayerTimes
    )

    // ── Fasting ─────────────────────────────────────────────────────
    @Test
    fun crawl_fasting() = visitAll(
        Route.FastingHome, Route.FastingTracker, Route.FastingStats
    )

    // ── Tasbih ──────────────────────────────────────────────────────
    @Test
    fun crawl_tasbih() = visitAll(
        Route.TasbihHome,
        Route.TasbihCounter(),
        Route.TasbihPresets,
        Route.TasbihStats,
        Route.TasbihHistory,
        Route.TasbihAddPreset
    )

    // ── Zakat ───────────────────────────────────────────────────────
    @Test
    fun crawl_zakat() = visitAll(Route.ZakatCalculator, Route.ZakatHistory)

    // ── Calendar / search / bookmarks ───────────────────────────────
    @Test
    fun crawl_calendarAndSearch() = visitAll(
        Route.IslamicCalendar,
        Route.IslamicMonth(month = 9, year = 1446),
        Route.AllBookmarks,
        Route.GlobalSearch
    )

    // ── Content libraries (names, prophets, khatam) ─────────────────
    @Test
    fun crawl_contentLibraries() = visitAll(
        Route.AsmaUlHusnaList,
        Route.AsmaUlHusnaDetail(nameId = 1),
        Route.AsmaUnNabiList,
        Route.AsmaUnNabiDetail(nameId = 1),
        Route.ProphetsList,
        Route.ProphetDetail(prophetId = 1),
        Route.KhatamList,
        Route.KhatamCreate
    )

    // ── Settings & about ────────────────────────────────────────────
    @Test
    fun crawl_settings() = visitAll(
        Route.Settings,
        Route.SettingsPrayerCalculation,
        Route.SettingsNotifications,
        Route.SettingsAppearance,
        Route.SettingsLanguage,
        Route.SettingsLocation,
        Route.SettingsQuran,
        Route.SettingsWidgets,
        Route.SettingsHelp,
        Route.SettingsSync,
        Route.SettingsAbout,
        Route.Licenses
    )
}
