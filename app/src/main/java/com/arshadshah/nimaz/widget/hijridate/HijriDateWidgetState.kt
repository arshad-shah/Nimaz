package com.arshadshah.nimaz.widget.hijridate

import kotlinx.serialization.Serializable

@Serializable
sealed interface HijriDateWidgetState {

    /**
     * Whether this state is a reading worth keeping on screen when a refresh fails — see
     * `refreshWidget`. The default state every widget starts on is `Success` with an empty
     * payload, so "is it Success" is not the question; carrying loaded values is.
     */
    val hasData: Boolean get() = false

    @Serializable
    data object Loading : HijriDateWidgetState

    @Serializable
    data class Success(val data: HijriDateData) : HijriDateWidgetState {
        override val hasData: Boolean get() = data.hijriMonth.isNotEmpty()
    }

    @Serializable
    data class Error(val message: String?) : HijriDateWidgetState
}

@Serializable
data class HijriDateData(
    val hijriDay: Int = 1,
    val hijriMonth: String = "",
    val hijriYear: Int = 1446,
    val gregorianDayOfWeek: String = "",
    val gregorianDate: String = ""
)
