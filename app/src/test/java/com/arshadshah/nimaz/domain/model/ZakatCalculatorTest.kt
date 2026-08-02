package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The zakat calculation, in one place.
 *
 * [ZakatCalculator.calculate] shipped computing `totalAssets = assets.total`, and
 * [ZakatAssets.total] deliberately **excludes** gold and silver — the class even carries a
 * comment saying the grams "need to be converted to value separately". Nothing converted them.
 * Anyone calling it got a zakat figure that silently ignored the user's precious metals, which
 * for this app's audience is the most commonly held zakatable asset there is.
 *
 * It survived because it had **no callers**: `ZakatViewModel` re-implemented the whole
 * calculation inline, correctly. So the domain carried a plausible-looking, wrong calculator
 * next to a correct one hidden in a ViewModel, and a second copy of the nisab constants in an
 * unused `ZakatConstants` object — three ways for a future edit to make the app disagree with
 * itself about who owes zakat.
 *
 * These tests define the one calculation. The ViewModel now calls it.
 */
class ZakatCalculatorTest {

    private val goldPrice = 65.0     // per gram
    private val silverPrice = 0.80   // per gram

    /** Nisab at these prices: gold 87.48g * 65 = 5686.20; silver 612.36g * 0.80 = 489.888. */
    private val goldNisab = ZakatCalculator.GOLD_NISAB_GRAMS * goldPrice
    private val silverNisab = ZakatCalculator.SILVER_NISAB_GRAMS * silverPrice

    @Test
    fun `gold and silver holdings count towards total assets`() {
        val result = calculate(
            assets = ZakatAssets(cashOnHand = 1_000.0, goldGrams = 100.0, silverGrams = 500.0)
        )

        // 1000 cash + (100g * 65) + (500g * 0.80) = 1000 + 6500 + 400
        assertThat(result.totalAssets).isWithin(TOLERANCE).of(7_900.0)
        assertThat(result.goldValue).isWithin(TOLERANCE).of(6_500.0)
        assertThat(result.silverValue).isWithin(TOLERANCE).of(400.0)
    }

    @Test
    fun `someone holding only gold above nisab owes zakat on it`() {
        // 100g of gold is above the 87.48g gold nisab, and nothing else is held. If metals
        // were excluded from the total this comes out as zero assets and no zakat at all.
        val result = calculate(assets = ZakatAssets(goldGrams = 100.0))

        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.zakatDue).isWithin(TOLERANCE).of(6_500.0 * 0.025)
    }

    @Test
    fun `net worth is assets minus liabilities`() {
        val result = calculate(
            assets = ZakatAssets(cashOnHand = 10_000.0),
            liabilities = ZakatLiabilities(debts = 2_000.0, loans = 500.0)
        )

        assertThat(result.totalLiabilities).isWithin(TOLERANCE).of(2_500.0)
        assertThat(result.netWorth).isWithin(TOLERANCE).of(7_500.0)
    }

    @Test
    fun `below nisab means nothing is due`() {
        // Amounts here are against the silver nisab, which is what `calculate` defaults to.
        val result = calculate(assets = ZakatAssets(cashOnHand = silverNisab - 1.0))

        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `exactly at nisab is above it`() {
        val result = calculate(assets = ZakatAssets(cashOnHand = silverNisab))

        assertThat(result.isAboveNisab).isTrue()
        assertThat(result.zakatDue).isWithin(TOLERANCE).of(silverNisab * 0.025)
    }

    @Test
    fun `the silver nisab is the lower threshold, so it catches more payers`() {
        // A holding between the two thresholds owes on silver nisab and not on gold — the
        // reason the choice is offered at all.
        val between = silverNisab + 100.0
        assertThat(between).isLessThan(goldNisab)

        assertThat(calculate(ZakatAssets(cashOnHand = between)).isAboveNisab).isTrue()
        assertThat(
            calculate(ZakatAssets(cashOnHand = between), nisabType = NisabType.GOLD).isAboveNisab
        ).isFalse()
    }

    @Test
    fun `nisab is valued from the metal that names it, not the other one`() {
        val gold = calculate(ZakatAssets(), nisabType = NisabType.GOLD)
        val silver = calculate(ZakatAssets(), nisabType = NisabType.SILVER)

        assertThat(gold.nisabValue).isWithin(TOLERANCE).of(goldNisab)
        assertThat(silver.nisabValue).isWithin(TOLERANCE).of(silverNisab)
    }

    @Test
    fun `owing more than you own is below nisab and reports the real net worth`() {
        // Reporting a clamped 0 would tell someone in debt they are exactly at break-even.
        val result = calculate(
            assets = ZakatAssets(cashOnHand = 1_000.0),
            liabilities = ZakatLiabilities(debts = 3_000.0)
        )

        assertThat(result.netWorth).isWithin(TOLERANCE).of(-2_000.0)
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `a zero metal price cannot make everyone liable`() {
        // nisab = grams * price, so a price of 0 makes the threshold 0 and `netWorth >= 0`
        // true for anyone who owns anything — or nothing. A threshold that cannot be valued
        // is not a threshold that has been met.
        // Zero the price of the metal that names the threshold in use (silver, by default).
        val result = calculate(assets = ZakatAssets(cashOnHand = 500.0), silverPricePerGram = 0.0)

        assertThat(result.nisabValue).isEqualTo(0.0)
        assertThat(result.isAboveNisab).isFalse()
        assertThat(result.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `zakat is two and a half percent of net worth`() {
        val result = calculate(assets = ZakatAssets(cashOnHand = 100_000.0))

        assertThat(ZakatCalculator.ZAKAT_RATE).isEqualTo(0.025)
        assertThat(result.zakatDue).isWithin(TOLERANCE).of(2_500.0)
    }

    private fun calculate(
        assets: ZakatAssets,
        liabilities: ZakatLiabilities = ZakatLiabilities(),
        nisabType: NisabType = NisabType.SILVER,
        goldPricePerGram: Double = goldPrice,
        silverPricePerGram: Double = silverPrice,
    ) = ZakatCalculator.calculate(
        assets = assets,
        liabilities = liabilities,
        nisabType = nisabType,
        goldPricePerGram = goldPricePerGram,
        silverPricePerGram = silverPricePerGram,
        currency = "USD"
    )

    private companion object {
        const val TOLERANCE = 1e-6
    }
}
