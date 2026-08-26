package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sheet's own layout logic — the part of `NimazSheet.kt` that is arithmetic rather than
 * decoration.
 *
 * `NimazSheetActionGrid` builds its rows by hand instead of using a `LazyVerticalGrid`, because
 * the sheet body already scrolls and nesting a scrollable inside it is a measurement error rather
 * than a style choice. That hand-rolled pairing is the thing worth testing: it holds one action
 * back until it can pair it, flushes the pending one when a **wide** action interrupts, and
 * flushes again at the end. Get the flush order wrong and a wide action jumps above the one that
 * was declared before it — the actions reorder themselves, which on a delete/share sheet is how
 * somebody taps the wrong thing.
 *
 * `NimazSheetActionRow` has a smaller version of the same decision: past five actions it becomes
 * horizontally scrollable, because `SpaceEvenly` on six pills squeezes them to unreadable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazSheetLayoutTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun action(
        label: String,
        wide: Boolean = false,
        selected: Boolean = false,
        tint: Color? = null,
        onClick: () -> Unit = {},
    ) = NimazSheetAction(
        icon = Icons.Filled.Star,
        label = label,
        onClick = onClick,
        tint = tint,
        selected = selected,
        wide = wide,
    )

    @Test
    fun `an action grid renders every action it is given`() {
        composeRule.setThemedContent {
            NimazSheetActionGrid(
                actions = listOf(action("Share"), action("Edit"), action("Delete")),
            )
        }

        listOf("Share", "Edit", "Delete").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `an odd count keeps the grid a grid`() {
        // The lone trailing action holds its half of the row rather than stretching across it —
        // three actions must not render as two full-width ones and a wide stray.
        composeRule.setThemedContent {
            NimazSheetActionGrid(actions = listOf(action("A"), action("B"), action("C")))
        }

        val a = composeRule.onNodeWithText("A").fetchSemanticsNode().size.width
        val c = composeRule.onNodeWithText("C").fetchSemanticsNode().size.width

        assertThat(c).isEqualTo(a)
    }

    @Test
    fun `a wide action takes a row of its own without reordering the ones around it`() {
        // The pending flush. A wide action interrupting a half-built pair has to emit the pending
        // action *first*; emitting the wide one first silently swaps two rows of a delete sheet.
        composeRule.setThemedContent {
            NimazSheetActionGrid(
                actions = listOf(
                    action("First"),
                    action("Wide", wide = true),
                    action("Third"),
                    action("Fourth"),
                ),
            )
        }

        val first = composeRule.onNodeWithText("First").fetchSemanticsNode().positionInRoot.y
        val wide = composeRule.onNodeWithText("Wide").fetchSemanticsNode().positionInRoot.y
        val third = composeRule.onNodeWithText("Third").fetchSemanticsNode().positionInRoot.y

        assertThat(first).isLessThan(wide)
        assertThat(wide).isLessThan(third)
    }

    @Test
    fun `a wide action fills its row`() {
        composeRule.setThemedContent {
            NimazSheetActionGrid(actions = listOf(action("Wide", wide = true), action("Half")))
        }

        val wide = composeRule.onNodeWithText("Wide").fetchSemanticsNode().size.width
        val half = composeRule.onNodeWithText("Half").fetchSemanticsNode().size.width

        assertThat(wide).isGreaterThan(half)
    }

    @Test
    fun `a pair shares a row`() {
        composeRule.setThemedContent {
            NimazSheetActionGrid(actions = listOf(action("Left"), action("Right")))
        }

        val left = composeRule.onNodeWithText("Left").fetchSemanticsNode().positionInRoot
        val right = composeRule.onNodeWithText("Right").fetchSemanticsNode().positionInRoot

        assertThat(left.y).isEqualTo(right.y)
        assertThat(right.x).isGreaterThan(left.x)
    }

    @Test
    fun `each action tile runs its own callback`() {
        var shared = 0
        var deleted = 0
        composeRule.setThemedContent {
            NimazSheetActionGrid(
                actions = listOf(
                    action("Share", onClick = { shared++ }),
                    action("Delete", onClick = { deleted++ }),
                ),
            )
        }

        composeRule.onNodeWithText("Delete").performClick()

        assertThat(deleted).isEqualTo(1)
        assertThat(shared).isEqualTo(0)
    }

    @Test
    fun `a selected tile and a tinted one both render`() {
        // The three-arm `when` for the tile's content colour: an explicit tint wins over selection,
        // which wins over the default. A destructive action carries its own tint and must not be
        // repainted by selection.
        composeRule.setThemedContent {
            NimazSheetActionGrid(
                actions = listOf(
                    action("Selected", selected = true),
                    action("Tinted", tint = Color.Red),
                    action("Both", selected = true, tint = Color.Red),
                    action("Plain"),
                ),
            )
        }

        listOf("Selected", "Tinted", "Both", "Plain").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `an action row spreads a few pills and scrolls many`() {
        // Five is the boundary: at five or fewer the pills are spread evenly, past it the row
        // scrolls rather than squeezing them. Both arms are composed, and the pill's label is its
        // own content description so it is addressable either way.
        composeRule.setThemedContent {
            Column {
                NimazSheetActionRow(
                    actions = listOf(action("One"), action("Two"), action("Three")),
                )
                NimazSheetActionRow(
                    actions = (1..6).map { action("Pill $it") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("One").assertExists()
        composeRule.onNodeWithContentDescription("Pill 6").assertExists()
    }

    @Test
    fun `a selected pill renders differently without losing its label`() {
        composeRule.setThemedContent {
            NimazSheetActionRow(
                actions = listOf(action("Bookmarked", selected = true), action("Plain")),
            )
        }

        composeRule.onNodeWithContentDescription("Bookmarked").assertExists()
        composeRule.onNodeWithContentDescription("Plain").assertExists()
    }

    @Test
    fun `footer buttons offer both actions and run the right one`() {
        var confirmed = 0
        var cancelled = 0
        composeRule.setThemedContent {
            NimazSheetFooterButtons(
                primaryText = "Save",
                onPrimary = { confirmed++ },
                secondaryText = "Cancel",
                onSecondary = { cancelled++ },
            )
        }

        composeRule.onNodeWithText("Cancel").performClick()
        assertThat(cancelled).isEqualTo(1)
        assertThat(confirmed).isEqualTo(0)

        composeRule.onNodeWithText("Save").performClick()
        assertThat(confirmed).isEqualTo(1)
    }

    @Test
    fun `a footer with no secondary shows only the primary`() {
        // Gated on *both* the label and the callback being present — half a pair would render a
        // button that does nothing.
        composeRule.setThemedContent {
            NimazSheetFooterButtons(primaryText = "Save", onPrimary = {}, secondaryText = "Cancel")
        }

        composeRule.onNodeWithText("Save").assertExists()
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun `a disabled primary cannot be pressed`() {
        composeRule.setThemedContent {
            NimazSheetFooterButtons(
                primaryText = "Save",
                onPrimary = {},
                primaryEnabled = false,
            )
        }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `a destructive primary is still pressable`() {
        // The destructive variant only changes the paint; a delete button that stopped working
        // would be reported as "nothing happens", not as a styling bug.
        var deleted = 0
        composeRule.setThemedContent {
            NimazSheetFooterButtons(
                primaryText = "Delete",
                onPrimary = { deleted++ },
                isDestructive = true,
            )
        }

        composeRule.onNodeWithText("Delete").assertIsEnabled().performClick()
        assertThat(deleted).isEqualTo(1)
    }

    @Test
    fun `a header badge and a close button coexist`() {
        // Both are optional and both sit at the trailing edge, with a spacer between them that is
        // itself conditional — the one place two independent nullables interact.
        var closed = 0
        composeRule.setThemedContent {
            NimazSheetHeader(
                title = "Bookmarks",
                subtitle = "12 saved",
                icon = Icons.Filled.Bookmark,
                badge = "12",
                onClose = { closed++ },
            )
        }

        composeRule.onNodeWithText("Bookmarks").assertIsDisplayed()
        composeRule.onNodeWithText("12 saved").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_close)).performClick()
        assertThat(closed).isEqualTo(1)
    }

    @Test
    fun `a bare header is just its title`() {
        composeRule.setThemedContent { NimazSheetHeader(title = "Bookmarks") }

        composeRule.onNodeWithText("Bookmarks").assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_close))
            .assertDoesNotExist()
    }

    @Test
    fun `a preview card wraps whatever it is given`() {
        composeRule.setThemedContent {
            NimazSheetPreviewCard { Text("inside the card") }
        }

        composeRule.onNodeWithText("inside the card").assertExists()
    }
}
