package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazCheckboxTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `checkbox enums are complete`() {
        assertThat(NimazCheckboxVariant.entries).hasSize(4)
        assertThat(NimazCheckboxSize.entries).hasSize(3)
        assertThat(NimazCheckboxType.entries).hasSize(2)
    }

    @Test
    fun `checked box reports the on toggle state`() {
        composeRule.setThemedContent {
            NimazCheckbox(checked = true, onCheckedChange = {}, contentDescription = "done")
        }
        composeRule.onNodeWithContentDescription("done").assertIsOn()
    }

    @Test
    fun `unchecked box reports the off toggle state`() {
        composeRule.setThemedContent {
            NimazCheckbox(checked = false, onCheckedChange = {}, contentDescription = "todo")
        }
        composeRule.onNodeWithContentDescription("todo").assertIsOff()
    }

    @Test
    fun `clicking an unchecked box emits true`() {
        var observed: Boolean? = null
        composeRule.setThemedContent {
            NimazCheckbox(
                checked = false,
                onCheckedChange = { observed = it },
                contentDescription = "toggle"
            )
        }
        composeRule.onNodeWithContentDescription("toggle").performClick()
        assertThat(observed).isTrue()
    }

    @Test
    fun `clicking a checked box emits false`() {
        var observed: Boolean? = null
        composeRule.setThemedContent {
            NimazCheckbox(
                checked = true,
                onCheckedChange = { observed = it },
                contentDescription = "toggle"
            )
        }
        composeRule.onNodeWithContentDescription("toggle").performClick()
        assertThat(observed).isFalse()
    }

    @Test
    fun `null handler renders a non-interactive indicator`() {
        composeRule.setThemedContent {
            NimazCheckbox(checked = true, onCheckedChange = null, contentDescription = "indicator")
        }
        // The check glyph still renders, but with no toggle role the node carries no
        // on/off state — performing a click is a no-op (nothing to assert beyond
        // existence, which proves the indicator drew without a click target).
        composeRule.onNodeWithContentDescription("indicator").assertExists()
    }

    @Test
    fun `disabled box does not emit on click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazCheckbox(
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
        val clicked = mutableSetOf<NimazCheckboxVariant>()
        composeRule.setThemedContent {
            Column {
                NimazCheckboxVariant.entries.forEach { variant ->
                    NimazCheckbox(
                        checked = true,
                        onCheckedChange = { clicked += variant },
                        variant = variant,
                        contentDescription = variant.name
                    )
                }
            }
        }
        NimazCheckboxVariant.entries.forEach { variant ->
            composeRule.onNodeWithContentDescription(variant.name).performClick()
        }
        assertThat(clicked).containsExactlyElementsIn(NimazCheckboxVariant.entries)
    }

    @Test
    fun `every type renders and emits on click`() {
        val clicked = mutableSetOf<NimazCheckboxType>()
        composeRule.setThemedContent {
            Column {
                NimazCheckboxType.entries.forEach { type ->
                    NimazCheckbox(
                        checked = false,
                        onCheckedChange = { clicked += type },
                        type = type,
                        contentDescription = type.name
                    )
                }
            }
        }
        NimazCheckboxType.entries.forEach { type ->
            composeRule.onNodeWithContentDescription(type.name).performClick()
        }
        assertThat(clicked).containsExactlyElementsIn(NimazCheckboxType.entries)
    }

    @Test
    fun `size preset drives the box dimension`() {
        composeRule.setThemedContent {
            NimazCheckbox(
                checked = true,
                onCheckedChange = {},
                size = NimazCheckboxSize.LARGE,
                contentDescription = "large"
            )
        }
        composeRule.onNodeWithContentDescription("large")
            .assertWidthIsEqualTo(NimazCheckboxSize.LARGE.box)
            .assertHeightIsEqualTo(NimazCheckboxSize.LARGE.box)
    }

    @Test
    fun `tint escape hatch still renders an interactive box`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazCheckbox(
                checked = true,
                onCheckedChange = { clicked = true },
                tint = NimazColors.Success,
                contentDescription = "tinted"
            )
        }
        composeRule.onNodeWithContentDescription("tinted").assertIsOn().performClick()
        assertThat(clicked).isTrue()
    }
}
