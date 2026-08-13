package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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
class NimazTreeNodeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a collapsed node does not compose its children`() {
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                count = 214,
                expanded = false,
                onToggleExpand = {},
            ) { Text("God") }
        }
        composeRule.onNodeWithText("Doctrine").assertIsDisplayed()
        composeRule.onNodeWithText("God").assertDoesNotExist()
    }

    @Test
    fun `an expanded node composes its children`() {
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                count = 214,
                expanded = true,
                onToggleExpand = {},
            ) { Text("God") }
        }
        composeRule.onNodeWithText("God").assertIsDisplayed()
    }

    @Test
    fun `the count is rendered when supplied`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "Doctrine", count = 214, onToggleExpand = {})
        }
        composeRule.onNodeWithText("214").assertIsDisplayed()
    }

    @Test
    fun `no count is rendered when absent`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "Doctrine", onToggleExpand = {})
        }
        composeRule.onNodeWithText("214").assertDoesNotExist()
    }

    @Test
    fun `a leaf node has no expand control`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "The hereafter", count = 54, onToggleExpand = null)
        }
        composeRule.onNodeWithContentDescription("Expand The hereafter").assertDoesNotExist()
    }

    @Test
    fun `the chevron toggles expansion`() {
        var toggles = 0
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                expanded = false,
                onToggleExpand = { toggles++ },
                onClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Expand Doctrine").performClick()
        assertThat(toggles).isEqualTo(1)
    }

    @Test
    fun `the chevron announces collapse when expanded`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "Doctrine", expanded = true, onToggleExpand = {})
        }
        composeRule.onNodeWithContentDescription("Collapse Doctrine").assertIsDisplayed()
    }

    @Test
    fun `the label navigates when onClick is supplied`() {
        var opened = 0
        var toggles = 0
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                onToggleExpand = { toggles++ },
                onClick = { opened++ },
            )
        }
        composeRule.onNodeWithText("Doctrine").performClick()
        assertThat(opened).isEqualTo(1)
        assertThat(toggles).isEqualTo(0)
    }

    @Test
    fun `the whole row toggles when there is nothing to navigate to`() {
        var toggles = 0
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                onToggleExpand = { toggles++ },
                onClick = null,
            )
        }
        composeRule.onNodeWithText("Doctrine").performClick()
        assertThat(toggles).isEqualTo(1)
    }
}
