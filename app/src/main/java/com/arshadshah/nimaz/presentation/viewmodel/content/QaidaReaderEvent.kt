@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaLetter

sealed interface QaidaReaderEvent {
    data class SelectLesson(val lessonId: Int) : QaidaReaderEvent
    data class CellTapped(val cell: QaidaCell) : QaidaReaderEvent
    data class PlayLine(val lineId: Int) : QaidaReaderEvent
    data class PlayLetter(val letter: QaidaLetter) : QaidaReaderEvent
    data object NextLesson : QaidaReaderEvent
    data object PreviousLesson : QaidaReaderEvent
    data object Resume : QaidaReaderEvent
    data object ResetJourney : QaidaReaderEvent
}
