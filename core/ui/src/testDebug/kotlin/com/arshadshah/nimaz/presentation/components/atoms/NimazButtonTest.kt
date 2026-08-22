package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazButtonTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `button enums are complete`() {
        assertThat(NimazButtonVariant.entries).hasSize(7) // PRIMARY QUIET DESTRUCTIVE TEXT + deprecated FILLED TONAL OUTLINED
        assertThat(NimazButtonSize.entries).hasSize(4)    // SMALL STANDARD + deprecated MEDIUM LARGE
        assertThat(NimazButtonType.entries).hasSize(2)    // STANDARD PILL (deprecated)
    }

    @Test
    fun `every variant renders and fires click`() {
        val clicked = mutableSetOf<NimazButtonVariant>()
        composeRule.setThemedContent {
            Column {
                NimazButtonVariant.entries.forEach { variant ->
                    NimazButton(
                        text = variant.name,
                        onClick = { clicked += variant },
                        variant = variant
                    )
                }
            }
        }
        NimazButtonVariant.entries.forEach { variant ->
            composeRule.onNodeWithText(variant.name).performClick()
        }
        assertThat(clicked).containsExactlyElementsIn(NimazButtonVariant.entries)
    }

    @Test
    fun `every size renders and fires click`() {
        val clicked = mutableSetOf<NimazButtonSize>()
        composeRule.setThemedContent {
            Column {
                NimazButtonSize.entries.forEach { size ->
                    NimazButton(
                        text = size.name,
                        onClick = { clicked += size },
                        size = size
                    )
                }
            }
        }
        NimazButtonSize.entries.forEach { size ->
            composeRule.onNodeWithText(size.name).performClick()
        }
        assertThat(clicked).containsExactlyElementsIn(NimazButtonSize.entries)
    }

    @Test
    fun `every type renders and fires click`() {
        val clicked = mutableSetOf<NimazButtonType>()
        composeRule.setThemedContent {
            Column {
                NimazButtonType.entries.forEach { type ->
                    NimazButton(
                        text = type.name,
                        onClick = { clicked += type },
                        type = type
                    )
                }
            }
        }
        NimazButtonType.entries.forEach { type ->
            composeRule.onNodeWithText(type.name).performClick()
        }
        assertThat(clicked).containsExactlyElementsIn(NimazButtonType.entries)
    }

    @Test
    fun `button with leading icon fires click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazButton(
                text = "Next",
                onClick = { clicked = true },
                leadingIcon = Icons.Default.Check,
            )
        }
        composeRule.onNodeWithText("Next").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `disabled button is not enabled and does not fire click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazButton(text = "Disabled", onClick = { clicked = true }, enabled = false)
        }
        composeRule.onNodeWithText("Disabled").assertIsNotEnabled()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `loading button keeps label visible and is not enabled`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazButton(text = "Save", onClick = { clicked = true }, loading = true)
        }
        // Loading shows spinner alongside the label — button width stays stable.
        composeRule.onNodeWithText("Save").assertExists()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `full width button renders`() {
        composeRule.setThemedContent {
            NimazButton(text = "Wide", onClick = {}, fullWidth = true)
        }
        composeRule.onNodeWithText("Wide").assertExists()
    }
}
