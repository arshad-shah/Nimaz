package com.arshadshah.nimaz.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController

/**
 * Everything that arrives from outside the app's own navigation, turned into navigation.
 *
 * `MainActivity` collects these from the intent it was launched or re-launched with — a tapped
 * Quran audio notification, the Hijri calendar widget, an FCM announcement, a worship reminder
 * — and this is where each becomes a destination, then is consumed so a recomposition does not
 * navigate twice. Every one goes through [navigateFromOutside]: a tab stays a tab (with its
 * bottom bar), anything else lands on top of Home so Back returns there.
 *
 * Out of `NavGraph` so it can be exercised on a real `NavHost` without the app's Hilt graph —
 * every one of these used to be a `LaunchedEffect` no test reached.
 */
@Composable
fun PendingEntryEffects(
    navController: NavController,
    pendingQuranSurah: Int?,
    onPendingQuranSurahConsumed: () -> Unit,
    pendingIslamicCalendar: Boolean,
    onPendingIslamicCalendarConsumed: () -> Unit,
    pendingAnnouncementRoute: String?,
    onPendingAnnouncementRouteConsumed: () -> Unit,
    pendingRoute: Route?,
    onPendingRouteConsumed: () -> Unit,
    openUrl: (String) -> Unit,
) {
    // The Quran audio notification / lock-screen player: the surah that is playing.
    LaunchedEffect(pendingQuranSurah) {
        val surah = pendingQuranSurah ?: return@LaunchedEffect
        navController.navigateFromOutside(Route.QuranReader(surahNumber = surah))
        onPendingQuranSurahConsumed()
    }

    // The Hijri calendar home-screen widget.
    LaunchedEffect(pendingIslamicCalendar) {
        if (!pendingIslamicCalendar) return@LaunchedEffect
        navController.navigateFromOutside(Route.IslamicCalendar)
        onPendingIslamicCalendarConsumed()
    }

    // A destination MainActivity has already resolved — today a tapped worship reminder.
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        navController.navigateFromOutside(route)
        onPendingRouteConsumed()
    }

    // A tapped FCM announcement. Allowlist-resolved: an unknown key lands nowhere new (Home,
    // where its banner shows), and an https link opens in the browser.
    LaunchedEffect(pendingAnnouncementRoute) {
        val key = pendingAnnouncementRoute ?: return@LaunchedEffect
        if (key.startsWith("https://")) {
            openUrl(key)
        } else {
            announcementRoute(key)?.let { route -> navController.navigateFromOutside(route) }
        }
        onPendingAnnouncementRouteConsumed()
    }
}
