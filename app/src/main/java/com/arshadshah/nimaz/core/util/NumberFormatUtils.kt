package com.arshadshah.nimaz.core.util

import java.text.NumberFormat
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
 * Overload that accepts a [currency] code. The code is currently unused (the
 * app formats everything with the US locale); the parameter is kept so the
 * Zakat calculator call sites read naturally and so a real per-currency
 * implementation can be slotted in later without touching callers.
 */
@Suppress("UNUSED_PARAMETER")
fun formatCurrency(amount: Double, currency: String): String =
    formatCurrency(amount)

/** Formats [value] with locale grouping separators, e.g. `1,234,567`. */
fun formatGrouped(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())
