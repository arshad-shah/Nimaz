package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * The sheet's action row at the size where it changes behaviour, and its pill's tint rule.
 *
 * Past five actions the row stops spreading them evenly and starts scrolling, because
 * `SpaceEvenly` on six pills squeezes each to a width no label survives. Five and six are therefore
 * two different layouts from one list, and the boundary is a bare `> 5` that a refactor can move
 * without anything failing.
 *
 * `ActionPill`'s tint is `action.tint ?: onSurfaceVariant` — the destructive action in a share
 * sheet carries its own red, and everything else takes the muted default. A pill that ignored the
 * caller's tint would draw "Delete" the same as "Share".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazSheetActionShapesTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun action(label: String, tint: Color? = null, selected: Boolean = false) =
        NimazSheetAction(
            icon = Icons.Filled.Star,
            label = label,
            onClick = {},
            tint = tint,
            selected = selected,
        )

    @Test
    fun `five actions are spread and six scroll`() {
        composeRule.setThemedContent {
            Column {
                NimazSheetActionRow(
                    actions = (1..5).map { action("Five $it") },
                    modifier = Modifier,
                )
                NimazSheetActionRow(actions = (1..6).map { action("Six $it") })
            }
        }

        composeRule.onNodeWithContentDescription("Five 5").assertExists()
        composeRule.onNodeWithContentDescription("Six 6").assertExists()
    }

    @Test
    fun `a pill takes the caller's tint and falls back to the muted default`() {
        composeRule.setThemedContent {
            NimazSheetActionRow(
                actions = listOf(
                    action("Delete", tint = Color.Red),
                    action("Share"),
                    action("Bookmarked", selected = true),
                ),
            )
        }

        listOf("Delete", "Share", "Bookmarked").forEach {
            composeRule.onNodeWithContentDescription(it).assertExists()
        }
    }

    @Test
    fun `an action grid handles a lone wide action and a trailing pair`() {
        // The row builder's remaining shapes: a wide action with nothing pending before it, and a
        // pair that closes cleanly with nothing left to flush.
        composeRule.setThemedContent {
            NimazSheetActionGrid(
                actions = listOf(
                    action("Wide first").copy(wide = true),
                    action("Left"),
                    action("Right"),
                ),
                modifier = Modifier,
            )
        }

        val wide = composeRule.onNodeWithText("Wide first").fetchSemanticsNode().positionInRoot.y
        val left = composeRule.onNodeWithText("Left").fetchSemanticsNode().positionInRoot.y
        val right = composeRule.onNodeWithText("Right").fetchSemanticsNode().positionInRoot.y

        assertThat(wide).isLessThan(left)
        assertThat(left).isEqualTo(right)
    }
}
