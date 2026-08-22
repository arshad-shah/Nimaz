package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.presentation.screens.qibla.QiblaScreen
import com.arshadshah.nimaz.presentation.screens.worship.NightWorshipScreen

/**
 * The 8 Prayer destinations — prayer times, qibla and the night-worship screens.
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
fun NavGraphBuilder.prayerGraph(navController: NavController) {
    taggedComposable<Route.QiblaNav>(ScreenTags.QiblaNav) {
        QiblaScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Prayer screens
    taggedComposable<Route.PrayerTimes>(ScreenTags.PrayerTimes) {
        PrayerTimesScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(Route.SettingsPrayerCalculation) }
        )
    }

    taggedComposable<Route.PrayerTracker>(ScreenTags.PrayerTracker) {
        PrayerTrackerScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToStats = { navController.navigate(Route.PrayerStats) },
            onNavigateToQada = { navController.navigate(Route.QadaPrayers) },
        )
    }

    taggedComposable<Route.PrayerStats>(ScreenTags.PrayerStats) {
        PrayerStatsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.QadaPrayers>(ScreenTags.QadaPrayers) {
        QadaPrayersScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.MonthlyPrayerTimes>(ScreenTags.MonthlyPrayerTimes) {
        MonthlyPrayerTimesScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.NightWorship>(ScreenTags.NightWorship) {
        NightWorshipScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenSurah = { surah: Int -> navController.navigate(Route.QuranReader(surah)) },
            onOpenDuaCategory = { id: String -> navController.navigate(Route.DuaCategory(id)) },
            onOpenHadith = { id: String -> navController.navigate(Route.HadithReader(id)) },
        )
    }

    // Qibla
    taggedComposable<Route.Qibla>(ScreenTags.Qibla) {
        QiblaScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
