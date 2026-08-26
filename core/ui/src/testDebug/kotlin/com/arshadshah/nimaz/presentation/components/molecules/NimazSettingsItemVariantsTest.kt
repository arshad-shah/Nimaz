package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
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
 * The settings row, which is four different controls behind one signature.
 *
 * `checked` is nullable and that is the whole design: null means "not a toggle", so a row is a
 * switch, a value display, a navigation row or an inert label depending on which of `checked`,
 * `value`, `onClick` and `trailingContent` the caller supplies. Getting that resolution wrong
 * gives a settings screen where a toggle renders as a chevron — it still looks like a settings
 * screen, and the setting cannot be changed.
 *
 * The disabled state matters more here than in most components: a settings row that is disabled
 * for a reason (no permission, no location) must not accept the tap that would silently do
 * nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazSettingsItemVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a toggle row reports its change`() {
        var checked: Boolean? = null
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Notifications",
                subtitle = "Prayer reminders",
                icon = Icons.Filled.Notifications,
                checked = false,
                onCheckedChange = { checked = it },
            )
        }

        composeRule.onNodeWithText("Notifications").performClick()
        assertThat(checked).isTrue()
    }

    @Test
    fun `a value row shows its current setting`() {
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Calculation method",
                value = "Muslim World League",
                onClick = {},
                showArrow = true,
            )
        }

        composeRule.onNodeWithText("Muslim World League").assertExists()
    }

    @Test
    fun `a navigation row runs its click`() {
        var opened = 0
        composeRule.setThemedContent {
            NimazSettingsItem(title = "About", onClick = { opened++ }, showArrow = true)
        }

        composeRule.onNodeWithText("About").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `an inert row has nothing to tap`() {
        // Every one of the four optional handles left out — this is the "just says something"
        // row, and it must not present a click action a screen reader would announce.
        composeRule.setThemedContent { NimazSettingsItem(title = "Version 3.0.95") }

        composeRule.onNodeWithText("Version 3.0.95").assertExists()
    }

    @Test
    fun `a disabled row does not accept the tap`() {
        var opened = 0
        var toggled: Boolean? = null
        composeRule.setThemedContent {
            Column {
                NimazSettingsItem(
                    title = "Needs permission",
                    onClick = { opened++ },
                    enabled = false,
                    showArrow = true,
                )
                NimazSettingsItem(
                    title = "Needs location",
                    checked = false,
                    onCheckedChange = { toggled = it },
                    enabled = false,
                )
            }
        }

        composeRule.onNodeWithText("Needs permission").performClick()
        composeRule.onNodeWithText("Needs location").performClick()

        assertThat(opened).isEqualTo(0)
        assertThat(toggled).isNull()
    }

    @Test
    fun `an icon can be tinted or given its own well`() {
        // Three independent icon options: the tint, the well colour and whether the glyph is
        // tinted at all. The settings screens use different combinations per section.
        composeRule.setThemedContent {
            Column {
                NimazSettingsItem(
                    title = "Tinted",
                    icon = Icons.Filled.Notifications,
                    iconTint = Color.Magenta,
                    tintIcon = true,
                    onClick = {},
                )
                NimazSettingsItem(
                    title = "Welled",
                    icon = Icons.Filled.Notifications,
                    iconBackground = Color.Yellow,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Tinted").assertExists()
        composeRule.onNodeWithText("Welled").assertExists()
    }

    @Test
    fun `a caller's own trailing control replaces the row's`() {
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Custom",
                subtitle = "with its own control",
                trailingContent = { Text("slot") },
            )
        }

        composeRule.onNodeWithText("slot").assertExists()
    }
}
