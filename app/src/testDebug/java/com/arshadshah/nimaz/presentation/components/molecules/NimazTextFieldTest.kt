package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The app's text field, member by member and state by state.
 *
 * The rules worth guarding are the ones the twelve `OutlinedTextField` call sites this replaced
 * each got slightly differently: when an error is allowed to appear, whether a message comes
 * with it, whether an over-long value is cut, and whether a disabled or read-only field can
 * still be typed into.
 */
@RunWith(RobolectricTestRunner::class)
class NimazTextFieldTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /**
     * A field plus a button that drops focus, so a test can blur without depending on another
     * focusable stealing it.
     */
    @Composable
    private fun WithBlurButton(field: @Composable () -> Unit) {
        val focusManager = LocalFocusManager.current
        Column {
            field()
            NimazButton(
                text = "blur",
                onClick = { focusManager.clearFocus() },
                modifier = Modifier.testTag("blur"),
            )
        }
    }

    private fun field() = composeRule.onNode(hasSetTextAction())

    // ── Rendering ──────────────────────────────────────────────────────────

    @Test
    fun `renders its label above the field`() {
        composeRule.setThemedContent {
            NimazTextField(value = "", onValueChange = {}, label = "Preset name")
        }
        composeRule.onNodeWithText("Preset name").assertExists()
    }

    @Test
    fun `shows the placeholder only while empty`() {
        var value by mutableStateOf("")
        composeRule.setThemedContent {
            NimazTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = "Morning adhkar",
            )
        }
        composeRule.onNodeWithText("Morning adhkar").assertExists()

        field().performTextInput("Evening")

        composeRule.onNodeWithText("Morning adhkar").assertDoesNotExist()
        composeRule.onNodeWithText("Evening").assertExists()
    }

    @Test
    fun `the required marker is decorative and the optional one is announced`() {
        // The asterisk carries no information a screen reader can use — "Name asterisk" is
        // noise — so it is cleared out of the semantics tree. "optional" is a real word and
        // stays in it.
        composeRule.setThemedContent {
            Column {
                NimazTextField(
                    value = "",
                    onValueChange = {},
                    label = "Name",
                    required = true,
                )
                NimazTextField(
                    value = "",
                    onValueChange = {},
                    label = "Note",
                    optionalLabel = "optional",
                )
            }
        }
        composeRule.onNodeWithText("Name").assertExists()
        composeRule.onNodeWithText("*", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("optional").assertExists()
    }

    @Test
    fun `renders the helper line, the affixes and a leading icon together`() {
        composeRule.setThemedContent {
            NimazTextField(
                value = "12,480.00",
                onValueChange = {},
                label = "Zakat on savings",
                variant = NimazFieldVariant.NUMERIC,
                helper = "Across every account",
                prefix = "€",
                suffix = "EUR",
                leadingIcon = Icons.Default.Person,
            )
        }
        composeRule.onNodeWithText("Across every account").assertExists()
        composeRule.onNodeWithText("€").assertExists()
        composeRule.onNodeWithText("EUR").assertExists()
        composeRule.onNodeWithText("12,480.00").assertExists()
    }

    @Test
    fun `every variant renders its value`() {
        composeRule.setThemedContent {
            Column {
                NimazTextField(value = "plain", onValueChange = {})
                NimazTextField(
                    value = "سُبْحَانَ",
                    onValueChange = {},
                    variant = NimazFieldVariant.ARABIC,
                )
                NimazTextField(
                    value = "86.40",
                    onValueChange = {},
                    variant = NimazFieldVariant.NUMERIC,
                    density = NimazFieldDensity.COMPACT,
                )
                NimazTextField(
                    value = "a longer thought",
                    onValueChange = {},
                    variant = NimazFieldVariant.NOTE,
                )
            }
        }
        composeRule.onNodeWithText("plain").assertExists()
        composeRule.onNodeWithText("سُبْحَانَ").assertExists()
        composeRule.onNodeWithText("86.40").assertExists()
        composeRule.onNodeWithText("a longer thought").assertExists()
    }

    // ── Typing and clearing ────────────────────────────────────────────────

    @Test
    fun `typing reports every keystroke`() {
        val seen = mutableListOf<String>()
        composeRule.setThemedContent {
            var value by remember { mutableStateOf("") }
            NimazTextField(
                value = value,
                onValueChange = {
                    value = it
                    seen += it
                },
            )
        }
        field().performTextInput("a")
        field().performTextInput("b")
        assertThat(seen).containsExactly("a", "ab").inOrder()
    }

    @Test
    fun `the clear button appears with text and empties the field`() {
        composeRule.setThemedContent {
            var value by remember { mutableStateOf("") }
            NimazTextField(value = value, onValueChange = { value = it })
        }
        composeRule.onNodeWithContentDescription("Clear").assertDoesNotExist()

        field().performTextInput("Morning adhkar")
        composeRule.onNodeWithContentDescription("Clear").performClick()

        composeRule.onNodeWithText("Morning adhkar").assertDoesNotExist()
    }

    @Test
    fun `a note has no clear button - one tap must not destroy a paragraph`() {
        composeRule.setThemedContent {
            NimazTextField(
                value = "Ask about the weight of speech here.",
                onValueChange = {},
                variant = NimazFieldVariant.NOTE,
            )
        }
        composeRule.onNodeWithContentDescription("Clear").assertDoesNotExist()
    }

    @Test
    fun `tapping the box padding reaches the field, not nothing`() {
        // The caret occupies the middle ~26dp of a 50dp box. Without the shell's rippleless
        // inner tap target, a tap on the field's own padding does nothing and the field reads
        // as broken at its edges — which Material's OutlinedTextField never did.
        composeRule.setThemedContent {
            var value by remember { mutableStateOf("") }
            NimazTextField(
                value = value,
                onValueChange = { value = it },
                label = "Preset name",
            )
        }
        // The clickable-but-not-editable node is the box's own tap target, which spans the
        // full 50dp; the editable node inside it is only the ~26dp caret row.
        composeRule.onNode(hasClickAction() and !hasSetTextAction()).performClick()

        field().assertIsFocused()
    }

    // ── Counter and the limit ──────────────────────────────────────────────

    @Test
    fun `the counter tracks what has been typed`() {
        composeRule.setThemedContent {
            var value by remember { mutableStateOf("") }
            NimazTextField(value = value, onValueChange = { value = it }, maxLength = 40)
        }
        composeRule.onNodeWithText("0 / 40").assertExists()

        field().performTextInput("abc")

        composeRule.onNodeWithText("3 / 40").assertExists()
    }

    @Test
    fun `going over the limit is marked, never truncated`() {
        // Cutting off what somebody typed is worse than letting them cut it: the value keeps
        // every character and the counter reports the overrun.
        var value = ""
        composeRule.setThemedContent {
            var text by remember { mutableStateOf("") }
            NimazTextField(
                value = text,
                onValueChange = {
                    text = it
                    value = it
                },
                maxLength = 5,
            )
        }
        field().performTextInput("abcdefgh")

        assertThat(value).isEqualTo("abcdefgh")
        composeRule.onNodeWithText("8 / 5").assertExists()
        composeRule.onNodeWithText("abcdefgh").assertExists()
    }

    // ── Errors ─────────────────────────────────────────────────────────────

    @Test
    fun `an error replaces the helper rather than stacking under it`() {
        composeRule.setThemedContent {
            NimazTextField(
                value = "",
                onValueChange = {},
                helper = "Shown in your list of presets",
                error = "A name is needed.",
            )
        }
        composeRule.onNodeWithText("A name is needed.").assertExists()
        composeRule.onNodeWithText("Shown in your list of presets").assertDoesNotExist()
    }

    @Test
    fun `a field nobody has touched is not in error`() {
        // onFocusChanged reports "not focused" on the first composition too. Treating that as a
        // blur would red-flag every empty required field the moment a form appeared.
        composeRule.setThemedContent {
            WithBlurButton {
                NimazTextField(
                    value = "",
                    onValueChange = {},
                    required = true,
                    validator = { if (it.isBlank()) "A name is needed." else null },
                )
            }
        }
        composeRule.onNodeWithText("A name is needed.").assertDoesNotExist()
    }

    @Test
    fun `the validator does not fire on the first keystroke`() {
        composeRule.setThemedContent {
            var value by remember { mutableStateOf("") }
            NimazTextField(
                value = value,
                onValueChange = { value = it },
                validator = { if (it.length < 3) "At least 3 characters." else null },
            )
        }
        field().performTextInput("a")
        composeRule.onNodeWithText("At least 3 characters.").assertDoesNotExist()
    }

    @Test
    fun `the validator fires on blur`() {
        composeRule.setThemedContent {
            WithBlurButton {
                var value by remember { mutableStateOf("") }
                NimazTextField(
                    value = value,
                    onValueChange = { value = it },
                    validator = { if (it.length < 3) "At least 3 characters." else null },
                )
            }
        }
        field().performClick()
        field().performTextInput("ab")
        composeRule.onNodeWithTag("blur").performClick()

        composeRule.onNodeWithText("At least 3 characters.").assertExists()
    }

    @Test
    fun `an error already on screen clears as it is fixed`() {
        composeRule.setThemedContent {
            WithBlurButton {
                var value by remember { mutableStateOf("") }
                NimazTextField(
                    value = value,
                    onValueChange = { value = it },
                    validator = { if (it.length < 3) "At least 3 characters." else null },
                )
            }
        }
        field().performClick()
        field().performTextInput("ab")
        composeRule.onNodeWithTag("blur").performClick()
        composeRule.onNodeWithText("At least 3 characters.").assertExists()

        field().performTextReplacement("abc")

        composeRule.onNodeWithText("At least 3 characters.").assertDoesNotExist()
    }

    @Test
    fun `a caller's error wins over the validator's`() {
        // The screen knows things the field does not — a failed save, a name already taken.
        composeRule.setThemedContent {
            WithBlurButton {
                var value by remember { mutableStateOf("") }
                NimazTextField(
                    value = value,
                    onValueChange = { value = it },
                    error = "That name is already used.",
                    validator = { "At least 3 characters." },
                )
            }
        }
        field().performClick()
        composeRule.onNodeWithTag("blur").performClick()

        composeRule.onNodeWithText("That name is already used.").assertExists()
        composeRule.onNodeWithText("At least 3 characters.").assertDoesNotExist()
    }

    // ── Disabled and read-only ─────────────────────────────────────────────

    @Test
    fun `a disabled field shows its value and cannot be typed into`() {
        composeRule.setThemedContent {
            NimazTextField(
                value = "Set by your region",
                onValueChange = {},
                label = "Fidya rate",
                enabled = false,
            )
        }
        composeRule.onNodeWithText("Set by your region").assertExists()
        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Clear").assertDoesNotExist()
    }

    @Test
    fun `a read-only field shows its value and cannot be typed into`() {
        composeRule.setThemedContent {
            NimazTextField(
                value = "€312.00",
                onValueChange = {},
                label = "Calculated from your entries",
                readOnly = true,
            )
        }
        composeRule.onNodeWithText("€312.00").assertExists()
        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Clear").assertDoesNotExist()
    }
}
