package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.currencyLabel
import com.arshadshah.nimaz.core.common.formatCurrency
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatCalculator
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.presentation.viewmodel.settings.ZakatSettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.ZakatSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.ZakatSettingsViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.settingsRow
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The zakat basis: which nisab applies, what the two metals are worth, and the currency.
 *
 * The hero at the top is the reason this screen is ordered as it is — it shows the exact figure
 * `ZakatCalculator` will compare net wealth against, so every control below has a visible
 * consequence. It is asserted against `ZakatCalculator.nisabValue` rather than against a number
 * spelled out here: a preview computing its own threshold would eventually promise one the
 * calculation does not use, and nothing on screen would say so.
 *
 * Two formatting rules carry real weight. The subtitle shows the *working* — "612.36g @ $0.80/g" —
 * and it used to be built from `price.toInt()`, which rendered silver's 0.80 as "0" and made the
 * threshold look like it came from nothing. The currency symbol used to be a hardcoded "$",
 * which quietly relabelled everyone else's money.
 *
 * `ZakatSummaryHero` sets `clearAndSetSemantics`, so the hero is addressable only by its content
 * description — which is the better contract to pin anyway, since it is what TalkBack reads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ZakatSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val state = MutableStateFlow(ZakatSettingsUiState())
    private val events = mutableListOf<ZakatSettingsEvent>()
    private val viewModel: ZakatSettingsViewModel = mockk(relaxed = true) {
        every { uiState } returns this@ZakatSettingsScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<ZakatSettingsEvent>() }
    }
    private var backs = 0

    private fun setContent(uiState: ZakatSettingsUiState = ZakatSettingsUiState()) {
        state.value = uiState
        composeRule.setThemedContent {
            ZakatSettingsScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private inline fun <reified T : ZakatSettingsEvent> only(): T = events.filterIsInstance<T>()
        .also { check(it.size == 1) { "expected one ${T::class.simpleName}, got $events" } }
        .single()

    @Test
    fun `the four sections render`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.zakat_section_nisab)).onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.zakat_metal_prices)).assertExists()
        composeRule.onAllNodesWithText(string(R.string.zakat_currency)).onFirst().assertExists()
    }

    @Test
    fun `the hero shows the threshold the calculator will actually use`() {
        val uiState = ZakatSettingsUiState(
            nisabType = NisabType.GOLD,
            goldPricePerGram = 70.0,
            silverPricePerGram = 0.9,
            currency = "USD",
        )
        setContent(uiState)

        val expected = formatCurrency(
            ZakatCalculator.nisabValue(NisabType.GOLD, 70.0, 0.9),
            "USD",
        )
        composeRule.onNodeWithContentDescription(expected, substring = true).assertExists()
    }

    @Test
    fun `switching the basis changes the threshold to the other metal's`() {
        // Gold and silver nisab are different weights of different metals, and the silver one
        // works out far lower — which is the whole reason the choice exists.
        setContent(
            ZakatSettingsUiState(
                nisabType = NisabType.SILVER,
                goldPricePerGram = 65.0,
                silverPricePerGram = 0.8,
            )
        )

        val silverThreshold = formatCurrency(
            ZakatCalculator.nisabValue(NisabType.SILVER, 65.0, 0.8),
            ZakatDefaults.CURRENCY,
        )
        composeRule.onNodeWithContentDescription(silverThreshold, substring = true).assertExists()
    }

    @Test
    fun `a silver price under one is shown as a price, not rounded away to zero`() {
        // `price.toInt()` rendered 0.80 as "0", so the subtitle read "612.36g @ $0/g" and the
        // threshold above it looked as if it had come from nothing.
        setContent(
            ZakatSettingsUiState(nisabType = NisabType.SILVER, silverPricePerGram = 0.80)
        )

        composeRule.onAllNodesWithText(
            string(R.string.zakat_nisab_silver_subtitle, formatCurrency(0.80, "USD"))
        ).onFirst().assertExists()
    }

    @Test
    fun `prices are shown in the chosen currency, not in dollars`() {
        // A hardcoded "$" quietly relabelled everyone else's money — the figure was right and
        // the unit was not, which is the version of wrong nobody checks.
        setContent(
            ZakatSettingsUiState(
                nisabType = NisabType.GOLD,
                goldPricePerGram = 55.0,
                currency = "EUR",
            )
        )

        composeRule.onAllNodesWithText(
            string(R.string.zakat_nisab_gold_subtitle, formatCurrency(55.0, "EUR"))
        ).onFirst().assertExists()
    }

    @Test
    fun `both metal prices are offered whichever basis is selected`() {
        // Gold and silver held as *assets* are valued from these prices regardless of basis, so
        // hiding the unselected one would hide a price that is still in the sum.
        setContent(ZakatSettingsUiState(nisabType = NisabType.SILVER))

        composeRule.onNodeWithText(string(R.string.zakat_gold_price_label)).assertExists()
        composeRule.onNodeWithText(string(R.string.zakat_silver_price_label)).assertExists()
    }

    @Test
    fun `picking gold as the basis dispatches gold`() {
        setContent(ZakatSettingsUiState(nisabType = NisabType.SILVER))

        composeRule.onAllNodesWithText(string(R.string.gold)).onFirst().performClick()

        assertThat(only<ZakatSettingsEvent.SetNisabType>().nisabType).isEqualTo(NisabType.GOLD)
    }

    @Test
    fun `picking silver as the basis dispatches silver`() {
        // Two cards built from one composable with the label swapped. Crossing them switches the
        // ruling the user follows to the other one.
        setContent(ZakatSettingsUiState(nisabType = NisabType.GOLD))

        composeRule.onAllNodesWithText(string(R.string.silver)).onFirst().performClick()

        assertThat(only<ZakatSettingsEvent.SetNisabType>().nisabType).isEqualTo(NisabType.SILVER)
    }

    @Test
    fun `the currency row names the currency and its full name`() {
        setContent(ZakatSettingsUiState(currency = "GBP"))

        composeRule.onNodeWithText(currencyLabel("GBP")).assertExists()
    }

    @Test
    fun `picking a currency dispatches that code`() {
        setContent(ZakatSettingsUiState(currency = "USD"))

        composeRule.settingsRow(string(R.string.zakat_currency)).performClick()
        composeRule.onNodeWithText(currencyLabel(ZakatDefaults.CURRENCIES[1])).performClick()

        assertThat(only<ZakatSettingsEvent.SetCurrency>().currency)
            .isEqualTo(ZakatDefaults.CURRENCIES[1])
    }

    @Test
    fun `the screen says these prices are estimates, not looked-up rates`() {
        // A default read as a market rate is how a zakat figure goes quietly wrong, and there is
        // no other signal on the screen that 65.00 was not fetched from anywhere.
        setContent()

        composeRule.onNodeWithText(string(R.string.zakat_metal_prices_hint)).assertExists()
        composeRule.onNodeWithText(string(R.string.zakat_settings_nisab_hint)).assertExists()
    }

    @Test
    fun `a zero threshold is not presented as a full-strength figure`() {
        // A nisab priced at zero has not been met; it has failed to be established, and
        // `ZakatCalculator` treats it that way too.
        setContent(
            ZakatSettingsUiState(
                nisabType = NisabType.GOLD,
                goldPricePerGram = 0.0,
                silverPricePerGram = 0.0,
            )
        )

        // The hero reads as one phrase — label then amount — so the assertion is the whole
        // a11y string rather than the figure, which at zero also matches both price tiles.
        composeRule.onNodeWithContentDescription(
            "${string(R.string.nisab_threshold)}, ${formatCurrency(0.0, ZakatDefaults.CURRENCY)}",
            substring = true,
        ).assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
