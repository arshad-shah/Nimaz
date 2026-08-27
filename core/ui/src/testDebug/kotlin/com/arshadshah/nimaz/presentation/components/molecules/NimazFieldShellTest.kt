package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The shell, exercised through the members that share it.
 *
 * These are the guarantees that make the family a family rather than three components that
 * happen to look alike: the same label, the same helper/error line, the same disabled
 * behaviour — whether the thing inside the box is a caret, a chevron or a number.
 */
@RunWith(RobolectricTestRunner::class)
class NimazFieldShellTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun methods() = listOf(
        NimazDropdownItem("mwl", "Muslim World League"),
        NimazDropdownItem("uaq", "Umm al-Qura"),
    )

    // ── One label, one helper line, across the family ──────────────────────

    @Test
    fun `the dropdown wears the same label, helper and required marker as a text field`() {
        composeRule.setThemedContent {
            Column {
                NimazDropdownField(
                    items = methods(),
                    selected = "mwl",
                    onSelected = {},
                    label = "Calculation method",
                    required = true,
                    helper = "Used for every prayer time",
                )
                NimazTextField(
                    value = "",
                    onValueChange = {},
                    label = "Preset name",
                    required = true,
                    helper = "Shown in your list of presets",
                )
            }
        }
        composeRule.onNodeWithText("Calculation method").assertExists()
        composeRule.onNodeWithText("Used for every prayer time").assertExists()
        composeRule.onNodeWithText("Preset name").assertExists()
        composeRule.onNodeWithText("Shown in your list of presets").assertExists()
    }

    @Test
    fun `an error replaces the helper on a dropdown too`() {
        composeRule.setThemedContent {
            NimazDropdownField(
                items = methods(),
                selected = null,
                onSelected = {},
                label = "Calculation method",
                helper = "Used for every prayer time",
                error = "Pick a method before continuing.",
            )
        }
        composeRule.onNodeWithText("Pick a method before continuing.").assertExists()
        composeRule.onNodeWithText("Used for every prayer time").assertDoesNotExist()
    }

    @Test
    fun `the dropdown still opens, selects and closes on the shared shell`() {
        // The shell owns the box and the anchor; the popup has to keep working through it.
        var picked: String? = null
        composeRule.setThemedContent {
            NimazDropdownField(
                items = methods(),
                selected = "mwl",
                onSelected = { picked = it },
                label = "Calculation method",
            )
        }
        composeRule.onNodeWithText("Umm al-Qura").assertDoesNotExist()

        composeRule.onNodeWithText("Muslim World League").performClick()
        composeRule.onNodeWithText("Umm al-Qura").performClick()

        assertThat(picked).isEqualTo("uaq")
    }

    @Test
    fun `a disabled dropdown ignores taps`() {
        var picked: String? = null
        composeRule.setThemedContent {
            NimazDropdownField(
                items = methods(),
                selected = "mwl",
                onSelected = { picked = it },
                enabled = false,
            )
        }
        composeRule.onNodeWithText("Muslim World League").performClick()

        composeRule.onNodeWithText("Umm al-Qura").assertDoesNotExist()
        assertThat(picked).isNull()
    }

    // ── The amount field, on the same shell ────────────────────────────────

    @Test
    fun `the amount input groups thousands as they are typed`() {
        composeRule.setThemedContent {
            var text by remember { mutableStateOf("") }
            NimazAmountInput(
                value = text,
                onValueChange = { text = it },
                currencySymbol = "€",
            )
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("42180")

        composeRule.onNodeWithText("42,180").assertExists()
        composeRule.onNodeWithText("€").assertExists()
    }

    @Test
    fun `a half-typed decimal keeps its point`() {
        // The bug the amount field exists for: parsing every keystroke to a Double ate the "."
        // before the next digit landed, so a decimal amount was unenterable.
        var reported = 0.0
        composeRule.setThemedContent {
            NimazAmountField(
                value = 0.0,
                onValueChange = { reported = it },
                currencySymbol = "£",
            )
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("1284.")

        composeRule.onNodeWithText("1,284.").assertExists()
        assertThat(reported).isEqualTo(1284.0)
    }

    @Test
    fun `a stored amount arrives already formatted and a real change still wins`() {
        var stored by mutableStateOf(42180.5)
        composeRule.setThemedContent {
            NimazAmountField(value = stored, onValueChange = { stored = it })
        }
        composeRule.onNodeWithText("42,180.5").assertExists()

        // "Clear all" — a genuinely different value, so it must overwrite the text.
        stored = 0.0
        composeRule.waitForIdle()

        composeRule.onNodeWithText("42,180.5").assertDoesNotExist()
        composeRule.onNodeWithText("0.00").assertExists()
    }

    @Test
    fun `a weight follows its unit rather than leading with a symbol`() {
        composeRule.setThemedContent {
            NimazAmountInput(
                value = "87.48",
                onValueChange = {},
                unitSuffix = "g",
                placeholder = "0",
            )
        }
        composeRule.onNodeWithText("87.48").assertExists()
        composeRule.onNodeWithText("g").assertExists()
    }

    @Test
    fun `a third decimal is refused rather than rounded away`() {
        composeRule.setThemedContent {
            var text by remember { mutableStateOf("") }
            NimazAmountInput(value = text, onValueChange = { text = it }, currencySymbol = "$")
        }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("10.555")

        composeRule.onNodeWithText("10.55").assertExists()
    }
}
