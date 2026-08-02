package com.arshadshah.nimaz.widget.nextprayer

import kotlinx.serialization.Serializable

@Serializable
sealed interface NextPrayerWidgetState {

    @Serializable
    data object Loading : NextPrayerWidgetState

    @Serializable
    data class Success(val data: NextPrayerData) : NextPrayerWidgetState

    @Serializable
    data class Error(val message: String?) : NextPrayerWidgetState
}

@Serializable
data class NextPrayerData(
    /**
     * The canonical English prayer name, **not** display text: `prayerIconRes` and
     * `prayerShortName` both key off it, and the widget localizes it at render time so a
     * language change lands without waiting for the next worker run.
     */
    val prayerName: String = "",
    val prayerTime: String = "",
    /** True when [prayerTime] is not a clock time but "the first prayer of tomorrow". */
    val isTomorrow: Boolean = false,
    val countdown: String = "",
    val isValid: Boolean = true,
    val nextPrayerEpochMillis: Long = 0L
)
