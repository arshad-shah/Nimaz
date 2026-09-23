package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
 * The A–Z rail beside a long list.
 *
 * What is pinned is what makes it trustworthy: a scrub lands only on letters that have entries
 * (a jump never opens onto nothing), the bubble shows while the finger is down and names the
 * letter, the rail says which letter is current, and each letter is reachable by TalkBack as its
 * own "Jump to M" — a drag is not something a screen reader can perform.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazIndexRailTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun str(id: Int, vararg args: Any) =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val letters = listOf('A', 'B', 'C', 'D')
    private val selected = mutableListOf<Char>()

    private fun render(available: Set<Char> = setOf('A', 'B', 'D'), current: Char? = 'A') {
        composeRule.setThemedContent {
            Box(Modifier.height(400.dp)) {
                NimazIndexRail(
                    letters = letters,
                    available = available,
                    current = current,
                    onSelect = { selected += it },
                    bubbleLabel = { "$it entries" },
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("rail"),
                )
            }
        }
    }

    @Test
    fun `each letter is its own jump for TalkBack`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_index_rail_jump, "B")).performClick()

        assertThat(selected).containsExactly('B')
    }

    @Test
    fun `a letter with nothing under it offers no jump`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_index_rail_jump, "C"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun `the current letter is marked as selected`() {
        render(current = 'B')

        composeRule.onNodeWithContentDescription(str(R.string.cd_index_rail_jump, "B"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.onNodeWithContentDescription(str(R.string.cd_index_rail_jump, "A"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun `scrubbing hands over each letter passed, once`() {
        render()

        composeRule.onNodeWithTag("rail").performTouchInput {
            down(center.copy(y = height * 0.1f)) // A
            moveTo(center.copy(y = height * 0.12f)) // still A
            moveTo(center.copy(y = height * 0.35f)) // B
            up()
        }

        assertThat(selected).containsExactly('A', 'B').inOrder()
    }

    @Test
    fun `scrubbing over an empty letter lands on the nearest one with entries`() {
        render()

        composeRule.onNodeWithTag("rail").performTouchInput {
            down(center.copy(y = height * 0.6f)) // C, which is empty
            up()
        }

        assertThat(selected).hasSize(1)
        assertThat(selected.single()).isAnyOf('B', 'D')
    }

    @Test
    fun `the bubble names the letter while the finger is down, and goes when it lifts`() {
        render()

        composeRule.onNodeWithTag("rail").performTouchInput { down(center.copy(y = height * 0.9f)) }
        composeRule.onNodeWithText("D entries").assertExists()

        composeRule.onNodeWithTag("rail").performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("D entries").assertDoesNotExist()
    }
}
