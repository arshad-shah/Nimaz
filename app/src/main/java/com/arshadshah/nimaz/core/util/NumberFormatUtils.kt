package com.arshadshah.nimaz.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Shared number-formatting helpers.
 *
 * Previously `formatCurrency` was copy-pasted as a private top-level function in
 * both Zakat screens (ZakatCalculatorScreen / ZakatHistoryScreen). Centralised
 * here so the formatting stays consistent across the app.
 */

/** Formats [amount] as a US-locale currency string, e.g. `"$1,234.56"`. */
fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(amount)

/**
 * Formats [amount] in [currency] (an ISO 4217 code such as `"USD"` or `"EUR"`).
 *
 * This used to ignore [currency] and always format US dollars, so every Zakat figure
 * rendered with a `$` whatever the user had chosen. Grouping and decimal separators
 * follow the device locale — a German user sees `1.234,56 €` — while the *symbol*
 * follows the code, which is the pairing people expect.
 *
 * An unrecognised code falls back to plain grouped digits with the code alongside,
 * rather than throwing: codes arrive from persisted preferences and from synced
 * payloads, and a value this build does not know must not break the calculator.
 */
fun formatCurrency(amount: Double, currency: String): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return runCatching {
        format.currency = Currency.getInstance(currency)
        format.format(amount)
    }.getOrElse {
        "${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)} $currency"
    }
}

/** Formats [value] with locale grouping separators, e.g. `1,234,567`. */
fun formatGrouped(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())
