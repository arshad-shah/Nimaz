package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.components.atoms.createComponentComposeRule
import com.arshadshah.nimaz.presentation.components.atoms.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSegmentedTabsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val tabs = listOf("Outline", "By kind", "Index")

    @Test
    fun `the selected segment reports the selected state`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 1, onTabSelect = {})
        }
        composeRule.onNodeWithText("By kind").assertIsSelected()
        composeRule.onNodeWithText("Outline").assertIsNotSelected()
        composeRule.onNodeWithText("Index").assertIsNotSelected()
    }

    @Test
    fun `tapping a segment reports its index`() {
        var picked = -1
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelect = { picked = it })
        }
        composeRule.onNodeWithText("Index").performClick()
        assertThat(picked).isEqualTo(2)
    }

    @Test
    fun `tapping the already-selected segment still reports it`() {
        var calls = 0
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelect = { calls++ })
        }
        composeRule.onNodeWithText("Outline").performClick()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `disabled tabs do not report taps`() {
        var calls = 0
        composeRule.setThemedContent {
            NimazSegmentedTabs(
                tabs = tabs,
                selectedIndex = 0,
                onTabSelect = { calls++ },
                enabled = false,
            )
        }
        composeRule.onNodeWithText("Index").assertIsNotEnabled()
        composeRule.onNodeWithText("Index").performClick()
        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun `enabled tabs are enabled`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelect = {})
        }
        composeRule.onNodeWithText("Index").assertIsEnabled()
    }

    @Test
    fun `an out-of-range selection selects nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 9, onTabSelect = {})
        }
        composeRule.onNodeWithText("Outline").assertIsNotSelected()
        composeRule.onNodeWithText("By kind").assertIsNotSelected()
        composeRule.onNodeWithText("Index").assertIsNotSelected()
    }

    @Test
    fun `a negative selection selects nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = -1, onTabSelect = {})
        }
        composeRule.onNodeWithText("Outline").assertIsNotSelected()
        composeRule.onNodeWithText("By kind").assertIsNotSelected()
        composeRule.onNodeWithText("Index").assertIsNotSelected()
    }

    @Test
    fun `an empty tab list renders nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = emptyList(), selectedIndex = 0, onTabSelect = {})
        }
        composeRule.onNodeWithText("Outline").assertDoesNotExist()
    }
}
