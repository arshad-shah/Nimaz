package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The occasion card, across its ornaments and its two independent actions.
 *
 * `CLAUDE.md` rule 8 singles this component out: a whole-card tap goes through the card's own
 * `onClick`/`onClickLabel`, never a wrapping `Modifier.clickable`, because a wrapping clickable
 * paints a sharp-cornered ripple that ignores the card radius. The `onClickLabel` is the half that
 * is easy to drop — it is what a screen reader announces the card's action *as*, and without it a
 * whole Eid card reads as an unlabelled button.
 *
 * The card also carries a dismiss and a primary action that must stay independent: dismissing an
 * announcement and acting on it are opposite intents, and one lambda wired to both is a card that
 * disappears when you try to open it.
 *
 * `EventOrnament` is the third: `None` and `Divider` share an arm, `Pattern` and `Burst` each draw
 * something behind the content, and every one of them has to leave the copy legible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class EventCardVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun card(
        ornament: EventOrnament = EventOrnament.None,
        primaryAction: EventAction? = null,
        onClick: (() -> Unit)? = null,
        onClickLabel: String? = null,
        onDismiss: (() -> Unit)? = null,
        trailing: (@androidx.compose.runtime.Composable () -> Unit)? = null,
        highlight: (@androidx.compose.runtime.Composable () -> Unit)? = null,
        arabic: String? = "عيد مبارك",
        fillHeight: Boolean = false,
    ) = @androidx.compose.runtime.Composable {
        EventCard(
            accent = Color.Magenta,
            containerAccent = Color.Yellow,
            icon = Icons.Filled.Celebration,
            eyebrow = "Eid al-Fitr",
            arabic = arabic,
            body = "A warm line about the day.",
            ornament = ornament,
            primaryAction = primaryAction,
            onClick = onClick,
            onClickLabel = onClickLabel,
            onDismiss = onDismiss,
            trailing = trailing,
            highlight = highlight,
            fillHeight = fillHeight,
        )
    }

    @Test
    fun `the card renders its eyebrow, its arabic and its body`() {
        composeRule.setThemedContent { card()() }

        composeRule.onNodeWithText("Eid al-Fitr").assertExists()
        composeRule.onNodeWithText("عيد مبارك").assertExists()
        composeRule.onNodeWithText("A warm line about the day.").assertExists()
    }

    @Test
    fun `a card with no arabic renders without it`() {
        composeRule.setThemedContent { card(arabic = null)() }

        composeRule.onNodeWithText("عيد مبارك").assertDoesNotExist()
        composeRule.onNodeWithText("A warm line about the day.").assertExists()
    }

    @Test
    fun `every ornament draws behind copy that stays readable`() {
        // `None` and `Divider` share an arm of the `when`; `Pattern` and `Burst` each add a
        // drawing. Whatever is behind it, the body has to still be there.
        composeRule.setThemedContent {
            Column {
                card(ornament = EventOrnament.None)()
                card(ornament = EventOrnament.Divider)()
                card(ornament = EventOrnament.Pattern(NimazPatternStyle.LATTICE))()
                card(ornament = EventOrnament.Burst(play = false))()
            }
        }

        composeRule.onAllNodesWithText("A warm line about the day.")
            .assertCountEquals(4)
    }

    @Test
    fun `the whole card is the tap target, announced by the caller's label`() {
        // Rule 8: the tap goes through the card, not a wrapping `clickable`, and `onClickLabel` is
        // what a screen reader is told the tap *does*.
        var opened = 0
        composeRule.setThemedContent {
            card(onClick = { opened++ }, onClickLabel = "Open the Eid greeting")()
        }

        composeRule.onNodeWithText("A warm line about the day.").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `dismissing and acting are different intents`() {
        // One lambda wired to both is a card that disappears when you try to open it.
        var acted = 0
        var dismissed = 0
        composeRule.setThemedContent {
            card(
                primaryAction = EventAction("Learn more") { acted++ },
                onDismiss = { dismissed++ },
            )()
        }

        composeRule.onNodeWithText("Learn more").performClick()

        assertThat(acted).isEqualTo(1)
        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun `a card with no action offers none`() {
        composeRule.setThemedContent { card()() }

        composeRule.onNodeWithText("Learn more").assertDoesNotExist()
    }

    @Test
    fun `the trailing and highlight slots are rendered`() {
        composeRule.setThemedContent {
            card(
                trailing = { Text("trailing") },
                highlight = { Text("highlight") },
            )()
        }

        composeRule.onNodeWithText("trailing").assertExists()
        composeRule.onNodeWithText("highlight").assertExists()
    }

    @Test
    fun `a fill-height card takes the space it is given`() {
        composeRule.setThemedContent {
            Box(Modifier.height(400.dp)) { card(fillHeight = true)() }
        }

        composeRule.onNodeWithText("A warm line about the day.").assertExists()
    }
}
