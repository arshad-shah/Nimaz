package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDayRailTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // Mon 10 to Sun 16 August; today is Thursday the 13th, so the 14th onward is in the future.
    private val week = (10..16).mapIndexed { index, day ->
        NimazDayRailItem(
            weekdayLabel = listOf("M", "T", "W", "T", "F", "S", "S")[index],
            dayLabel = day.toString(),
            marker = if (day == 11) NimazStatusDotSpec(NimazTone.SUCCESS) else null,
            isToday = day == 13,
            enabled = day <= 13,
            contentDescription = "August $day",
        )
    }

    @Test
    fun `every day is exposed to accessibility`() {
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = {})
        }
        week.forEach {
            composeRule.onNodeWithContentDescription(it.contentDescription).assertIsDisplayed()
        }
    }

    @Test
    fun `the selected day reports itself as selected`() {
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = {})
        }
        composeRule.onNodeWithContentDescription("August 13").assertIsSelected()
        composeRule.onNodeWithContentDescription("August 10").assertIsNotSelected()
    }

    @Test
    fun `tapping a day emits its index`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = { observed = it })
        }
        composeRule.onNodeWithContentDescription("August 11").performClick()
        assertThat(observed).isEqualTo(1)
    }

    @Test
    fun `a disabled future day does not emit`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = { observed = it })
        }
        composeRule.onNodeWithContentDescription("August 16").performClick()
        assertThat(observed).isNull()
    }

    @Test
    fun `a null selection selects nothing`() {
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = null, onSelect = {})
        }
        composeRule.onNodeWithContentDescription("August 13").assertIsNotSelected()
    }

    @Test
    fun `an item defaults to enabled with no marker`() {
        val item = NimazDayRailItem(
            weekdayLabel = "M",
            dayLabel = "1",
            contentDescription = "August 1",
        )
        assertThat(item.enabled).isTrue()
        assertThat(item.marker).isNull()
        assertThat(item.isToday).isFalse()
    }
}
