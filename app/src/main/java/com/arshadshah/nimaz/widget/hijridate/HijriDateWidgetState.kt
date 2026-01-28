package com.arshadshah.nimaz.widget.hijridate

import kotlinx.serialization.Serializable

@Serializable
sealed interface HijriDateWidgetState {

    @Serializable
    data object Loading : HijriDateWidgetState

    @Serializable
    data class Success(val data: HijriDateData) : HijriDateWidgetState

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
