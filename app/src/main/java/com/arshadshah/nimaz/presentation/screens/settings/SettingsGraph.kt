package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.restartApp
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveSettingsScreen

/**
 * The 16 Settings destinations — every settings screen, including the notification and worship trees.
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
fun NavGraphBuilder.settingsGraph(navController: NavController) {
    taggedComposable<Route.SettingsZakat>(ScreenTags.SettingsZakat) {
        ZakatSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Settings
    taggedComposable<Route.Settings>(ScreenTags.Settings) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AdaptiveSettingsScreen(
            onNavigate = { navController.navigate(it) },
            onBack = { navController.popBackStack() },
            onRestartApp = { restartApp(context) },
        )
    }

    taggedComposable<Route.SettingsPrayerCalculation>(ScreenTags.SettingsPrayerCalculation) {
        PrayerSettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) }
        )
    }

    taggedComposable<Route.SettingsNotifications>(ScreenTags.SettingsNotifications) {
        NotificationSettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPrayers = { navController.navigate(Route.SettingsNotificationsPrayers) },
            onNavigateToWorshipReminders = { navController.navigate(Route.SettingsWorshipReminders) },
            onNavigateToWeekly = { navController.navigate(Route.SettingsNotificationsWeekly) },
            onNavigateToSound = { navController.navigate(Route.SettingsNotificationsSound) },
            onNavigateToDiagnostics = { navController.navigate(Route.SettingsNotificationsDiagnostics) },
        )
    }

    taggedComposable<Route.SettingsWorshipReminders>(
        ScreenTags.SettingsWorshipReminders
    ) {
        WorshipRemindersScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.SettingsNotificationsPrayers>(
        ScreenTags.SettingsNotificationsPrayers
    ) {
        PrayerNotificationsScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.SettingsNotificationsWeekly>(
        ScreenTags.SettingsNotificationsWeekly
    ) {
        NotificationWeeklyScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.SettingsNotificationsSound>(
        ScreenTags.SettingsNotificationsSound
    ) {
        NotificationSoundScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.SettingsNotificationsDiagnostics>(
        ScreenTags.SettingsNotificationsDiagnostics
    ) {
        NotificationDiagnosticsScreen(onNavigateBack = { navController.popBackStack() })
    }

    taggedComposable<Route.SettingsAppearance>(ScreenTags.SettingsAppearance) {
        AppearanceSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SettingsLanguage>(ScreenTags.SettingsLanguage) {
        LanguageScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SettingsLocation>(ScreenTags.SettingsLocation) {
        LocationScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SettingsQuran>(ScreenTags.SettingsQuran) {
        QuranSettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSelectReciter = { navController.navigate(Route.SelectReciter) },
            onNavigateToSelectTranslation = {
                navController.navigate(Route.SelectTranslation)
            }
        )
    }

    taggedComposable<Route.SettingsWidgets>(ScreenTags.SettingsWidgets) {
        WidgetsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SettingsSync>(ScreenTags.SettingsSync) {
        com.arshadshah.nimaz.presentation.screens.settings.SyncScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SearchSettings>(ScreenTags.SearchSettings) {
        SearchSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
