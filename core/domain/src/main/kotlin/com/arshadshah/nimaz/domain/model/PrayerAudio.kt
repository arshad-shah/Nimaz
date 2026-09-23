package com.arshadshah.nimaz.domain.model

/** Only recordings whose published text matches the lesson are enabled. */
enum class PrayerAudio { FATIHAH, IKHLAS, RUKU, SUJUD, SITTING, TASHAHHUD, SALAWAT }

data class PrayerAudioState(
    val current: PrayerAudio? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)
