package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the Zakat money math: nisab thresholds, the 2.5% rate, and the
 * assets − liabilities net-worth calculation. Financial correctness matters, so
 * the boundaries (exactly at nisab, negative net worth) are covered explicitly.
 */
class ZakatCalculatorTest {

    private val tolerance = 1e-6

    private fun assets(
        cashOnHand: Double = 0.0,
        bankBalance: Double = 0.0,
        goldGrams: Double = 0.0,
        silverGrams: Double = 0.0,
        investments: Double = 0.0,
        businessInventory: Double = 0.0,
        receivables: Double = 0.0,
        rentalIncome: Double = 0.0,
        otherAssets: Double = 0.0
    ) = ZakatAssets(
        cashOnHand, bankBalance, goldGrams, silverGrams,
        investments, businessInventory, receivables, rentalIncome, otherAssets
    )

    // ── Constants ───────────────────────────────────────────────────────────

    @Test
    fun `zakat rate is two and a half percent`() {
        assertThat(ZakatCalculator.ZAKAT_RATE).isEqualTo(0.025)
        assertThat(ZakatConstants.ZAKAT_RATE).isEqualTo(0.025)
    }

    @Test
    fun `nisab gram constants match the canonical gold and silver weights`() {
        assertThat(ZakatCalculator.GOLD_NISAB_GRAMS).isEqualTo(87.48)
        assertThat(ZakatCalculator.SILVER_NISAB_GRAMS).isEqualTo(612.36)
        assertThat(ZakatConstants.GOLD_NISAB_GRAMS).isEqualTo(87.48)
        assertThat(ZakatConstants.SILVER_NISAB_GRAMS).isEqualTo(612.36)
    }

    @Test
    fun `NisabType reports the correct weight in grams`() {
        assertThat(NisabType.GOLD.weightInGrams()).isEqualTo(87.48)
        assertThat(NisabType.SILVER.weightInGrams()).isEqualTo(612.36)
    }

    // ── ZakatAssets / ZakatLiabilities totals ───────────────────────────────

    @Test
    fun `asset total sums the monetary fields but excludes raw metal grams`() {
        val a = assets(
            cashOnHand = 100.0,
            bankBalance = 200.0,
            goldGrams = 50.0,        // intentionally excluded from total
            silverGrams = 500.0,     // intentionally excluded from total
            investments = 300.0,
            businessInventory = 400.0,
            receivables = 500.0,
            rentalIncome = 600.0,
            otherAssets = 700.0
        )
        // 100 + 200 + 300 + 400 + 500 + 600 + 700 = 2800 (grams not included).
        assertThat(a.total).isWithin(tolerance).of(2800.0)
    }

    @Test
    fun `liability total sums all liability fields`() {
        val l = ZakatLiabilities(debts = 100.0, loans = 200.0, billsDue = 50.0, otherLiabilities = 25.0)
        assertThat(l.total).isWithin(tolerance).of(375.0)
    }

    // ── calculate: above nisab ──────────────────────────────────────────────

    @Test
    fun `wealth above gold nisab owes 2_5 percent of net worth`() {
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 10_000.0),
            liabilities = ZakatLiabilities(),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 60.0, // gold nisab = 87.48 * 60 = 5248.8
            currency = "USD"
        )
        assertThat(result.nisabValue).isWithin(tolerance).of(87.48 * 60.0)
        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.netWorth).isWithin(tolerance).of(10_000.0)
        assertThat(result.zakatDue).isWithin(tolerance).of(250.0)
    }

    @Test
    fun `silver nisab uses the silver gram weight`() {
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 1_000.0),
            liabilities = ZakatLiabilities(),
            nisabType = NisabType.SILVER,
            metalPricePerGram = 0.8, // silver nisab = 612.36 * 0.8 = 489.888
            currency = "USD"
        )
        assertThat(result.nisabValue).isWithin(tolerance).of(612.36 * 0.8)
        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.zakatDue).isWithin(tolerance).of(25.0)
    }

    // ── calculate: at and below nisab ───────────────────────────────────────

    @Test
    fun `wealth below nisab owes nothing`() {
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 1_000.0),
            liabilities = ZakatLiabilities(),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 60.0, // nisab 5248.8 > 1000
            currency = "USD"
        )
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `net worth exactly at nisab is considered above nisab`() {
        // Choose a price so the gold nisab equals exactly 10,000.
        val price = 10_000.0 / 87.48
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 10_000.0),
            liabilities = ZakatLiabilities(),
            nisabType = NisabType.GOLD,
            metalPricePerGram = price,
            currency = "USD"
        )
        assertThat(result.nisabValue).isWithin(1e-6).of(10_000.0)
        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.zakatDue).isWithin(tolerance).of(250.0)
    }

    // ── calculate: liabilities and clamping ─────────────────────────────────

    @Test
    fun `liabilities reduce net worth below nisab`() {
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 10_000.0),
            liabilities = ZakatLiabilities(debts = 8_000.0),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 60.0, // nisab 5248.8; net worth 2000 < nisab
            currency = "USD"
        )
        assertThat(result.totalAssets).isWithin(tolerance).of(10_000.0)
        assertThat(result.totalLiabilities).isWithin(tolerance).of(8_000.0)
        assertThat(result.netWorth).isWithin(tolerance).of(2_000.0)
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `net worth is clamped to zero when liabilities exceed assets`() {
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 1_000.0),
            liabilities = ZakatLiabilities(loans = 5_000.0),
            nisabType = NisabType.SILVER,
            metalPricePerGram = 0.8,
            currency = "USD"
        )
        assertThat(result.netWorth).isEqualTo(0.0)
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    // ── calculate: passthrough fields ───────────────────────────────────────

    @Test
    fun `zakatable amount mirrors net worth and currency is preserved`() {
        val result = ZakatCalculator.calculate(
            assets = assets(cashOnHand = 12_345.0),
            liabilities = ZakatLiabilities(),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 60.0,
            currency = "GBP"
        )
        assertThat(result.zakatableAmount).isWithin(tolerance).of(result.netWorth)
        assertThat(result.currency).isEqualTo("GBP")
        assertThat(result.nisabType).isEqualTo(NisabType.GOLD)
    }

    @Test
    fun `raw gold grams in assets do not contribute to zakatable wealth`() {
        // Documents the current behaviour: goldGrams/silverGrams are NOT valued
        // into the total, only the cash-equivalent fields are.
        val result = ZakatCalculator.calculate(
            assets = assets(goldGrams = 1_000.0, silverGrams = 1_000.0),
            liabilities = ZakatLiabilities(),
            nisabType = NisabType.GOLD,
            metalPricePerGram = 60.0,
            currency = "USD"
        )
        assertThat(result.totalAssets).isEqualTo(0.0)
        assertThat(result.netWorth).isEqualTo(0.0)
        assertThat(result.zakatDue).isEqualTo(0.0)
    }
}
