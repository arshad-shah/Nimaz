package com.arshadshah.nimaz.presentation.screens.adaptive

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.presentation.screens.about.AboutScreen
import com.arshadshah.nimaz.presentation.screens.help.HelpScreen
import com.arshadshah.nimaz.presentation.screens.more.MoreMenuScreen
import com.arshadshah.nimaz.presentation.screens.settings.AppearanceSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.LanguageScreen
import com.arshadshah.nimaz.presentation.screens.settings.LocationScreen
import com.arshadshah.nimaz.presentation.screens.settings.NotificationSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.WidgetsScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveMoreScreen(
    navController: NavController,
    onRestartApp: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val shouldRestart by settingsViewModel.shouldRestart.collectAsState()

    LaunchedEffect(shouldRestart) {
        if (shouldRestart) onRestartApp()
    }

    val shareApp = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out Nimaz - Prayer Times App: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Nimaz"))
    }

    val rateApp = {
        val activity = context as? Activity
        if (activity != null) {
            val manager = ReviewManagerFactory.create(context)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(activity, task.result)
                } else {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.arshadshah.nimaz")
                    )
                    context.startActivity(intent)
                }
            }
        }
    }

    if (windowSizeClass.isCompact) {
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
            onNavigateToTafseer = {
                navController.navigate(Route.Tafseer(surahNumber = 1, ayahNumber = 1))
            },
            onNavigateToCalculationMethod = { navController.navigate(Route.SettingsPrayerCalculation) },
            onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
            onNavigateToPrayerTimes = { navController.navigate(Route.PrayerTimes) },
            onNavigateToMonthlyPrayerTimes = { navController.navigate(Route.MonthlyPrayerTimes) },
            onNavigateToKhatam = { navController.navigate(Route.KhatamList) },
            onNavigateToAsmaUlHusna = { navController.navigate(Route.AsmaUlHusnaList) },
            onNavigateToAsmaUnNabi = { navController.navigate(Route.AsmaUnNabiList) },
            onNavigateToProphets = { navController.navigate(Route.ProphetsList) },
            onNavigateToQaida = { navController.navigate(Route.QaidaHome) },
            onShareApp = shareApp,
            onRateApp = rateApp,
            onDeleteAllData = { settingsViewModel.onEvent(SettingsEvent.DeleteAllData) },
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<MoreDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    MoreMenuScreen(
                        onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
                        onNavigateToLocation = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.LOCATION)
                                )
                            }
                        },
                        onNavigateToNotifications = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.NOTIFICATIONS)
                                )
                            }
                        },
                        onNavigateToAppearance = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.APPEARANCE)
                                )
                            }
                        },
                        onNavigateToLanguage = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.LANGUAGE)
                                )
                            }
                        },
                        onNavigateToWidgets = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.WIDGETS)
                                )
                            }
                        },
                        onNavigateToAbout = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.ABOUT)
                                )
                            }
                        },
                        onNavigateToHelp = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    MoreDetailArgs(MoreDetailPane.HELP)
                                )
                            }
                        },
                        onNavigateToHadith = { navController.navigate(Route.HadithHome) },
                        onNavigateToFasting = { navController.navigate(Route.FastingHome) },
                        onNavigateToZakat = { navController.navigate(Route.ZakatCalculator) },
                        onNavigateToDuas = { navController.navigate(Route.DuaHome) },
                        onNavigateToTafseer = {
                            navController.navigate(Route.Tafseer(surahNumber = 1, ayahNumber = 1))
                        },
                        onNavigateToCalculationMethod = { navController.navigate(Route.SettingsPrayerCalculation) },
                        onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
                        onNavigateToPrayerTimes = { navController.navigate(Route.PrayerTimes) },
                        onNavigateToMonthlyPrayerTimes = { navController.navigate(Route.MonthlyPrayerTimes) },
                        onNavigateToKhatam = { navController.navigate(Route.KhatamList) },
                        onNavigateToAsmaUlHusna = { navController.navigate(Route.AsmaUlHusnaList) },
                        onNavigateToAsmaUnNabi = { navController.navigate(Route.AsmaUnNabiList) },
                        onNavigateToProphets = { navController.navigate(Route.ProphetsList) },
                        onNavigateToQaida = { navController.navigate(Route.QaidaHome) },
                        onShareApp = shareApp,
                        onRateApp = rateApp,
                        onDeleteAllData = { settingsViewModel.onEvent(SettingsEvent.DeleteAllData) },
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        when (args.pane) {
                            MoreDetailPane.LOCATION -> LocationScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            MoreDetailPane.NOTIFICATIONS -> NotificationSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            MoreDetailPane.APPEARANCE -> AppearanceSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            MoreDetailPane.LANGUAGE -> LanguageScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            MoreDetailPane.WIDGETS -> WidgetsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            MoreDetailPane.ABOUT -> AboutScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                onNavigateToPrivacyPolicy = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://nimaz.arshadshah.com/privacy")
                                    )
                                    context.startActivity(intent)
                                },
                                onNavigateToTerms = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://nimaz.arshadshah.com/terms")
                                    )
                                    context.startActivity(intent)
                                },
                                onNavigateToLicenses = {
                                    navController.navigate(Route.Licenses)
                                },
                                onRateApp = rateApp,
                                onShareApp = shareApp,
                                onContactUs = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:arshad@arshadshah.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Nimaz App Feedback")
                                    }
                                    context.startActivity(intent)
                                }
                            )

                            MoreDetailPane.HELP -> {
                                val supportEmail =
                                    stringResource(com.arshadshah.nimaz.R.string.support_email)
                                val supportSubject =
                                    stringResource(com.arshadshah.nimaz.R.string.nimaz_support_request)
                                HelpScreen(
                                    onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                    onNavigateToTopic = { topicId ->
                                        navController.navigate(Route.HelpTopicDetail(topicId))
                                    },
                                    onContact = {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$supportEmail")
                                            putExtra(Intent.EXTRA_SUBJECT, supportSubject)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                supportEmail
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

enum class MoreDetailPane {
    LOCATION, NOTIFICATIONS, APPEARANCE, LANGUAGE, WIDGETS, ABOUT, HELP
}

@kotlinx.parcelize.Parcelize
data class MoreDetailArgs(
    val pane: MoreDetailPane,
) : android.os.Parcelable
