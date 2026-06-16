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
    fun rendersPlainText_inViewMode() {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "Hello tafseer world",
                highlights = emptyList(),
                isHighlightMode = false,
                selectedColor = "#FDE68A",
                onHighlightCreated = { _, _, _ -> },
                onHighlightTapped = {}
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
                isHighlightMode = false,
                selectedColor = "#BBF7D0",
                onHighlightCreated = { _, _, _ -> },
                onHighlightTapped = {}
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
                isHighlightMode = false,
                selectedColor = "#FDE68A",
                onHighlightCreated = { _, _, _ -> },
                onHighlightTapped = {}
            )
        }

        composeRule.onNodeWithText("Bismillah بسم الله الرحمن الرحيم end").assertExists()
    }

    @Test
    fun highlightMode_rendersText_withoutSelectionIndicatorInitially() {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "Highlight me please",
                highlights = emptyList(),
                isHighlightMode = true,
                selectedColor = "#BFDBFE",
                onHighlightCreated = { _, _, _ -> },
                onHighlightTapped = {}
            )
        }

        composeRule.onNodeWithText("Highlight me please").assertExists()
        // No tap yet -> selectionStart < 0 -> indicator hidden
        composeRule.onNodeWithText("Tap end position to highlight").assertDoesNotExist()
    }

    @Test
    fun rendersText_withOutOfRangeHighlightOffsets() {
        // endOffset beyond text length is coerced; should not crash
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = "short",
                highlights = listOf(highlight(startOffset = 0, endOffset = 999)),
                isHighlightMode = false,
                selectedColor = "#FED7AA",
                onHighlightCreated = { _, _, _ -> },
                onHighlightTapped = {}
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
