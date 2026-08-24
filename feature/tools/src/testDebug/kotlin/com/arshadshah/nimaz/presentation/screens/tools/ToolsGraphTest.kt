package com.arshadshah.nimaz.presentation.screens.tools

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
 * The Tools feature's slice of the route graph — the zakat calculator and its history.
 *
 * A destination that fails to register throws `IllegalArgumentException: navigation destination …
 * is not a direct child of this NavGraph`, and nothing catches that at build time. The two here
 * navigate to each other — the calculator's top bar opens the history, the history's FAB opens the
 * calculator — so a missing registration is not a dead entry point but a **crash on tap**, from a
 * screen the user reached deliberately. It surfaces first on a device, in the middle of someone's
 * zakat.
 *
 * The graph is built without a `NavHost`, so nothing is composed and neither screen needs a Hilt
 * ViewModel. This asserts registration; `ZakatCalculatorScreenTest` and `ZakatHistoryScreenTest`
 * assert that the screens render.
 */
@RunWith(RobolectricTestRunner::class)
class ToolsGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.ZakatCalculator) {
            toolsGraph(navController)
        }.toList()

    @Test
    fun `both zakat destinations are registered`() {
        // Two, and exactly two: the KDoc on `toolsGraph` says "the 2 Tools destinations", and a
        // third arriving without its own test is how a route ships unreachable.
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(2)
        assertThat(routes.any { it.contains(Route.ZakatCalculator::class.qualifiedName!!) }).isTrue()
        assertThat(routes.any { it.contains(Route.ZakatHistory::class.qualifiedName!!) }).isTrue()
    }

    @Test
    fun `the calculator can be the start destination`() {
        // `createGraph` throws here if the route it is handed is not among the destinations the
        // builder registered — the same failure the app would hit on the first frame.
        val graph = navController.createGraph(startDestination = Route.ZakatCalculator) {
            toolsGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }

    @Test
    fun `the history can be the start destination`() {
        // Not symmetry for its own sake: the history is what the announcement deep link and the
        // calculator's own top-bar action open, so it has to resolve as a destination in its own
        // right rather than only as somewhere the calculator happens to push.
        val graph = navController.createGraph(startDestination = Route.ZakatHistory) {
            toolsGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
