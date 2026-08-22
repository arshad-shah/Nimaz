package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazChipTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `chip enums are complete`() {
        assertThat(NimazChipStyle.entries).hasSize(4)
        assertThat(NimazChipVariant.entries).hasSize(4)
    }

    // ── NimazChip (unified) ─────────────────────────────────────────────────

    private fun assertVariantDispatch(variant: NimazChipVariant) {
        var clicked = false
        composeRule.setThemedContent {
            NimazChip(
                text = "Chip",
                onClick = { clicked = true },
                variant = variant,
                selected = true,
                leadingIcon = Icons.Default.Star
            )
        }
        composeRule.onNodeWithText("Chip").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `NimazChip dispatches filter variant`() = assertVariantDispatch(NimazChipVariant.FILTER)

    @Test
    fun `NimazChip dispatches suggestion variant`() =
        assertVariantDispatch(NimazChipVariant.SUGGESTION)

    @Test
    fun `NimazChip dispatches assist variant`() = assertVariantDispatch(NimazChipVariant.ASSIST)

    @Test
    fun `NimazChip dispatches input variant`() = assertVariantDispatch(NimazChipVariant.INPUT)

    // ── NimazFilterChip ─────────────────────────────────────────────────────

    @Test
    fun `filter chip shows selected check icon`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazFilterChip(
                selected = true,
                onClick = { clicked = true },
                label = "Selected",
            )
        }
        composeRule.onNodeWithText("Selected").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `filter chip shows leading icon when not selected and elevated`() {
        composeRule.setThemedContent {
            NimazFilterChip(
                selected = false,
                onClick = {},
                label = "Leading",
                leadingIcon = Icons.Default.Star,
                elevated = true,
            )
        }
        composeRule.onNodeWithText("Leading").assertExists()
    }

    @Test
    fun `filter chip without icons renders`() {
        composeRule.setThemedContent {
            NimazFilterChip(
                selected = false,
                onClick = {},
                label = "Plain",
                showSelectedIcon = false,
            )
        }
        composeRule.onNodeWithText("Plain").assertExists()
    }

    // ── NimazAssistChip ─────────────────────────────────────────────────────

    @Test
    fun `assist chip renders plain and elevated with icon`() {
        composeRule.setThemedContent {
            NimazAssistChip(onClick = {}, label = "Assist plain")
        }
        composeRule.onNodeWithText("Assist plain").assertExists()
    }

    @Test
    fun `assist chip renders elevated with leading icon`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazAssistChip(
                onClick = { clicked = true },
                label = "Assist icon",
                leadingIcon = Icons.Default.Star,
                elevated = true
            )
        }
        composeRule.onNodeWithText("Assist icon").performClick()
        assertThat(clicked).isTrue()
    }

    // ── NimazInputChip ──────────────────────────────────────────────────────

    @Test
    fun `input chip renders minimal`() {
        composeRule.setThemedContent {
            NimazInputChip(selected = false, onClick = {}, label = "Input plain")
        }
        composeRule.onNodeWithText("Input plain").assertExists()
    }

    @Test
    fun `input chip renders with icon avatar and dismiss`() {
        composeRule.setThemedContent {
            NimazInputChip(
                selected = true,
                onClick = {},
                label = "Input rich",
                onDismiss = {},
                leadingIcon = Icons.Default.Star,
                avatar = { Box(androidx.compose.ui.Modifier) }
            )
        }
        composeRule.onNodeWithText("Input rich").assertExists()
    }

    // ── NimazSuggestionChip ─────────────────────────────────────────────────

    @Test
    fun `suggestion chip renders minimal`() {
        composeRule.setThemedContent {
            NimazSuggestionChip(onClick = {}, label = "Suggest plain")
        }
        composeRule.onNodeWithText("Suggest plain").assertExists()
    }

    @Test
    fun `suggestion chip renders elevated with icon`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazSuggestionChip(
                onClick = { clicked = true },
                label = "Suggest icon",
                icon = Icons.Default.Star,
                elevated = true
            )
        }
        composeRule.onNodeWithText("Suggest icon").performClick()
        assertThat(clicked).isTrue()
    }

    // ── RevelationTypeChip ──────────────────────────────────────────────────

    @Test
    fun `revelation type chip renders meccan and medinan`() {
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Row {
                RevelationTypeChip(isMeccan = true)
                RevelationTypeChip(isMeccan = false)
            }
        }
        composeRule.onNodeWithText("Meccan").assertExists()
        composeRule.onNodeWithText("Medinan").assertExists()
    }
}
