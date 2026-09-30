package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.presentation.theme.ArabicTextStyles
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.PlusJakartaSansFontFamily

val highlightColors = listOf(
    "#FDE68A" to "Yellow", "#BBF7D0" to "Green", "#BFDBFE" to "Blue",
    "#FBCFE8" to "Pink", "#FED7AA" to "Orange",
)

fun parseColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(NimazColors.Secondary)

/**
 * Compose owns selection gestures, handles, magnification and the platform context menu.
 * A read-only field exposes exact UTF-16 ranges, including repeated phrases and mixed scripts.
 */
@Composable
fun TafseerHighlightableText(
    text: String,
    highlights: List<TafseerHighlight>,
    onSelectionChange: (Int, Int) -> Unit,
    onHighlightTapped: (TafseerHighlight) -> Unit,
    clearSelectionToken: Int,
    modifier: Modifier = Modifier,
    onHighlightSelection: (Int, Int) -> Unit = { _, _ -> },
    textSize: Float = 16f,
) {
    val styled = remember(text, highlights, textSize) { styledTafseerText(text, highlights, textSize) }
    var range by remember(text, clearSelectionToken) { mutableStateOf(TextRange.Zero) }
    val selectionChanged by rememberUpdatedState(onSelectionChange)
    LaunchedEffect(range) {
        if (range.collapsed) selectionChanged(-1, -1)
        else selectionChanged(range.min, range.max)
    }
    DisposableEffect(Unit) {
        onDispose { selectionChanged(-1, -1) }
    }
    val label = stringResource(R.string.tafseer_highlight)
    val highlightSelection = {
        val selected = range
        if (!selected.collapsed) {
            val existing = highlights.firstOrNull {
                it.startOffset < selected.max && it.endOffset > selected.min
            }
            if (existing != null) onHighlightTapped(existing)
            else onHighlightSelection(selected.min, selected.max)
            range = TextRange.Zero
        }
    }
    BasicTextField(
        value = TextFieldValue(styled, selection = range),
        onValueChange = { range = it.selection },
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = textSize.sp,
            lineHeight = (textSize * 1.7f).sp,
            textDirection = TextDirection.Content,
        ),
        modifier = modifier
            .appendTextContextMenuComponents {
                if (!range.collapsed) {
                    item(key = "tafseer-highlight", label = label) {
                        highlightSelection()
                        close()
                    }
                }
            }
            .semantics {
                if (!range.collapsed) {
                    customActions = listOf(CustomAccessibilityAction(label) {
                        highlightSelection()
                        true
                    })
                }
            },
    )
}

/** Styles only: preserve every character so stored ranges remain valid. */
internal fun styledTafseerText(
    text: String, highlights: List<TafseerHighlight>, size: Float,
): AnnotatedString = buildAnnotatedString {
    append(text)
    // Harakat and shadda have Unicode script INHERITED, not ARABIC. Keep attached
    // marks in the same shaping run as their letters instead of changing font per letter.
    val arabic = Regex("\\p{IsArabic}[\\p{IsArabic}\\p{M}]*(?:[ \\t،؛؟]+\\p{IsArabic}[\\p{IsArabic}\\p{M}]*)*")
    val arabicStyle = ArabicTextStyles.quranMedium.let { base ->
        base.copy(
            fontSize = (size + 8f).sp,
            lineHeight = base.lineHeight * ((size + 8f) / base.fontSize.value),
            textDirection = TextDirection.Content,
        )
    }
    for (match in arabic.findAll(text)) {
        addStyle(arabicStyle.toSpanStyle(), match.range.first, match.range.last + 1)
    }
    var offset = 0
    text.split('\n').forEach { line ->
        if (arabic.containsMatchIn(line)) {
            // Wrapped Arabic lines need room for stacked marks at the actual Arabic size.
            addStyle(arabicStyle.toParagraphStyle(), offset, offset + line.length)
        }
        if (line.isNotBlank() && line.length <= 100 &&
            line.none { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC } &&
            !line.endsWith('.') && !line.endsWith(',') && !line.startsWith('-')
        ) addStyle(SpanStyle(fontWeight = FontWeight.Bold), offset, offset + line.length)
        offset += line.length + 1
    }
    highlights.forEach { highlight ->
        val start = highlight.startOffset.coerceIn(0, length)
        val end = highlight.endOffset.coerceIn(start, length)
        if (start < end) addStyle(SpanStyle(
            background = parseColor(highlight.color), color = NimazColors.OnSurfaceLight,
        ), start, end)
    }
}
