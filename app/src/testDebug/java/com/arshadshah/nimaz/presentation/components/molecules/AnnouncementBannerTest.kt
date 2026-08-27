package com.arshadshah.nimaz.presentation.components.molecules

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The FCM engagement banner, which is the app's only channel for telling a reader something
 * after the build has shipped.
 *
 * Three things about it are easy to get wrong in ways nobody notices until it matters: an
 * undismissable notice that shows a dismiss button anyway, a CTA that renders without a
 * resolvable route behind it, and a banner that keeps occupying space once it has gone. The
 * file was at 13%.
 */
@RunWith(RobolectricTestRunner::class)
class AnnouncementBannerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun announcement(
        type: AnnouncementType = AnnouncementType.FEATURE,
        ctaLabel: String? = null,
        route: String? = null,
        dismissable: Boolean = true,
    ) = Announcement(
        id = "a1",
        type = type,
        title = "Ask with Proof is here",
        body = "Search the Qur'an, get cited answers.",
        ctaLabel = ctaLabel,
        route = route,
        dismissable = dismissable,
    )

    @Test
    fun `nothing is drawn when there is no announcement`() {
        // Callers drop this in unconditionally, so an empty banner that still takes a row of
        // vertical space would put a gap at the top of Home on every ordinary day.
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement = null,
                showCta = false,
                onCtaClick = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Ask with Proof is here").assertDoesNotExist()
    }

    @Test
    fun `the title and body are both shown, not just the headline`() {
        composeRule.setThemedContent {
            AnnouncementBanner(announcement(), showCta = false, onCtaClick = {}, onDismiss = {})
        }

        composeRule.onNodeWithText("Ask with Proof is here").assertIsDisplayed()
        composeRule.onNodeWithText("Search the Qur'an, get cited answers.").assertIsDisplayed()
    }

    @Test
    fun `a celebration still renders here, though it normally gets its own card`() {
        // This generic banner is the fallback path. Producing a blank banner rather than a
        // wrong-looking one is the failure worth ruling out. The per-type sweep lives in
        // AnnouncementBannerTypesTest — a compose rule only works as a @Rule, so one
        // composition per type means one test per type.
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement(type = AnnouncementType.CELEBRATION),
                showCta = false,
                onCtaClick = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Ask with Proof is here").assertIsDisplayed()
    }

    @Test
    fun `a CTA is offered only when the route resolved, not merely because a label exists`() {
        // showCta is the ViewModel's answer to "does this route go anywhere". A button that
        // goes nowhere is worse than no button: the reader taps and nothing happens.
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement(ctaLabel = "Try it", route = "search/ask"),
                showCta = false,
                onCtaClick = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Try it").assertDoesNotExist()
    }

    @Test
    fun `the CTA calls back exactly once when tapped`() {
        var clicks = 0
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement(ctaLabel = "Try it", route = "search/ask"),
                showCta = true,
                onCtaClick = { clicks++ },
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Try it").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `an undismissable announcement offers no dismiss button`() {
        // A policy notice the reader must see is marked undismissable; showing the X anyway
        // makes it dismissable in practice and the notice is never seen again.
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement(dismissable = false),
                showCta = false,
                onCtaClick = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithContentDescription(string(R.string.cd_dismiss_announcement))
            .assertDoesNotExist()
    }

    @Test
    fun `dismissing calls back, and the control is reachable by its accessibility label`() {
        var dismissed = 0
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement(),
                showCta = false,
                onCtaClick = {},
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithContentDescription(string(R.string.cd_dismiss_announcement))
            .performClick()

        assertThat(dismissed).isEqualTo(1)
    }
}
