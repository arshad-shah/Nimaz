package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.NisabType

/**
 * Everything `ZakatSettingsScreen` can change.
 *
 * These four moved off `ZakatEvent`: the calculator now only carries the figures a person
 * types into the form, and the basis those figures are judged against is set once, here.
 */
sealed interface ZakatSettingsEvent {
    data class SetNisabType(val nisabType: NisabType) : ZakatSettingsEvent
    data class SetGoldPrice(val pricePerGram: Double) : ZakatSettingsEvent
    data class SetSilverPrice(val pricePerGram: Double) : ZakatSettingsEvent
    data class SetCurrency(val currency: String) : ZakatSettingsEvent
}
