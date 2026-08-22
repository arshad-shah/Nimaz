package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazBannerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `banner variants enum is complete`() {
        assertThat(NimazBannerVariant.entries).hasSize(5)
    }

    // ── INFO ────────────────────────────────────────────────────────────────

    @Test
    fun `info banner renders title only`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Info message",
                variant = NimazBannerVariant.INFO
            )
        }
        composeRule.onNodeWithText("Info message").assertExists()
    }

    @Test
    fun `info banner renders with icon`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Info with icon",
                variant = NimazBannerVariant.INFO,
                icon = Icons.Default.Info
            )
        }
        composeRule.onNodeWithText("Info with icon").assertExists()
    }

    @Test
    fun `info banner renders title and message`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Info title",
                message = "Details about info",
                variant = NimazBannerVariant.INFO,
            )
        }
        composeRule.onNodeWithText("Info title").assertExists()
        composeRule.onNodeWithText("Details about info").assertExists()
    }

    @Test
    fun `info banner inline density renders`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Inline info",
                variant = NimazBannerVariant.INFO,
                density = NimazBannerDensity.INLINE,
            )
        }
        composeRule.onNodeWithText("Inline info").assertExists()
    }

    // ── WARNING ──────────────────────────────────────────────────────────────

    @Test
    fun `warning banner renders full content and fires action`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazBanner(
                title = "Warning title",
                message = "Warning message",
                variant = NimazBannerVariant.WARNING,
                icon = Icons.Default.Warning,
                actionLabel = "Fix",
                onAction = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Warning title").assertExists()
        composeRule.onNodeWithText("Warning message").assertExists()
        composeRule.onNodeWithText("Fix").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `warning banner renders minimal content`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Plain warning",
                variant = NimazBannerVariant.WARNING
            )
        }
        composeRule.onNodeWithText("Plain warning").assertExists()
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    fun `update banner renders action and fires it`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazBanner(
                title = "Update available",
                variant = NimazBannerVariant.UPDATE,
                actionLabel = "Update",
                onAction = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Update").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `update banner renders loading spinner`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Downloading",
                variant = NimazBannerVariant.UPDATE,
                isLoading = true
            )
        }
        composeRule.onNodeWithText("Downloading").assertExists()
    }

    @Test
    fun `update banner renders title only`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Up to date",
                variant = NimazBannerVariant.UPDATE
            )
        }
        composeRule.onNodeWithText("Up to date").assertExists()
    }

    // ── ERROR ────────────────────────────────────────────────────────────────

    @Test
    fun `error banner renders clickable surface and fires click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazBanner(
                title = "Calibration",
                message = "Tap for help",
                variant = NimazBannerVariant.ERROR,
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Calibration").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `error banner renders action button and fires it`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazBanner(
                title = "Calibration",
                message = "Calibrate compass",
                variant = NimazBannerVariant.ERROR,
                actionLabel = "Calibrate",
                onAction = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Calibrate").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `error banner renders minimal content`() {
        composeRule.setThemedContent {
            NimazBanner(
                title = "Plain error",
                variant = NimazBannerVariant.ERROR
            )
        }
        composeRule.onNodeWithText("Plain error").assertExists()
    }

    // ── EVENT ────────────────────────────────────────────────────────────────

    @Test
    fun `event banner renders title and action`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazBanner(
                title = "Ramadan Mubarak",
                message = "Wishing you a blessed month.",
                variant = NimazBannerVariant.EVENT,
                actionLabel = "Explore",
                onAction = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Ramadan Mubarak").assertExists()
        composeRule.onNodeWithText("Explore").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `event banner renders with dismiss`() {
        var dismissed = false
        composeRule.setThemedContent {
            NimazBanner(
                title = "Event announcement",
                variant = NimazBannerVariant.EVENT,
                onDismiss = { dismissed = true }
            )
        }
        composeRule.onNodeWithText("Event announcement").assertExists()
    }
}
