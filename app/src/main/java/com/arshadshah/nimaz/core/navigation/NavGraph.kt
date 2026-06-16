package com.arshadshah.nimaz.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.play.core.review.ReviewManagerFactory
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import kotlin.system.exitProcess
import com.arshadshah.nimaz.presentation.screens.about.AboutScreen
import com.arshadshah.nimaz.presentation.screens.about.LicenseDetailScreen
import com.arshadshah.nimaz.presentation.screens.about.LicensesScreen
import com.arshadshah.nimaz.presentation.screens.bookmarks.BookmarksScreen
import com.arshadshah.nimaz.presentation.screens.calendar.IslamicCalendarScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaCategoryScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaReaderScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuasCollectionScreen
import com.arshadshah.nimaz.presentation.screens.fasting.FastTrackerScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithChaptersScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithReaderScreen
import com.arshadshah.nimaz.presentation.screens.home.HomeScreen
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaDetailScreen
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiDetailScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamCreateScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamDetailScreen
import com.arshadshah.nimaz.presentation.screens.prophets.ProphetDetailScreen
import com.arshadshah.nimaz.presentation.screens.onboarding.OnboardingScreen
import com.arshadshah.nimaz.presentation.screens.prayer.MonthlyPrayerTimesScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerStatsScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerTrackerScreen
import com.arshadshah.nimaz.presentation.screens.qibla.QiblaScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveAsmaUlHusnaScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveAsmaUnNabiScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveDuaScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveHadithScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveKhatamScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveMoreScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveProphetsScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveQuranScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveSettingsScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranReaderScreen
import com.arshadshah.nimaz.presentation.screens.quran.SelectReciterScreen
import com.arshadshah.nimaz.presentation.screens.quran.TafseerScreen
import com.arshadshah.nimaz.presentation.screens.quran.SurahInfoScreen
import com.arshadshah.nimaz.presentation.screens.search.SearchScreen
import com.arshadshah.nimaz.presentation.screens.settings.AppearanceSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.LanguageScreen
import com.arshadshah.nimaz.presentation.screens.settings.LocationScreen
import com.arshadshah.nimaz.presentation.screens.settings.NotificationSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.PrayerSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.QuranSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.WidgetsScreen
import com.arshadshah.nimaz.presentation.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatCalculatorScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatHistoryScreen
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingViewModel

