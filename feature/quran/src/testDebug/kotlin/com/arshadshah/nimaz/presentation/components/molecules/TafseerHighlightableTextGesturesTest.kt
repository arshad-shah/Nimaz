package com.arshadshah.nimaz.presentation.components.molecules

import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerHighlightableTextGesturesTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private var created: Pair<Int, Int>? = null
    private var edited: Long? = null
    private fun render(highlights: List<TafseerHighlight> = emptyList()) {
        composeRule.setThemedContent {
            TafseerHighlightableText("Mercy بسم الله and peace.", highlights, -1, -1, { _, _ -> },
                { edited = it.id }, 0, onHighlightSelection = { start, end -> created = start to end })
        }
    }
    @Test fun `native selection exposes highlight without replacing platform menu`() {
        render()
        onView(isAssignableFrom(NativeTafseerTextView::class.java)).check { view, _ ->
            val text = view as NativeTafseerTextView
            assertThat(text.isTextSelectable).isTrue()
            val menu = mockk<Menu>(relaxed = true)
            val mode = mockk<ActionMode>(relaxed = true)
            assertThat(text.customSelectionActionModeCallback.onCreateActionMode(mode, menu)).isTrue()
            verify(exactly = 0) { menu.clear() }
            Selection.setSelection(text.text as Spannable, 6, 13)
            val action = mockk<MenuItem> { every { itemId } returns HIGHLIGHT_ACTION }
            assertThat(text.customSelectionActionModeCallback.onActionItemClicked(mode, action)).isTrue()
            assertThat(created).isEqualTo(6 to 13)
            verify { mode.finish() }
        }
    }
    @Test fun `native copy is not intercepted and existing highlight opens editor`() {
        render(listOf(TafseerHighlight(7, 1, "ibn_kathir_en", 0, 5, "#FDE68A", null, 0, 0)))
        onView(isAssignableFrom(NativeTafseerTextView::class.java)).check { view, _ ->
            val text = view as NativeTafseerTextView
            val mode = mockk<ActionMode>(relaxed = true)
            val copy = mockk<MenuItem> { every { itemId } returns android.R.id.copy }
            assertThat(text.customSelectionActionModeCallback.onActionItemClicked(mode, copy)).isFalse()
            Selection.setSelection(text.text as Spannable, 0, 5)
            val action = mockk<MenuItem> { every { itemId } returns HIGHLIGHT_ACTION }
            text.customSelectionActionModeCallback.onActionItemClicked(mode, action)
            assertThat(edited).isEqualTo(7)
            assertThat(created).isNull()
        }
    }
}
