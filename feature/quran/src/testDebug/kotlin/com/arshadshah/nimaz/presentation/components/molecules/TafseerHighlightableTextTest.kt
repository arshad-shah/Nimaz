package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerHighlightableTextTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun highlight(
        id: Long = 1L,
        startOffset: Int = 0,
        endOffset: Int = 4,
        color: String = "#FDE68A"
    ) = TafseerHighlight(
        id = id,
        ayahId = 1,
        tafseerId = "ibn-kathir",
        startOffset = startOffset,
        endOffset = endOffset,
        color = color,
        note = null,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun rendersPlainText_withoutSelection() {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "Hello tafseer world",
                highlights = emptyList(),
                selectionStart = -1,
                selectionEnd = -1,
                onSelectionChange = { _, _ -> },
                onHighlightTapped = {},
                clearSelectionToken = 0
            )
        }

        composeRule.onNodeWithText("Hello tafseer world").assertExists()
    }

    @Test
    fun rendersText_withExistingHighlights() {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "Hello tafseer world",
                highlights = listOf(highlight(startOffset = 0, endOffset = 5)),
                selectionStart = -1,
                selectionEnd = -1,
                onSelectionChange = { _, _ -> },
                onHighlightTapped = {},
                clearSelectionToken = 0
            )
        }

        composeRule.onNodeWithText("Hello tafseer world").assertExists()
    }

    @Test
    fun rendersText_withArabicContent() {
        // Exercises applyArabicStyling Arabic detection branch
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "Bismillah بسم الله الرحمن الرحيم end",
                highlights = emptyList(),
                selectionStart = -1,
                selectionEnd = -1,
                onSelectionChange = { _, _ -> },
                onHighlightTapped = {},
                clearSelectionToken = 0
            )
        }

        composeRule.onNodeWithText("Bismillah بسم الله الرحمن الرحيم end").assertExists()
    }

    @Test
    fun rendersText_withActiveSelectionRange() {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "Highlight me please",
                highlights = emptyList(),
                selectionStart = 0,
                selectionEnd = 9,
                onSelectionChange = { _, _ -> },
                onHighlightTapped = {},
                clearSelectionToken = 0
            )
        }

        composeRule.onNodeWithText("Highlight me please").assertExists()
    }

    @Test
    fun rendersText_withOutOfRangeHighlightOffsets() {
        // endOffset beyond text length is coerced; should not crash
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "short",
                highlights = listOf(highlight(startOffset = 0, endOffset = 999)),
                selectionStart = -1,
                selectionEnd = -1,
                onSelectionChange = { _, _ -> },
                onHighlightTapped = {},
                clearSelectionToken = 0
            )
        }

        composeRule.onNodeWithText("short").assertExists()
    }

    @Test
    fun parseColor_sixDigitHex_addsOpaqueAlpha() {
        assertThat(parseColor("#FDE68A")).isEqualTo(Color(0xFFFDE68A))
    }

    @Test
    fun parseColor_eightDigitHex_usedDirectly() {
        assertThat(parseColor("#80FDE68A")).isEqualTo(Color(0x80FDE68A))
    }

    @Test
    fun parseColor_invalidLength_fallsBackToYellow() {
        assertThat(parseColor("#ABC")).isEqualTo(Color(0xFFFDE68A))
    }

    @Test
    fun parseColor_invalidHex_fallsBackToYellow() {
        assertThat(parseColor("not-a-color")).isEqualTo(Color(0xFFFDE68A))
    }
}
