package com.arshadshah.nimaz.presentation.viewmodel.tools

import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatCalculation
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.model.ZakatLiabilities

data class ZakatCalculatorUiState(
    val assets: ZakatAssets = ZakatAssets(),
    val liabilities: ZakatLiabilities = ZakatLiabilities(),
    val nisabType: NisabType = NisabType.GOLD,
    val goldPricePerGram: Double = ZakatDefaults.GOLD_PRICE_PER_GRAM,
    val silverPricePerGram: Double = ZakatDefaults.SILVER_PRICE_PER_GRAM,
    val currency: String = ZakatDefaults.CURRENCY,
    val calculation: ZakatCalculation? = null,
    val isCalculating: Boolean = false,
    val showBreakdown: Boolean = false,
    val error: String? = null
)

data class ZakatHistoryUiState(
    val history: List<ZakatHistoryEntry> = emptyList(),
    val totalZakatPaid: Double = 0.0,
    val isLoading: Boolean = true
)
