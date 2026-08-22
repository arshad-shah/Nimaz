package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
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

    @Test
    fun `widths and purposes are complete`() {
        assertThat(NimazSegmentedWidth.entries).hasSize(2)
        assertThat(NimazSegmentedPurpose.entries).hasSize(2)
    }

    @Test
    fun `an index past the end of the list selects nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = 9, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertIsNotSelected()
        composeRule.onNodeWithText("Not fasting").assertIsNotSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `a negative index selects nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = -1, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertIsNotSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `an empty option list renders nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = emptyList(), selectedIndex = 0, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertDoesNotExist()
    }

    @Test
    fun `a disabled control reports itself disabled and an enabled one enabled`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = 0,
                onSelect = {},
                enabled = false
            )
        }
        composeRule.onNodeWithText("Exempt").assertIsNotEnabled()
    }

    @Test
    fun `an enabled control reports itself enabled`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = 0, onSelect = {})
        }
        composeRule.onNodeWithText("Exempt").assertIsEnabled()
    }

    @Test
    fun `a view-switching control still emits and renders its labels`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = listOf("Outline", "By kind", "Index").asSegments(),
                selectedIndex = 0,
                onSelect = { observed = it },
                width = NimazSegmentedWidth.WRAP,
                purpose = NimazSegmentedPurpose.VIEW,
            )
        }
        composeRule.onNodeWithText("Outline").assertIsSelected()
        composeRule.onNodeWithText("Index").performClick()
        assertThat(observed).isEqualTo(2)
    }

    @Test
    fun `plain labels take the accent tone by default`() {
        val segments = listOf("One", "Two").asSegments()
        assertThat(segments.map { it.label }).containsExactly("One", "Two").inOrder()
        assertThat(segments.map { it.selectedTone }).containsExactly(
            NimazTone.ACCENT, NimazTone.ACCENT
        )
    }

    @Test
    fun `disabled content colour is faded relative to enabled`() {
        val base = Color(0xFF112233)
        val enabled = resolveSegmentContentColor(base, enabled = true)
        val disabled = resolveSegmentContentColor(base, enabled = false)

        assertThat(enabled).isEqualTo(base)
        assertThat(disabled.alpha).isLessThan(enabled.alpha)
    }
}
