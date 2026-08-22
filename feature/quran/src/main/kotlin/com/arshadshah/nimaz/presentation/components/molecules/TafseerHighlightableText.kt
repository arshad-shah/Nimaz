package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.HighlightArtColors
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlin.math.roundToInt

val highlightColors = listOf(
    "#FDE68A" to "Yellow",
    "#BBF7D0" to "Green",
    "#BFDBFE" to "Blue",
    "#FBCFE8" to "Pink",
    "#FED7AA" to "Orange"
)

private const val HIGHLIGHT_TAG = "HIGHLIGHT"

// Highlight backgrounds are light pastels, so highlighted text needs a dark
// foreground to stay legible in both light and dark themes (the default
// onSurface colour is near-white in dark mode and disappears on the pastel).
private val HighlightedTextColor = NimazColors.OnSurfaceLight

/** Which of the two selection handles the user is currently dragging. */
private enum class ActiveHandle { START, END }

/**
 * Tafseer commentary text with **gesture-first** highlighting:
 *
 * - **Long-press** anywhere in the text selects the word under the finger and
 *   reveals two drag handles.
 * - **Drag either handle** to grow or shrink the selection one character at a
 *   time — select anything from a single word to a whole paragraph.
 * - **Tap an existing highlight** to open it for editing.
 *
 * Selection is reported up via [onSelectionChange] (page-local character offsets,
 * or `-1, -1` when cleared); the parent decides what to do with it (show the
 * "Add highlight" action, open the editor sheet, …). The parent clears the live
 * selection by bumping [clearSelectionToken].
 *
 * This deliberately avoids `SelectionContainer`/`BasicTextField` so the only
 * affordance is *our* highlight flow — no competing system copy/paste toolbar —
 * and so we get the exact character offsets the highlight model needs.
 */
@Composable
fun TafseerHighlightableText(
    text: String,
    highlights: List<TafseerHighlight>,
    selectionStart: Int,
    selectionEnd: Int,
    onSelectionChange: (start: Int, end: Int) -> Unit,
    onHighlightTapped: (TafseerHighlight) -> Unit,
    clearSelectionToken: Int,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val selectionColor = MaterialTheme.colorScheme.primary

    val hasSelection = selectionStart in 0..text.length &&
            selectionEnd in 0..text.length &&
            selectionStart < selectionEnd

    val annotatedString =
        remember(text, highlights, selectionStart, selectionEnd, selectionColor) {
            buildStyledText(text, highlights, selectionStart, selectionEnd, selectionColor)
        }

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = annotatedString,
            // The type scale's own leading, not a multiple of it. `bodyLarge.lineHeight` is
            // already 1.5x its font size — the same ratio `Type.kt` calls LATIN_LEADING — so
            // multiplying by 1.6 again gave Latin commentary prose ~2.4x leading: an Arabic
            // line height, applied to a paragraph that is not Arabic, and the reason a page of
            // Ibn Kathir read as a list of separated lines rather than as prose.
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(text, highlights, clearSelectionToken) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val layout = textLayoutResult
                            if (layout != null) {
                                val charOffset = layout.getOffsetForPosition(tapOffset)
                                val tapped = findTappedHighlight(charOffset, highlights, text)
                                if (tapped != null) {
                                    onHighlightTapped(tapped)
                                } else if (hasSelection) {
                                    // Tapping outside the selection dismisses it.
                                    onSelectionChange(-1, -1)
                                }
                            }
                        },
                        onLongPress = { pressOffset ->
                            val layout = textLayoutResult
                            if (layout != null) {
                                val charOffset = layout.getOffsetForPosition(pressOffset)
                                val (start, end) = wordRangeAt(charOffset, text)
                                if (start < end) onSelectionChange(start, end)
                            }
                        }
                    )
                }
        )

        // Selection handles — only while a range is selected and we have a layout.
        val layout = textLayoutResult
        if (hasSelection && layout != null) {
            SelectionHandle(
                handle = ActiveHandle.START,
                caret = layout.getCursorRect(selectionStart.coerceIn(0, text.length)),
                color = selectionColor,
                onDrag = { delta, dragPos ->
                    val newStart = layout.getOffsetForPosition(dragPos)
                        .coerceIn(0, (selectionEnd - 1).coerceAtLeast(0))
                    if (newStart != selectionStart) onSelectionChange(newStart, selectionEnd)
                }
            )
            SelectionHandle(
                handle = ActiveHandle.END,
                caret = layout.getCursorRect(selectionEnd.coerceIn(0, text.length)),
                color = selectionColor,
                onDrag = { delta, dragPos ->
                    val newEnd = layout.getOffsetForPosition(dragPos)
                        .coerceIn((selectionStart + 1).coerceAtMost(text.length), text.length)
                    if (newEnd != selectionEnd) onSelectionChange(selectionStart, newEnd)
                }
            )
        }
    }
}

