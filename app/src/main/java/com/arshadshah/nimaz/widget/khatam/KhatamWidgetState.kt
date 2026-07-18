package com.arshadshah.nimaz.widget.khatam

import kotlinx.serialization.Serializable

@Serializable
sealed interface KhatamWidgetState {

    @Serializable
    data object Loading : KhatamWidgetState

    @Serializable
    data class Success(val data: KhatamWidgetData) : KhatamWidgetState

    @Serializable
    data class Error(val message: String?) : KhatamWidgetState
}

/**
 * Everything the widget draws, pre-resolved by the worker.
 *
 * [hasActiveKhatam] is false when there is no active khatam at all — the widget
 * then renders the empty state instead of the progress card, so the remaining
 * fields are ignored.
 */
@Serializable
data class KhatamWidgetData(
    val hasActiveKhatam: Boolean = false,
    val name: String = "",
    val progressPercent: Int = 0,
    val currentJuz: Int = 1,
    val remainingAyahs: Int = 0
)
