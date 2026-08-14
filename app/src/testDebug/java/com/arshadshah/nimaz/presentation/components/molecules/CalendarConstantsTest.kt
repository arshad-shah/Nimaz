package com.arshadshah.nimaz.presentation.components.molecules

import com.arshadshah.nimaz.presentation.foundation.calendar.FRIDAY_INDEX
import com.arshadshah.nimaz.presentation.foundation.calendar.WEEKDAY_LABELS
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarConstantsTest {

    @Test
    fun `weekday labels has seven entries`() {
        assertThat(WEEKDAY_LABELS).hasSize(7)
    }

    @Test
    fun `weekday labels start on sunday and are uppercase`() {
        assertThat(WEEKDAY_LABELS).containsExactly(
            "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"
        ).inOrder()
    }

    @Test
    fun `friday index points at FRI`() {
        assertThat(FRIDAY_INDEX).isEqualTo(5)
        assertThat(WEEKDAY_LABELS[FRIDAY_INDEX]).isEqualTo("FRI")
    }
}
