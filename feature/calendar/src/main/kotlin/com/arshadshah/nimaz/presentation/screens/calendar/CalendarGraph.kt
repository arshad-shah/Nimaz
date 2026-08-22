package com.arshadshah.nimaz.presentation.screens.calendar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.taggedComposable

/**
 * The 2 Calendar destinations — the Islamic calendar and its events.
 *
 * Split out of `NavGraph.kt` in PR 12 of #551. That file registered all 94 destinations and
 * imported 69 screen composables, which meant every screen in the app was reachable from one
 * place — and no feature could move into its own module while that was true, because `:app`
 * would have had to import from all eleven feature modules at once.
 *
 * The bodies are unchanged; only their location is. `:app` keeps the `NavHost` and calls this.
 *
 * It takes a `NavController` rather than the `onNavigate` lambda #563 sketches because 11 of the
 * 158 `navigate` calls in these blocks pass a `NavOptionsBuilder` — `popUpTo`, `launchSingleTop` —
 * which `(Route) -> Unit` cannot express, and flattening them would change back-stack behaviour
 * silently. A graph function *is* navigation wiring, so holding the controller is what it is for;
 * the rule that matters is that a **screen** must not, which `NavControllerConfinementTest`
 * enforces.
 */
fun NavGraphBuilder.calendarGraph(navController: NavController) {
    // Islamic Calendar
    taggedComposable<Route.IslamicCalendar>(ScreenTags.IslamicCalendar) {
        IslamicCalendarScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.IslamicMonth>(ScreenTags.IslamicMonth) { backStackEntry ->
        backStackEntry.toRoute<Route.IslamicMonth>()
        IslamicCalendarScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
