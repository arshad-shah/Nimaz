package com.arshadshah.nimaz.presentation.viewmodel.tools

import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatCalculation
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.model.ZakatLiabilities
import com.arshadshah.nimaz.presentation.viewmodel.UiError

data class ZakatCalculatorUiState(
    val assets: ZakatAssets = ZakatAssets(),
    val liabilities: ZakatLiabilities = ZakatLiabilities(),
    val nisabType: NisabType = NisabType.GOLD,
    val goldPricePerGram: Double = ZakatDefaults.GOLD_PRICE_PER_GRAM,
    val silverPricePerGram: Double = ZakatDefaults.SILVER_PRICE_PER_GRAM,
    val currency: String = ZakatDefaults.CURRENCY,
    val calculation: ZakatCalculation? = null,
    val isCalculating: Boolean = false,
    /**
     * Whether the line-by-line working is open. Starts open: it is the part of the screen that
     * explains where the figure came from, and the toggle exists to get it out of the way once
     * you have read it — not to make you find it first.
     */
    val showBreakdown: Boolean = true,

    /**
     * A calculation that could not be completed, or a result that could not be saved.
     *
     * Rendered INLINE beside the result, never as a screen: everything the user typed is
     * still on the form and still valid, and taking that away to report a failed sum would
     * lose work they cannot get back with a button.
     */
    val error: UiError? = null,
)

data class ZakatHistoryUiState(
    val history: List<ZakatHistoryEntry> = emptyList(),
    val totalZakatPaid: Double = 0.0,
    val isLoading: Boolean = true,
    /** Set when past calculations could not be read. */
    val error: UiError? = null,
)
