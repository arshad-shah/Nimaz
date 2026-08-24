package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The running head over a long document: one pill per section, saying where the reader is.
 *
 * Two things it must not do. It must not appear over a document with only one section — an index
 * of one is a label pretending to be a control. And its pills must not collide: the caller labels
 * them from a small closed vocabulary, so a surah whose source prints two "Background" sections
 * hands the row the same label twice, which a lazy list treats as fatal unless the key carries
 * the position too.
 *
 * [rememberScrollSpyIndex] is the other half. Scrolling *through* a long section must keep naming
 * that section rather than falling back to the one before it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazScrollSpyIndexTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val selections = mutableListOf<Int>()

    private fun render(labels: List<String>, selectedIndex: Int = 0) {
        composeRule.setThemedContent {
            NimazScrollSpyIndex(
                labels = labels,
                selectedIndex = selectedIndex,
                onSelect = { selections += it },
            )
        }
    }

    @Test
    fun `every section gets a pill`() {
        render(listOf("Name", "Revelation", "Theme"))

        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("Revelation").assertIsDisplayed()
        composeRule.onNodeWithText("Theme").assertIsDisplayed()
    }

    @Test
    fun `an index over a single section is not drawn`() {
        // An index of one is a label pretending to be a control.
        render(listOf("Name"))

        composeRule.onNodeWithText("Name").assertDoesNotExist()
    }

    @Test
    fun `an index over nothing is not drawn`() {
        render(emptyList())

        composeRule.onNodeWithText("Name").assertDoesNotExist()
    }

    @Test
    fun `tapping a pill names its position`() {
        render(listOf("Name", "Revelation", "Theme"))

        composeRule.onNodeWithText("Theme").performClick()

        assertThat(selections).containsExactly(2)
    }

    @Test
    fun `two sections with the same heading both survive`() {
        // The label alone was the key once; a surah with two "Background" sections crashed the
        // row rather than showing a duplicate.
        render(listOf("Name", "Background", "Theme", "Background"))

        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onAllNodesWithText("Background").assertCountEquals(2)
    }

    @Test
    fun `the duplicate pills are still told apart when tapped`() {
        render(listOf("Name", "Background", "Theme", "Background"))

        composeRule.onAllNodesWithText("Background")[1].performClick()

        assertThat(selections).containsExactly(3)
    }

    // ---- Which section is being read ----

    @Test
    fun `the spy names the section the reader is in`() {
        var reported = -1
        composeRule.setThemedContent {
            val listState = rememberLazyListState()
            val index by rememberScrollSpyIndex(listState, anchors = listOf(0, 5, 10))
            reported = index
            LazyColumn(state = listState, modifier = Modifier.height(200.dp).testTag("list")) {
                items(20) { Text("row $it", modifier = Modifier.fillMaxWidth().height(40.dp)) }
            }
        }

        assertThat(reported).isEqualTo(0)
    }

    @Test
    fun `scrolling through a long section keeps naming that section`() {
        var reported = -1
        composeRule.setThemedContent {
            val listState = rememberLazyListState()
            val index by rememberScrollSpyIndex(listState, anchors = listOf(0, 5, 15))
            reported = index
            LazyColumn(state = listState, modifier = Modifier.height(200.dp).testTag("list")) {
                items(30) { Text("row $it", modifier = Modifier.fillMaxWidth().height(40.dp)) }
            }
        }

        // Item 9 is past the second anchor but well short of the third: still section two.
        composeRule.onNodeWithTag("list").performScrollToIndex(9)
        composeRule.waitForIdle()

        assertThat(reported).isEqualTo(1)
    }

    @Test
    fun `a document whose first section starts late still names a section`() {
        // indexOfLast returns -1 when nothing is at or before the first visible item; the index
        // must not be a negative pill position.
        var reported = -1
        composeRule.setThemedContent {
            val listState = rememberLazyListState()
            val index by rememberScrollSpyIndex(listState, anchors = listOf(4, 8))
            reported = index
            LazyColumn(state = listState, modifier = Modifier.height(200.dp)) {
                items(20) { Text("row $it", modifier = Modifier.fillMaxWidth().height(40.dp)) }
            }
        }

        assertThat(reported).isEqualTo(0)
    }
}
