package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSwitchTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `switch variants are complete`() {
        assertThat(NimazSwitchVariant.entries).hasSize(4)
    }

    @Test
    fun `checked switch reports the on toggle state`() {
        composeRule.setThemedContent {
            NimazSwitch(checked = true, onCheckedChange = {}, contentDescription = "on")
        }
        composeRule.onNodeWithContentDescription("on").assertIsOn()
    }

    @Test
    fun `unchecked switch reports the off toggle state`() {
        composeRule.setThemedContent {
            NimazSwitch(checked = false, onCheckedChange = {}, contentDescription = "off")
        }
        composeRule.onNodeWithContentDescription("off").assertIsOff()
    }

    @Test
    fun `clicking an unchecked switch emits true`() {
        var observed: Boolean? = null
        composeRule.setThemedContent {
            NimazSwitch(
                checked = false,
                onCheckedChange = { observed = it },
                contentDescription = "toggle"
            )
        }
        composeRule.onNodeWithContentDescription("toggle").performClick()
        assertThat(observed).isTrue()
    }

    @Test
    fun `clicking a checked switch emits false`() {
        var observed: Boolean? = null
        composeRule.setThemedContent {
            NimazSwitch(
                checked = true,
                onCheckedChange = { observed = it },
                contentDescription = "toggle"
            )
        }
        composeRule.onNodeWithContentDescription("toggle").performClick()
        assertThat(observed).isFalse()
    }

    @Test
    fun `null handler renders a non-interactive switch deferring to the parent`() {
        composeRule.setThemedContent {
            NimazSwitch(checked = true, onCheckedChange = null, contentDescription = "deferred")
        }
        // With no own handler the switch carries no toggle role/click target — it simply
        // renders its state for the enclosing clickable row to drive.
        composeRule.onNodeWithContentDescription("deferred").assertExists()
    }

    @Test
    fun `disabled switch does not emit on click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazSwitch(
                checked = false,
                onCheckedChange = { clicked = true },
                enabled = false,
                contentDescription = "disabled"
            )
        }
        composeRule.onNodeWithContentDescription("disabled").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `every variant renders checked and emits on click`() {
        val clicked = mutableSetOf<NimazSwitchVariant>()
        composeRule.setThemedContent {
            Column {
                NimazSwitchVariant.entries.forEach { variant ->
                    NimazSwitch(
                        checked = true,
                        onCheckedChange = { clicked += variant },
                        variant = variant,
                        contentDescription = variant.name
                    )
                }
            }
        }
        NimazSwitchVariant.entries.forEach { variant ->
            composeRule.onNodeWithContentDescription(variant.name).performClick()
        }
        assertThat(clicked).containsExactlyElementsIn(NimazSwitchVariant.entries)
    }

    @Test
    fun `plain thumb without a glyph still toggles`() {
        var observed: Boolean? = null
        composeRule.setThemedContent {
            NimazSwitch(
                checked = false,
                onCheckedChange = { observed = it },
                thumbIcon = null,
                contentDescription = "plain"
            )
        }
        composeRule.onNodeWithContentDescription("plain").assertIsOff().performClick()
        assertThat(observed).isTrue()
    }

    @Test
    fun `track tint escape hatch still renders an interactive switch`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazSwitch(
                checked = true,
                onCheckedChange = { clicked = true },
                trackTint = NimazColors.Success,
                contentDescription = "tinted"
            )
        }
        composeRule.onNodeWithContentDescription("tinted").assertIsOn().performClick()
        assertThat(clicked).isTrue()
    }
}
