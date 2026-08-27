package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlassPillTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders label text`() {
        composeRule.setThemedContent { GlassPill(text = "Manchester, UK") }
        composeRule.onNodeWithText("Manchester, UK").assertExists()
    }

    @Test
    fun `renders with leading icon`() {
        composeRule.setThemedContent {
            GlassPill(text = "Located", leadingIcon = Icons.Filled.Place)
        }
        composeRule.onNodeWithText("Located").assertExists()
    }

    @Test
    fun `onClick fires when tapped`() {
        var fired = false
        composeRule.setThemedContent {
            GlassPill(text = "Tap me", onClick = { fired = true })
        }
        composeRule.onNodeWithText("Tap me").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `GlassPillTone has three entries`() {
        assertThat(GlassPillTone.entries).hasSize(3)
    }

    @Test
    fun `GlassPillSize has two entries`() {
        assertThat(GlassPillSize.entries).hasSize(2)
    }

    @Test
    fun `GlassIconButton fires onClick`() {
        var fired = false
        composeRule.setThemedContent {
            GlassIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                onClick = { fired = true },
            )
        }
        composeRule.onRoot().assertExists()
        composeRule.waitForIdle()
        composeRule.onNode(hasContentDescription("Settings")).performClick()
        assertThat(fired).isTrue()
    }
}
