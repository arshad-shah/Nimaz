package com.arshadshah.nimaz.presentation.screens.adaptive

import android.app.Activity
import android.content.Intent
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.presentation.components.molecules.ShareAppSheet
import com.arshadshah.nimaz.presentation.screens.about.AboutScreen
import com.arshadshah.nimaz.presentation.screens.help.HelpScreen
import com.arshadshah.nimaz.presentation.screens.more.MoreMenuScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveMoreScreen(
    onNavigate: (Route) -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()
    val context = LocalContext.current

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
            onNavigateToSettings = { onNavigate(Route.Settings) },
            onNavigateToCalendar = { onNavigate(Route.IslamicCalendar) },
            onNavigateToAbout = { onNavigate(Route.SettingsAbout) },
            onNavigateToHelp = { onNavigate(Route.SettingsHelp) },
            onNavigateToHadith = { onNavigate(Route.HadithHome) },
            onNavigateToFasting = { onNavigate(Route.FastingHome) },
            onNavigateToZakat = { onNavigate(Route.ZakatCalculator) },
            onNavigateToDuas = { onNavigate(Route.DuaHome) },
            onNavigateToTafseer = {
                onNavigate(Route.TafseerChapters)
            },
            onNavigateToPrayerTracker = { onNavigate(Route.PrayerTracker) },
            onNavigateToNightWorship = { onNavigate(Route.NightWorship) },
            onNavigateToPrayerTimes = { onNavigate(Route.PrayerTimes) },
            onNavigateToMonthlyPrayerTimes = { onNavigate(Route.MonthlyPrayerTimes) },
            onNavigateToKhatam = { onNavigate(Route.KhatamList) },
            onNavigateToNames = { onNavigate(Route.Names()) },
            onNavigateToQaida = { onNavigate(Route.QaidaHome) },
            onNavigateToTasbih = { onNavigate(Route.TasbihHome) },
            onNavigateToQibla = { onNavigate(Route.Qibla) },
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
                        onNavigateToSettings = { onNavigate(Route.Settings) },
                        onNavigateToCalendar = { onNavigate(Route.IslamicCalendar) },
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
                        onNavigateToHadith = { onNavigate(Route.HadithHome) },
                        onNavigateToFasting = { onNavigate(Route.FastingHome) },
                        onNavigateToZakat = { onNavigate(Route.ZakatCalculator) },
                        onNavigateToDuas = { onNavigate(Route.DuaHome) },
                        onNavigateToTafseer = {
                            onNavigate(Route.Tafseer(surahNumber = 1, ayahNumber = 1))
                        },
                        onNavigateToPrayerTracker = { onNavigate(Route.PrayerTracker) },
                        onNavigateToNightWorship = { onNavigate(Route.NightWorship) },
                        onNavigateToPrayerTimes = { onNavigate(Route.PrayerTimes) },
                        onNavigateToMonthlyPrayerTimes = { onNavigate(Route.MonthlyPrayerTimes) },
                        onNavigateToKhatam = { onNavigate(Route.KhatamList) },
                        onNavigateToNames = { onNavigate(Route.Names()) },
                        onNavigateToQaida = { onNavigate(Route.QaidaHome) },
                        onNavigateToTasbih = { onNavigate(Route.TasbihHome) },
                        onNavigateToQibla = { onNavigate(Route.Qibla) },
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
                                        onNavigate(Route.Licenses)
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
                                        onNavigate(Route.HelpTopicDetail(topicId))
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
