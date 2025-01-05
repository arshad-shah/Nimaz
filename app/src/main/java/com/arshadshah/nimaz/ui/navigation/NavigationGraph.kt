package com.arshadshah.nimaz.ui.navigation

import LicensesScreen
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.ABOUT_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CALENDER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.DEBUG_MODE
import com.arshadshah.nimaz.constants.AppConstants.HADITH_CHAPTERS_LIST_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.HADITH_LIST_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.HADITH_SHELF_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.LICENCES_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.MY_QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.NAMESOFALLAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TRACKER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_LIST_SCREEN
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.WEB_VIEW_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.screens.Dashboard
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.hadith.BookShelf
import com.arshadshah.nimaz.ui.screens.hadith.HadithChaptersList
import com.arshadshah.nimaz.ui.screens.hadith.HadithList
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage
import com.arshadshah.nimaz.ui.screens.more.MoreScreen
import com.arshadshah.nimaz.ui.screens.more.NamesOfAllah
import com.arshadshah.nimaz.ui.screens.more.QiblaScreen
import com.arshadshah.nimaz.ui.screens.more.ShahadahScreen
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen
import com.arshadshah.nimaz.ui.screens.settings.About
import com.arshadshah.nimaz.ui.screens.settings.DebugScreen
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen
import com.arshadshah.nimaz.ui.screens.settings.WebViewScreen
import com.arshadshah.nimaz.ui.screens.tasbih.Categories
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList
import com.arshadshah.nimaz.ui.screens.tasbih.ListOfTasbih
import com.arshadshah.nimaz.ui.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.ui.screens.tracker.CalenderScreen
import com.arshadshah.nimaz.ui.screens.tracker.PrayerTracker
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(
    navController: NavController,
    context: MainActivity,
    isFirstInstall: Boolean,
    settingsViewModel: SettingsViewModel,
) {

    val startDestination = if (isFirstInstall) "Intro" else BottomNavItem.Dashboard.screen_route

    NavHost(
        navController = navController as NavHostController,
        startDestination = startDestination,
    ) {
        composable("Intro") {
            IntroPage(navController = navController)
        }

        composable(BottomNavItem.Dashboard.screen_route) {
            Dashboard(
                navController = navController,
                context = context,
                onNavigateToCalender = {
                    navController.navigate(CALENDER_SCREEN_ROUTE)
                },
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, language: String, scrollToAya: Int ->
                    navController.navigate(
                        MY_QURAN_SCREEN_ROUTE.replace(
                            "{number}",
                            number
                        )
                            .replace(
                                "{isSurah}",
                                isSurah.toString()
                            )
                            .replace(
                                "{language}",
                                language
                            )
                            .replace(
                                "{scrollTo}",
                                scrollToAya.toString()
                            )
                    )
                },
                onNavigateToTasbihScreen = { id: String, arabic: String, translation: String, transliteration: String ->
                    navController.navigate(
                        TASBIH_SCREEN_ROUTE
                            .replace(
                                "{id}",
                                id
                            )
                            .replace(
                                "{arabic}",
                                arabic
                            )
                            .replace(
                                "{translation}",
                                translation
                            )
                            .replace(
                                "{transliteration}",
                                transliteration
                            )
                    )
                },
                onNavigateToTasbihListScreen = {
                    navController.navigate(TASBIH_LIST_SCREEN)
                },
            )

        }

        composable(BottomNavItem.PrayerTimesScreen.screen_route)
        {
            PrayerTimesScreen(
                navController = navController,
            )
        }

        composable(CALENDER_SCREEN_ROUTE) {
            CalenderScreen(navController = navController)
        }

        composable(QIBLA_SCREEN_ROUTE) {
            QiblaScreen(navController)
        }
        composable(BottomNavItem.QuranScreen.screen_route) {
            QuranScreen(
                navController = navController,
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, language: String, scrollToAya: Int? ->
                    if (scrollToAya != null) {
                        navController.navigate(
                            MY_QURAN_SCREEN_ROUTE.replace(
                                "{number}",
                                number
                            )
                                .replace(
                                    "{isSurah}",
                                    isSurah.toString()
                                )
                                .replace(
                                    "{language}",
                                    language
                                )
                                .replace(
                                    "{scrollTo}",
                                    scrollToAya.toString()
                                )
                        ) {
                            popUpTo(MY_QURAN_SCREEN_ROUTE) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(
                            QURAN_AYA_SCREEN_ROUTE.replace(
                                "{number}",
                                number
                            )
                                .replace(
                                    "{isSurah}",
                                    isSurah.toString()
                                )
                                .replace(
                                    "{language}",
                                    language
                                )
                        )
                    }
                }
            )
        }
        composable(MY_QURAN_SCREEN_ROUTE) {
            AyatScreen(
                number = it.arguments?.getString("number")!!,
                isSurah = it.arguments?.getString("isSurah")!!,
                language = it.arguments?.getString("language")!!,
                scrollToAya = it.arguments?.getString("scrollTo")!!.toInt(),
                context = context,
                navController = navController
            )
        }

        composable(QURAN_AYA_SCREEN_ROUTE) {
            AyatScreen(
                navController = navController,
                number = it.arguments?.getString("number")!!,
                isSurah = it.arguments?.getString("isSurah")!!,
                language = it.arguments?.getString("language")!!,
                context = context
            )
        }


        composable(BottomNavItem.MoreScreen.screen_route) {
            MoreScreen(
                navController = navController,
                onNavigateToTasbihScreen = { id: String, arabic: String, translation: String, transliteration: String ->
                    navController.navigate(
                        TASBIH_SCREEN_ROUTE
                            .replace(
                                "{id}",
                                id
                            )
                            .replace(
                                "{arabic}",
                                arabic
                            )
                            .replace(
                                "{translation}",
                                translation
                            )
                            .replace(
                                "{transliteration}",
                                transliteration
                            )
                    )
                },
                onNavigateToTasbihListScreen = {
                    navController.navigate(TASBIH_LIST_SCREEN)
                },
                onNavigateToNames = {
                    navController.navigate(NAMESOFALLAH_SCREEN_ROUTE)
                },
                onNavigateToListOfTasbeeh = {
                    navController.navigate(AppConstants.CATEGORY_SCREEN_ROUTE)
                },
                onNavigateToQibla = {
                    navController.navigate(QIBLA_SCREEN_ROUTE)
                },
                onNavigateToShadah = {
                    navController.navigate(SHAHADAH_SCREEN_ROUTE)
                },
                onNavigateToPrayerTracker = {
                    navController.navigate(PRAYER_TRACKER_SCREEN_ROUTE)
                },
                onNavigateToCalender = {
                    navController.navigate(CALENDER_SCREEN_ROUTE)
                },
                onNavigateToZakat = {
                    navController.navigate("Zakat")
                },
                onNavigateToHadithShelf = {
                    navController.navigate(HADITH_SHELF_SCREEN_ROUTE)
                }
            )
        }

        composable(HADITH_SHELF_SCREEN_ROUTE) {
            BookShelf(navController = navController,
                onNavigateToChapterFromFavourite = { bookId: Int, chapterId: Int ->
                    navController.navigate(
                        HADITH_LIST_SCREEN_ROUTE.replace(
                            "{bookId}", bookId.toString()
                        ).replace("{chapterId}", chapterId.toString())
                    )
                },
                onNavigateToChaptersList = { id: Int, title: String ->
                    navController.navigate(
                        HADITH_CHAPTERS_LIST_SCREEN_ROUTE.replace(
                            "{bookId}",
                            id.toString()
                        ).replace("{bookName}", title)
                    )
                }
            )
        }

        composable(HADITH_CHAPTERS_LIST_SCREEN_ROUTE) {
            HadithChaptersList(
                navController = navController,
                bookId = it.arguments?.getString("bookId"),
                onNavigateToAChapter = { bookId: Int, chapterId: Int ->
                    navController.navigate(
                        HADITH_LIST_SCREEN_ROUTE.replace(
                            "{bookId}", bookId.toString()
                        ).replace("{chapterId}", chapterId.toString())
                    )
                }
            )
        }

        composable(HADITH_LIST_SCREEN_ROUTE) {
            HadithList(
                navController = navController,
                bookId = it.arguments?.getString("bookId"),
                chapterId = it.arguments?.getString("chapterId")
            )
        }


        composable(TASBIH_LIST_SCREEN) {
            ListOfTasbih(navController = navController) { id: String, arabic: String, translation: String, transliteration: String ->
                //replace the placeholder with the actual route TASBIH_SCREEN_ROUTE
                //tasbih_screen/{arabic}/{translation}/{transliteration}
                navController.navigate(
                    TASBIH_SCREEN_ROUTE
                        .replace(
                            "{id}",
                            id
                        )
                        .replace(
                            "{arabic}",
                            arabic
                        )
                        .replace(
                            "{translation}",
                            translation
                        )
                        .replace(
                            "{transliteration}",
                            transliteration
                        )
                )
            }
        }

        composable(PRAYER_TRACKER_SCREEN_ROUTE) {
            PrayerTracker(navController = navController)
        }

        composable(TASBIH_SCREEN_ROUTE) {
            TasbihScreen(
                tasbihId = it.arguments?.getString("id")!!,
                tasbihArabic = it.arguments?.getString("arabic")!!,
                tasbihEnglish = it.arguments?.getString("translation")!!,
                tasbihTranslitration = it.arguments?.getString("transliteration")!!,
                navController = navController
            )
        }

        composable(NAMESOFALLAH_SCREEN_ROUTE) {
            NamesOfAllah(navController = navController)
        }

        composable(AppConstants.CATEGORY_SCREEN_ROUTE) {
            Categories(
                navController = navController,
            )
            //pass the category name to the next screen
            { category: String, id: Int ->
                Log.d("Category", category)
                navController.navigate(
                    CHAPTERS_SCREEN_ROUTE
                        .replace(
                            "{title}",
                            category
                        )
                        .replace("{id}", id.toString())
                )
            }
        }

        composable(CHAPTERS_SCREEN_ROUTE) {
            ChapterList(
                categoryId = it.arguments?.getString("id")!!,
                navController = navController,
                onNavigateToChapter = { chapterId: Int, categoryName: String ->
                    navController.navigate(
                        CHAPTER_SCREEN_ROUTE
                            .replace(
                                "{chapterId}",
                                chapterId.toString()
                            )
                            .replace(
                                "{categoryName}",
                                categoryName
                            )
                    )
                },
            )
        }

        composable(CHAPTER_SCREEN_ROUTE) {
            DuaList(
                chapterId = it.arguments?.getString("chapterId")!!,
                navController = navController
            )
        }

        composable(SHAHADAH_SCREEN_ROUTE) {
            ShahadahScreen(navController)
        }

        composable(BottomNavItem.SettingsScreen.screen_route) {
            SettingsScreen(
                activity = context,
                navController = navController,
                viewModelSettings = settingsViewModel,
                onNavigateToPrayerTimeCustomizationScreen = {
                    navController.navigate(
                        PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
                    )
                },
                onNavigateToLicencesScreen = {
                    navController.navigate(
                        LICENCES_SCREEN_ROUTE
                    )
                },
                onNavigateToAboutScreen = {
                    navController.navigate(
                        ABOUT_SCREEN_ROUTE
                    )
                },
                onNavigateToWebViewScreen = { url: String ->
                    navController.navigate(
                        WEB_VIEW_SCREEN_ROUTE
                            .replace(
                                "{url}",
                                url
                            ),
                    )
                },
                onNavigateToDebugScreen = {
                    navController.navigate(
                        DEBUG_MODE
                    )
                },
            )
        }
        composable(WEB_VIEW_SCREEN_ROUTE) {
            WebViewScreen(
                url = it.arguments?.getString("url")!!,
                navController = navController
            )
        }
        composable(ABOUT_SCREEN_ROUTE) {
            About(
                navController = navController
            )
            //navigate to the debug screen
            {
                navController.navigate(DEBUG_MODE)
            }
        }
        composable(LICENCES_SCREEN_ROUTE) {
            LicensesScreen(
                navController = navController,
            )
        }
        composable(PRAYER_TIMES_SETTINGS_SCREEN_ROUTE) {
            PrayerTimesCustomizations(navController)
        }

        composable(DEBUG_MODE) {
            DebugScreen(
                navController = navController,
            )
        }
    }
}