package com.arshadshah.nimaz.domain.model

/**
 * Starting values for the Zakat calculator's metal prices.
 *
 * These were previously literals on `ZakatCalculatorUiState` that **no user could
 * change** — the events that would have changed them were emitted by no screen — so
 * every zakat figure the app produced was wrong by however stale they had become.
 * `ZakatCalculator` derives both the metal valuation *and* the nisab threshold from
 * them, so near the threshold the error changed the answer to "is any zakat due at
 * all", not just the amount.
 *
 * They are now only *defaults*: the real values live in `SettingsRepository` and are
 * editable. Treat these as a starting point to be corrected by the user, never as a
 * current market rate — the UI should say as much.
 */
object ZakatDefaults {
    const val GOLD_PRICE_PER_GRAM: Double = 65.0
    const val SILVER_PRICE_PER_GRAM: Double = 0.80
    const val CURRENCY: String = "USD"

    /**
     * The currencies offered by the calculator, as ISO 4217 codes.
     *
     * Codes only — no names. `java.util.Currency` resolves both the display name and the
     * symbol in the user's locale, so a German reader picking `USD` sees "US-Dollar", and
     * nothing here has to be translated. Curated rather than
     * `Currency.getAvailableCurrencies()`, which is ~150 entries deep in codes nobody is
     * calculating zakat in.
     */
    val CURRENCIES: List<String> = listOf(
        "USD", "EUR", "GBP", "CAD", "AUD", "CHF", "JPY", "CNY",
        "SAR", "AED", "QAR", "KWD", "BHD", "OMR", "JOD", "EGP",
        "PKR", "INR", "BDT", "IDR", "MYR", "TRY", "NGN", "ZAR",
        "MAD", "DZD", "TND", "LKR", "SGD", "SEK", "NOK", "DKK",
    )
}
