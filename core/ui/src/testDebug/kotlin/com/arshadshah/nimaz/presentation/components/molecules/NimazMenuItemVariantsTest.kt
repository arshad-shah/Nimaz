package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotEnabled
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
 * The menu row, across the shapes the More and Settings hubs actually build.
 *
 * `CLAUDE.md` rule 8 makes this component *mandatory* — a hub row is a `NimazMenuItem`, never a
 * hand-rolled `Row` with a `Modifier.clickable`, because the hand-rolled version got the ripple
 * radius and the target size wrong. That makes its own options the app's row vocabulary: a
 * trailing chevron by default (the "this opens something" signal), a trailing *slot* for a switch
 * or a value, a disabled state, and a selected state.
 *
 * The chevron default is the one worth stating. `trailingIcon` defaults to `NimazIcons.Forward`,
 * so a row that opens a screen needs no argument at all — and a row that does *not* navigate has
 * to pass `null` deliberately, or it promises a destination it does not have.
 *
 * `NimazMenuDivider`'s inset is the other: `inset = true` clears the icon column, and rows with no
 * icon pass `false` so the rule does not start two-thirds of the way across.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazMenuItemVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a row renders its title, its subtitle and its icon`() {
        composeRule.setThemedContent {
            NimazMenuGroup {
                NimazMenuItem(
                    title = "Prayer Tracker",
                    subtitle = "Track your daily prayers",
                    icon = Icons.Filled.Schedule,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Prayer Tracker").assertExists()
        composeRule.onNodeWithText("Track your daily prayers").assertExists()
    }

    @Test
    fun `a bare row is just its title`() {
        composeRule.setThemedContent { NimazMenuItem(title = "Bare", onClick = {}) }

        composeRule.onNodeWithText("Bare").assertExists()
    }

    @Test
    fun `tapping the row runs its action`() {
        var opened = 0
        composeRule.setThemedContent {
            NimazMenuItem(title = "Prayer Tracker", onClick = { opened++ })
        }

        composeRule.onNodeWithText("Prayer Tracker").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `a disabled row cannot be tapped`() {
        var opened = 0
        composeRule.setThemedContent {
            NimazMenuItem(title = "Locked", onClick = { opened++ }, enabled = false)
        }

        composeRule.onNodeWithText("Locked").assertIsNotEnabled()
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `a row can carry its own trailing content instead of the chevron`() {
        // The switch/value slot. A row that showed both would promise navigation *and* offer an
        // inline control, which is two different affordances in one target.
        composeRule.setThemedContent {
            NimazMenuGroup {
                NimazMenuItem(
                    title = "Notifications",
                    onClick = {},
                    trailing = { Text("On") },
                )
            }
        }

        composeRule.onNodeWithText("On").assertExists()
    }

    @Test
    fun `a row that opens nothing can drop the chevron`() {
        // `trailingIcon = null` is the deliberate opt-out. Nothing else in the app says a row is
        // terminal.
        composeRule.setThemedContent {
            NimazMenuGroup {
                NimazMenuItem(title = "Terminal", onClick = {}, trailingIcon = null)
                NimazMenuDivider(inset = false)
                NimazMenuItem(title = "Navigates", onClick = {})
            }
        }

        composeRule.onNodeWithText("Terminal").assertExists()
        composeRule.onNodeWithText("Navigates").assertExists()
    }

    @Test
    fun `a selected row and a tinted icon both render`() {
        composeRule.setThemedContent {
            NimazMenuGroup {
                NimazMenuItem(
                    title = "Selected",
                    icon = Icons.Filled.Schedule,
                    iconTint = Color.Magenta,
                    selected = true,
                    onClick = {},
                )
                NimazMenuItem(
                    title = "Styled subtitle",
                    subtitle = "smaller",
                    subtitleStyle = MaterialTheme.typography.labelSmall,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Selected").assertExists()
        composeRule.onNodeWithText("smaller").assertExists()
    }

    @Test
    fun `the divider insets past the icon column, or does not`() {
        // `inset = true` clears the 56dp icon column so the rule starts under the text; rows with
        // no icon pass `false` so it does not begin two-thirds of the way across. The two insets
        // are different constants and a single value for both is visible on every settings screen.
        assertThat(NimazMenuDefaults.RowDividerInset.value)
            .isGreaterThan(NimazMenuDefaults.SectionDividerInset.value)

        composeRule.setThemedContent {
            NimazMenuGroup {
                NimazMenuItem(title = "Above", icon = Icons.Filled.Schedule, onClick = {})
                NimazMenuDivider()
                NimazMenuItem(title = "Below", icon = Icons.Filled.Schedule, onClick = {})
                NimazMenuDivider(inset = false)
                NimazMenuItem(title = "Section", onClick = {})
            }
        }

        composeRule.onNodeWithText("Section").assertExists()
    }
}
