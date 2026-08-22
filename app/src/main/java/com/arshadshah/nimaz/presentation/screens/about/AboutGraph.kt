package com.arshadshah.nimaz.presentation.screens.about

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.helpDeepLinkRoute
import com.arshadshah.nimaz.core.navigation.restartApp
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.presentation.components.molecules.ShareAppSheet
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveMoreScreen
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

/**
 * The 7 About destinations — about, help, licenses and the More menu.
 *
 * Split out of `NavGraph.kt` in PR 12 of #551. That file registered all 94 destinations and
 * imported 69 screen composables, which meant every screen in the app was reachable from one
 * place — and no feature could move into its own module while that was true, because `:app`
 * would have had to import from all eleven feature modules at once.
 *
 * The bodies are unchanged; only their location is. `:app` keeps the `NavHost` and calls this.
 *
 * It takes a `NavController` rather than the `onNavigate` lambda #563 sketches because 11 of the
 * 158 `navigate` calls in these blocks pass a `NavOptionsBuilder` — `popUpTo`, `launchSingleTop` —
 * which `(Route) -> Unit` cannot express, and flattening them would change back-stack behaviour
 * silently. A graph function *is* navigation wiring, so holding the controller is what it is for;
 * the rule that matters is that a **screen** must not, which `NavControllerConfinementTest`
 * enforces.
 */
fun NavGraphBuilder.aboutGraph(navController: NavController) {
    taggedComposable<Route.More>(ScreenTags.More) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AdaptiveMoreScreen(
            onNavigate = { navController.navigate(it) },
            onRestartApp = { restartApp(context) },
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
            stringResource(com.arshadshah.nimaz.core.ui.R.string.contact_email)
        val contactSubject =
            stringResource(com.arshadshah.nimaz.core.ui.R.string.contact_email_subject)
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
            onNavigateToDetail = { libraryId ->
                navController.navigate(Route.LicenseDetail(libraryId))
            }
        )
    }

    taggedComposable<Route.LicenseDetail>(ScreenTags.LicenseDetail) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.LicenseDetail>()
        LicenseDetailScreen(
            libraryId = args.libraryHashCode,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SettingsHelp>(ScreenTags.SettingsHelp) {
        val context = androidx.compose.ui.platform.LocalContext.current
        // Unified support inbox — same address the About screen contacts.
        val supportEmail =
            androidx.compose.ui.res.stringResource(com.arshadshah.nimaz.core.ui.R.string.contact_email)
        val supportSubject =
            androidx.compose.ui.res.stringResource(com.arshadshah.nimaz.core.ui.R.string.nimaz_support_request)
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
}
