package com.arshadshah.nimaz.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.screens.about.aboutGraph
import com.arshadshah.nimaz.presentation.screens.calendar.calendarGraph
import com.arshadshah.nimaz.presentation.screens.content.contentGraph
import com.arshadshah.nimaz.presentation.screens.home.homeGraph
import com.arshadshah.nimaz.presentation.screens.onboarding.onboardingGraph
import com.arshadshah.nimaz.presentation.screens.prayer.prayerGraph
import com.arshadshah.nimaz.presentation.screens.quran.quranGraph
import com.arshadshah.nimaz.presentation.screens.search.searchGraph
import com.arshadshah.nimaz.presentation.screens.settings.settingsGraph
import com.arshadshah.nimaz.presentation.screens.tools.toolsGraph
import com.arshadshah.nimaz.presentation.screens.tracker.trackerGraph
import com.arshadshah.nimaz.presentation.theme.LocalAnimationsEnabled
import com.arshadshah.nimaz.presentation.theme.isTablet
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    pendingQuranSurah: Int? = null,
    onPendingQuranSurahConsumed: () -> Unit = {},
    pendingIslamicCalendar: Boolean = false,
    onPendingIslamicCalendarConsumed: () -> Unit = {},
    pendingAnnouncementRoute: String? = null,
    onPendingAnnouncementRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Analytics: log a screen_view whenever the active destination changes.
    // Type-safe routes serialize as a fully-qualified name with optional args
    // (e.g. "...Route$QuranReader/{surahNumber}?..."), so trim down to the simple
    // screen name for readable analytics.
    val analyticsContext = androidx.compose.ui.platform.LocalContext.current
    var previousScreen by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentDestination?.route) {
        val route = currentDestination?.route ?: return@LaunchedEffect
        val screenName = route
            .substringBefore('/')
            .substringBefore('?')
            .substringAfterLast('.')
            .substringAfterLast('$')
        // Attach the screen the user came from so funnel reports can surface
        // dead-ends and back-and-forth loops where users get stuck.
        AppAnalytics.logScreenView(
            screenName = screenName,
            previousScreen = previousScreen,
            context = analyticsContext,
        )
        previousScreen = screenName
    }

    // Deep-link from the Quran audio notification / lock-screen player.
    // Per UX choice: clear the back stack to Home so Back returns to the Quran
    // home screen rather than wherever the user happened to be.
    LaunchedEffect(pendingQuranSurah) {
        val surah = pendingQuranSurah ?: return@LaunchedEffect
        navController.navigate(Route.QuranReader(surahNumber = surah)) {
            popUpTo(Route.Home) { inclusive = false }
            launchSingleTop = true
        }
        onPendingQuranSurahConsumed()
    }

    // Deep-link from the Hijri calendar home-screen widget. popUpTo(Home) so
    // system Back returns the user to the home screen — never strands them
    // mid-stack on a cold launch from the widget tap.
    LaunchedEffect(pendingIslamicCalendar) {
        if (!pendingIslamicCalendar) return@LaunchedEffect
        navController.navigate(Route.IslamicCalendar) {
            popUpTo(Route.Home) { inclusive = false }
            launchSingleTop = true
        }
        onPendingIslamicCalendarConsumed()
    }

    // Deep-link from a tapped FCM announcement notification. Allowlist-resolved;
    // unknown keys just land on Home (where the banner shows) and https URLs
    // open in the browser.
    LaunchedEffect(pendingAnnouncementRoute) {
        val key = pendingAnnouncementRoute ?: return@LaunchedEffect
        if (key.startsWith("https://")) {
            runCatching {
                analyticsContext.startActivity(
                    Intent(Intent.ACTION_VIEW, key.toUri())
                )
            }
        } else {
            announcementRoute(key)?.let { route ->
                navController.navigate(route) {
                    popUpTo(Route.Home) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        onPendingAnnouncementRouteConsumed()
    }

    // Onboarding ViewModel to check status
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.state.collectAsState()

    // Track if we've determined the start destination
    var startDestinationDetermined by remember { mutableStateOf(false) }

    // Check if we should show navigation (only on main bottom nav screens)
    val showNav = currentDestination?.hierarchy?.any { dest ->
        dest.hasRoute<Route.Home>() ||
                dest.hasRoute<Route.Quran>() ||
                dest.hasRoute<Route.Tasbih>() ||
                dest.hasRoute<Route.QiblaNav>() ||
                dest.hasRoute<Route.More>()
    } == true

    // Show loading while determining start destination
    if (onboardingState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Determine start destination based on onboarding status
    val startDestination: Route = if (onboardingState.onboardingCompleted) {
        Route.Home
    } else {
        Route.Onboarding
    }

    // Navigate to correct start destination when determined
    LaunchedEffect(onboardingState.onboardingCompleted, startDestinationDetermined) {
        if (!startDestinationDetermined) {
            startDestinationDetermined = true
        }
    }

    // Determine navigation layout type:
    // - Phone (Compact): BottomNav
    // - Tablet (Medium/Expanded): NavigationRail
    // - Hidden on detail screens
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navLayoutType = if (!showNav) {
        NavigationSuiteType.None
    } else {
        val defaultType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        // Force NavigationRail on all non-compact sizes (user preference: no drawer)
        if (adaptiveInfo.windowSizeClass.isTablet) {
            NavigationSuiteType.NavigationRail
        } else {
            defaultType
        }
    }

    NavigationSuiteScaffold(
        layoutType = navLayoutType,
        // Transparent container so the app-wide NimazPatternBackground (drawn at the
        // root in MainActivity) shows through. The default is an OPAQUE
        // colorScheme.background, which painted over the ornament between the root
        // and every screen — the reason patterns appeared nowhere despite the
        // per-screen scaffolds being transparent. contentColor is pinned explicitly
        // since it would otherwise derive from the (now transparent) container.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationSuiteItems = {
            bottomNavItems.forEach { navItem ->
                val selected = currentDestination?.hierarchy?.any {
                    it.hasRoute(navItem.route::class)
                } == true

                item(
                    modifier = Modifier.testTag(ScreenTags.bottomNav(navItem.label)),
                    icon = { NimazIcon(navItem.icon, contentDescription = navItem.label) },
                    label = { Text(navItem.label) },
                    selected = selected,
                    onClick = {
                        navController.navigate(navItem.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        // Screen transitions, honouring the Appearance > Animations preference. `NavHost` had no
        // transition arguments at all, which meant Navigation Compose's 700 ms crossfade for all
        // 94 destinations — the same in both directions, so forward and back looked identical.
        // Set here rather than per-destination: a `taggedComposable` that overrides them is then a
        // deliberate exception, and there are none.
        val animationsEnabled = LocalAnimationsEnabled.current

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { NimazNavTransitions.enter(animationsEnabled) },
            exitTransition = { NimazNavTransitions.exit(animationsEnabled) },
            popEnterTransition = { NimazNavTransitions.popEnter(animationsEnabled) },
            popExitTransition = { NimazNavTransitions.popExit(animationsEnabled) },
        ) {
            // Every destination lives in its feature's graph extension. See #563: one file
            // registering all 94 of them is what stopped any feature moving into its own
            // module, because :app would have had to import from all eleven at once.
            onboardingGraph(navController)
            homeGraph(navController, analyticsContext)
            quranGraph(navController)
            contentGraph(navController)
            trackerGraph(navController)
            prayerGraph(navController)
            calendarGraph(navController)
            searchGraph(navController)
            toolsGraph(navController)
            aboutGraph(navController)
            settingsGraph(navController)
        }
    }
}

private data class BottomNavItem(
    val route: Route,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Route.Home, "Home", Icons.Default.Home),
    BottomNavItem(Route.Quran, "Quran", Icons.AutoMirrored.Filled.MenuBook),
    BottomNavItem(Route.Tasbih, "Tasbih", Icons.Default.TouchApp),
    BottomNavItem(Route.QiblaNav, "Qibla", Icons.Default.Explore),
    BottomNavItem(Route.More, "More", Icons.Default.MoreHoriz)
)