/**
 * A single draggable selection handle. Renders a small circle that hangs just
 * below the caret at [caret]; dragging it reports the new absolute pointer
 * position (in the text's coordinate space) back via [onDrag] so the caller can
 * map it to a character offset. Deltas are translation-invariant, so we seed the
 * tracked position from the caret on drag-start and accumulate from there.
 */
@Composable
private fun SelectionHandle(
    handle: ActiveHandle,
    caret: androidx.compose.ui.geometry.Rect,
    color: Color,
    onDrag: (delta: Offset, dragPos: Offset) -> Unit
) {
    val handleSize = 22.dp
    var tracked by remember(handle) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset {
                // Centre the handle on the caret x, hanging just below the line.
                IntOffset(
                    x = (caret.left - handleSize.toPx() / 2f).roundToInt(),
                    y = caret.bottom.roundToInt()
                )
            }
            .size(handleSize)
            .pointerInput(handle) {
                detectDragGestures(
                    onDragStart = {
                        // Seed from the caret centre in text-space.
                        tracked = Offset(caret.left, (caret.top + caret.bottom) / 2f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tracked += dragAmount
                        onDrag(dragAmount, tracked)
                    }
                )
            }
            .background(color, CircleShape)
    )
}

/**
 * Build the annotated string with:
 * - Existing highlights as coloured backgrounds (tagged for hit-testing).
 * - Arabic text ranges rendered with the Amiri font.
 * - The live selection rendered with a translucent primary tint.
 */
private fun buildStyledText(
    text: String,
    highlights: List<TafseerHighlight>,
    selectionStart: Int,
    selectionEnd: Int,
    selectionColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        // Apply Arabic font styling to Arabic text ranges
        applyArabicStyling(text)

        // Apply existing highlights
        for (highlight in highlights) {
            val start = highlight.startOffset.coerceIn(0, text.length)
            val end = highlight.endOffset.coerceIn(start, text.length)
            if (start < end) {
                addStyle(
                    style = SpanStyle(
                        background = parseColor(highlight.color),
                        color = HighlightedTextColor
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = HIGHLIGHT_TAG,
                    annotation = highlight.id.toString(),
                    start = start,
                    end = end
                )
            }
        }

        // Live selection preview
        if (selectionStart in 0..text.length && selectionEnd in 0..text.length && selectionStart < selectionEnd) {
            addStyle(
                style = SpanStyle(background = selectionColor.copy(alpha = 0.30f)),
                start = selectionStart,
                end = selectionEnd
            )
        }
    }
}

/**
 * Detect Arabic Unicode character ranges and apply Amiri font styling.
 */
