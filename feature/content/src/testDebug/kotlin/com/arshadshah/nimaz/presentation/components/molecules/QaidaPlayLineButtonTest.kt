package com.arshadshah.nimaz.presentation.components.molecules

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
class QaidaPlayLineButtonTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders label`() {
        composeRule.setThemedContent {
            QaidaPlayLineButton(onClick = {}, label = "Play line")
        }
        composeRule.onNodeWithText("Play line").assertExists()
    }

    @Test
    fun `click fires callback`() {
        var fired = false
        composeRule.setThemedContent {
            QaidaPlayLineButton(onClick = { fired = true }, label = "Play line")
        }
        composeRule.onNodeWithText("Play line").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `disabled button does not fire callback`() {
        var fired = false
        composeRule.setThemedContent {
            QaidaPlayLineButton(onClick = { fired = true }, enabled = false, label = "Play line")
        }
        composeRule.onNodeWithText("Play line").performClick()
        assertThat(fired).isFalse()
    }

    @Test
    fun `audio ui flag is disabled`() {
        assertThat(QAIDA_AUDIO_UI_ENABLED).isFalse()
    }
}
