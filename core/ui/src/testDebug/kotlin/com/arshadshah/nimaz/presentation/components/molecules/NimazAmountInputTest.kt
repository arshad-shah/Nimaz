package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The money field, and the two composables over it.
 *
 * `formatAmountInput` and its friends carry their reasoning in KDoc and had no test: the awkward
 * cases are all *mid-typing*, which a finished-value formatter never sees, and every one of them
 * has a bug behind it. A field that parsed each keystroke to a `Double` made a decimal amount
 * literally unenterable, because `"10."` became `"10"` before the next digit landed — and a
 * per-gram silver price is nothing but decimals.
 *
 * [NimazAmountField]'s sync guard is the other half. It only overwrites its local text when the
 * incoming `Double` disagrees with what that text already parses to, so the ViewModel echoing a
 * keystroke back cannot erase a trailing point, while "Clear all" still wins.
 */
@RunWith(RobolectricTestRunner::class)
class NimazAmountInputTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `thousands are grouped as you type`() {
        assertThat(formatAmountInput("42180")).isEqualTo("42,180")
        assertThat(formatAmountInput("1234567")).isEqualTo("1,234,567")
        assertThat(formatAmountInput("999")).isEqualTo("999")
    }

    @Test
    fun `formatting its own output again changes nothing`() {
        // The field feeds this its previous output on every keystroke. Without the
        // separator-stripping first pass the commas would multiply as you type.
        val once = formatAmountInput("42180.50")
        assertThat(formatAmountInput(once)).isEqualTo(once)
        assertThat(once).isEqualTo("42,180.50")
    }

    @Test
    fun `a trailing decimal point survives, because someone is still typing`() {
        assertThat(formatAmountInput("1284.")).isEqualTo("1,284.")
        assertThat(parseAmountInput("1,284.")).isEqualTo(1284.0)
    }

    @Test
    fun `a third decimal is refused where it was typed rather than rounded away later`() {
        // Rounding someone's money without telling them is the worst available option.
        assertThat(formatAmountInput("12.345")).isEqualTo("12.34")
    }

    @Test
    fun `a second decimal point is dropped, keeping what was already typed`() {
        assertThat(formatAmountInput("12.3.4")).isEqualTo("12.34")
    }

    @Test
    fun `pasted currency symbols and letters are dropped rather than rejecting the string`() {
        assertThat(formatAmountInput("$1,200 usd")).isEqualTo("1,200")
    }

    @Test
    fun `an empty or symbol-only string stays empty`() {
        assertThat(formatAmountInput("")).isEmpty()
        assertThat(formatAmountInput("abc")).isEmpty()
        assertThat(parseAmountInput("")).isEqualTo(0.0)
    }

    @Test
    fun `zero renders as empty, so an untouched row shows its placeholder`() {
        // A literal "0" invites the user to delete it before typing.
        assertThat(amountToInput(0.0)).isEmpty()
    }

    @Test
    fun `a whole amount loses the decimals nobody typed`() {
        assertThat(amountToInput(1200.0)).isEqualTo("1,200")
        assertThat(amountToInput(87.48)).isEqualTo("87.48")
    }

    @Test
    fun `the input passes grouped text back to its caller`() {
        var seen: String? = null
        composeRule.setThemedContent {
            NimazAmountInput(value = "", onValueChange = { seen = it }, currencySymbol = "$")
        }
        composeRule.onNodeWithText("0.00").performTextReplacement("42180")
        assertThat(seen).isEqualTo("42,180")
    }

    @Test
    fun `the bound field reports the parsed number, not the text`() {
        var seen: Double? = null
        composeRule.setThemedContent {
            NimazAmountField(value = 0.0, onValueChange = { seen = it }, unitSuffix = "g")
        }
        composeRule.onNodeWithText("0.00").performTextReplacement("87.48")
        assertThat(seen).isEqualTo(87.48)
    }

    @Test
    fun `the bound field starts from its stored amount`() {
        composeRule.setThemedContent {
            NimazAmountField(value = 42180.5, onValueChange = {}, currencySymbol = "$")
        }
        composeRule.onNodeWithText("42,180.5").assertExists()
    }
}
