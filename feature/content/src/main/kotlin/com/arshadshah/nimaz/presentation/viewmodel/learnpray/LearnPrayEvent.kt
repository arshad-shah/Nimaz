package com.arshadshah.nimaz.presentation.viewmodel.learnpray

import com.arshadshah.nimaz.presentation.model.PrayerFigure

sealed interface LearnPrayEvent {
    data class SelectFigure(val figure: PrayerFigure) : LearnPrayEvent
    data object Next : LearnPrayEvent
    data object Previous : LearnPrayEvent
    data object Restart : LearnPrayEvent
}
