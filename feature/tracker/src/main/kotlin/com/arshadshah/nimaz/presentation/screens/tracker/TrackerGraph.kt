package com.arshadshah.nimaz.presentation.screens.tracker

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerStatsScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerTrackerScreen
import com.arshadshah.nimaz.presentation.screens.prayer.QadaPrayersScreen
import com.arshadshah.nimaz.presentation.screens.fasting.FastTrackerScreen
import com.arshadshah.nimaz.presentation.screens.fasting.MakeupFastsScreen
import com.arshadshah.nimaz.presentation.screens.tasbih.TasbihScreen

/**
 * The 14 Tracker destinations — the prayer tracker, fasting and tasbih.
 *
 * Fourteen, not the eleven this said before #613: `docs/NAVIGATION.md` was corrected to 14 in
 * #625 (which recounted every graph and found five wrong), and `TrackerGraphTest` now asserts the
 * number against the graph itself. `check_docs.py`'s NAV-03 cannot catch a drift here — it
 * compares the 94-destination total against `Routes.kt`, not the per-graph split.
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
fun NavGraphBuilder.trackerGraph(navController: NavController) {
    // Prayer *tracking*, moved out of `prayerGraph` in PR 18 of #551. Their screens live in
    // `screens/prayer` but drive `viewmodel/tracker`, and by the axis this migration follows —
    // a screen belongs to the module owning the ViewModel it drives — they are tracking surfaces,
    // not prayer-time ones. Prayer *times* (calculation, adhan, qibla) stay behind for PR 20.
    taggedComposable<Route.PrayerTracker>(ScreenTags.PrayerTracker) {
        PrayerTrackerScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToStats = { navController.navigate(Route.PrayerStats) },
            onNavigateToQada = { navController.navigate(Route.QadaPrayers) },
        )
    }

    taggedComposable<Route.PrayerStats>(ScreenTags.PrayerStats) {
        PrayerStatsScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.QadaPrayers>(ScreenTags.QadaPrayers) {
        QadaPrayersScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.Tasbih>(ScreenTags.Tasbih) {
        TasbihScreen(
            onNavigateToHistory = { navController.navigate(Route.TasbihHistory) },
            onNavigateToChooseDhikr = { navController.navigate(Route.TasbihPresets) },
            onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset()) },
            onNavigateToSettings = { navController.navigate(Route.Settings) }
        )
    }

    // Fasting screens
    taggedComposable<Route.FastingHome>(ScreenTags.FastingHome) {
        FastTrackerScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
            onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
        )
    }

    taggedComposable<Route.FastingTracker>(ScreenTags.FastingTracker) {
        FastTrackerScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
            onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
        )
    }

    taggedComposable<Route.FastingStats>(ScreenTags.FastingStats) {
        FastTrackerScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
            onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
        )
    }

    taggedComposable<Route.MakeupFasts>(ScreenTags.MakeupFasts) {
        MakeupFastsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Tasbih screens
    taggedComposable<Route.TasbihHome>(ScreenTags.TasbihHome) {
        TasbihScreen(
            onNavigateToHistory = { navController.navigate(Route.TasbihHistory) },
            onNavigateToChooseDhikr = { navController.navigate(Route.TasbihPresets) },
            onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset()) },
            onNavigateToSettings = { navController.navigate(Route.Settings) }
        )
    }

    taggedComposable<Route.TasbihCounter>(ScreenTags.TasbihCounterScreen) { backStackEntry ->
        backStackEntry.toRoute<Route.TasbihCounter>()
        TasbihScreen(
            onNavigateToHistory = { navController.navigate(Route.TasbihStats) },
            onNavigateToChooseDhikr = { navController.navigate(Route.TasbihPresets) },
            onNavigateToSettings = { navController.navigate(Route.Settings) }
        )
    }

    taggedComposable<Route.TasbihPresets>(ScreenTags.TasbihPresets) {
        com.arshadshah.nimaz.presentation.screens.tasbih.ChooseDhikrScreen(
            onBack = { navController.popBackStack() },
            onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset()) },
            onEditPreset = { presetId ->
                navController.navigate(Route.TasbihAddPreset(presetId))
            }
        )
    }

    taggedComposable<Route.TasbihStats>(ScreenTags.TasbihStats) {
        TasbihScreen(
            onNavigateToHistory = { },
            onNavigateToSettings = { navController.navigate(Route.Settings) }
        )
    }

    taggedComposable<Route.TasbihHistory>(ScreenTags.TasbihHistory) {
        com.arshadshah.nimaz.presentation.screens.tasbih.TasbihHistoryScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.TasbihAddPreset>(ScreenTags.TasbihAddPreset) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.TasbihAddPreset>()
        com.arshadshah.nimaz.presentation.screens.tasbih.AddPresetScreen(
            presetId = args.presetId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
