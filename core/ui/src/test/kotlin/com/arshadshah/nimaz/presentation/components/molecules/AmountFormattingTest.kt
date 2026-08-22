package com.arshadshah.nimaz.presentation.components.molecules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a person may type into a money field, and what it should read back as. Grouping is
 * applied as you type, so the rule has to survive a partially-typed number — not just a
 * finished one.
 */
class AmountFormattingTest {

    @Test
    fun `thousands are grouped as you type`() {
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "42180"
            )
        ).isEqualTo("42,180")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "1234567"
            )
        ).isEqualTo("1,234,567")
    }

    @Test
    fun `a decimal point survives and is never grouped`() {
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "42180.5"
            )
        ).isEqualTo("42,180.5")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "42180.50"
            )
        ).isEqualTo("42,180.50")
    }

    @Test
    fun `a trailing point is kept so the next keystroke lands after it`() {
        // The field this replaced parsed every keystroke to a Double and re-rendered it, so
        // the point vanished the instant you typed it and a decimal amount was unenterable.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "42180."
            )
        ).isEqualTo("42,180.")
    }

    @Test
    fun `more than two decimals are refused rather than silently rounded`() {
        // Rounding someone's money without telling them is worse than not accepting the key.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "10.555"
            )
        ).isEqualTo("10.55")
    }

    @Test
    fun `junk is dropped, not rejected wholesale`() {
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "4a2b1"
            )
        ).isEqualTo("421")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                ""
            )
        ).isEqualTo("")
    }

    @Test
    fun `a second decimal point is ignored`() {
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "10.5.5"
            )
        ).isEqualTo("10.55")
    }

    @Test
    fun `already-grouped text re-groups to the same thing`() {
        // The field is fed its own output on every keystroke, so the rule has to be
        // idempotent — otherwise the separators multiply as you type.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                "42,180"
            )
        ).isEqualTo("42,180")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                    "1234567"
                )
            )
        ).isEqualTo("1,234,567")
    }

    @Test
    fun `a leading point is left alone rather than grown a zero`() {
        // Someone mid-typing ".5" has not finished. Inserting the 0 they did not type moves
        // their cursor, and they were going to be at "0.5" in one more keystroke anyway.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.formatAmountInput(
                ".5"
            )
        ).isEqualTo(".5")
    }

    @Test
    fun `parsing reverses the grouping`() {
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                "42,180.50"
            )
        ).isEqualTo(42180.50)
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                "1,234,567"
            )
        ).isEqualTo(1234567.0)
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                ".5"
            )
        ).isEqualTo(0.5)
    }

    @Test
    fun `a half-typed amount parses to the number so far, not to zero`() {
        // "42,180." is a real intermediate state and the calculator recomputes on every
        // keystroke. Parsing it to 0.0 would blank the running total mid-entry.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                "42,180."
            )
        ).isEqualTo(42180.0)
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                ""
            )
        ).isEqualTo(0.0)
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                "."
            )
        ).isEqualTo(0.0)
    }

    @Test
    fun `a stored amount renders without trailing noise`() {
        // What a saved Double looks like when it comes back into the field. 42180.0 is
        // "42,180", not "42,180.0" — the zero decimals are not something anyone typed.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.amountToInput(
                42180.0
            )
        ).isEqualTo("42,180")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.amountToInput(
                42180.5
            )
        ).isEqualTo("42,180.5")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.amountToInput(
                1284.55
            )
        ).isEqualTo("1,284.55")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.amountToInput(
                0.0
            )
        ).isEqualTo("")
    }

    @Test
    fun `a stored amount round-trips through the field`() {
        for (amount in listOf(0.0, 0.5, 65.0, 1284.55, 42180.0, 1234567.89)) {
            assertThat(
                _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.parseAmountInput(
                    _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.amountToInput(
                        amount
                    )
                )
            ).isEqualTo(amount)
        }
    }
}
