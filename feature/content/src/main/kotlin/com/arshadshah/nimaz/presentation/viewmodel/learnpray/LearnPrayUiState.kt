package com.arshadshah.nimaz.presentation.viewmodel.learnpray

import com.arshadshah.nimaz.presentation.model.PrayerFigure

/** -1 is preparation; stepCount is completion. Learning never marks a prayer as performed. */
data class LearnPrayUiState(val page: Int = -1, val figure: PrayerFigure = PrayerFigure.MAN) {
    val preparing get() = page == -1
    val complete get() = page == STEP_COUNT

    companion object {
        const val STEP_COUNT = 18
    }
}
