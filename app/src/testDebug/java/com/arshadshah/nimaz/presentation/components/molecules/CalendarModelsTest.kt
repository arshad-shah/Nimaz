package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarHeaderAlignment
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.IndicatorPosition
import com.arshadshah.nimaz.presentation.components.molecules.calendar.SelectionStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarModelsTest {

    @Test
    fun `indicator position enum values`() {
        assertThat(IndicatorPosition.values()).asList()
            .containsExactly(IndicatorPosition.BOTTOM_CENTER, IndicatorPosition.TOP_END)
    }

    @Test
    fun `selection style enum values`() {
        assertThat(SelectionStyle.values()).asList()
            .containsExactly(SelectionStyle.BACKGROUND, SelectionStyle.BORDER)
    }

    @Test
    fun `header alignment enum has start center end`() {
        assertThat(CalendarHeaderAlignment.values()).asList().containsExactly(
            CalendarHeaderAlignment.START,
            CalendarHeaderAlignment.CENTER,
            CalendarHeaderAlignment.END
        ).inOrder()
    }

    @Test
    fun `day state defaults are sensible`() {
        val state = CalendarDayState()
        assertThat(state.indicatorColor).isNull()
        assertThat(state.indicatorPosition).isEqualTo(IndicatorPosition.BOTTOM_CENTER)
        assertThat(state.backgroundColor).isNull()
        assertThat(state.textColor).isNull()
        assertThat(state.fontWeight).isNull()
        assertThat(state.primaryLabel).isNull()
        assertThat(state.secondaryLabel).isNull()
        assertThat(state.emphasizePrimary).isFalse()
        assertThat(state.emphasizeSecondary).isFalse()
        assertThat(state.indicatorBar).isNull()
        assertThat(state.indicatorBarColor).isNull()
    }

    @Test
    fun `the fill bar is independent of the indicator dot`() {
        val barOnly = CalendarDayState(indicatorBar = 0.6f)
        assertThat(barOnly.indicatorBar).isEqualTo(0.6f)
        assertThat(barOnly.indicatorColor).isNull()

        val both = CalendarDayState(indicatorColor = Color.Red, indicatorBar = 1f)
        assertThat(both.indicatorColor).isEqualTo(Color.Red)
        assertThat(both.indicatorBar).isEqualTo(1f)
    }

    @Test
    fun `day state copy overrides only named fields`() {
        val base = CalendarDayState(primaryLabel = "1")
        val copy = base.copy(secondaryLabel = "29", emphasizeSecondary = true)
        assertThat(copy.primaryLabel).isEqualTo("1")
        assertThat(copy.secondaryLabel).isEqualTo("29")
        assertThat(copy.emphasizeSecondary).isTrue()
        assertThat(copy).isNotEqualTo(base)
    }

    @Test
    fun `day state equals matches identical content`() {
        assertThat(CalendarDayState(primaryLabel = "5", fontWeight = FontWeight.Bold))
            .isEqualTo(CalendarDayState(primaryLabel = "5", fontWeight = FontWeight.Bold))
    }

    @Test
    fun `legend item holds color and label`() {
        val item = CalendarLegendItem(color = Color.Red, label = "Event")
        assertThat(item.color).isEqualTo(Color.Red)
        assertThat(item.label).isEqualTo("Event")
        assertThat(item).isEqualTo(CalendarLegendItem(Color.Red, "Event"))
    }
}
