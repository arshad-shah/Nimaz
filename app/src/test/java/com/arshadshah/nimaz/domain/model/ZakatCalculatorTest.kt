package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [ZakatCalculator] — the pure wealth-calculation logic that
 * determines how much Zakat a user owes. Bugs here produce religiously
 * significant wrong answers, so the nisab thresholds, the 2.5% rate and the
 * asset/liability netting are all pinned down explicitly.
 */
class ZakatCalculatorTest {

    private val noLiabilities = ZakatLiabilities()

    // ── Nisab value derivation ──────────────────────────────────────

    @Test
    fun `gold nisab value is 87_48 grams times metal price`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 10_000.0),
            liabilities = noLiabilities,
            nisabType = NisabType.GOLD,
            metalPricePerGram = 70.0,
            currency = "USD"
        )
        assertThat(result.nisabValue).isWithin(1e-9).of(87.48 * 70.0)
    }

    @Test
    fun `silver nisab value is 612_36 grams times metal price`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 10_000.0),
            liabilities = noLiabilities,
            nisabType = NisabType.SILVER,
            metalPricePerGram = 0.8,
            currency = "USD"
        )
        assertThat(result.nisabValue).isWithin(1e-9).of(612.36 * 0.8)
    }

    // ── Zakat due (2.5% of net worth above nisab) ───────────────────

    @Test
    fun `zakat due is 2_5 percent of net worth when above nisab`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 10_000.0),
            liabilities = noLiabilities,
            nisabType = NisabType.GOLD,
            metalPricePerGram = 70.0, // nisab = 6123.6, below 10000
            currency = "USD"
        )
        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.zakatDue).isWithin(1e-9).of(10_000.0 * 0.025)
        assertThat(result.zakatableAmount).isWithin(1e-9).of(10_000.0)
    }

    @Test
    fun `no zakat is due when net worth is below nisab`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 5_000.0),
            liabilities = noLiabilities,
            nisabType = NisabType.GOLD,
            metalPricePerGram = 70.0, // nisab = 6123.6, above 5000
            currency = "USD"
        )
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `net worth exactly equal to nisab is above threshold and owes zakat`() {
        // nisab = 87.48 * 100 = 8748, net worth = 8748 exactly
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 8_748.0),
            liabilities = noLiabilities,
            nisabType = NisabType.GOLD,
            metalPricePerGram = 100.0,
            currency = "USD"
        )
        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.zakatDue).isWithin(1e-9).of(8_748.0 * 0.025)
    }

    // ── Asset / liability netting ───────────────────────────────────

    @Test
    fun `liabilities are subtracted from assets to get net worth`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 10_000.0, bankBalance = 5_000.0),
            liabilities = ZakatLiabilities(debts = 3_000.0, loans = 2_000.0),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 70.0,
            currency = "USD"
        )
        assertThat(result.totalAssets).isWithin(1e-9).of(15_000.0)
        assertThat(result.totalLiabilities).isWithin(1e-9).of(5_000.0)
        assertThat(result.netWorth).isWithin(1e-9).of(10_000.0)
    }

    @Test
    fun `net worth is clamped to zero when liabilities exceed assets`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 1_000.0),
            liabilities = ZakatLiabilities(debts = 3_000.0),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 70.0,
            currency = "USD"
        )
        assertThat(result.netWorth).isEqualTo(0.0)
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `metal grams are excluded from the cash-style asset total`() {
        // ZakatAssets.total intentionally omits goldGrams / silverGrams because
        // they must be converted to value separately.
        val assets = ZakatAssets(goldGrams = 100.0, silverGrams = 500.0)
        assertThat(assets.total).isEqualTo(0.0)
    }

    // ── Pass-through metadata ───────────────────────────────────────

    @Test
    fun `currency and nisab type are carried through to the result`() {
        val result = ZakatCalculator.calculate(
            assets = ZakatAssets(cashOnHand = 10_000.0),
            liabilities = noLiabilities,
            nisabType = NisabType.SILVER,
            metalPricePerGram = 0.8,
            currency = "GBP"
        )
        assertThat(result.currency).isEqualTo("GBP")
        assertThat(result.nisabType).isEqualTo(NisabType.SILVER)
    }

    @Test
    fun `zakat rate constant is 2_5 percent`() {
        assertThat(ZakatCalculator.ZAKAT_RATE).isEqualTo(0.025)
        assertThat(ZakatCalculator.GOLD_NISAB_GRAMS).isEqualTo(87.48)
        assertThat(ZakatCalculator.SILVER_NISAB_GRAMS).isEqualTo(612.36)
    }
}
