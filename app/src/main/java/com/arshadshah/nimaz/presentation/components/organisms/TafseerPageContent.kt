package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazReaderBottomBar
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetSectionLabel
import com.arshadshah.nimaz.presentation.components.molecules.NimazTextField
import com.arshadshah.nimaz.presentation.components.molecules.QuranFrame
import com.arshadshah.nimaz.presentation.components.molecules.QuranFrameVariant
import com.arshadshah.nimaz.presentation.components.molecules.TafseerHighlightableText
import com.arshadshah.nimaz.presentation.components.molecules.highlightColors
import com.arshadshah.nimaz.presentation.components.molecules.parseColor
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.theme.asTranslationText

/**
 * Represents a page of tafseer content with its character range in the full text.
 */
private data class TafseerPage(
    val text: String,
    val globalStartOffset: Int,
    val globalEndOffset: Int
)

/**
 * What the highlight editor sheet is currently working on: a brand-new highlight
 * from a fresh text selection, or an existing highlight being edited.
 */
private sealed interface EditorTarget {
    data class New(val globalStart: Int, val globalEnd: Int, val snippet: String) : EditorTarget
    data class Existing(val highlight: TafseerHighlight, val snippet: String) : EditorTarget
}

private const val MAX_CHARS_PER_PAGE = 800

private fun splitTafseerIntoPages(fullText: String): List<TafseerPage> {
    if (fullText.length <= MAX_CHARS_PER_PAGE) {
        return listOf(TafseerPage(fullText, 0, fullText.length))
    }

    val pages = mutableListOf<TafseerPage>()
    var currentStart = 0

    while (currentStart < fullText.length) {
        val remaining = fullText.length - currentStart
        if (remaining <= MAX_CHARS_PER_PAGE) {
            pages.add(TafseerPage(fullText.substring(currentStart), currentStart, fullText.length))
            break
        }

        val searchEnd = (currentStart + MAX_CHARS_PER_PAGE).coerceAtMost(fullText.length)
        val chunk = fullText.substring(currentStart, searchEnd)

        val paragraphBreak = chunk.lastIndexOf("\n\n")
        val splitPoint = if (paragraphBreak > MAX_CHARS_PER_PAGE / 4) {
            paragraphBreak + 2
        } else {
            val sentenceBreak = chunk.lastIndexOf(". ")
            val lineBreak = chunk.lastIndexOf('\n')
            val bestBreak = maxOf(sentenceBreak, lineBreak)
            if (bestBreak > MAX_CHARS_PER_PAGE / 4) {
                bestBreak + 1
            } else {
                val spaceBreak = chunk.lastIndexOf(' ')
                if (spaceBreak > MAX_CHARS_PER_PAGE / 4) spaceBreak + 1 else MAX_CHARS_PER_PAGE
            }
        }

        val pageEnd = currentStart + splitPoint
        pages.add(
            TafseerPage(
                fullText.substring(currentStart, pageEnd).trimEnd(),
                currentStart,
                pageEnd
            )
        )
        currentStart = pageEnd
        while (currentStart < fullText.length && fullText[currentStart].isWhitespace()) currentStart++
    }

    return pages
}

