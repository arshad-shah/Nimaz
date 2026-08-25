package com.arshadshah.nimaz.presentation.screens.about

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
 * The About feature's slice of the route graph — seven destinations for three surfaces.
 *
 * All seven are reached directly rather than only as somewhere another screen pushes.
 * `Route.More` is a bottom-nav tab; `SettingsAbout` and `SettingsHelp` are opened from the menu
 * *and* from the Settings screen; `Licenses` from About; and the three argument-carrying routes
 * from a list row. A destination that fails to register throws
 * `IllegalArgumentException: navigation destination … is not a direct child of this NavGraph`
 * — at the moment a **user taps the row**, not at build time, and not in any of the four gates
 * `CLAUDE.md` lists.
 *
 * The graph is built without a `NavHost`, so nothing composes and none of these screens'
 * Hilt ViewModels is constructed. This asserts registration only; the screen tests beside it
 * assert what each destination renders, and `:core:navigation`'s `HelpDeepLinkTest` asserts
 * where a help deep link resolves to.
 */
@RunWith(RobolectricTestRunner::class)
class AboutGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.More) {
            aboutGraph(navController)
        }.toList()

    @Test
    fun `all seven about destinations are registered`() {
        // Seven, and exactly seven: `aboutGraph`'s KDoc says "the 7 About destinations", and an
        // eighth arriving without its own case is how a route ships unreachable.
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(7)
        listOf(
            Route.More::class,
            Route.SettingsAbout::class,
            Route.Licenses::class,
            Route.LicenseDetail::class,
            Route.SettingsHelp::class,
            Route.HelpTopicDetail::class,
            Route.HelpGuide::class,
        ).forEach { route ->
            assertThat(routes.any { it.contains(route.qualifiedName!!) }).isTrue()
        }
    }

    @Test
    fun `the More tab resolves as a destination in its own right`() {
        // `createGraph` throws here if the route it is handed is not among the destinations the
        // builder registered — the same failure the app would hit on its first frame.
        assertStartDestination(Route.More)
    }

    @Test
    fun `about and help resolve as destinations in their own right`() {
        // Both are reached from the More menu and from Settings, so neither is only ever a
        // child of the other.
        assertStartDestination(Route.SettingsAbout)
        assertStartDestination(Route.SettingsHelp)
    }

    @Test
    fun `the licence list and one licence resolve as destinations`() {
        assertStartDestination(Route.Licenses)
        assertStartDestination(Route.LicenseDetail(libraryHashCode = 42))
    }

    @Test
    fun `a help topic and a help guide resolve as destinations`() {
        // These two are what a help deep link lands on, so an unregistered one turns "take me
        // there" into a crash rather than into a no-op.
        assertStartDestination(Route.HelpTopicDetail(topicId = "prayer"))
        assertStartDestination(Route.HelpGuide(guideId = "set-alert"))
    }

    private fun assertStartDestination(route: Route) {
        val graph = navController.createGraph(startDestination = route) {
            aboutGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
