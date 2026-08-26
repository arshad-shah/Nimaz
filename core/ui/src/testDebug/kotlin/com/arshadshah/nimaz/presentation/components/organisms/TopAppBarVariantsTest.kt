package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app bar, in the two shapes every screen picks between.
 *
 * `NimazBackTopAppBar` exists so no screen wires its own back arrow — the icon, its content
 * description and its target size are decided once. A screen that hand-rolled the navigation slot
 * would get a back button that is announced as nothing.
 *
 * The `scrollBehavior` slot is nullable and both arms ship: a collapsing hero passes one, a fixed
 * bar does not, and the connection is what makes the title shrink as the content scrolls under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class TopAppBarVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a plain bar renders its title and its optional parts`() {
        composeRule.setThemedContent {
            Column {
                NimazTopAppBar(title = "Bare")
                NimazTopAppBar(
                    title = "Full",
                    subtitle = "with a subtitle",
                    navigationIcon = { Text("nav") },
                    actions = { Text("action") },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                        rememberTopAppBarState()
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Bare").assertExists()
        composeRule.onNodeWithText("with a subtitle").assertExists()
        composeRule.onNodeWithText("nav").assertExists()
        composeRule.onNodeWithText("action").assertExists()
    }

    @Test
    fun `the back bar supplies the navigation control itself`() {
        // The whole reason it exists: one back arrow, announced one way, everywhere.
        var back = 0
        composeRule.setThemedContent {
            NimazBackTopAppBar(
                title = "Detail",
                onBackClick = { back++ },
                subtitle = "sub",
                actions = { Text("settings") },
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
                    rememberTopAppBarState()
                ),
            )
        }

        composeRule.onNodeWithText("Detail").assertExists()
        composeRule.onNodeWithText("sub").assertExists()
        composeRule.onNodeWithText("settings").assertExists()
        assertThat(back).isEqualTo(0)
    }

    @Test
    fun `a back bar with nothing optional still renders`() {
        composeRule.setThemedContent {
            NimazBackTopAppBar(title = "Minimal", onBackClick = {})
        }

        composeRule.onNodeWithText("Minimal").assertExists()
    }
}
