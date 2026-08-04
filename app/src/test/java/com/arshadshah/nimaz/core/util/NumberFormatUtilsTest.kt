package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `formatCurrency(amount, currency)` used to ignore its `currency` argument entirely
 * (`@Suppress("UNUSED_PARAMETER")`) and always format as US dollars. Every Zakat
 * figure therefore rendered with a `$`, whatever the user had chosen — and the Zakat
 * redesign in #368 puts those figures on a shareable card, where a wrong currency
 * symbol travels.
 */
class NumberFormatUtilsTest {

    @Test
    fun `the currency argument is honoured, not ignored`() {
        val euros = formatCurrency(1_234.56, "EUR")
        val dollars = formatCurrency(1_234.56, "USD")

        assertThat(euros).isNotEqualTo(dollars)
        assertThat(euros).contains("€")
        assertThat(dollars).contains("$")
    }

    @Test
    fun `an unknown currency code falls back rather than throwing`() {
        // Codes reach this from persisted preferences and from synced payloads, so a
        // value this build does not recognise must not crash the calculator.
        val formatted = formatCurrency(10.0, "NOT_A_CODE")

        assertThat(formatted).contains("10")
    }

    @Test
    fun `sub-unit amounts keep their decimals`() {
        // The silver price is 0.80/g. Rendering it through toInt() showed "0", which is
        // what the nisab subtitle used to display.
        assertThat(formatCurrency(0.80, "USD")).contains("0.80")
    }

    @Test
    fun `the single-argument overload still formats US dollars`() {
        assertThat(formatCurrency(1_234.56)).contains("$")
    }
}
