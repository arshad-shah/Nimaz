package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily

val highlightColors = listOf(
    "#FDE68A" to "Yellow",
    "#BBF7D0" to "Green",
    "#BFDBFE" to "Blue",
    "#FBCFE8" to "Pink",
    "#FED7AA" to "Orange"
)

private const val HIGHLIGHT_TAG = "HIGHLIGHT"

@Composable
fun TafseerHighlightableText(
    text: String,
    highlights: List<TafseerHighlight>,
    isHighlightMode: Boolean,
    selectedColor: String,
    onHighlightCreated: (startOffset: Int, endOffset: Int, color: String) -> Unit,
    onHighlightTapped: (TafseerHighlight) -> Unit,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var selectionStart by remember { mutableIntStateOf(-1) }
    var selectionEnd by remember { mutableIntStateOf(-1) }

    // Build annotated string with highlights, Arabic font detection, and selection preview
    val annotatedString = remember(text, highlights, selectionStart, selectionEnd, selectedColor, isHighlightMode) {
        buildStyledText(text, highlights, selectionStart, selectionEnd, selectedColor, isHighlightMode)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f
            ),
            color = MaterialTheme.colorScheme.onSurface,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(isHighlightMode, selectedColor, highlights) {
                    detectTapGestures { tapOffset ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val charOffset = layout.getOffsetForPosition(tapOffset)

                        if (isHighlightMode) {
                            handleHighlightModeTap(
                                charOffset = charOffset,
                                text = text,
                                selectionStart = selectionStart,
                                selectedColor = selectedColor,
                                onSelectionStartSet = { selectionStart = it },
                                onHighlightCreated = { start, end ->
                                    onHighlightCreated(start, end, selectedColor)
                                    selectionStart = -1
                                    selectionEnd = -1
                                }
                            )
                        } else {
                            // Check if tapped on existing highlight
                            val tappedHighlight = findTappedHighlight(charOffset, highlights, text)
                            if (tappedHighlight != null) {
                                onHighlightTapped(tappedHighlight)
                            }
                        }
                    }
                }
        )

        // Selection indicator when first tap placed
        if (isHighlightMode && selectionStart >= 0) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = "Tap end position to highlight",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Build annotated string with:
 * - Existing highlights as colored backgrounds with HIGHLIGHT tag annotations
 * - Arabic text ranges rendered with Amiri font
 * - Live selection preview between start and end taps
 */
private fun buildStyledText(
    text: String,
    highlights: List<TafseerHighlight>,
    selectionStart: Int,
    selectionEnd: Int,
    selectedColor: String,
    isHighlightMode: Boolean
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
                    style = SpanStyle(background = parseColor(highlight.color)),
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

        // Show live selection preview
        if (isHighlightMode && selectionStart >= 0) {
            val previewStart = selectionStart.coerceIn(0, text.length)
            val previewEnd = if (selectionEnd >= 0) {
                selectionEnd.coerceIn(0, text.length)
            } else {
                // Show a small marker at the start position
                (previewStart + 1).coerceAtMost(text.length)
            }

            if (previewStart != previewEnd) {
                val actualStart = minOf(previewStart, previewEnd)
                val actualEnd = maxOf(previewStart, previewEnd)
                addStyle(
                    style = SpanStyle(
                        background = parseColor(selectedColor).copy(alpha = 0.35f)
                    ),
                    start = actualStart,
                    end = actualEnd
                )
            }
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
            while (i < text.length && (isArabicChar(text[i]) || text[i].isWhitespace() || isPunctuation(text[i]))) {
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
            c == '-' || c == '\u060C' || c == '\u061B' || c == '\u061F' // Arabic comma, semicolon, question mark
}

/**
 * Handle tap in highlight mode: first tap sets start, second tap creates highlight.
 * Snaps to word boundaries for cleaner selections.
 */
private fun handleHighlightModeTap(
    charOffset: Int,
    text: String,
    selectionStart: Int,
    selectedColor: String,
    onSelectionStartSet: (Int) -> Unit,
    onHighlightCreated: (Int, Int) -> Unit
) {
    val snappedOffset = snapToWordBoundary(charOffset, text, isStart = selectionStart < 0)

    if (selectionStart < 0) {
        // First tap - set start
        onSelectionStartSet(snappedOffset)
    } else {
        // Second tap - create highlight
        val start = minOf(selectionStart, snappedOffset)
        val end = maxOf(selectionStart, snappedOffset)
        if (start < end && end <= text.length) {
            onHighlightCreated(start, end)
        } else {
            // Reset if invalid
            onSelectionStartSet(-1)
        }
    }
}

/**
 * Snap a character offset to the nearest word boundary.
 */
private fun snapToWordBoundary(offset: Int, text: String, isStart: Boolean): Int {
    if (text.isEmpty() || offset < 0 || offset >= text.length) return offset.coerceIn(0, text.length)

    return if (isStart) {
        // Snap to start of word
        var i = offset
        while (i > 0 && !text[i - 1].isWhitespace()) i--
        i
    } else {
        // Snap to end of word
        var i = offset
        while (i < text.length && !text[i].isWhitespace()) i++
        i
    }
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
            else -> Color(0xFFFDE68A) // fallback yellow
        }
    } catch (e: Exception) {
        Color(0xFFFDE68A)
    }
}