private fun AnnotatedString.Builder.applyArabicStyling(text: String) {
    var i = 0
    while (i < text.length) {
        if (isArabicChar(text[i])) {
            val start = i
            while (i < text.length && (isArabicChar(text[i]) || text[i].isWhitespace() || isPunctuation(
                    text[i]
                ))
            ) {
                // Look ahead: if it's whitespace/punctuation, only include if followed by Arabic
                if (!isArabicChar(text[i])) {
                    val lookahead = (i + 1 until text.length).firstOrNull { isArabicChar(text[it]) }
                    if (lookahead == null) break
                    // Only bridge short gaps
                    if (lookahead - i > 3) break
                }
                i++
            }
            if (i > start) {
                addStyle(
                    style = SpanStyle(
                        fontFamily = AmiriFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    start = start,
                    end = i
                )
            }
        } else {
            i++
        }
    }
}

private fun isArabicChar(c: Char): Boolean {
    val code = c.code
    return code in 0x0600..0x06FF ||  // Arabic
            code in 0x0750..0x077F ||  // Arabic Supplement
            code in 0x08A0..0x08FF ||  // Arabic Extended-A
            code in 0xFB50..0xFDFF ||  // Arabic Presentation Forms-A
            code in 0xFE70..0xFEFF ||  // Arabic Presentation Forms-B
            code in 0xFD00..0xFD3F     // Arabic ligatures
}

private fun isPunctuation(c: Char): Boolean {
    return c == ',' || c == '.' || c == ':' || c == ';' || c == '(' || c == ')' ||
            c == '-' || c == '،' || c == '؛' || c == '؟' // Arabic comma, semicolon, question mark
}

/**
 * Return the [start, end) character range of the word containing [offset]. Used
 * to seed the selection from a long-press, so the user starts with a whole word
 * and refines from there with the handles.
 */
private fun wordRangeAt(offset: Int, text: String): Pair<Int, Int> {
    if (text.isEmpty()) return 0 to 0
    val clamped = offset.coerceIn(0, text.length - 1)
    if (text[clamped].isWhitespace()) {
        // Long-pressed a gap — select the single position so handles still appear.
        return clamped to (clamped + 1).coerceAtMost(text.length)
    }
    var start = clamped
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    var end = clamped
    while (end < text.length && !text[end].isWhitespace()) end++
    return start to end
}

/**
 * Find which highlight was tapped based on character offset.
 */
private fun findTappedHighlight(
    charOffset: Int,
    highlights: List<TafseerHighlight>,
    text: String
): TafseerHighlight? {
    return highlights.firstOrNull { highlight ->
        val start = highlight.startOffset.coerceIn(0, text.length)
        val end = highlight.endOffset.coerceIn(start, text.length)
        charOffset in start until end
    }
}

fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)
        when (cleanHex.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> HighlightArtColors.FallbackYellow // fallback yellow
        }
    } catch (e: Exception) {
        HighlightArtColors.FallbackYellow
    }
}


// ==================== PREVIEWS ====================

private val sampleHighlightText =
    "And remember when your Lord said to the angels, 'Indeed, I will make upon " +
            "the earth a successive authority.' بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ " +
            "They said, 'Will You place upon it one who causes corruption therein?'"

private val sampleHighlights = listOf(
    TafseerHighlight(
        id = 1L,
        ayahId = 30,
        tafseerId = "ibn_kathir_en",
        startOffset = 0,
        endOffset = 35,
        color = "#FDE68A",
        note = "Key theme: stewardship",
        createdAt = 0L,
        updatedAt = 0L
    ),
    TafseerHighlight(
        id = 2L,
        ayahId = 30,
        tafseerId = "ibn_kathir_en",
        startOffset = 120,
        endOffset = 160,
        color = "#BBF7D0",
        note = null,
        createdAt = 0L,
        updatedAt = 0L
    )
)

@Composable
private fun TafseerHighlightableTextShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TafseerHighlightableText(
            text = sampleHighlightText,
            highlights = sampleHighlights,
            selectionStart = 49,
            selectionEnd = 85,
            onSelectionChange = { _, _ -> },
            onHighlightTapped = {},
            clearSelectionToken = 0
        )
    }
}

@Preview(showBackground = true, name = "HighlightableText — Light")
@Composable
private fun TafseerHighlightableTextLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        TafseerHighlightableTextShowcase()
    }
}

@Preview(showBackground = true, name = "HighlightableText — Dark")
@Composable
private fun TafseerHighlightableTextDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        TafseerHighlightableTextShowcase()
    }
}
