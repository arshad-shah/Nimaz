package com.arshadshah.nimaz.widget.khatam

import kotlinx.serialization.Serializable

@Serializable
sealed interface KhatamWidgetState {

    /**
     * Whether this state is a reading worth keeping on screen when a refresh fails — see
     * `refreshWidget`. "No active khatam" is indistinguishable from the never-loaded default
     * here — both are `Success(KhatamWidgetData())` — and nothing is lost by letting the error
     * frame replace it.
     */
    val hasData: Boolean get() = false

    @Serializable
    data object Loading : KhatamWidgetState

    @Serializable
    data class Success(val data: KhatamWidgetData) : KhatamWidgetState {
        override val hasData: Boolean get() = data.hasActiveKhatam
    }

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
    val juzCompleted: Int = 0,
    val remainingAyahs: Int = 0,
    val dailyTarget: Int = 0,
    val currentStreak: Int = 0
)