@Composable
fun NavGraph(
    pendingQuranSurah: Int? = null,
    onPendingQuranSurahConsumed: () -> Unit = {},
    pendingIslamicCalendar: Boolean = false,
    onPendingIslamicCalendarConsumed: () -> Unit = {},
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
        if (adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT) {
            NavigationSuiteType.NavigationRail
        } else {
            defaultType
        }
    }

    NavigationSuiteScaffold(
        layoutType = navLayoutType,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationSuiteItems = {
            bottomNavItems.forEach { navItem ->
                val selected = currentDestination?.hierarchy?.any {
                    it.hasRoute(navItem.route::class)
                } == true

                item(
                    icon = { Icon(navItem.icon, contentDescription = navItem.label) },
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
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            // Onboarding
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            // Main screens
            composable<Route.Home> {
                HomeScreen(
                    onNavigateToQuran = { navController.navigate(Route.Quran) },
                    onNavigateToHadith = { navController.navigate(Route.HadithHome) },
                    onNavigateToDua = { navController.navigate(Route.DuaHome) },
                    onNavigateToTasbih = { navController.navigate(Route.TasbihHome) },
                    onNavigateToQibla = { navController.navigate(Route.Qibla) },
                    onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
                    onNavigateToFasting = { navController.navigate(Route.FastingHome) },
                    onNavigateToZakat = { navController.navigate(Route.ZakatCalculator) },
                    onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) },
                    onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) }
                )
            }

            composable<Route.Quran> {
                AdaptiveQuranScreen(
                    navController = navController,
                    onNavigateToSearch = { navController.navigate(Route.GlobalSearch) },
                    onNavigateToBookmarks = { navController.navigate(Route.QuranBookmarks) },
                    onNavigateToSettings = { navController.navigate(Route.SettingsQuran) },
                    onNavigateToSurahInfo = { surahNumber ->
                        navController.navigate(Route.SurahInfo(surahNumber))
                    },
                    onNavigateToKhatam = { navController.navigate(Route.KhatamList) },
                    onNavigateToKhatamDetail = { khatamId ->
                        navController.navigate(Route.KhatamDetail(khatamId))
                    }
                )
            }

            composable<Route.Tasbih> {
                TasbihScreen(
                    onNavigateToHistory = { navController.navigate(Route.TasbihHistory) },
                    onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.QiblaNav> {
                QiblaScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.More> {
                val context = androidx.compose.ui.platform.LocalContext.current
                AdaptiveMoreScreen(
                    navController = navController,
                    onRestartApp = { restartApp(context) },
                )
            }

            // Quran screens
            composable<Route.QuranReader> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QuranReader>()
                QuranReaderScreen(
                    surahNumber = args.surahNumber,
                    initialAyahNumber = args.ayahNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
                    onNavigateToTafseer = { surah, ayah ->
                        navController.navigate(Route.Tafseer(surah, ayah))
                    },
                    onNavigateToNextSurah = { nextSurah ->
                        navController.navigate(Route.QuranReader(nextSurah)) {
                            popUpTo<Route.QuranReader> { inclusive = true }
                        }
                    }
                )
            }

            composable<Route.Tafseer> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.Tafseer>()
                TafseerScreen(
                    surahNumber = args.surahNumber,
                    ayahNumber = args.ayahNumber,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SurahInfo> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.SurahInfo>()
                SurahInfoScreen(
                    surahNumber = args.surahNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onStartReading = {
                        navController.navigate(Route.QuranReader(args.surahNumber))
                    }
                )
            }

            composable<Route.QuranPage> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QuranPage>()
                QuranReaderScreen(
                    pageNumber = args.pageNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
                    onNavigateToTafseer = { surah, ayah ->
                        navController.navigate(Route.Tafseer(surah, ayah))
                    }
                )
            }

            composable<Route.QuranJuz> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QuranJuz>()
                QuranReaderScreen(
                    juzNumber = args.juzNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
                    onNavigateToTafseer = { surah, ayah ->
                        navController.navigate(Route.Tafseer(surah, ayah))
                    }
                )
            }

            composable<Route.QuranSearch> {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToSurah = { surah ->
                        navController.navigate(Route.QuranReader(surah))
                    },
                    onNavigateToHadith = { bookId, hadithId ->
                        navController.navigate(Route.HadithReader(hadithId))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            composable<Route.QuranBookmarks> {
                BookmarksScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToHadith = { bookId, hadithNumber ->
                        navController.navigate(Route.HadithReader(hadithNumber.toString()))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            // Hadith screens
            composable<Route.HadithHome> {
                AdaptiveHadithScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSearch = { navController.navigate(Route.HadithSearch) },
                    onNavigateToBookmarks = { navController.navigate(Route.HadithBookmarks) },
                )
            }

            composable<Route.HadithBook> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithBook>()
                HadithChaptersScreen(
                    bookId = args.bookId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChapter = { bookId, chapterId ->
                        navController.navigate(Route.HadithChapter(bookId, chapterId))
                    }
                )
            }

            composable<Route.HadithChapter> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithChapter>()
                HadithReaderScreen(
                    bookId = args.bookId,
                    chapterId = args.chapterId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.HadithReader> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithReader>()
                HadithReaderScreen(
                    bookId = "",
                    chapterId = args.hadithId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.HadithSearch> {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToSurah = { surah ->
                        navController.navigate(Route.QuranReader(surah))
                    },
                    onNavigateToHadith = { bookId, hadithId ->
                        navController.navigate(Route.HadithReader(hadithId))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            composable<Route.HadithBookmarks> {
                BookmarksScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToHadith = { bookId, hadithNumber ->
                        navController.navigate(Route.HadithReader(hadithNumber.toString()))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            // Dua screens
            composable<Route.DuaHome> {
                AdaptiveDuaScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) },
                )
            }

            composable<Route.DuaCategory> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.DuaCategory>()
                DuaCategoryScreen(
                    categoryId = args.categoryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            composable<Route.DuaReader> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.DuaReader>()
                DuaReaderScreen(
                    duaId = args.duaId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.DuaFavorites> {
                DuasCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Route.DuaCategory(categoryId))
                    },
                    onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) }
                )
            }

            composable<Route.DuaSearch> {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToSurah = { surah ->
                        navController.navigate(Route.QuranReader(surah))
                    },
                    onNavigateToHadith = { bookId, hadithId ->
                        navController.navigate(Route.HadithReader(hadithId))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            // Prayer screens
            composable<Route.PrayerTimes> {
                HomeScreen(
                    onNavigateToQuran = { navController.navigate(Route.Quran) },
                    onNavigateToHadith = { navController.navigate(Route.HadithHome) },
                    onNavigateToDua = { navController.navigate(Route.DuaHome) },
                    onNavigateToTasbih = { navController.navigate(Route.TasbihHome) },
                    onNavigateToQibla = { navController.navigate(Route.Qibla) },
                    onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
                    onNavigateToFasting = { navController.navigate(Route.FastingHome) },
                    onNavigateToZakat = { navController.navigate(Route.ZakatCalculator) },
                    onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) },
                    onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) }
                )
            }

            composable<Route.PrayerTracker> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.PrayerTracker>()
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) },
                    initialTab = route.initialTab
                )
            }

            composable<Route.PrayerStats> {
                PrayerStatsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Redirect QadaPrayers to PrayerTracker with Qada tab selected
            composable<Route.QadaPrayers> {
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) },
                    initialTab = 1
                )
            }

            composable<Route.MonthlyPrayerTimes> {
                MonthlyPrayerTimesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Fasting screens
            composable<Route.FastingHome> {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.FastingStats) }
                )
            }

            composable<Route.FastingTracker> {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.FastingStats) }
                )
            }

            composable<Route.FastingStats> {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { }
                )
            }

            // Tasbih screens
            composable<Route.TasbihHome> {
                TasbihScreen(
                    onNavigateToHistory = { navController.navigate(Route.TasbihHistory) },
                    onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihCounter> { backStackEntry ->
                backStackEntry.toRoute<Route.TasbihCounter>()
                TasbihScreen(
                    onNavigateToHistory = { navController.navigate(Route.TasbihStats) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihPresets> {
                TasbihScreen(
                    onNavigateToHistory = { navController.navigate(Route.TasbihStats) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihStats> {
                TasbihScreen(
                    onNavigateToHistory = { },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihHistory> {
                com.arshadshah.nimaz.presentation.screens.tasbih.TasbihHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.TasbihAddPreset> {
                com.arshadshah.nimaz.presentation.screens.tasbih.AddPresetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Zakat screens
            composable<Route.ZakatCalculator> {
                ZakatCalculatorScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.ZakatHistory) }
                )
            }

            composable<Route.ZakatHistory> {
                ZakatHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCalculator = { navController.navigate(Route.ZakatCalculator) }
                )
            }

            // Qibla
            composable<Route.Qibla> {
                QiblaScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Islamic Calendar
            composable<Route.IslamicCalendar> {
                IslamicCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.IslamicMonth> { backStackEntry ->
                backStackEntry.toRoute<Route.IslamicMonth>()
                IslamicCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable<Route.Settings> {
                val context = androidx.compose.ui.platform.LocalContext.current
                AdaptiveSettingsScreen(
                    navController = navController,
                    onRestartApp = { restartApp(context) },
                )
            }

            composable<Route.SettingsPrayerCalculation> {
                PrayerSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) }
                )
            }

            composable<Route.SettingsNotifications> {
                NotificationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsAppearance> {
                AppearanceSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsLanguage> {
                LanguageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsLocation> {
                LocationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsQuran> {
                QuranSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSelectReciter = { navController.navigate(Route.SelectReciter) }
                )
            }

            composable<Route.SelectReciter> {
                SelectReciterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsWidgets> {
                WidgetsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsAbout> {
                val context = androidx.compose.ui.platform.LocalContext.current
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacyPolicy = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nimaz.arshadshah.com/privacy-policy"))
                        context.startActivity(intent)
                    },
                    onNavigateToTerms = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nimaz.arshadshah.com/terms-of-service"))
                        context.startActivity(intent)
                    },
                    onNavigateToLicenses = {
                        navController.navigate(Route.Licenses)
                    },
                    onRateApp = {
                        val activity = context as? Activity
                        if (activity != null) {
                            val manager = ReviewManagerFactory.create(context)
                            manager.requestReviewFlow().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    manager.launchReviewFlow(activity, task.result)
                                } else {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    onShareApp = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out Nimaz - Prayer Times App: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Nimaz"))
                    },
                    onContactUs = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:arshad@arshadshah.com")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Nimaz App Feedback")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            composable<Route.Licenses> {
                LicensesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { hashCode ->
                        navController.navigate(Route.LicenseDetail(hashCode))
                    }
                )
            }

            composable<Route.LicenseDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.LicenseDetail>()
                LicenseDetailScreen(
                    libraryHashCode = args.libraryHashCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Asma ul Husna screens
            composable<Route.AsmaUlHusnaList> {
                AdaptiveAsmaUlHusnaScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Route.AsmaUlHusnaDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.AsmaUlHusnaDetail>()
                AsmaUlHusnaDetailScreen(
                    nameId = args.nameId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Asma un Nabi screens
            composable<Route.AsmaUnNabiList> {
                AdaptiveAsmaUnNabiScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Route.AsmaUnNabiDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.AsmaUnNabiDetail>()
                AsmaUnNabiDetailScreen(
                    nameId = args.nameId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Prophets screens
            composable<Route.ProphetsList> {
                AdaptiveProphetsScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Route.ProphetDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.ProphetDetail>()
                ProphetDetailScreen(
                    prophetId = args.prophetId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Khatam screens
            composable<Route.KhatamList> {
                AdaptiveKhatamScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(Route.KhatamCreate) },
                )
            }

            composable<Route.KhatamDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.KhatamDetail>()
                KhatamDetailScreen(
                    khatamId = args.khatamId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRead = { surahNumber, ayahNumber ->
                        navController.navigate(Route.QuranReader(surahNumber, ayahNumber))
                    }
                )
            }

            composable<Route.KhatamCreate> {
                KhatamCreateScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsHelp> {
                com.arshadshah.nimaz.presentation.screens.help.HelpSupportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsSync> {
                com.arshadshah.nimaz.presentation.screens.settings.SyncScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Bookmarks
            composable<Route.AllBookmarks> {
                BookmarksScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToHadith = { bookId, hadithNumber ->
                        navController.navigate(Route.HadithReader(hadithNumber.toString()))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            // Global Search
            composable<Route.GlobalSearch> {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuranAyah = { surah, ayah ->
                        navController.navigate(Route.QuranReader(surah, ayah))
                    },
                    onNavigateToSurah = { surah ->
                        navController.navigate(Route.QuranReader(surah))
                    },
                    onNavigateToHadith = { bookId, hadithId ->
                        navController.navigate(Route.HadithReader(hadithId))
                    },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }
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
    BottomNavItem(Route.Quran, "Quran", Icons.Default.MenuBook),
    BottomNavItem(Route.Tasbih, "Tasbih", Icons.Default.TouchApp),
    BottomNavItem(Route.QiblaNav, "Qibla", Icons.Default.Explore),
    BottomNavItem(Route.More, "More", Icons.Default.MoreHoriz)
)

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    if (context is Activity) {
        context.finish()
    }
    exitProcess(0)
}
