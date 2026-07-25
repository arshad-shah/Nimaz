package com.arshadshah.nimaz.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContentScope
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.ShareAppSheet
import com.arshadshah.nimaz.presentation.screens.about.AboutScreen
import com.arshadshah.nimaz.presentation.screens.about.LicenseDetailScreen
import com.arshadshah.nimaz.presentation.screens.about.LicensesScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveAsmaUlHusnaScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveAsmaUnNabiScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveDuaScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveHadithScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveKhatamScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveMoreScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveProphetsScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveQuranScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveSettingsScreen
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaDetailScreen
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiDetailScreen
import com.arshadshah.nimaz.presentation.screens.bookmarks.BookmarksScreen
import com.arshadshah.nimaz.presentation.screens.calendar.IslamicCalendarScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaCategoryScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaReaderScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaSettingsScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuasCollectionScreen
import com.arshadshah.nimaz.presentation.screens.fasting.FastTrackerScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithChaptersScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithReaderScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithSettingsScreen
import com.arshadshah.nimaz.presentation.screens.home.HomeScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamDetailScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamFormScreen
import com.arshadshah.nimaz.presentation.screens.onboarding.OnboardingScreen
import com.arshadshah.nimaz.presentation.screens.prayer.MonthlyPrayerTimesScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerStatsScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerTimesScreen
import com.arshadshah.nimaz.presentation.screens.prayer.PrayerTrackerScreen
import com.arshadshah.nimaz.presentation.screens.prophets.ProphetDetailScreen
import com.arshadshah.nimaz.presentation.screens.qaida.QaidaHomeScreen
import com.arshadshah.nimaz.presentation.screens.qaida.QaidaLettersScreen
import com.arshadshah.nimaz.presentation.screens.qaida.QaidaReaderScreen
import com.arshadshah.nimaz.presentation.screens.qibla.QiblaScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranReaderScreen
import com.arshadshah.nimaz.presentation.screens.quran.SelectReciterScreen
import com.arshadshah.nimaz.presentation.screens.quran.SurahInfoScreen
import com.arshadshah.nimaz.presentation.screens.quran.TafseerChaptersScreen
import com.arshadshah.nimaz.presentation.screens.quran.TafseerScreen
import com.arshadshah.nimaz.presentation.screens.search.SearchScreen
import com.arshadshah.nimaz.presentation.screens.settings.AppearanceSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.LanguageScreen
import com.arshadshah.nimaz.presentation.screens.settings.LocationScreen
import com.arshadshah.nimaz.presentation.screens.settings.NotificationSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.WorshipRemindersScreen
import com.arshadshah.nimaz.presentation.screens.settings.PrayerSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.QuranSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.SearchSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.WidgetsScreen
import com.arshadshah.nimaz.presentation.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatCalculatorScreen
import com.arshadshah.nimaz.presentation.screens.zakat.ZakatHistoryScreen
import com.arshadshah.nimaz.presentation.theme.isTablet
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingViewModel
import com.arshadshah.nimaz.presentation.viewmodel.SearchFilter
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

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
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            // Onboarding
            taggedComposable<Route.Onboarding>(ScreenTags.Onboarding) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            // Main screens
            taggedComposable<Route.Home>(ScreenTags.Home) {
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
                    onNavigateToPrayerSettings = { navController.navigate(Route.SettingsPrayerCalculation) },
                    onNavigateToPrayerTimes = { navController.navigate(Route.PrayerTimes) },
                    onOpenHadith = { hadithId -> navController.navigate(Route.HadithReader(hadithId)) },
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

            taggedComposable<Route.Quran>(ScreenTags.Quran) {
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

            taggedComposable<Route.Tasbih>(ScreenTags.Tasbih) {
                TasbihScreen(
                    onNavigateToHistory = { navController.navigate(Route.TasbihHistory) },
                    onNavigateToChooseDhikr = { navController.navigate(Route.TasbihPresets) },
                    onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset) },
                    onNavigateToSettings = { navController.navigate(Route.Settings) }
                )
            }

            taggedComposable<Route.QiblaNav>(ScreenTags.QiblaNav) {
                QiblaScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Qaida (children's Arabic reader) screens
            taggedComposable<Route.QaidaHome>(ScreenTags.QaidaHome) {
                QaidaHomeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenLesson = { lessonId -> navController.navigate(Route.QaidaReader(lessonId)) },
                    onOpenLetters = { navController.navigate(Route.QaidaLetters) },
                )
            }

            taggedComposable<Route.QaidaReader>(ScreenTags.QaidaReader) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.QaidaReader>()
                QaidaReaderScreen(
                    lessonId = args.lessonId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            taggedComposable<Route.QaidaLetters>(ScreenTags.QaidaLetters) {
                QaidaLettersScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            taggedComposable<Route.More>(ScreenTags.More) {
                val context = androidx.compose.ui.platform.LocalContext.current
                AdaptiveMoreScreen(
                    navController = navController,
                    onRestartApp = { restartApp(context) },
                )
            }

            // Quran screens
            taggedComposable<Route.QuranReader>(ScreenTags.QuranReader) { backStackEntry ->
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

            taggedComposable<Route.TafseerChapters>(ScreenTags.TafseerChapters) {
                TafseerChaptersScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenTafseer = { surah, ayah ->
                        navController.navigate(
                            Route.Tafseer(
                                surahNumber = surah,
                                ayahNumber = ayah
                            )
                        )
                    }
                )
            }

            taggedComposable<Route.Tafseer>(ScreenTags.Tafseer) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.Tafseer>()
                TafseerScreen(
                    surahNumber = args.surahNumber,
                    ayahNumber = args.ayahNumber,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.SurahInfo>(ScreenTags.SurahInfo) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.SurahInfo>()
                SurahInfoScreen(
                    surahNumber = args.surahNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onStartReading = {
                        navController.navigate(Route.QuranReader(args.surahNumber))
                    }
                )
            }

            taggedComposable<Route.QuranPage>(ScreenTags.QuranPage) { backStackEntry ->
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

            taggedComposable<Route.QuranJuz>(ScreenTags.QuranJuz) { backStackEntry ->
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

            taggedComposable<Route.QuranSearch>(ScreenTags.QuranSearch) {
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

            taggedComposable<Route.QuranBookmarks>(ScreenTags.QuranBookmarks) {
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
            taggedComposable<Route.HadithHome>(ScreenTags.HadithHome) {
                AdaptiveHadithScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSearch = { navController.navigate(Route.HadithSearch) },
                    onNavigateToBookmarks = { navController.navigate(Route.HadithBookmarks) },
                )
            }

            taggedComposable<Route.HadithBook>(ScreenTags.HadithBook) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithBook>()
                HadithChaptersScreen(
                    bookId = args.bookId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChapter = { bookId, chapterId ->
                        navController.navigate(Route.HadithChapter(bookId, chapterId))
                    }
                )
            }

            taggedComposable<Route.HadithChapter>(ScreenTags.HadithChapter) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithChapter>()
                HadithReaderScreen(
                    bookId = args.bookId,
                    chapterId = args.chapterId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Route.HadithSettings) }
                )
            }

            taggedComposable<Route.HadithReader>(ScreenTags.HadithReader) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HadithReader>()
                HadithReaderScreen(
                    bookId = "",
                    chapterId = args.hadithId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Route.HadithSettings) }
                )
            }

            taggedComposable<Route.HadithSettings>(ScreenTags.HadithSettings) {
                HadithSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.HadithSearch>(ScreenTags.HadithSearch) {
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

            taggedComposable<Route.HadithBookmarks>(ScreenTags.HadithBookmarks) {
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
            taggedComposable<Route.DuaHome>(ScreenTags.DuaHome) {
                AdaptiveDuaScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) },
                    onNavigateToSearch = { navController.navigate(Route.DuaSearch) },
                )
            }

            taggedComposable<Route.DuaCategory>(ScreenTags.DuaCategory) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.DuaCategory>()
                DuaCategoryScreen(
                    categoryId = args.categoryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDua = { duaId ->
                        navController.navigate(Route.DuaReader(duaId))
                    }
                )
            }

            taggedComposable<Route.DuaReader>(ScreenTags.DuaReader) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.DuaReader>()
                DuaReaderScreen(
                    duaId = args.duaId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Route.DuaSettings) }
                )
            }

            taggedComposable<Route.DuaSettings>(ScreenTags.DuaSettings) {
                DuaSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.DuaFavorites>(ScreenTags.DuaFavorites) {
                DuasCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Route.DuaCategory(categoryId))
                    },
                    onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) },
                    onNavigateToSearch = { navController.navigate(Route.DuaSearch) }
                )
            }

            taggedComposable<Route.DuaSearch>(ScreenTags.DuaSearch) {
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
                    },
                    initialFilter = SearchFilter.DUA
                )
            }

            // Prayer screens
            taggedComposable<Route.PrayerTimes>(ScreenTags.PrayerTimes) {
                PrayerTimesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Route.SettingsPrayerCalculation) }
                )
            }

            taggedComposable<Route.PrayerTracker>(ScreenTags.PrayerTracker) { backStackEntry ->
                val route = backStackEntry.toRoute<Route.PrayerTracker>()
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) },
                    initialTab = route.initialTab
                )
            }

            taggedComposable<Route.PrayerStats>(ScreenTags.PrayerStats) {
                PrayerStatsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Redirect QadaPrayers to PrayerTracker with Qada tab selected
            taggedComposable<Route.QadaPrayers>(ScreenTags.QadaPrayers) {
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) },
                    initialTab = 1
                )
            }

            taggedComposable<Route.MonthlyPrayerTimes>(ScreenTags.MonthlyPrayerTimes) {
                MonthlyPrayerTimesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Fasting screens
            taggedComposable<Route.FastingHome>(ScreenTags.FastingHome) {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.FastingStats) }
                )
            }

            taggedComposable<Route.FastingTracker>(ScreenTags.FastingTracker) {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.FastingStats) }
                )
            }

            taggedComposable<Route.FastingStats>(ScreenTags.FastingStats) {
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { }
                )
            }

            // Tasbih screens
            taggedComposable<Route.TasbihHome>(ScreenTags.TasbihHome) {
                TasbihScreen(
                    onNavigateToHistory = { navController.navigate(Route.TasbihHistory) },
                    onNavigateToChooseDhikr = { navController.navigate(Route.TasbihPresets) },
                    onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset) },
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
                    onNavigateToAddPreset = { navController.navigate(Route.TasbihAddPreset) }
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

            taggedComposable<Route.TasbihAddPreset>(ScreenTags.TasbihAddPreset) {
                com.arshadshah.nimaz.presentation.screens.tasbih.AddPresetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Zakat screens
            taggedComposable<Route.ZakatCalculator>(ScreenTags.ZakatCalculator) {
                ZakatCalculatorScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Route.ZakatHistory) }
                )
            }

            taggedComposable<Route.ZakatHistory>(ScreenTags.ZakatHistory) {
                ZakatHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCalculator = { navController.navigate(Route.ZakatCalculator) }
                )
            }

            // Qibla
            taggedComposable<Route.Qibla>(ScreenTags.Qibla) {
                QiblaScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Islamic Calendar
            taggedComposable<Route.IslamicCalendar>(ScreenTags.IslamicCalendar) {
                IslamicCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.IslamicMonth>(ScreenTags.IslamicMonth) { backStackEntry ->
                backStackEntry.toRoute<Route.IslamicMonth>()
                IslamicCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings
            taggedComposable<Route.Settings>(ScreenTags.Settings) {
                val context = androidx.compose.ui.platform.LocalContext.current
                AdaptiveSettingsScreen(
                    navController = navController,
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
                    onNavigateToWorshipReminders = { navController.navigate(Route.SettingsWorshipReminders) }
                )
            }

            composable<Route.SettingsWorshipReminders> {
                WorshipRemindersScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
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
                    onNavigateToSelectReciter = { navController.navigate(Route.SelectReciter) }
                )
            }

            taggedComposable<Route.SelectReciter>(ScreenTags.SelectReciter) {
                SelectReciterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.SettingsWidgets>(ScreenTags.SettingsWidgets) {
                WidgetsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.SettingsAbout>(ScreenTags.SettingsAbout) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val shareScope = rememberCoroutineScope()
                var showShareSheet by remember { mutableStateOf(false) }
                if (showShareSheet) {
                    ShareAppSheet(
                        onDismiss = { showShareSheet = false },
                        onShareLink = {
                            showShareSheet = false
                            shareScope.launch {
                                ContentShareManager.shareBranded(
                                    context,
                                    Shareables.appInvite(context),
                                    includeText = true,
                                )
                            }
                        },
                    )
                }
                val contactEmail =
                    stringResource(com.arshadshah.nimaz.R.string.contact_email)
                val contactSubject =
                    stringResource(com.arshadshah.nimaz.R.string.contact_email_subject)
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacyPolicy = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://nimaz.arshadshah.com/privacy".toUri()
                        )
                        context.startActivity(intent)
                    },
                    onNavigateToTerms = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://nimaz.arshadshah.com/terms".toUri()
                        )
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
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://play.google.com/store/apps/details?id=com.arshadshah.nimaz".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    onShareApp = { showShareSheet = true },
                    onContactUs = {
                        ContentShareManager.sendEmail(
                            context,
                            address = contactEmail,
                            subject = contactSubject,
                        )
                    }
                )
            }

            taggedComposable<Route.Licenses>(ScreenTags.Licenses) {
                LicensesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { hashCode ->
                        navController.navigate(Route.LicenseDetail(hashCode))
                    }
                )
            }

            taggedComposable<Route.LicenseDetail>(ScreenTags.LicenseDetail) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.LicenseDetail>()
                LicenseDetailScreen(
                    libraryHashCode = args.libraryHashCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Asma ul Husna screens
            taggedComposable<Route.AsmaUlHusnaList>(ScreenTags.AsmaUlHusnaList) {
                AdaptiveAsmaUlHusnaScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            taggedComposable<Route.AsmaUlHusnaDetail>(ScreenTags.AsmaUlHusnaDetail) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.AsmaUlHusnaDetail>()
                AsmaUlHusnaDetailScreen(
                    nameId = args.nameId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Asma un Nabi screens
            taggedComposable<Route.AsmaUnNabiList>(ScreenTags.AsmaUnNabiList) {
                AdaptiveAsmaUnNabiScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            taggedComposable<Route.AsmaUnNabiDetail>(ScreenTags.AsmaUnNabiDetail) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.AsmaUnNabiDetail>()
                AsmaUnNabiDetailScreen(
                    nameId = args.nameId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Prophets screens
            taggedComposable<Route.ProphetsList>(ScreenTags.ProphetsList) {
                AdaptiveProphetsScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            taggedComposable<Route.ProphetDetail>(ScreenTags.ProphetDetail) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.ProphetDetail>()
                ProphetDetailScreen(
                    prophetId = args.prophetId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Khatam screens
            taggedComposable<Route.KhatamList>(ScreenTags.KhatamList) {
                AdaptiveKhatamScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(Route.KhatamCreate) },
                )
            }

            taggedComposable<Route.KhatamDetail>(ScreenTags.KhatamDetail) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.KhatamDetail>()
                KhatamDetailScreen(
                    khatamId = args.khatamId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRead = { surahNumber, ayahNumber ->
                        navController.navigate(Route.QuranReader(surahNumber, ayahNumber))
                    },
                    onNavigateToEdit = { khatamId ->
                        navController.navigate(Route.KhatamEdit(khatamId))
                    }
                )
            }

            // Create and edit share one screen; a null id means "create".
            taggedComposable<Route.KhatamCreate>(ScreenTags.KhatamCreate) {
                KhatamFormScreen(
                    khatamId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.KhatamEdit>(ScreenTags.KhatamEdit) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.KhatamEdit>()
                KhatamFormScreen(
                    khatamId = args.khatamId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            taggedComposable<Route.SettingsHelp>(ScreenTags.SettingsHelp) {
                val context = androidx.compose.ui.platform.LocalContext.current
                // Unified support inbox — same address the About screen contacts.
                val supportEmail =
                    androidx.compose.ui.res.stringResource(com.arshadshah.nimaz.R.string.contact_email)
                val supportSubject =
                    androidx.compose.ui.res.stringResource(com.arshadshah.nimaz.R.string.nimaz_support_request)
                com.arshadshah.nimaz.presentation.screens.help.HelpScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTopic = { topicId ->
                        navController.navigate(
                            Route.HelpTopicDetail(
                                topicId
                            )
                        )
                    },
                    onContact = {
                        ContentShareManager.sendEmail(
                            context,
                            address = supportEmail,
                            subject = supportSubject,
                        )
                    }
                )
            }

            taggedComposable<Route.HelpTopicDetail>(ScreenTags.HelpTopicDetail) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HelpTopicDetail>()
                com.arshadshah.nimaz.presentation.screens.help.HelpTopicDetailScreen(
                    topicId = args.topicId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenGuide = { guideId -> navController.navigate(Route.HelpGuide(guideId)) }
                )
            }

            taggedComposable<Route.HelpGuide>(ScreenTags.HelpGuide) { backStackEntry ->
                val args = backStackEntry.toRoute<Route.HelpGuide>()
                com.arshadshah.nimaz.presentation.screens.help.HelpGuideScreen(
                    guideId = args.guideId,
                    onNavigateBack = { navController.popBackStack() },
                    onDeepLink = { key ->
                        helpDeepLinkRoute(key)?.let { route ->
                            navController.navigate(route)
                        }
                    }
                )
            }

            taggedComposable<Route.SettingsSync>(ScreenTags.SettingsSync) {
                com.arshadshah.nimaz.presentation.screens.settings.SyncScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Bookmarks
            taggedComposable<Route.AllBookmarks>(ScreenTags.AllBookmarks) {
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
            taggedComposable<Route.GlobalSearch>(ScreenTags.GlobalSearch) {
                SearchScreen(
                    enableAsk = true,
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
                    },
                    onNavigateToSearchSettings = { navController.navigate(Route.SearchSettings) },
                    onNavigateToProof = { route -> navController.navigate(route) }
                )
            }

            taggedComposable<Route.SearchSettings>(ScreenTags.SearchSettings) {
                SearchSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Like [composable], but wraps the destination content in a full-size [Box] carrying a
 * stable [testTag] ([ScreenTags]). This gives the instrumented UI tests a locale- and
 * copy-independent way to assert which screen is currently shown. The wrapper is
 * otherwise transparent: it forwards the [AnimatedContentScope] receiver and the
 * [NavBackStackEntry] so existing destination bodies (including `toRoute()` arg
 * extraction and any shared-element usage) behave exactly as before.
 */
private inline fun <reified T : Any> NavGraphBuilder.taggedComposable(
    tag: String,
    crossinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        val scope = this
        Box(modifier = Modifier
            .fillMaxSize()
            .testTag(tag)) {
            scope.content(entry)
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

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    if (context is Activity) {
        context.finish()
    }
    exitProcess(0)
}
