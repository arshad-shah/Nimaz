package com.arshadshah.nimaz.presentation.screens.onboarding

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
 * The onboarding feature's slice of the route graph — one destination, and the one the app cannot
 * start without.
 *
 * `NavGraph` resolves its start destination from `onboardingCompleted` exactly once, so on a
 * first run `Route.Onboarding` *is* the start destination. A destination that fails to register
 * throws `IllegalArgumentException: navigation destination … is not a direct child of this
 * NavGraph`, and nothing catches that at build time: here it would be thrown on the first frame
 * of the first launch, before the user has seen anything at all.
 *
 * The graph is built without a `NavHost`, so nothing is composed and the screen needs no Hilt
 * ViewModel. This asserts registration; `OnboardingScreenTest` asserts that the screen renders.
 */
@RunWith(RobolectricTestRunner::class)
class OnboardingGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.Onboarding) {
            onboardingGraph(navController)
        }.toList()

    @Test
    fun `the onboarding destination is registered`() {
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(1)
        assertThat(routes.single()).contains(Route.Onboarding::class.qualifiedName)
    }

    @Test
    fun `the graph can be built with onboarding as the start destination`() {
        // The first-run case: `NavGraph` picks this start destination when `onboardingCompleted`
        // is false, and `createGraph` throws here if the route it is given is not among the
        // destinations the builder registered.
        val graph = navController.createGraph(startDestination = Route.Onboarding) {
            onboardingGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
