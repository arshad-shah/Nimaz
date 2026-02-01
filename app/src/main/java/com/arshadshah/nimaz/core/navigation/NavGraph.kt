package com.arshadshah.nimaz.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.arshadshah.nimaz.presentation.screens.dua.DuaCategoryScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaReaderScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuasCollectionScreen
import com.arshadshah.nimaz.presentation.screens.fasting.FastTrackerScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithChaptersScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithCollectionScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithReaderScreen
import com.arshadshah.nimaz.presentation.screens.home.HomeScreen
import com.arshadshah.nimaz.presentation.screens.more.MoreMenuScreen
import com.arshadshah.nimaz.presentation.screens.onboarding.OnboardingScreen
import com.arshadshah.nimaz.presentation.screens.prayer.MonthlyPrayerTimesScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerStatsScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerTrackerScreen
import com.arshadshah.nimaz.presentation.screens.qibla.QiblaScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranHomeScreen
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
import com.arshadshah.nimaz.presentation.screens.settings.SettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.WidgetsScreen
import com.arshadshah.nimaz.presentation.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatCalculatorScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatHistoryScreen
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
            // Only apply bottom padding (for nav bar). Each screen's own
            // Scaffold/TopAppBar handles the top status bar inset.
            modifier = Modifier.padding(
                PaddingValues(bottom = innerPadding.calculateBottomPadding())
            )
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
                QuranHomeScreen(
                    onNavigateToSearch = { navController.navigate(Route.GlobalSearch) },
                    onNavigateToSurah = { surahNumber ->
                        navController.navigate(Route.QuranReader(surahNumber))
                    },
                    onNavigateToJuz = { juzNumber ->
                        navController.navigate(Route.QuranJuz(juzNumber))
                    },
                    onNavigateToPage = { pageNumber ->
                        navController.navigate(Route.QuranPage(pageNumber))
                    },
                    onNavigateToBookmarks = { navController.navigate(Route.QuranBookmarks) },
                    onNavigateToSettings = { navController.navigate(Route.SettingsQuran) },
                    onNavigateToSurahInfo = { surahNumber ->
                        navController.navigate(Route.SurahInfo(surahNumber))
                    },
                    onNavigateToQuranAyah = { surahNumber, ayahNumber ->
                        navController.navigate(Route.QuranReader(surahNumber, ayahNumber))
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
                MoreMenuScreen(
                    onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
                    onNavigateToLocation = { navController.navigate(Route.SettingsLocation) },
                    onNavigateToNotifications = { navController.navigate(Route.SettingsNotifications) },
                    onNavigateToAppearance = { navController.navigate(Route.SettingsAppearance) },
                    onNavigateToLanguage = { navController.navigate(Route.SettingsLanguage) },
                    onNavigateToWidgets = { navController.navigate(Route.SettingsWidgets) },
                    onNavigateToAbout = { navController.navigate(Route.SettingsAbout) },
                    onNavigateToHelp = { navController.navigate(Route.SettingsHelp) },
                    onNavigateToHadith = { navController.navigate(Route.HadithHome) },
                    onNavigateToFasting = { navController.navigate(Route.FastingHome) },
                    onNavigateToZakat = { navController.navigate(Route.ZakatCalculator) },
                    onNavigateToDuas = { navController.navigate(Route.DuaHome) },
                    onNavigateToCalculationMethod = { navController.navigate(Route.SettingsPrayerCalculation) },
                    onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
                    onNavigateToMonthlyPrayerTimes = { navController.navigate(Route.MonthlyPrayerTimes) },
                    onShareApp = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out Nimaz - Prayer Times App: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Nimaz"))
                    },
                    onRateApp = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"))
                        context.startActivity(intent)
                    },
                    onDeleteAllData = {
                        // TODO: Implement delete all data via SettingsViewModel
                    }
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
                DuasCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Route.DuaCategory(categoryId))
                    },
                    onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) }
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
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nimazapp.com/privacy-policy"))
                        context.startActivity(intent)
                    },
                    onNavigateToTerms = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nimazapp.com/terms-of-service"))
                        context.startActivity(intent)
                    },
                    onNavigateToLicenses = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nimazapp.com/licenses"))
                        context.startActivity(intent)
                    },
                    onRateApp = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"))
                        context.startActivity(intent)
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
                            data = android.net.Uri.parse("mailto:support@nimazapp.com")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Nimaz App Feedback")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            composable<Route.SettingsHelp> {
                com.arshadshah.nimaz.presentation.screens.help.HelpSupportScreen(
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
