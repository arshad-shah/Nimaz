package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextRange
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerHighlightableTextGesturesTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private val text = "Mercy بسم الله Mercy and peace."
    private var created: Pair<Int, Int>? = null
    private var edited: Long? = null
    private var selected = -1 to -1
    private val clearToken = mutableStateOf(0)
    private val size = mutableStateOf(16f)

    private fun render(highlights: List<TafseerHighlight> = emptyList()) {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = text, highlights = highlights,
                onSelectionChange = { start, end -> selected = start to end },
                onHighlightTapped = { edited = it.id },
                clearSelectionToken = clearToken.value,
                onHighlightSelection = { start, end -> created = start to end },
                textSize = size.value,
            )
        }
    }
    private fun select(start: Int, end: Int) {
        composeRule.onNodeWithText(text).performSemanticsAction(SemanticsActions.SetSelection) {
            assertThat(it(start, end, false)).isTrue()
        }
        composeRule.waitForIdle()
    }
    private fun highlight() {
        val action = composeRule.onNodeWithText(text).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single()
        composeRule.runOnIdle { assertThat(action.action()).isTrue() }
    }

    @Test fun `repeated phrase and reversed selection retain exact offsets`() {
        render()
        val start = text.lastIndexOf("Mercy")
        select(start + 5, start)
        assertThat(selected).isEqualTo(start to start + 5)
        highlight()
        assertThat(created).isEqualTo(start to start + 5)
        composeRule.waitForIdle()
        assertThat(selected).isEqualTo(-1 to -1)
    }

    @Test fun `existing highlight opens shared editor and copy stays available`() {
        render(listOf(TafseerHighlight(7, 1, "ibn_kathir_en", 0, 5, "#FDE68A", null, 0, 0)))
        select(0, 5)
        val config = composeRule.onNodeWithText(text).fetchSemanticsNode().config
        assertThat(config.contains(SemanticsActions.CopyText)).isTrue()
        assertThat(config.contains(SemanticsActions.SetText)).isFalse()
        highlight()
        assertThat(edited).isEqualTo(7L)
        assertThat(created).isNull()
    }

    @Test fun `mixed Arabic selection survives typography changes and clears explicitly`() {
        render()
        val start = text.indexOf("بسم")
        val end = text.indexOf(" Mercy")
        select(start, end)
        composeRule.runOnIdle { size.value = 22f }
        composeRule.onNodeWithText(text).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(start, end)))
        composeRule.runOnIdle { clearToken.value++ }
        composeRule.onNodeWithText(text).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange.Zero))
        assertThat(selected).isEqualTo(-1 to -1)
    }
}
