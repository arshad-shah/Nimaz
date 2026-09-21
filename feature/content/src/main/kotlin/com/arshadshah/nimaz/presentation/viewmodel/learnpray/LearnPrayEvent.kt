package com.arshadshah.nimaz.presentation.viewmodel.learnpray

import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.arshadshah.nimaz.domain.model.PrayerAudio

sealed interface LearnPrayEvent {
    data class ToggleAudio(val clip: PrayerAudio) : LearnPrayEvent
    data object StopAudio : LearnPrayEvent
    data class SelectFigure(val figure: PrayerFigure) : LearnPrayEvent
    data object Next : LearnPrayEvent
    data object Previous : LearnPrayEvent
    data object Restart : LearnPrayEvent
}
