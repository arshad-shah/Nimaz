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

object ZakatConstants {
    const val ZAKAT_RATE = 0.025 // 2.5%
    const val GOLD_NISAB_GRAMS = 87.48
    const val SILVER_NISAB_GRAMS = 612.36
}

data class ZakatHistory(
    val calculations: List<ZakatCalculation>,
    val totalZakatPaid: Double,
    val lastCalculationDate: Long?
)

// Calculator helper
object ZakatCalculator {
    const val ZAKAT_RATE = 0.025 // 2.5%
    const val GOLD_NISAB_GRAMS = 87.48
    const val SILVER_NISAB_GRAMS = 612.36

    fun calculate(
        assets: ZakatAssets,
        liabilities: ZakatLiabilities,
        nisabType: NisabType,
        metalPricePerGram: Double,
        currency: String
    ): ZakatCalculation {
        val nisabValue = when (nisabType) {
            NisabType.GOLD -> GOLD_NISAB_GRAMS * metalPricePerGram
            NisabType.SILVER -> SILVER_NISAB_GRAMS * metalPricePerGram
        }

        val totalAssets = assets.total
        val totalLiabilities = liabilities.total
        val netWorth = maxOf(0.0, totalAssets - totalLiabilities)
        val isAboveNisab = netWorth >= nisabValue
        val zakatDue = if (isAboveNisab) {
            netWorth * ZAKAT_RATE
        } else {
            0.0
        }

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
            currency = currency,
            note = null
        )
    }
}
