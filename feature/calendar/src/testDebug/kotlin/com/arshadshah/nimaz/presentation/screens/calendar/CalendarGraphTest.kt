package com.arshadshah.nimaz.presentation.screens.calendar

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
import kotlin.reflect.KClass

/**
 * The calendar feature's slice of the route graph.
 *
 * Two destinations, and a destination that fails to register does not fail anything at build
 * time: it throws `IllegalArgumentException: navigation destination … is not a direct child of
 * this NavGraph` the first time a *user* taps whatever opens it.
 *
 * `Route.IslamicMonth` is the one worth naming. Nothing in the app's own UI opens it — it is
 * reached from an announcement deep link — so it is exactly the kind of destination that can go
 * missing in a refactor and stay missing until a campaign goes out.
 *
 * The graph is built without a `NavHost`, so nothing is composed and neither screen needs a Hilt
 * view model. This asserts registration; that the screen renders is `IslamicCalendarScreenTest`.
 */
@RunWith(RobolectricTestRunner::class)
class CalendarGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun graph() = navController.createGraph(startDestination = Route.IslamicCalendar) {
        calendarGraph(navController)
    }

    private fun destinations(): List<NavDestination> = graph().toList()

    private fun hasDestination(route: KClass<*>): Boolean =
        destinations().any { it.route?.contains(route.qualifiedName.orEmpty()) == true }

    @Test
    fun `both calendar destinations are registered`() {
        assertThat(destinations()).hasSize(2)
    }

    @Test
    fun `the calendar itself is registered`() {
        assertThat(hasDestination(Route.IslamicCalendar::class)).isTrue()
    }

    @Test
    fun `the month destination an announcement deep-links to is registered`() {
        // No screen in the app opens this one; a campaign does.
        assertThat(hasDestination(Route.IslamicMonth::class)).isTrue()
    }

    @Test
    fun `every destination carries a route`() {
        destinations().forEach { assertThat(it.route).isNotEmpty() }
    }

    @Test
    fun `the two destinations are distinct`() {
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes.toSet()).hasSize(2)
    }
}
