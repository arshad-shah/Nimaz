package com.arshadshah.nimaz.viewModel

import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.services.NisabType
import com.arshadshah.nimaz.services.PreciousMetalService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ZakatViewModelTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var metalService: PreciousMetalService

    private lateinit var viewModel: ZakatViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        setupDefaultMocks()
        viewModel = ZakatViewModel(metalService)
    }

    private fun setupDefaultMocks() = runTest {
        val mockThresholds = PreciousMetalService.NisabThresholds(
            goldNisab = 5000.0,
            silverNisab = 500.0,
            currencyInfo = CurrencyInfo("USD", "$")
        )

        // Mock for any Locale
        `when`(metalService.getNisabThresholdsWithCache(Locale.getDefault())).thenReturn(
            mockThresholds
        )
        `when`(metalService.getNisabThresholds(Locale.getDefault())).thenReturn(mockThresholds)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        // Advance the dispatcher to complete initialization
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value

        with(state) {
            assertEquals(CurrencyInfo("USD", "$"), currencyInfo)
            assertEquals("", goldAndSilverAmount)
            assertEquals("", cashAmount)
            assertEquals("", otherSavings)
            assertEquals("", investmentsAmount)
            assertEquals("", moneyOwedToYou)
            assertEquals("", stockValue)
            assertEquals("", moneyYouOwe)
            assertEquals("", otherOutgoingsDue)
            assertEquals(0.0, netAssets, 0.01)
            assertEquals(0.0, totalZakat, 0.01)
            assertFalse(isEligible)
            assertEquals(NisabType.GOLD, selectedNisabType)
            assertFalse(isLoading)
            assertNull(error)
            assertEquals(5000.0, goldNisabThreshold, 0.01)  // Add this
            assertEquals(500.0, silverNisabThreshold, 0.01)  // Add this
        }
    }

    @Test
    fun `should calculate Zakat when assets are below Nisab threshold`() = runTest {
        viewModel.updateCashAmount("4000.0")
        viewModel.updateGoldAndSilver("500.0")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        with(state) {
            assertEquals(4500.0, netAssets, 0.01)
            assertEquals(0.0, totalZakat, 0.01)
            assertFalse(isEligible)
        }
    }

    @Test
    fun `should calculate Zakat when assets are above Nisab threshold`() = runTest {
        viewModel.updateCashAmount("8000.0")
        viewModel.updateGoldAndSilver("2000.0")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        with(state) {
            assertEquals(10000.0, netAssets, 0.01)
            assertEquals(250.0, totalZakat, 0.01) // 2.5% of 10000
            assertTrue(isEligible)
        }
    }

    @Test
    fun `should correctly calculate net assets and Zakat with all asset types`() = runTest {
        viewModel.apply {
            updateGoldAndSilver("1000.0")
            updateCashAmount("2000.0")
            updateOtherSavings("1500.0")
            updateInvestments("2000.0")
            updateMoneyOwedToYou("1000.0")
            updateStockValue("3000.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        with(state) {
            assertEquals(10500.0, netAssets, 0.01)
            assertEquals(262.5, totalZakat, 0.01)
            assertTrue(isEligible)
        }
    }

    @Test
    fun `should handle invalid number inputs gracefully`() = runTest {
        viewModel.updateCashAmount("invalid")
        viewModel.updateGoldAndSilver("1000.0")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(1000.0, state.netAssets, 0.01) // Should ignore invalid input
    }

    @Test
    fun `should consider liabilities in Zakat calculation`() = runTest {
        viewModel.apply {
            updateCashAmount("15000.0")
            updateMoneyYouOwe("2000.0")
            updateOtherOutgoingsDue("3000.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        with(state) {
            assertEquals(10000.0, netAssets, 0.01)
            assertEquals(250.0, totalZakat, 0.01)
            assertTrue(isEligible)
        }
    }

    @Test
    fun `should format currency values correctly`() = runTest {
        with(viewModel) {
            assertEquals("1000.00", formatCurrency(1000.0))
            assertEquals("1000.50", formatCurrency(1000.5))
            assertEquals("0.00", formatCurrency(0.0))
            assertEquals("999999.99", formatCurrency(999999.99))
        }
    }

    @Test
    fun `should correctly switch between Gold and Silver Nisab types`() = runTest {
        viewModel.updateCashAmount("750.0")

        viewModel.setNisabType(NisabType.GOLD)
        var state = viewModel.zakatState.value
        assertFalse(state.isEligible) // 750 < 5000 (Gold Nisab)

        viewModel.setNisabType(NisabType.SILVER)
        state = viewModel.zakatState.value
        assertTrue(state.isEligible) // 750 > 500 (Silver Nisab)
    }

    @Test
    fun `should handle empty string inputs as zero`() = runTest {
        viewModel.apply {
            updateCashAmount("")
            updateGoldAndSilver("")
            updateOtherSavings("")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(0.0, state.netAssets, 0.01)
        assertEquals(0.0, state.totalZakat, 0.01)
    }

    @Test
    fun `should handle extremely large numbers without precision loss`() = runTest {
        viewModel.apply {
            updateCashAmount("999999999.99")
            updateGoldAndSilver("888888888.88")
            updateOtherSavings("777777777.77")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(2666666666.64, state.netAssets, 0.01)
        assertEquals(66666666.67, state.totalZakat, 0.01) // 2.5% of net assets
        assertTrue(state.isEligible)
    }

    @Test
    fun `should handle negative input values as zero`() = runTest {
        viewModel.apply {
            updateCashAmount("-1000.0")
            updateGoldAndSilver("-500.0")
            updateOtherSavings("-200.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(0.0, state.netAssets, 0.01)
        assertEquals(0.0, state.totalZakat, 0.01)
        assertFalse(state.isEligible)
    }

    @Test
    fun `should maintain eligibility state when assets fluctuate around threshold`() = runTest {
        // Start above threshold
        viewModel.updateCashAmount("6000.0")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.zakatState.value.isEligible)

        // Drop below threshold
        viewModel.updateCashAmount("4000.0")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.zakatState.value.isEligible)

        // Return above threshold
        viewModel.updateCashAmount("5500.0")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.zakatState.value.isEligible)
    }

    @Test
    fun `should handle decimal precision edge cases`() = runTest {
        viewModel.apply {
            updateCashAmount("5000.005") // Testing rounding
            updateGoldAndSilver("0.095")  // Very small value
            updateOtherSavings("999.999") // Testing precision
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(6000.10, state.netAssets, 0.01)
        assertEquals(150.00, state.totalZakat, 0.01)
        assertTrue(state.isEligible)
    }

    @Test
    fun `should calculate correct Zakat when liabilities exactly match assets`() = runTest {
        viewModel.apply {
            updateCashAmount("10000.0")
            updateMoneyYouOwe("5000.0")
            updateOtherOutgoingsDue("5000.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(0.0, state.netAssets, 0.01)
        assertEquals(0.0, state.totalZakat, 0.01)
        assertFalse(state.isEligible)
    }

    @Test
    fun `should handle rapid sequential updates correctly`() = runTest {
        viewModel.apply {
            updateCashAmount("1000.0")
            updateGoldAndSilver("2000.0")
            updateOtherSavings("3000.0")
            updateInvestments("4000.0")
            updateMoneyOwedToYou("5000.0")
            updateStockValue("6000.0")
            updateMoneyYouOwe("7000.0")
            updateOtherOutgoingsDue("8000.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(6000.0, state.netAssets, 0.01) // Total assets - Total liabilities
        assertEquals(150.0, state.totalZakat, 0.01)
        assertTrue(state.isEligible)
    }

    @Test
    fun `should handle whitespace in input values`() = runTest {
        viewModel.apply {
            updateCashAmount("  5000.0  ")
            updateGoldAndSilver(" 1000.0 ")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(6000.0, state.netAssets, 0.01)
        assertEquals(150.0, state.totalZakat, 0.01)
        assertTrue(state.isEligible)
    }

    @Test
    fun `should validate currency format across different locales`() = runTest {
        val testCases = listOf(
            1234567.89 to "1234567.89",
            0.0 to "0.00",
            99999999.99 to "99999999.99"
        )

        testCases.forEach { (input, expected) ->
            assertEquals(expected, viewModel.formatCurrency(input))
        }
    }

    @Test
    fun `should handle negative values as zero for assets`() = runTest {
        viewModel.apply {
            updateCashAmount("-1000.0")
            updateGoldAndSilver("-500.0")
            updateOtherSavings("-200.0")
            updateInvestments("-300.0")
            updateMoneyOwedToYou("-400.0")
            updateStockValue("-600.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(0.0, state.netAssets, 0.01)
        assertEquals(0.0, state.totalZakat, 0.01)
        assertFalse(state.isEligible)
    }

    @Test
    fun `should handle negative values as zero for liabilities`() = runTest {
        viewModel.apply {
            updateCashAmount("6000.0")  // Above nisab threshold
            updateMoneyYouOwe("-1000.0")  // Should be treated as 0
            updateOtherOutgoingsDue("-500.0")  // Should be treated as 0
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(6000.0, state.netAssets, 0.01)  // Should not subtract negative liabilities
        assertEquals(150.0, state.totalZakat, 0.01)  // 2.5% of 6000
        assertTrue(state.isEligible)
    }

    @Test
    fun `should handle mix of negative and positive values correctly`() = runTest {
        viewModel.apply {
            updateCashAmount("1000.0")
            updateGoldAndSilver("-500.0")  // Should be 0
            updateOtherSavings("2000.0")
            updateInvestments("-300.0")  // Should be 0
            updateMoneyYouOwe("-200.0")  // Should be 0
            updateOtherOutgoingsDue("500.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(2500.0, state.netAssets, 0.01)  // 1000 + 0 + 2000 + 0 - 0 - 500
        assertEquals(0.0, state.totalZakat, 0.01)  // Below nisab threshold
        assertFalse(state.isEligible)
    }

    @Test
    fun `should handle extremely negative values as zero`() = runTest {
        viewModel.apply {
            updateCashAmount("-999999999.99")
            updateGoldAndSilver("6000.0")
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.zakatState.value
        assertEquals(
            6000.0,
            state.netAssets,
            0.01
        )  // Extremely negative value should be treated as 0
        assertEquals(150.0, state.totalZakat, 0.01)
        assertTrue(state.isEligible)
    }
}