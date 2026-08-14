package com.arshadshah.nimaz.presentation.components.atoms

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
        assertThat(_root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.entries).hasSize(4)
    }

    // ── INFO ────────────────────────────────────────────────────────────────

    @Test
    fun `info banner renders without border or icon`() {
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Info message",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.INFO
            )
        }
        composeRule.onNodeWithText("Info message").assertExists()
    }

    @Test
    fun `info banner renders with icon`() {
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Info with icon",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.INFO,
                icon = Icons.Default.Info
            )
        }
        composeRule.onNodeWithText("Info with icon").assertExists()
    }

    @Test
    fun `info banner renders with border and icon`() {
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Bordered info",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.INFO,
                icon = Icons.Default.Info,
                showBorder = true
            )
        }
        composeRule.onNodeWithText("Bordered info").assertExists()
    }

    @Test
    fun `info banner renders with border and no icon`() {
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Bordered no icon",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.INFO,
                showBorder = true
            )
        }
        composeRule.onNodeWithText("Bordered no icon").assertExists()
    }

    // ── WARNING ──────────────────────────────────────────────────────────────

    @Test
    fun `warning banner renders full content and fires action`() {
        var clicked = false
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Warning message",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.WARNING,
                icon = Icons.Default.Warning,
                title = "Warning title",
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
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Plain warning",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.WARNING
            )
        }
        composeRule.onNodeWithText("Plain warning").assertExists()
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    fun `update banner renders action and fires it`() {
        var clicked = false
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Update available",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.UPDATE,
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
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Downloading",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.UPDATE,
                isLoading = true
            )
        }
        composeRule.onNodeWithText("Downloading").assertExists()
    }

    @Test
    fun `update banner renders message only`() {
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Up to date",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.UPDATE
            )
        }
        composeRule.onNodeWithText("Up to date").assertExists()
    }

    // ── ERROR ────────────────────────────────────────────────────────────────

    @Test
    fun `error banner renders clickable surface and fires click`() {
        var clicked = false
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Tap for help",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.ERROR,
                icon = Icons.Default.Warning,
                title = "Calibration",
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Tap for help").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `error banner renders action button and fires it`() {
        var clicked = false
        composeRule.setThemedContent {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Calibrate compass",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.ERROR,
                title = "Calibration",
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
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBanner(
                message = "Plain error",
                variant = _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant.ERROR
            )
        }
        composeRule.onNodeWithText("Plain error").assertExists()
    }
}
