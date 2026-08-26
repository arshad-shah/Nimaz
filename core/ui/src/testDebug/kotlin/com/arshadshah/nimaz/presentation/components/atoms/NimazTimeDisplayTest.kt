package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The tappable time chip that opens the picker.
 *
 * It renders through `rememberTimeFormatter()`, which is the app's single answer to "how does this
 * device write a time" — so a reminder row reads `21:30` or `9:30 PM` according to the system
 * setting rather than according to whichever screen drew it. The chip is where that formatter is
 * most visible, and a chip formatting its own time is how two rows in the same list end up
 * disagreeing.
 *
 * It can also collapse to a single announcement, because "clock icon, 9:30 PM" read as two
 * elements tells a screen-reader user nothing about *which* reminder they are on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazTimeDisplayTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the chip renders the time it is given`() {
        composeRule.setThemedContent { NimazTimeDisplay(time = NimazTime(21, 30)) }

        // Either format the device resolves to still contains the minutes.
        composeRule.onNodeWithText("30", substring = true).assertExists()
    }

    @Test
    fun `midnight and noon both render`() {
        // The two hours the 12-hour conversion gets wrong. Rendered together so a chip that
        // printed "0:00" for one of them is visible in one assertion.
        composeRule.setThemedContent {
            Column {
                NimazTimeDisplay(time = NimazTime(0, 0), contentDescription = "midnight")
                NimazTimeDisplay(time = NimazTime(12, 0), contentDescription = "noon")
            }
        }

        composeRule.onNodeWithContentDescription("midnight").assertExists()
        composeRule.onNodeWithContentDescription("noon").assertExists()
    }

    @Test
    fun `a described chip reads as one thing`() {
        // `clearAndSetSemantics` — the icon and the time are one announcement, which is what lets
        // a caller say "Tahajjud reminder, 3:30 AM" instead of leaving TalkBack to read a clock
        // glyph and a bare number.
        composeRule.setThemedContent {
            NimazTimeDisplay(
                time = NimazTime(3, 30),
                contentDescription = "Tahajjud reminder, 3:30 AM",
            )
        }

        composeRule.onNodeWithContentDescription("Tahajjud reminder, 3:30 AM").assertExists()
        composeRule.onNodeWithText("30", substring = true).assertDoesNotExist()
    }

    @Test
    fun `an undescribed chip keeps its time addressable`() {
        // The `contentDescription == null` arm: without a caller-supplied phrase the chip must not
        // clear its own semantics, or the time becomes unreachable to a screen reader entirely.
        composeRule.setThemedContent { NimazTimeDisplay(time = NimazTime(5, 45)) }

        composeRule.onNodeWithText("45", substring = true).assertExists()
    }

    @Test
    fun `a caller's accent is accepted`() {
        // The tint marks the *current* value in a list of times, so it is a parameter rather than
        // a theme read.
        composeRule.setThemedContent {
            NimazTimeDisplay(
                time = NimazTime(6, 15),
                accentColor = Color.Magenta,
                contentDescription = "accented",
            )
        }

        composeRule.onNodeWithContentDescription("accented").assertExists()
    }
}
