package com.arshadshah.nimaz.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arshadshah.nimaz.presentation.screens.about.AboutScreen
import com.arshadshah.nimaz.presentation.screens.bookmarks.BookmarksScreen
import com.arshadshah.nimaz.presentation.screens.calendar.IslamicCalendarScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaReaderScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuasCollectionScreen
import com.arshadshah.nimaz.presentation.screens.fasting.FastTrackerScreen
import com.arshadshah.nimaz.presentation.screens.fasting.MakeupFastsScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithCollectionScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithReaderScreen
import com.arshadshah.nimaz.presentation.screens.home.HomeScreen
import com.arshadshah.nimaz.presentation.screens.more.MoreMenuScreen
import com.arshadshah.nimaz.presentation.screens.onboarding.OnboardingScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerStatsScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerTrackerScreen
import com.arshadshah.nimaz.presentation.screens.qibla.QiblaScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranHomeScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranReaderScreen
import com.arshadshah.nimaz.presentation.screens.quran.SurahInfoScreen
import com.arshadshah.nimaz.presentation.screens.search.SearchScreen
import com.arshadshah.nimaz.presentation.screens.settings.AppearanceSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.LanguageScreen
import com.arshadshah.nimaz.presentation.screens.settings.LocationScreen
import com.arshadshah.nimaz.presentation.screens.settings.NotificationSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.PrayerSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.QuranSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.SettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.WidgetsScreen
import com.arshadshah.nimaz.presentation.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatCalculatorScreen
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Onboarding ViewModel to check status
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.state.collectAsState()

    // Track if we've determined the start destination
    var startDestinationDetermined by remember { mutableStateOf(false) }

    // Check if we should show bottom navigation
    val showBottomNav = currentDestination?.hierarchy?.any { dest ->
        dest.hasRoute<Route.Home>() ||
        dest.hasRoute<Route.Quran>() ||
        dest.hasRoute<Route.Prayer>() ||
        dest.hasRoute<Route.More>()
    } == true

    // Show loading while determining start destination
    if (onboardingState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NimazColors.Primary)
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

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
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
                    onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) },
                    onNavigateToSearch = { navController.navigate(Route.GlobalSearch) },
                    onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) },
                    onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) }
                )
            }

            composable<Route.Quran> {
                QuranHomeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurah = { surahNumber ->
                        navController.navigate(Route.QuranReader(surahNumber))
                    },
                    onNavigateToBookmarks = { navController.navigate(Route.QuranBookmarks) }
                )
            }

            composable<Route.Prayer> {
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) }
                )
            }

            composable<Route.More> {
                MoreMenuScreen(
                    onNavigateToSettings = { navController.navigate(Route.Settings) },
                    onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) },
                    onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
                    onNavigateToLocation = { navController.navigate(Route.SettingsLocation) },
                    onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) },
                    onNavigateToAppearance = { navController.navigate(Route.SettingsAppearance) },
                    onNavigateToLanguage = { navController.navigate(Route.SettingsLanguage) },
                    onNavigateToWidgets = { navController.navigate(Route.SettingsWidgets) },
                    onNavigateToAbout = { navController.navigate(Route.SettingsAbout) },
                    onNavigateToHelp = { navController.navigate(Route.SettingsHelp) },
                    onShareApp = { /* Implement share intent */ },
                    onRateApp = { /* Implement rate intent */ }
                )
            }

            // Quran screens
            composable<Route.QuranReader> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QuranReader>()
                QuranReaderScreen(
                    surahNumber = args.surahNumber,
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
                    },
                    onPlayAudio = { /* TODO: Implement audio playback */ }
                )
            }

            composable<Route.QuranPage> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QuranPage>()
                // TODO: Implement page-based navigation
                QuranReaderScreen(
                    surahNumber = 1,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.QuranJuz> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QuranJuz>()
                // TODO: Implement juz-based navigation
                QuranReaderScreen(
                    surahNumber = 1,
                    onNavigateBack = { navController.popBackStack() }
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
                HadithCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBook = { bookId ->
                        navController.navigate(Route.HadithBook(bookId))
                    },
                    onNavigateToSearch = { navController.navigate(Route.HadithSearch) },
                    onNavigateToBookmarks = { navController.navigate(Route.HadithBookmarks) }
                )
            }

            composable<Route.HadithBook> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithBook>()
                HadithCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBook = { bookId ->
                        navController.navigate(Route.HadithBook(bookId))
                    },
                    onNavigateToSearch = { navController.navigate(Route.HadithSearch) },
                    onNavigateToBookmarks = { navController.navigate(Route.HadithBookmarks) }
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
                DuasCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Route.DuaCategory(categoryId))
                    },
                    onNavigateToFavorites = { navController.navigate(Route.DuaFavorites) },
                    onNavigateToSearch = { navController.navigate(Route.DuaSearch) }
                )
            }

            composable<Route.DuaCategory> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.DuaCategory>()
                DuasCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Route.DuaCategory(categoryId))
                    },
                    onNavigateToFavorites = { navController.navigate(Route.DuaFavorites) },
                    onNavigateToSearch = { navController.navigate(Route.DuaSearch) }
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
                    onNavigateToFavorites = { },
                    onNavigateToSearch = { navController.navigate(Route.DuaSearch) }
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
                    onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) },
                    onNavigateToSearch = { navController.navigate(Route.GlobalSearch) },
                    onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) },
                    onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) }
                )
            }

            composable<Route.PrayerTracker> {
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) }
                )
            }

            composable<Route.PrayerStats> {
                PrayerStatsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.QadaPrayers> {
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) }
                )
            }

            // Fasting screens
            composable<Route.FastingHome> {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
                    onNavigateToHistory = { navController.navigate(Route.FastingStats) }
                )
            }

            composable<Route.FastingTracker> {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
                    onNavigateToHistory = { navController.navigate(Route.FastingStats) }
                )
            }

            composable<Route.FastingStats> {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
                    onNavigateToHistory = { }
                )
            }

            composable<Route.MakeupFasts> {
                MakeupFastsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Tasbih screens
            composable<Route.TasbihHome> {
                TasbihScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.TasbihStats) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihCounter> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.TasbihCounter>()
                TasbihScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.TasbihStats) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihPresets> {
                TasbihScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.TasbihStats) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.TasbihStats> {
                TasbihScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
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
                ZakatCalculatorScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { }
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
                val args = backStackEntry.toRoute<Route.IslamicMonth>()
                IslamicCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable<Route.Settings> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) },
                    onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) },
                    onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
                    onNavigateToAppearance = { navController.navigate(Route.SettingsAppearance) },
                    onNavigateToLocation = { navController.navigate(Route.SettingsLocation) },
                    onNavigateToLanguage = { navController.navigate(Route.SettingsLanguage) },
                    onNavigateToWidgets = { navController.navigate(Route.SettingsWidgets) }
                )
            }

            composable<Route.SettingsPrayerCalculation> {
                PrayerSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsWidgets> {
                WidgetsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.SettingsAbout> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacyPolicy = { /* TODO: Open privacy policy */ },
                    onNavigateToTerms = { /* TODO: Open terms */ },
                    onNavigateToLicenses = { /* TODO: Show licenses */ },
                    onRateApp = { /* TODO: Open Play Store */ },
                    onShareApp = { /* TODO: Share intent */ },
                    onContactUs = { /* TODO: Email intent */ }
                )
            }

            composable<Route.SettingsHelp> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacyPolicy = { },
                    onNavigateToTerms = { },
                    onNavigateToLicenses = { },
                    onRateApp = { },
                    onShareApp = { },
                    onContactUs = { }
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
    BottomNavItem(Route.Prayer, "Prayer", Icons.Default.Mosque),
    BottomNavItem(Route.More, "More", Icons.Default.MoreHoriz)
)
