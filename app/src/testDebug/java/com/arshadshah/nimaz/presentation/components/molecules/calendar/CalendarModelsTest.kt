package com.arshadshah.nimaz.presentation.components.molecules.calendar

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarLegendItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarModelsTest {

    @Test
    fun `day state indicators default to filled so existing callers are unchanged`() {
        assertThat(CalendarDayState().indicatorStyle).isEqualTo(NimazStatusDotStyle.FILLED)
    }

    @Test
    fun `legend items default to filled so existing callers are unchanged`() {
        val item = CalendarLegendItem(color = Color.Red, label = "Fasted")
        assertThat(item.indicatorStyle).isEqualTo(NimazStatusDotStyle.FILLED)
    }

    @Test
    fun `a day state can request an outlined indicator`() {
        assertThat(CalendarDayState(indicatorStyle = NimazStatusDotStyle.OUTLINED).indicatorStyle)
            .isEqualTo(NimazStatusDotStyle.OUTLINED)
    }

    @Test
    fun `a legend item can request an outlined swatch`() {
        val item = CalendarLegendItem(
            color = Color.Red,
            label = "Not fasting",
            indicatorStyle = NimazStatusDotStyle.OUTLINED,
        )
        assertThat(item.indicatorStyle).isEqualTo(NimazStatusDotStyle.OUTLINED)
    }
}
