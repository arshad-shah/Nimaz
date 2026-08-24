package com.arshadshah.nimaz.presentation.screens.search

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
 * The Search feature's slice of the route graph — four ways into the same screen.
 *
 * They are not interchangeable. Three are scoped searches reached from a section (Qur'an, Hadith,
 * Duas) and one is Global Search, the only one that carries the Ask-with-Proof surface. A missing
 * registration throws `IllegalArgumentException: navigation destination … is not a direct child of
 * this NavGraph`, and nothing catches it at build time — it surfaces as a crash the moment someone
 * taps the search icon on that section, which is a deliberate action rather than a stray one.
 *
 * Each is asserted as a start destination in its own right because each is reached directly: the
 * home search field opens Global Search, and every section screen opens its own scoped one. A
 * route that only resolves as somewhere another screen happens to push is not enough.
 *
 * The graph is built without a `NavHost`, so nothing is composed and the screen's Hilt ViewModels
 * are never constructed. This asserts registration; `SearchScreenTest` and `SearchScreenAskTest`
 * assert that the screen renders, and `:core:navigation`'s `SearchProofNavigationTest` /
 * `ContentTargetRoutesTest` assert where a proof card's target resolves to.
 */
@RunWith(RobolectricTestRunner::class)
class SearchGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.GlobalSearch) {
            searchGraph(navController)
        }.toList()

    @Test
    fun `all four search destinations are registered`() {
        // Four, and exactly four: the KDoc on `searchGraph` says "the 4 Search destinations", and
        // a fifth arriving without its own test is how a route ships unreachable.
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(4)
        assertThat(routes.any { it.contains(Route.GlobalSearch::class.qualifiedName!!) }).isTrue()
        assertThat(routes.any { it.contains(Route.QuranSearch::class.qualifiedName!!) }).isTrue()
        assertThat(routes.any { it.contains(Route.HadithSearch::class.qualifiedName!!) }).isTrue()
        assertThat(routes.any { it.contains(Route.DuaSearch::class.qualifiedName!!) }).isTrue()
    }

    @Test
    fun `global search resolves as a destination in its own right`() {
        // `createGraph` throws here if the route it is handed is not among the destinations the
        // builder registered — the same failure the app would hit on the first frame.
        val graph = navController.createGraph(startDestination = Route.GlobalSearch) {
            searchGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }

    @Test
    fun `quran search resolves as a destination in its own right`() {
        val graph = navController.createGraph(startDestination = Route.QuranSearch) {
            searchGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }

    @Test
    fun `hadith search resolves as a destination in its own right`() {
        val graph = navController.createGraph(startDestination = Route.HadithSearch) {
            searchGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }

    @Test
    fun `dua search resolves as a destination in its own right`() {
        val graph = navController.createGraph(startDestination = Route.DuaSearch) {
            searchGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
