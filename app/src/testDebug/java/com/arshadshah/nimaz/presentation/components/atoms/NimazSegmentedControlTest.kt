package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSegmentedControlTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val options = listOf(
        NimazSegmentedOption("Fasted", Icons.Default.Check, NimazTone.SUCCESS),
        NimazSegmentedOption("Not fasting", null, NimazTone.NEUTRAL),
        NimazSegmentedOption("Exempt", null, NimazTone.WARNING),
    )

    @Test
    fun `sizes are complete`() {
        assertThat(NimazSegmentedSize.entries).hasSize(2)
    }

    @Test
    fun `an option defaults to the accent tone`() {
        assertThat(NimazSegmentedOption("Any").selectedTone).isEqualTo(NimazTone.ACCENT)
    }

    @Test
    fun `the selected option reports itself as selected`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = 0, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertIsSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `a null selection selects nothing`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = null, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertIsNotSelected()
        composeRule.onNodeWithText("Not fasting").assertIsNotSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `tapping an option emits its index`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = null,
                onSelect = { observed = it }
            )
        }
        composeRule.onNodeWithText("Exempt").performClick()
        assertThat(observed).isEqualTo(2)
    }

    @Test
    fun `tapping the already selected option still emits so callers can toggle off`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = 0,
                onSelect = { observed = it }
            )
        }
        composeRule.onNodeWithText("Fasted").performClick()
        assertThat(observed).isEqualTo(0)
    }

    @Test
    fun `a disabled control does not emit`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = null,
                onSelect = { observed = it },
                enabled = false
            )
        }
        composeRule.onNodeWithText("Fasted").performClick()
        assertThat(observed).isNull()
    }

    @Test
    fun `a two-option control renders both cells`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options.take(2),
                selectedIndex = 1,
                onSelect = {}
            )
        }
        composeRule.onNodeWithText("Fasted").assertIsNotSelected()
        composeRule.onNodeWithText("Not fasting").assertIsSelected()
    }
}
