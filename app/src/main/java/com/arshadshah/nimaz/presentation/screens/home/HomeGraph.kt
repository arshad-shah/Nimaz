package com.arshadshah.nimaz.presentation.screens.home

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.Home
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.announcementRoute
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.core.navigation.worshipCardDestination

/**
 * The 1 Home destination — the home screen.
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
fun NavGraphBuilder.homeGraph(navController: NavController, analyticsContext: Context) {
    // Main screens
    taggedComposable<Route.Home>(ScreenTags.Home) {
        HomeScreen(
            onNavigateToAlKahf = { navController.navigate(Route.QuranReader(surahNumber = 18)) },
            onNavigateToHadith = { navController.navigate(Route.HadithHome) },
            onNavigateToDua = { duaId -> navController.navigate(Route.DuaReader(duaId)) },
            onNavigateToTasbih = { navController.navigate(Route.TasbihHome) },
            onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
            onNavigateToFasting = { navController.navigate(Route.FastingHome) },
            onNavigateToZakat = { navController.navigate(Route.ZakatCalculator) },
            onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker) },
            onNavigateToSettings = { navController.navigate(Route.Settings) },
            onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) },
            onNavigateToPrayerTimes = { navController.navigate(Route.PrayerTimes) },
            onOpenHadith = { hadithId -> navController.navigate(Route.HadithReader(hadithId)) },
            // Every worship card now leads somewhere; the mapping is a pure function so
            // it is asserted in a JVM test rather than only through the UI.
            onOpenWorship = { type -> navController.navigate(worshipCardDestination(type)) },
            onOpenAnnouncementRoute = { key ->
                if (key.startsWith("https://")) {
                    runCatching {
                        analyticsContext.startActivity(
                            Intent(Intent.ACTION_VIEW, key.toUri())
                        )
                    }
                } else {
                    announcementRoute(key)?.let { navController.navigate(it) }
                }
            }
        )
    }
}
