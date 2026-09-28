package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Selection
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.presentation.theme.NimazColors

val highlightColors = listOf(
    "#FDE68A" to "Yellow", "#BBF7D0" to "Green", "#BFDBFE" to "Blue",
    "#FBCFE8" to "Pink", "#FED7AA" to "Orange",
)

fun parseColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(NimazColors.Secondary)

/** Native selection owns word boundaries, bidi handles, magnification and Copy/Share.
 * The text is never rewritten: its UTF-16 offsets are the persisted highlight offsets.
 */
@Composable
fun TafseerHighlightableText(
    text: String,
    highlights: List<TafseerHighlight>,
    selectionStart: Int,
    selectionEnd: Int,
    onSelectionChange: (Int, Int) -> Unit,
    onHighlightTapped: (TafseerHighlight) -> Unit,
    clearSelectionToken: Int,
    modifier: Modifier = Modifier,
    onHighlightSelection: (Int, Int) -> Unit = { _, _ -> },
    textSize: Float = 16f,
) {
    val selectionChanged = rememberUpdatedState(onSelectionChange)
    val createHighlight = rememberUpdatedState(onHighlightSelection)
    val editHighlight = rememberUpdatedState(onHighlightTapped)
    val currentHighlights = rememberUpdatedState(highlights)
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val selection = MaterialTheme.colorScheme.primary.copy(alpha = .3f).toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            NativeTafseerTextView(context).apply {
                setTextIsSelectable(true)
                setPadding(0, 0, 0, 0)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                textDirection = View.TEXT_DIRECTION_FIRST_STRONG
                setLineSpacing(0f, 1.5f)
                isFallbackLineSpacing = true
                typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_variable)
                fontVariationSettings = "'wght' 400"
                selectionListener = { start, end -> selectionChanged.value(start, end) }
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.add(Menu.NONE, HIGHLIGHT_ACTION, Menu.NONE, context.getString(R.string.tafseer_highlight))
                        return true // Keep the platform's Copy, Share and Select all actions.
                    }
                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        if (item.itemId != HIGHLIGHT_ACTION) return false
                        val start = minOf(this@apply.selectionStart, this@apply.selectionEnd)
                        val end = maxOf(this@apply.selectionStart, this@apply.selectionEnd)
                        if (start >= 0 && end > start) {
                            val existing = currentHighlights.value.firstOrNull {
                                it.startOffset < end && it.endOffset > start
                            }
                            if (existing == null) createHighlight.value(start, end)
                            else editHighlight.value(existing)
                        }
                        mode.finish()
                        return true
                    }
                    override fun onDestroyActionMode(mode: ActionMode) {
                        selectionChanged.value(-1, -1)
                    }
                }
            }
        },
        update = { view ->
            view.setTextColor(foreground)
            view.highlightColor = selection
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            // Avoid resetting native selection on unrelated recompositions / handle drags.
            val key = Triple(text, highlights, textSize)
            if (view.contentKey != key) {
                view.contentKey = key
                view.text = styledTafseerText(view.context, text, highlights, textSize)
            }
            if (view.clearToken != clearSelectionToken) {
                view.clearToken = clearSelectionToken
                (view.text as? Spannable)?.let { Selection.removeSelection(it) }
            }
        },
    )
}

internal const val HIGHLIGHT_ACTION = 0x746166

internal class NativeTafseerTextView(context: Context) : TextView(context) {
    var selectionListener: ((Int, Int) -> Unit)? = null
    var contentKey: Any? = null
    var clearToken: Int = -1
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        selectionListener?.invoke(minOf(selStart, selEnd), maxOf(selStart, selEnd))
    }
}

/** Styling changes spans only. Even paragraph whitespace stays byte-for-byte intact. */
internal fun styledTafseerText(
    context: Context, text: String, highlights: List<TafseerHighlight>, size: Float,
): SpannableString = SpannableString(text).apply {
    val flags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    val amiri = ResourcesCompat.getFont(context, R.font.amiri_regular)
    // Keep punctuation/spacing within an Arabic run, without swallowing adjacent Latin words.
    val arabic = Regex("[\\p{IsArabic}]+(?:[ \\t،؛؟]+[\\p{IsArabic}]+)*")
    for (match in arabic.findAll(text)) {
        val start = match.range.first
        val end = match.range.last + 1
        if (amiri != null) setSpan(TypefaceSpan(amiri), start, end, flags)
        setSpan(AbsoluteSizeSpan(((size + 8f) * context.resources.displayMetrics.scaledDensity).toInt()), start, end, flags)
    }
    // Short standalone lines are headings. No inserted characters, trimmed content or HTML.
    var offset = 0
    text.split('\n').forEach { line ->
        if (line.isNotBlank() && line.length <= 100 &&
            line.none { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC } &&
            !line.endsWith('.') && !line.endsWith(',') && !line.startsWith('-')
        ) setSpan(StyleSpan(Typeface.BOLD), offset, offset + line.length, flags)
        offset += line.length + 1
    }
    highlights.forEach { highlight ->
        val start = highlight.startOffset.coerceIn(0, length)
        val end = highlight.endOffset.coerceIn(start, length)
        if (start < end) {
            setSpan(BackgroundColorSpan(parseColor(highlight.color).toArgb()), start, end, flags)
            setSpan(ForegroundColorSpan(NimazColors.OnSurfaceLight.toArgb()), start, end, flags)
        }
    }
}
