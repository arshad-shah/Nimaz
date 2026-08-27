package com.arshadshah.nimaz.presentation.screens.prayer

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
 * The Prayer feature's slice of the route graph.
 *
 * A destination that fails to register throws `IllegalArgumentException: navigation destination …
 * is not a direct child of this NavGraph` — at *tap* time, on a device, with nothing catching it
 * at build time. Three of the five here are reached from outside this module: `PrayerTimes` and
 * `MonthlyPrayerTimes` from the home surface, `NightWorship` from the More menu. `Qibla` is
 * registered twice over, under two routes, because the compass is reachable both from the tools
 * hub and from a bottom-nav entry; a graph that registered only one of them would work from one
 * entry point and crash from the other, which is the hardest shape of this bug to notice.
 *
 * Nothing is composed here — the graph is built against a bare `NavHostController` — so none of
 * the five screens needs a Hilt ViewModel. That is also the limit of what this can assert: the
 * destination *bodies* only run inside a composed `NavHost`, where `hiltViewModel()` resolves,
 * which is `:app`'s instrumented suite. See the module's `nimazCoverage` KDoc.
 */
@RunWith(RobolectricTestRunner::class)
class PrayerGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.PrayerTimes) {
            prayerGraph(navController)
        }.toList()

    @Test
    fun `all five prayer destinations are registered`() {
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(5)
        listOf(
            Route.QiblaNav::class,
            Route.PrayerTimes::class,
            Route.MonthlyPrayerTimes::class,
            Route.NightWorship::class,
            Route.Qibla::class,
        ).forEach { route ->
            assertThat(routes.any { it.contains(route.qualifiedName!!) }).isTrue()
        }
    }

    @Test
    fun `each destination resolves as a start destination in its own right`() {
        // Not symmetry for its own sake. Every one of these is opened directly — from the home
        // screen, from the More menu, from the bottom bar — rather than only ever being pushed
        // on top of another prayer screen, and `createGraph` throws here if the route it is
        // handed is not among the destinations the builder registered.
        listOf<Any>(
            Route.QiblaNav,
            Route.PrayerTimes,
            Route.MonthlyPrayerTimes,
            Route.NightWorship,
            Route.Qibla,
        ).forEach { start ->
            val graph = navController.createGraph(startDestination = start) {
                prayerGraph(navController)
            }
            assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
        }
    }
}
