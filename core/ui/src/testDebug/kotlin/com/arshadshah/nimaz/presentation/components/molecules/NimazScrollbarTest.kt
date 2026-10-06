package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazScrollbarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val scrollbar = "Vertical scrollbar"

    @Test
    fun `lazy list gets a scrollbar that drives the list`() {
        lateinit var state: LazyListState
        composeRule.setThemedContent {
            state = rememberLazyListState()
            NimazScrollbarBox(state = state, modifier = Modifier.size(200.dp, 300.dp)) {
                LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                    items(200) { Text("Row $it", Modifier.height(40.dp)) }
                }
            }
        }
        composeRule.onNodeWithText("Row 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(scrollbar)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        composeRule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isGreaterThan(100)
    }

    @Test
    fun `fast scroll variant with a label renders over a grid`() {
        composeRule.setThemedContent {
            val state = rememberLazyGridState()
            NimazScrollbarBox(
                state = state,
                modifier = Modifier.size(200.dp, 300.dp),
                variant = NimazScrollbarVariant.FastScroll,
                label = { progress -> NimazScrollbarLabel("${scrollbarLabelIndex(progress, 99)}") },
            ) {
                LazyVerticalGrid(GridCells.Fixed(3), state = state, modifier = Modifier.fillMaxSize()) {
                    items(99) { Text("Cell $it", Modifier.height(60.dp)) }
                }
            }
        }
        composeRule.onNodeWithText("Cell 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(scrollbar).assertExists()
    }

    @Test
    fun `scroll state gets a scrollbar that drives the column`() {
        lateinit var state: ScrollState
        composeRule.setThemedContent {
            state = rememberScrollState()
            NimazScrollbarBox(state = state, modifier = Modifier.size(200.dp, 300.dp)) {
                Column(Modifier.verticalScroll(state)) {
                    repeat(50) { Text("Line $it", Modifier.height(40.dp)) }
                }
            }
        }
        composeRule.onNodeWithContentDescription(scrollbar)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        composeRule.waitForIdle()
        assertThat(state.value).isGreaterThan(0)
    }

    @Test
    fun `reversed scroll state renders`() {
        composeRule.setThemedContent {
            val state = rememberScrollState()
            NimazScrollbarBox(state = state, modifier = Modifier.size(200.dp, 300.dp), reverseScrolling = true) {
                Column(Modifier.verticalScroll(state, reverseScrolling = true)) {
                    repeat(50) { Text("Line $it", Modifier.height(40.dp)) }
                }
            }
        }
        composeRule.onNodeWithContentDescription(scrollbar).assertExists()
    }

    @Test
    fun `content that fits shows no scrollbar`() {
        composeRule.setThemedContent {
            val state = rememberLazyListState()
            NimazScrollbarBox(state = state, modifier = Modifier.size(200.dp, 300.dp)) {
                LazyColumn(state = state) { items(2) { Text("Row $it") } }
            }
        }
        composeRule.onNodeWithText("Row 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(scrollbar).assertDoesNotExist()
    }

    @Test
    fun `label index maps progress onto the list and clamps`() {
        assertThat(scrollbarLabelIndex(0f, 114)).isEqualTo(0)
        assertThat(scrollbarLabelIndex(1f, 114)).isEqualTo(113)
        assertThat(scrollbarLabelIndex(0.5f, 3)).isEqualTo(1)
        assertThat(scrollbarLabelIndex(-1f, 10)).isEqualTo(0)
        assertThat(scrollbarLabelIndex(2f, 10)).isEqualTo(9)
        assertThat(scrollbarLabelIndex(0.5f, 0)).isEqualTo(0)
    }
}
