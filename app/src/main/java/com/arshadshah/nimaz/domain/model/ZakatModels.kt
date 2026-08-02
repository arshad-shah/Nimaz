package com.arshadshah.nimaz.domain.model

data class ZakatCalculation(
    val id: Long = 0,
    val calculatedAt: Long = System.currentTimeMillis(),
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val nisabType: NisabType,
    val nisabValue: Double,
    val isAboveNisab: Boolean,
    val zakatableAmount: Double = netWorth,
    val zakatDue: Double,
    val goldValue: Double = 0.0,
    val silverValue: Double = 0.0,
    val currency: String = "USD",
    val note: String? = null
)

/** A persisted zakat calculation record, including payment status. */
data class ZakatHistoryEntry(
    val id: Long = 0,
    val calculatedAt: Long,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val zakatDue: Double,
    val nisabType: NisabType,
    val nisabValue: Double,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val notes: String? = null
)

data class ZakatAssets(
    val cashOnHand: Double = 0.0,
    val bankBalance: Double = 0.0,
    val goldGrams: Double = 0.0,
    val silverGrams: Double = 0.0,
    val investments: Double = 0.0,
    val businessInventory: Double = 0.0,
    val receivables: Double = 0.0,
    val rentalIncome: Double = 0.0,
    val otherAssets: Double = 0.0
) {
    val total: Double
        get() = cashOnHand + bankBalance + investments + businessInventory +
                receivables + rentalIncome + otherAssets
    // Note: goldGrams and silverGrams need to be converted to value separately
}

data class ZakatLiabilities(
    val debts: Double = 0.0,
    val loans: Double = 0.0,
    val billsDue: Double = 0.0,
    val otherLiabilities: Double = 0.0
) {
    val total: Double
        get() = debts + loans + billsDue + otherLiabilities
}

enum class NisabType {
    GOLD,
    SILVER;

    fun displayName(): String {
        return when (this) {
            GOLD -> "Gold (87.48g)"
            SILVER -> "Silver (612.36g)"
        }
    }

    fun weightInGrams(): Double {
        return when (this) {
            GOLD -> 87.48    // 7.5 tola
            SILVER -> 612.36 // 52.5 tola
        }
    }
}

data class MetalPrice(
    val type: MetalType,
    val pricePerGram: Double,
    val currency: String,
    val lastUpdated: Long
)

enum class MetalType {
    GOLD,
    SILVER
}

data class ZakatHistory(
    val calculations: List<ZakatCalculation>,
    val totalZakatPaid: Double,
    val lastCalculationDate: Long?
)

/**
 * The one zakat calculation.
 *
 * It used to be two: this object, and an inline copy inside `ZakatViewModel`. Only the
 * ViewModel's ran — and this one was wrong, because it took `assets.total`, which by design
 * excludes gold and silver (the grams have to be priced first). A caller would have got a
 * figure that ignored the user's precious metals entirely. A wrong calculator that nothing
 * calls is still a trap: it type-checks, it reads as canonical, and it sits in the domain
 * layer where the next person will reach for it.
 *
 * Metal prices are per gram and in the same currency as every other amount.
 */
object ZakatCalculator {
    const val ZAKAT_RATE = 0.025 // 2.5%
    const val GOLD_NISAB_GRAMS = 87.48
    const val SILVER_NISAB_GRAMS = 612.36

    fun calculate(
        assets: ZakatAssets,
        liabilities: ZakatLiabilities,
        nisabType: NisabType,
        goldPricePerGram: Double,
        silverPricePerGram: Double,
        currency: String
    ): ZakatCalculation {
        val goldValue = assets.goldGrams * goldPricePerGram
        val silverValue = assets.silverGrams * silverPricePerGram

        // `assets.total` is the cash-like holdings only; the metals are priced here.
        val totalAssets = assets.total + goldValue + silverValue
        val totalLiabilities = liabilities.total

        // Not clamped at zero: someone who owes more than they own should see that, not a
        // break-even 0. It cannot make them liable — a negative net worth is below any
        // positive threshold.
        val netWorth = totalAssets - totalLiabilities

        val nisabValue = when (nisabType) {
            NisabType.GOLD -> GOLD_NISAB_GRAMS * goldPricePerGram
            NisabType.SILVER -> SILVER_NISAB_GRAMS * silverPricePerGram
        }

        // A threshold priced at zero has not been *met*, it has failed to be established —
        // and `netWorth >= 0.0` would otherwise make every user liable, including one who
        // owns nothing.
        val isAboveNisab = nisabValue > 0.0 && netWorth >= nisabValue
        val zakatDue = if (isAboveNisab) netWorth * ZAKAT_RATE else 0.0

        return ZakatCalculation(
            calculatedAt = System.currentTimeMillis(),
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            nisabType = nisabType,
            nisabValue = nisabValue,
            isAboveNisab = isAboveNisab,
            zakatableAmount = netWorth,
            zakatDue = zakatDue,
            goldValue = goldValue,
            silverValue = silverValue,
            currency = currency,
            note = null
        )
    }
}
