package com.arshadshah.nimaz.presentation.screens.adaptive

import android.app.Activity
import android.content.Intent
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.presentation.components.molecules.ShareAppSheet
import com.arshadshah.nimaz.presentation.screens.about.AboutScreen
import com.arshadshah.nimaz.presentation.screens.help.HelpScreen
import com.arshadshah.nimaz.presentation.screens.more.MoreMenuScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
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
    val shouldRestart by settingsViewModel.shouldRestart.collectAsStateWithLifecycle()

    LaunchedEffect(shouldRestart) {
        if (shouldRestart) onRestartApp()
    }

    val shareScope = rememberCoroutineScope()
    var showShareSheet by remember { mutableStateOf(false) }
    val shareApp = { showShareSheet = true }

    if (showShareSheet) {
        ShareAppSheet(
            onDismiss = { showShareSheet = false },
            onShareLink = {
                showShareSheet = false
                // Branded invite card image + the tappable store link.
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
                        "https://play.google.com/store/apps/details?id=com.arshadshah.nimaz".toUri()
                    )
                    context.startActivity(intent)
                }
            }
        }
    }

    if (windowSizeClass.isCompact) {
        MoreMenuScreen(
            onNavigateToSettings = { navController.navigate(Route.Settings) },
            onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
            onNavigateToAbout = { navController.navigate(Route.SettingsAbout) },
            onNavigateToHelp = { navController.navigate(Route.SettingsHelp) },
            onNavigateToHadith = { navController.navigate(Route.HadithHome) },
            onNavigateToFasting = { navController.navigate(Route.FastingHome) },
            onNavigateToZakat = { navController.navigate(Route.ZakatCalculator) },
            onNavigateToDuas = { navController.navigate(Route.DuaHome) },
            onNavigateToTafseer = {
                navController.navigate(Route.TafseerChapters)
            },
            onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
            onNavigateToNightWorship = { navController.navigate(Route.NightWorship) },
            onNavigateToPrayerTimes = { navController.navigate(Route.PrayerTimes) },
            onNavigateToMonthlyPrayerTimes = { navController.navigate(Route.MonthlyPrayerTimes) },
            onNavigateToKhatam = { navController.navigate(Route.KhatamList) },
            onNavigateToAsmaUlHusna = { navController.navigate(Route.AsmaUlHusnaList) },
            onNavigateToAsmaUnNabi = { navController.navigate(Route.AsmaUnNabiList) },
            onNavigateToProphets = { navController.navigate(Route.ProphetsList) },
            onNavigateToQaida = { navController.navigate(Route.QaidaHome) },
            onShareApp = shareApp,
            onRateApp = rateApp,
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<MoreDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    MoreMenuScreen(
                        onNavigateToSettings = { navController.navigate(Route.Settings) },
                        onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
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
                        onNavigateToPrayerTracker = { navController.navigate(Route.PrayerTracker()) },
                        onNavigateToNightWorship = { navController.navigate(Route.NightWorship) },
                        onNavigateToPrayerTimes = { navController.navigate(Route.PrayerTimes) },
                        onNavigateToMonthlyPrayerTimes = { navController.navigate(Route.MonthlyPrayerTimes) },
                        onNavigateToKhatam = { navController.navigate(Route.KhatamList) },
                        onNavigateToAsmaUlHusna = { navController.navigate(Route.AsmaUlHusnaList) },
                        onNavigateToAsmaUnNabi = { navController.navigate(Route.AsmaUnNabiList) },
                        onNavigateToProphets = { navController.navigate(Route.ProphetsList) },
                        onNavigateToQaida = { navController.navigate(Route.QaidaHome) },
                        onShareApp = shareApp,
                        onRateApp = rateApp,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        when (args.pane) {
                            MoreDetailPane.ABOUT -> {
                                val contactEmail = stringResource(R.string.contact_email)
                                val contactSubject =
                                    stringResource(R.string.contact_email_subject)
                                AboutScreen(
                                    onNavigateBack = { scope.launch { navigator.navigateBack() } },
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
                                    onRateApp = rateApp,
                                    onShareApp = shareApp,
                                    onContactUs = {
                                        ContentShareManager.sendEmail(
                                            context,
                                            address = contactEmail,
                                            subject = contactSubject,
                                        )
                                    }
                                )
                            }

                            MoreDetailPane.HELP -> {
                                val supportEmail =
                                    stringResource(R.string.contact_email)
                                val supportSubject =
                                    stringResource(R.string.nimaz_support_request)
                                HelpScreen(
                                    onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                    onNavigateToTopic = { topicId ->
                                        navController.navigate(Route.HelpTopicDetail(topicId))
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
                        }
                    }
                }
            }
        )
    }
}

enum class MoreDetailPane {
    ABOUT, HELP
}

@kotlinx.parcelize.Parcelize
data class MoreDetailArgs(
    val pane: MoreDetailPane,
) : android.os.Parcelable
