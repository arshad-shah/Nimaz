package com.arshadshah.nimaz.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * The bottom-bar tab a route *is*, when it is one — including the two routes that duplicate a
 * tab under another name.
 *
 * `TasbihHome` and `Qibla` render the same screens as the `Tasbih` and `QiblaNav` tabs, but they
 * are not tabs: the bar only shows on the five [BottomNavDestination] routes, so landing on either
 * duplicate hid it. An announcement for `tasbih`, the Home screen's Tasbih shortcut and the More
 * screen all went there — and the Tasbih screen, being a tab, has no back button — so the reader
 * arrived on a page with no bar and no way on but the system back gesture.
 */
fun Route.asTab(): Route? = when (this) {
    Route.Home -> Route.Home
    Route.Quran -> Route.Quran
    Route.Tasbih, Route.TasbihHome -> Route.Tasbih
    Route.QiblaNav, Route.Qibla -> Route.QiblaNav
    Route.More -> Route.More
    else -> null
}

/**
 * Switch to [tab] exactly as tapping it in the bottom bar does: back to the start destination,
 * the tab's own back stack saved and restored, and never two copies on top of each other.
 */
fun NavController.navigateToTab(tab: Route) {
    navigate(tab) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Open [route] from inside the app. A route that is a tab — or duplicates one — becomes a tab
 * switch, so the bottom bar stays; anything else is an ordinary push.
 */
fun NavController.navigateInApp(route: Route) {
    val tab = route.asTab()
    if (tab != null) navigateToTab(tab) else navigate(route)
}

/**
 * Open [route] from outside the app's own navigation — an announcement, a notification, a widget.
 *
 * A tab becomes a switch to that tab's own screen, for the reason [asTab] gives. Anything else lands on top of Home,
 * so Back returns there rather than out of the app, and a second identical link does not stack
 * a second copy.
 */
fun NavController.navigateFromOutside(route: Route) {
    val tab = route.asTab()
    if (tab != null) {
        // The tab itself, not the tab as it was left. The bar's restoreState brings back the
        // tab's saved stack — and since everything pushed on top of Home belongs to Home's, a
        // `home` link opened over Settings restored Settings. A link names a place; land there.
        navigate(tab) {
            popUpTo(graph.findStartDestination().id)
            launchSingleTop = true
        }
        return
    }
    navigate(route) {
        popUpTo(Route.Home) { inclusive = false }
        launchSingleTop = true
    }
}