private fun highlightsForPage(
    allHighlights: List<TafseerHighlight>,
    page: TafseerPage
): List<TafseerHighlight> {
    return allHighlights.mapNotNull { highlight ->
        val overlapStart = maxOf(highlight.startOffset, page.globalStartOffset)
        val overlapEnd = minOf(highlight.endOffset, page.globalEndOffset)
        if (overlapStart < overlapEnd) {
            highlight.copy(
                startOffset = overlapStart - page.globalStartOffset,
                endOffset = overlapEnd - page.globalStartOffset
            )
        } else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerPageContent(
    ayah: Ayah,
    tafseer: TafseerText?,
    highlights: List<TafseerHighlight>,
    selectedSource: TafseerSource,
    availableSources: Set<TafseerSource>,
    // Hoisted to the caller so it can survive an ayah-by-ayah swipe within the
    // same commentary block instead of reopening the block from page 1.
    currentContentPage: Int,
    onContentPageChanged: (Int) -> Unit,
    onSourceSwitch: (TafseerSource) -> Unit,
    onHighlightCreated: (startOffset: Int, endOffset: Int, color: String, note: String?) -> Unit,
    onHighlightUpdated: (highlightId: Long, color: String, note: String?) -> Unit,
    onHighlightDeleted: (highlightId: Long) -> Unit,
    onShare: () -> Unit,
    /**
     * The subjects the corpus files this verse under (schemaVersion 24). Shown as chips under
     * the verse, on the first content page only — they belong to the *verse*, and a block can
     * span nine of them.
     */
    topics: List<QuranTopic> = emptyList(),
    onTopicClick: (topicId: Int) -> Unit = {},
    /**
     * Language of the translation printed above the commentary — it decides the face, direction
     * and leading. Urdu is Nastaliq; without this the app's Latin body face carries no
     * Arabic-script glyphs at all and the system substitutes a Naskh fallback.
     */
    translationLanguage: TranslationLanguage = TranslationLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    var showNotesSheet by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    // Live text selection (page-local to [currentContentPage]); -1 means none.
    var selStart by remember { mutableIntStateOf(-1) }
    var selEnd by remember { mutableIntStateOf(-1) }
    // Bumped to tell the text layer to drop its current selection.
    var clearSelectionToken by remember { mutableIntStateOf(0) }

    val tafseerPages = remember(tafseer?.text) {
        if (tafseer != null && tafseer.text.isNotBlank()) splitTafseerIntoPages(tafseer.text) else emptyList()
    }
    val totalContentPages = tafseerPages.size
    val tafseerFullText = tafseer?.text ?: ""
    // Guards a hoisted page index against a shorter page count after a source
    // switch or a block change that hasn't propagated a reset yet.
    val safeContentPage = currentContentPage.coerceIn(0, (totalContentPages - 1).coerceAtLeast(0))

    fun clearSelection() {
        selStart = -1
        selEnd = -1
        clearSelectionToken++
    }

    // Dismiss any in-progress selection when the page or ayah changes.
    LaunchedEffect(safeContentPage, tafseer?.id) { clearSelection() }

    val highlightsWithNotes =
        remember(highlights) { highlights.filter { !it.note.isNullOrBlank() } }

    val hasSelection = selStart in 0..tafseerFullText.length && selEnd > selStart

    val sources = TafseerSource.entries

    Column(modifier = modifier.fillMaxSize()) {
        // ── Source switcher (top) — the house segmented control ──
        if (sources.size > 1) {
            NimazSegmentedControl(
                options = sources.map { it.displayName }.asSegments(),
                selectedIndex = sources.indexOf(selectedSource).coerceAtLeast(0),
                onSelect = { onSourceSwitch(sources[it]) },
                purpose = NimazSegmentedPurpose.VIEW,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // ── Scrollable Content ──
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                QuranFrame(
                    variant = QuranFrameVariant.STUDY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Arabic ayah text on first content page
                        if (safeContentPage == 0) {
                            ArabicText(
                                text = ayah.textArabic,
                                size = ArabicTextSize.LARGE,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                            QuranOrnamentalDivider()

                            val translation = ayah.translation
                            if (!translation.isNullOrBlank()) {
                                Text(
                                    text = translation,
                                    style = MaterialTheme.typography.bodyMedium
                                        .asTranslationText(translationLanguage),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                                QuranOrnamentalDivider()
                            }

                            if (topics.isNotEmpty()) {
                                AyahTopicChips(
                                    topics = topics,
                                    onTopicClick = onTopicClick,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Tafseer text
                        if (tafseerPages.isNotEmpty() && tafseer != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Says what it's showing: a block covers a contiguous ayah
                            // range, not just the ayah currently on screen (#329).
                            Text(
                                text = if (tafseer.ayahStart == tafseer.ayahEnd) {
                                    stringResource(
                                        R.string.tafseer_commentary_range_single,
                                        tafseer.surahNumber,
                                        tafseer.ayahStart
                                    )
                                } else {
                                    stringResource(
                                        R.string.tafseer_commentary_range_span,
                                        tafseer.surahNumber,
                                        tafseer.ayahStart,
                                        tafseer.ayahEnd
                                    )
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            AnimatedContent(
                                targetState = safeContentPage,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        androidx.compose.animation.slideInHorizontally { it } togetherWith
                                                androidx.compose.animation.slideOutHorizontally { -it }
                                    } else {
                                        androidx.compose.animation.slideInHorizontally { -it } togetherWith
                                                androidx.compose.animation.slideOutHorizontally { it }
                                    }
                                },
                                label = "tafseer_page"
                            ) { pageIndex ->
                                val animPage =
                                    tafseerPages[pageIndex.coerceIn(0, tafseerPages.lastIndex)]
                                val animHighlights = highlightsForPage(highlights, animPage)
                                val isActivePage = pageIndex == safeContentPage

                                TafseerHighlightableText(
                                    text = animPage.text,
                                    highlights = animHighlights,
                                    selectionStart = if (isActivePage) selStart else -1,
                                    selectionEnd = if (isActivePage) selEnd else -1,
                                    onSelectionChange = { start, end ->
                                        if (start < 0) {
                                            selStart = -1; selEnd = -1
                                        } else {
                                            selStart = start; selEnd = end
                                        }
                                    },
                                    onHighlightTapped = { tapped ->
                                        // Remapped to page-local; resolve the full highlight by id.
                                        val full = highlights.find { it.id == tapped.id }
                                        if (full != null) {
                                            val s =
                                                full.startOffset.coerceIn(0, tafseerFullText.length)
                                            val e =
                                                full.endOffset.coerceIn(s, tafseerFullText.length)
                                            editorTarget = EditorTarget.Existing(
                                                highlight = full,
                                                snippet = tafseerFullText.substring(s, e)
                                            )
                                            clearSelection()
                                        }
                                    },
                                    clearSelectionToken = clearSelectionToken
                                )
                            }
                        } else {
                            TafseerEmptyState(
                                selectedSource = selectedSource,
                                availableSources = availableSources,
                                onSourceSwitch = onSourceSwitch
                            )
                        }

                        // Discoverability hint for the long-press gesture.
                        if (tafseerPages.isNotEmpty() && highlights.isEmpty() && !hasSelection) {
                            TafseerHighlightHint()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Contextual selection action (replaces the old colour rail) ──
        AnimatedVisibility(
            visible = hasSelection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SelectionActionBar(
                onAddHighlight = {
                    val page = tafseerPages.getOrNull(safeContentPage)
                    if (page != null) {
                        val localStart = selStart.coerceIn(0, page.text.length)
                        val localEnd = selEnd.coerceIn(localStart, page.text.length)
                        if (localStart < localEnd) {
                            editorTarget = EditorTarget.New(
                                globalStart = localStart + page.globalStartOffset,
                                globalEnd = localEnd + page.globalStartOffset,
                                snippet = page.text.substring(localStart, localEnd)
                            )
                        }
                    }
                },
                onClear = { clearSelection() }
            )
        }

        // ── Bottom bar (page nav + actions) — matches the Dua/Hadith readers ──
        NimazReaderBottomBar(
            currentPage = safeContentPage,
            pageCount = totalContentPages,
            onPrev = { if (safeContentPage > 0) onContentPageChanged(safeContentPage - 1) },
            onNext = {
                if (safeContentPage < totalContentPages - 1) onContentPageChanged(safeContentPage + 1)
            },
            prevContentDescription = stringResource(R.string.cd_previous_page),
            nextContentDescription = stringResource(R.string.cd_next_page)
        ) {
            NimazPillActionButton(
                icon = Icons.Outlined.EditNote,
                contentDescription = stringResource(R.string.cd_notes),
                onClick = { showNotesSheet = true }
            )
            NimazPillActionButton(
                icon = Icons.Default.Share,
                contentDescription = stringResource(R.string.cd_share_tafseer),
                onClick = onShare
            )
        }
    }

    // ── Unified highlight editor sheet (create + edit in one step) ──
    editorTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isEditing = target is EditorTarget.Existing
        val snippet = when (target) {
            is EditorTarget.New -> target.snippet
            is EditorTarget.Existing -> target.snippet
        }
        val initialColor = when (target) {
            is EditorTarget.New -> highlightColors.first().first
            is EditorTarget.Existing -> target.highlight.color
        }
        val initialNote = when (target) {
            is EditorTarget.New -> ""
            is EditorTarget.Existing -> target.highlight.note.orEmpty()
        }

        NimazBottomSheet(
            onDismissRequest = { editorTarget = null },
            sheetState = sheetState,
            title = stringResource(R.string.tafseer_highlight),
            icon = Icons.Outlined.EditNote,
            onClose = { editorTarget = null },
            scrollable = true
        ) {
            HighlightEditorSheetContent(
                snippet = snippet,
                initialColor = initialColor,
                initialNote = initialNote,
                isEditing = isEditing,
                onSave = { color, note ->
                    when (target) {
                        is EditorTarget.New -> onHighlightCreated(
                            target.globalStart,
                            target.globalEnd,
                            color,
                            note.ifBlank { null }
                        )

                        is EditorTarget.Existing -> onHighlightUpdated(
                            target.highlight.id,
                            color,
                            note.ifBlank { null }
                        )
                    }
                    editorTarget = null
                    clearSelection()
                },
                onDelete = (target as? EditorTarget.Existing)?.let { existing ->
                    {
                        onHighlightDeleted(existing.highlight.id)
                        editorTarget = null
                    }
                },
                onCancel = { editorTarget = null }
            )
        }
    }

    // ── Notes list bottom sheet ──
    if (showNotesSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        NimazBottomSheet(
            onDismissRequest = { showNotesSheet = false },
            sheetState = sheetState,
            title = stringResource(R.string.tafseer_highlight_notes),
            icon = Icons.Outlined.EditNote,
            onClose = { showNotesSheet = false },
            scrollable = true
        ) {
            HighlightNotesListContent(
                highlights = highlightsWithNotes,
                tafseerText = tafseerFullText,
                onHighlightTapped = { highlight ->
                    showNotesSheet = false
                    val s = highlight.startOffset.coerceIn(0, tafseerFullText.length)
                    val e = highlight.endOffset.coerceIn(s, tafseerFullText.length)
                    editorTarget = EditorTarget.Existing(
                        highlight = highlight,
                        snippet = tafseerFullText.substring(s, e)
                    )
                }
            )
        }
    }
}

// ── Long-press discoverability hint ───────────────────────────────────────────

@Composable
private fun TafseerHighlightHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        NimazIcon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            variant = NimazIconVariant.MUTED,
            iconSize = 16.dp
        )
        Text(
            text = stringResource(R.string.tafseer_highlight_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Contextual selection action bar ───────────────────────────────────────────

@Composable
private fun SelectionActionBar(
    onAddHighlight: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tafseer_selection_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            NimazButton(
                text = stringResource(R.string.tafseer_clear),
                onClick = onClear,
                variant = NimazButtonVariant.TEXT
            )
            NimazButton(
                text = stringResource(R.string.tafseer_add_highlight),
                onClick = onAddHighlight,
                variant = NimazButtonVariant.FILLED,
                type = NimazButtonType.PILL
            )
        }
    }
}

// ── Highlight editor (one-step colour + note) ─────────────────────────────────

@Composable
private fun HighlightEditorSheetContent(
    snippet: String,
    initialColor: String,
    initialNote: String,
    isEditing: Boolean,
    onSave: (color: String, note: String) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }
    var noteText by remember(initialNote) { mutableStateOf(initialNote) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Highlighted snippet preview, tinted with the chosen colour.
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = parseColor(selectedColor).copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (snippet.length > 200) snippet.take(200) + "…" else snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Colour picker (the previously-separate step, now inline).
        NimazSheetSectionLabel(text = stringResource(R.string.tafseer_colour))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            highlightColors.forEach { (hex, name) ->
                val isSelected = hex == selectedColor
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseColor(hex))
                        .then(
                            if (isSelected) Modifier.border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            ) else Modifier
                        )
                        .clickable { selectedColor = hex }
                ) {
                    if (isSelected) {
                        NimazIcon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_item_selected, name),
                            tint = MaterialTheme.colorScheme.onSurface,
                            iconSize = 20.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Note (optional).
        NimazTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = stringResource(R.string.tafseer_note_optional),
            variant = NimazFieldVariant.NOTE,
            placeholder = stringResource(R.string.tafseer_add_note),
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Actions: a high-emphasis primary "Save" (issue #208), plus delete when editing.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onDelete != null) {
                NimazButton(
                    text = stringResource(R.string.delete),
                    onClick = { showDeleteConfirm = true },
                    variant = NimazButtonVariant.DESTRUCTIVE,
                    type = NimazButtonType.PILL,
                    leadingIcon = Icons.Default.Delete,
                    modifier = Modifier.weight(1f)
                )
            } else {
                NimazButton(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel,
                    variant = NimazButtonVariant.OUTLINED,
                    type = NimazButtonType.PILL,
                    modifier = Modifier.weight(1f)
                )
            }
            NimazButton(
                text = stringResource(R.string.save),
                onClick = { onSave(selectedColor, noteText) },
                variant = NimazButtonVariant.FILLED,
                type = NimazButtonType.PILL,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        NimazConfirmDialog(
            title = stringResource(R.string.tafseer_delete_highlight_title),
            message = stringResource(R.string.tafseer_delete_highlight_message),
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            titleIcon = Icons.Default.Delete,
            isDestructive = true,
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun HighlightNotesListContent(
    highlights: List<TafseerHighlight>,
    tafseerText: String,
    onHighlightTapped: (TafseerHighlight) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        if (highlights.isEmpty()) {
            Text(
                text = stringResource(R.string.tafseer_no_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            highlights.forEach { highlight ->
                val start = highlight.startOffset.coerceIn(0, tafseerText.length)
                val end = highlight.endOffset.coerceIn(start, tafseerText.length)
                val snippet = if (start < end) tafseerText.substring(start, end) else ""

                NimazCard(
                    onClick = { onHighlightTapped(highlight) },
                    style = NimazCardStyle.OUTLINED,
                    tone = NimazTone.NEUTRAL,
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(highlight.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (snippet.length > 80) snippet.take(80) + "…" else snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = highlight.note ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
private fun TafseerEmptyState(
    selectedSource: TafseerSource,
    availableSources: Set<TafseerSource>,
    onSourceSwitch: (TafseerSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val alternate = TafseerSource.entries
        .firstOrNull { it != selectedSource && it in availableSources }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(
                R.string.tafseer_no_commentary_format,
                selectedSource.displayName
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (alternate != null) {
                stringResource(
                    R.string.tafseer_alternate_has_commentary_format,
                    alternate.displayName
                )
            } else {
                stringResource(R.string.tafseer_no_commentary_anywhere)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (alternate != null) {
            NimazButton(
                text = stringResource(R.string.tafseer_read_in_format, alternate.displayName),
                onClick = { onSourceSwitch(alternate) },
                variant = NimazButtonVariant.TEXT
            )
        }
    }
}


// ==================== PREVIEWS ====================

private val previewAyah = Ayah(
    id = 255,
    surahNumber = 2,
    ayahNumber = 255,
    textArabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
    textSimple = "Allahu la ilaha illa huwa al-hayyu al-qayyum",
    juzNumber = 3,
    hizbNumber = 5,
    rubNumber = 20,
    pageNumber = 42,
    sajdaType = null,
    sajdaNumber = null,
    translation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of existence."
)

private val previewTafseer = TafseerText(
    id = 1L,
    surahNumber = 2,
    ayahStart = 255,
    ayahEnd = 255,
    tafseerId = "ibn_kathir_en",
    text = "This is Ayat al-Kursi, and great virtues have been narrated about it. " +
            "It contains the greatest name of Allah, by which when He is called, He " +
            "responds, and when He is asked, He gives. The ayah affirms the oneness " +
            "of Allah and His perfect, eternal attributes: He is the Ever-Living who " +
            "never dies, and the Sustainer who maintains all of creation. Neither " +
            "drowsiness nor sleep overtakes Him, for these are signs of imperfection " +
            "from which He is free."
)

private val previewHighlights = listOf(
    TafseerHighlight(
        id = 1L,
        ayahId = 255,
        tafseerId = "ibn_kathir_en",
        startOffset = 0,
        endOffset = 16,
        color = "#FDE68A",
        note = "Ayat al-Kursi",
        createdAt = 0L,
        updatedAt = 0L
    )
)

@Composable
private fun TafseerPageContentShowcase() {
    TafseerPageContent(
        ayah = previewAyah,
        tafseer = previewTafseer,
        highlights = previewHighlights,
        selectedSource = TafseerSource.IBN_KATHIR,
        availableSources = setOf(TafseerSource.IBN_KATHIR, TafseerSource.MAARIFUL_QURAN),
        currentContentPage = 0,
        onContentPageChanged = {},
        onSourceSwitch = {},
        onHighlightCreated = { _, _, _, _ -> },
        onHighlightUpdated = { _, _, _ -> },
        onHighlightDeleted = {},
        onShare = {}
    )
}

@Preview(showBackground = true, name = "TafseerPageContent — Light")
@Composable
private fun TafseerPageContentLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        TafseerPageContentShowcase()
    }
}

@Preview(showBackground = true, name = "TafseerPageContent — Dark")
@Composable
private fun TafseerPageContentDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        TafseerPageContentShowcase()
    }
}

/**
 * The subjects a verse is filed under, as chips.
 *
 * Capped at six. "Allah" and "Quran" are cited by hundreds of verses each, so a busy verse can
 * carry a dozen topics and the row would push the commentary itself off the first screen — the
 * chips are a doorway out of this verse, not the reason to be on it. They are ordered by how
 * many verses the subject covers, so the six shown are the substantial ones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyahTopicChips(
    topics: List<QuranTopic>,
    onTopicClick: (topicId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.quran_ayah_topics),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.take(MAX_AYAH_TOPICS).forEach { topic ->
                NimazChip(
                    text = topic.name,
                    onClick = { onTopicClick(topic.id) },
                    variant = NimazChipVariant.SUGGESTION
                )
            }
        }
    }
}

private const val MAX_AYAH_TOPICS = 6
