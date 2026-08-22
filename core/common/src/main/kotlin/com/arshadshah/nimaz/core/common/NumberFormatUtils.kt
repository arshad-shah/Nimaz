package com.arshadshah.nimaz.core.common

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

/**
 * The bare symbol for an ISO 4217 code — `€` for `EUR` — falling back to the code itself.
 *
 * The same resolution [formatCurrency] performs, so the symbol inside an input field and the
 * symbol beside the total cannot disagree. A code with no symbol on this device renders as the
 * code, which is still true and still readable.
 *
 * It lived in `ZakatCalculatorScreen` while the calculator was the only screen editing money;
 * the zakat *settings* screen needs the same symbol on its price fields, and two screens sharing
 * one private helper by reaching across packages is how the copies start.
 */
fun currencySymbolOf(code: String): String = runCatching {
    Currency.getInstance(code).getSymbol(Locale.getDefault())
}.getOrDefault(code)

/**
 * A currency's full label — "US Dollar ($)" in English, "US-Dollar ($)" in German.
 *
 * Resolved by [java.util.Currency] from the ISO code, so the currency picker carries no
 * translated strings of its own. When the device has no symbol for a code, `Currency` returns
 * the code as the symbol; repeating it in brackets would read as "Cayman Islands Dollar (KYD)",
 * so the bracket is dropped in that case.
 */
fun currencyLabel(code: String): String = runCatching {
    val currency = Currency.getInstance(code)
    val name = currency.getDisplayName(Locale.getDefault())
    val symbol = currency.getSymbol(Locale.getDefault())
    if (symbol == code) name else "$name ($symbol)"
}.getOrDefault(code)

/** Formats [value] with locale grouping separators, e.g. `1,234,567`. */
fun formatGrouped(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())
