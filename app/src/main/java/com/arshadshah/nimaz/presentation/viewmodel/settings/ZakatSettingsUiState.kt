package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatCalculator
import com.arshadshah.nimaz.domain.model.ZakatDefaults

/**
 * The zakat basis: which nisab the calculator compares net wealth against, what the two metals
 * are worth, and which currency every figure is read in.
 *
 * These four are the only inputs to zakat that are *not* a figure the user types per
 * calculation — they are the ruling they follow and the rates they look up — which is why they
 * live on a settings screen rather than in the middle of the form.
 */
data class ZakatSettingsUiState(
    val nisabType: NisabType = NisabType.DEFAULT,
    val goldPricePerGram: Double = ZakatDefaults.GOLD_PRICE_PER_GRAM,
    val silverPricePerGram: Double = ZakatDefaults.SILVER_PRICE_PER_GRAM,
    val currency: String = ZakatDefaults.CURRENCY,
) {
    /**
     * What the chosen basis prices out to — the number the whole screen exists to set.
     *
     * Derived through [ZakatCalculator], not restated here: the calculator decides whether
     * zakat is owed by comparing net wealth against this exact figure, and a preview that
     * computed its own would eventually promise a threshold the calculation does not use.
     */
    val nisabValue: Double
        get() = ZakatCalculator.nisabValue(nisabType, goldPricePerGram, silverPricePerGram)
}
