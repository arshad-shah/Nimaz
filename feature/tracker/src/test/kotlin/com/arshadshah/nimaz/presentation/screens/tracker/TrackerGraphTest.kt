package com.arshadshah.nimaz.presentation.screens.tracker

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
 * The Tracker feature's slice of the route graph — fourteen destinations across three trackers.
 *
 * A destination that fails to register throws
 * `IllegalArgumentException: navigation destination … is not a direct child of this NavGraph`
 * at the moment a **user taps the thing that opens it** — not at build time, and not in any of
 * the four gates `CLAUDE.md` lists. Several of these are reachable only from a deep link or from
 * a shipped announcement (`fasting`, `prayer_tracker`, `prayer/tracker/{tab}`), so nobody
 * exercises them in a normal pass through the app at all.
 *
 * Fourteen is checked as a number as well as a set, because #625 found five graphs whose
 * documented counts had drifted — `trackerGraph` among them, recorded as 11 while it held 14 —
 * and `check_docs.py`'s NAV-03 could not catch it: it compares the total across all graphs
 * against `Routes.kt`, not the per-graph split.
 *
 * The graph is built without a `NavHost`, so nothing composes and none of these screens' Hilt
 * ViewModels is constructed. This asserts registration only; the screen tests beside it assert
 * what each destination renders.
 */
@RunWith(RobolectricTestRunner::class)
class TrackerGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.PrayerTracker) {
            trackerGraph(navController)
        }.toList()

    @Test
    fun `all fourteen tracker destinations are registered`() {
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(14)
        listOf(
            Route.PrayerTracker::class,
            Route.PrayerStats::class,
            Route.QadaPrayers::class,
            Route.Tasbih::class,
            Route.FastingHome::class,
            Route.FastingTracker::class,
            Route.FastingStats::class,
            Route.MakeupFasts::class,
            Route.TasbihHome::class,
            Route.TasbihCounter::class,
            Route.TasbihPresets::class,
            Route.TasbihStats::class,
            Route.TasbihHistory::class,
            Route.TasbihAddPreset::class,
        ).forEach { route ->
            assertThat(routes.any { it.contains(route.qualifiedName!!) }).isTrue()
        }
    }

    @Test
    fun `every tracker destination is registered exactly once`() {
        // Three of the fasting routes and four of the tasbih ones share a screen composable, so
        // a copy-paste that registered the same `Route` twice under two names would still look
        // right in review — and would leave the other route unreachable.
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).containsNoDuplicates()
    }

    @Test
    fun `the three prayer-tracking destinations resolve in their own right`() {
        // Split out of `prayerGraph` in PR 18 of #551 — they live in `screens/prayer` but drive
        // `viewmodel/tracker`. All three are reached directly: the tracker from the menu, stats
        // and qada from inside it.
        assertStartDestination(Route.PrayerTracker)
        assertStartDestination(Route.PrayerStats)
        assertStartDestination(Route.QadaPrayers)
    }

    @Test
    fun `all four fasting destinations resolve in their own right`() {
        // `FastingHome`, `FastingTracker` and `FastingStats` are three routes over one screen,
        // kept because shipped announcements and deep links name each of them.
        assertStartDestination(Route.FastingHome)
        assertStartDestination(Route.FastingTracker)
        assertStartDestination(Route.FastingStats)
        assertStartDestination(Route.MakeupFasts)
    }

    @Test
    fun `all seven tasbih destinations resolve in their own right`() {
        assertStartDestination(Route.Tasbih)
        assertStartDestination(Route.TasbihHome)
        assertStartDestination(Route.TasbihCounter())
        assertStartDestination(Route.TasbihPresets)
        assertStartDestination(Route.TasbihStats)
        assertStartDestination(Route.TasbihHistory)
    }

    @Test
    fun `the preset form resolves both with and without a preset to edit`() {
        // `TasbihAddPreset` is the one argument-carrying route here, and it is entered both ways:
        // empty from "New Tasbih", and with an id from a row's edit button. A route registered
        // for only one of them fails at the tap.
        assertStartDestination(Route.TasbihAddPreset())
        assertStartDestination(Route.TasbihAddPreset(presetId = 42L))
    }

    private fun assertStartDestination(route: Route) {
        // `createGraph` throws here if the route it is handed is not among the destinations the
        // builder registered — the same failure the app would hit on the tap that opens it.
        val graph = navController.createGraph(startDestination = route) {
            trackerGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
