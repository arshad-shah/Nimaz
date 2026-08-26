package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.Route
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The Settings feature's slice of the route graph — twenty destinations, the largest of the ten.
 *
 * A destination that fails to register throws `IllegalArgumentException: navigation destination …
 * is not a direct child of this NavGraph` at the moment a **user taps the row that opens it**, not
 * at build time and not in any of the four gates `CLAUDE.md` lists. That matters more here than
 * anywhere: five of these are two taps deep inside the notification tree, and nothing in an
 * ordinary pass through the app opens them.
 *
 * Twenty is asserted as a number as well as a set, because #625 found five graphs whose documented
 * counts had drifted — `settingsGraph` was the worst of them, recorded as 16 while it held 20 —
 * and `check_docs.py`'s NAV-03 cannot catch it: it compares the 94 total against `Routes.kt`,
 * never the per-graph split.
 *
 * Nothing is composed here, so none of these screens' Hilt ViewModels is constructed. This asserts
 * registration; the screen tests beside it assert what each destination renders.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.Settings) {
            settingsGraph(navController)
        }.toList()

    @Test
    fun `all twenty settings destinations are registered`() {
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(20)
        listOf(
            Route.Settings::class,
            Route.SettingsPrayerCalculation::class,
            Route.SettingsNotifications::class,
            Route.SettingsWorshipReminders::class,
            Route.SettingsNotificationsPrayers::class,
            Route.SettingsNotificationsWeekly::class,
            Route.SettingsNotificationsSound::class,
            Route.SettingsNotificationsDiagnostics::class,
            Route.SettingsAppearance::class,
            Route.SettingsLanguage::class,
            Route.SettingsLocation::class,
            Route.SettingsQuran::class,
            Route.SettingsWidgets::class,
            Route.SettingsSync::class,
            Route.SettingsZakat::class,
            Route.SearchSettings::class,
            Route.DuaSettings::class,
            Route.HadithSettings::class,
            Route.SelectReciter::class,
            Route.SelectTranslation::class,
        ).forEach { route ->
            assertThat(routes.any { it.contains(route.qualifiedName!!) }).isTrue()
        }
    }

    @Test
    fun `every settings destination is registered exactly once`() {
        // Five of these are near-identical two-line blocks in the notification tree, so a
        // copy-paste that registered `SettingsNotificationsSound` twice under two names would
        // still read correctly — and would leave the other route unreachable at the tap.
        assertThat(destinations().mapNotNull { it.route }).containsNoDuplicates()
    }

    @Test
    fun `the five notification destinations each resolve in their own right`() {
        // Two taps deep from the settings hub, and each one decides whether a class of alert
        // reaches the user at all.
        assertStartDestination(Route.SettingsNotifications)
        assertStartDestination(Route.SettingsNotificationsPrayers)
        assertStartDestination(Route.SettingsNotificationsWeekly)
        assertStartDestination(Route.SettingsNotificationsSound)
        assertStartDestination(Route.SettingsNotificationsDiagnostics)
    }

    @Test
    fun `the four screens that arrived from other features resolve here`() {
        // `DuaSettings`, `HadithSettings`, `SelectReciter` and `SelectTranslation` live under
        // `screens/{dua,hadith,quran}` and are registered by *this* graph, because the module
        // boundary follows the ViewModel axis. A future reader moving them back to the graph
        // matching their directory would unregister them from here and break the tap.
        assertStartDestination(Route.DuaSettings)
        assertStartDestination(Route.HadithSettings)
        assertStartDestination(Route.SelectReciter)
        assertStartDestination(Route.SelectTranslation)
    }

    @Test
    fun `the remaining settings destinations resolve in their own right`() {
        assertStartDestination(Route.Settings)
        assertStartDestination(Route.SettingsPrayerCalculation)
        assertStartDestination(Route.SettingsWorshipReminders)
        assertStartDestination(Route.SettingsAppearance)
        assertStartDestination(Route.SettingsLanguage)
        assertStartDestination(Route.SettingsLocation)
        assertStartDestination(Route.SettingsQuran)
        assertStartDestination(Route.SettingsWidgets)
        assertStartDestination(Route.SettingsSync)
        assertStartDestination(Route.SettingsZakat)
        assertStartDestination(Route.SearchSettings)
    }

    private fun assertStartDestination(route: Route) {
        val graph = navController.createGraph(startDestination = route) {
            settingsGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
