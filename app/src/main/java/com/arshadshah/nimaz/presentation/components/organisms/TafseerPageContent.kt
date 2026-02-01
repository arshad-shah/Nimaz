package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.TafseerBookFrame
import com.arshadshah.nimaz.presentation.components.molecules.TafseerHighlightableText
import com.arshadshah.nimaz.presentation.components.molecules.TafseerOrnamentalDivider
import com.arshadshah.nimaz.presentation.components.molecules.highlightColors
import com.arshadshah.nimaz.presentation.components.molecules.parseColor

/**
 * Represents a page of tafseer content with its character range in the full text.
 */
private data class TafseerPage(
    val text: String,
    val globalStartOffset: Int,
    val globalEndOffset: Int
)

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
        pages.add(TafseerPage(fullText.substring(currentStart, pageEnd).trimEnd(), currentStart, pageEnd))
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
    totalAyahs: Int,
    selectedSource: TafseerSource,
    onSourceSwitch: (TafseerSource) -> Unit,
    onHighlightCreated: (startOffset: Int, endOffset: Int, color: String) -> Unit,
    onHighlightDeleted: (highlightId: Long) -> Unit,
    onHighlightNoteUpdated: (highlightId: Long, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHighlightMode by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(highlightColors.first().first) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var currentContentPage by remember { mutableIntStateOf(0) }
    var tappedHighlightId by remember { mutableStateOf<Long?>(null) }

    val tafseerPages = remember(tafseer?.text) {
        if (tafseer != null && tafseer.text.isNotBlank()) splitTafseerIntoPages(tafseer.text) else emptyList()
    }
    val totalContentPages = tafseerPages.size
    val tafseerFullText = tafseer?.text ?: ""

    remember(tafseer?.id) { currentContentPage = 0; true }

    val highlightsWithNotes = remember(highlights) { highlights.filter { !it.note.isNullOrBlank() } }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Static Navigation Bar (always visible at top) ──
        TafseerNavBar(
            ayahNumber = ayah.ayahNumber,
            totalAyahs = totalAyahs,
            selectedSource = selectedSource,
            onSourceSwitch = onSourceSwitch,
            currentContentPage = currentContentPage,
            totalContentPages = totalContentPages,
            onPreviousPage = { if (currentContentPage > 0) currentContentPage-- },
            onNextPage = { if (currentContentPage < totalContentPages - 1) currentContentPage++ }
        )

        // ── Scrollable Content ──
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                TafseerBookFrame(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Arabic ayah text on first content page
                        if (currentContentPage == 0) {
                            ArabicText(
                                text = ayah.textArabic,
                                size = ArabicTextSize.LARGE,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                            TafseerOrnamentalDivider()

                            if (!ayah.translation.isNullOrBlank()) {
                                Text(
                                    text = ayah.translation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                                TafseerOrnamentalDivider()
                            }
                        }

                        // Tafseer text
                        if (tafseerPages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedContent(
                                targetState = currentContentPage,
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
                                val animPage = tafseerPages[pageIndex.coerceIn(0, tafseerPages.lastIndex)]
                                val animHighlights = highlightsForPage(highlights, animPage)

                                TafseerHighlightableText(
                                    text = animPage.text,
                                    highlights = animHighlights,
                                    isHighlightMode = isHighlightMode,
                                    selectedColor = selectedColor,
                                    onHighlightCreated = { localStart, localEnd, color ->
                                        onHighlightCreated(
                                            localStart + animPage.globalStartOffset,
                                            localEnd + animPage.globalStartOffset,
                                            color
                                        )
                                    },
                                    onHighlightTapped = { highlight ->
                                        tappedHighlightId = highlight.id
                                    }
                                )
                            }
                        } else {
                            Text(
                                text = "No tafseer available for this ayah.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Floating highlight controls
            TafseerHighlightControls(
                isHighlightMode = isHighlightMode,
                onToggleHighlightMode = { isHighlightMode = !isHighlightMode },
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
                noteCount = highlightsWithNotes.size,
                onNoteButtonClick = { showNotesSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    // Highlight detail bottom sheet
    tappedHighlightId?.let { highlightId ->
        val highlight = highlights.find { it.id == highlightId }
        if (highlight != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { tappedHighlightId = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                HighlightDetailSheetContent(
                    highlight = highlight,
                    tafseerText = tafseerFullText,
                    onNoteSaved = { note ->
                        onHighlightNoteUpdated(highlightId, note.ifBlank { null })
                        tappedHighlightId = null
                    },
                    onDelete = {
                        onHighlightDeleted(highlightId)
                        tappedHighlightId = null
                    },
                    onDismiss = { tappedHighlightId = null }
                )
            }
        }
    }

    // Notes list bottom sheet
    if (showNotesSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showNotesSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            HighlightNotesListContent(
                highlights = highlightsWithNotes,
                tafseerText = tafseerFullText,
                onHighlightTapped = { highlight ->
                    showNotesSheet = false
                    tappedHighlightId = highlight.id
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ── Static Navigation Bar ──────────────────────────────────────────────────────

@Composable
private fun TafseerNavBar(
    ayahNumber: Int,
    totalAyahs: Int,
    selectedSource: TafseerSource,
    onSourceSwitch: (TafseerSource) -> Unit,
    currentContentPage: Int,
    totalContentPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Top row: Ayah indicator + Source chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Ayah $ayahNumber / $totalAyahs",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Source chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TafseerSource.entries.forEach { source ->
                        FilterChip(
                            selected = selectedSource == source,
                            onClick = { onSourceSwitch(source) },
                            label = {
                                Text(
                                    text = source.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // Bottom row: Page navigation (only if multiple pages)
            if (totalContentPages > 1) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = currentContentPage > 0,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous page",
                            tint = if (currentContentPage > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Page ${currentContentPage + 1} of $totalContentPages",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = onNextPage,
                        enabled = currentContentPage < totalContentPages - 1,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next page",
                            tint = if (currentContentPage < totalContentPages - 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Bottom Sheet Contents ───────────────────────────────────────────────────────

@Composable
private fun HighlightDetailSheetContent(
    highlight: TafseerHighlight,
    tafseerText: String,
    onNoteSaved: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteText by remember(highlight.id) { mutableStateOf(highlight.note ?: "") }

    val start = highlight.startOffset.coerceIn(0, tafseerText.length)
    val end = highlight.endOffset.coerceIn(start, tafseerText.length)
    val snippet = if (start < end) tafseerText.substring(start, end) else ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(parseColor(highlight.color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Highlight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = parseColor(highlight.color).copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (snippet.length > 150) snippet.take(150) + "..." else snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text(if (highlight.note.isNullOrBlank()) "Add a note..." else "Edit note") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }

            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = { onNoteSaved(noteText) }
                ) {
                    Text("Save")
                }
            }
        }
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
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Highlight Notes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (highlights.isEmpty()) {
            Text(
                text = "No notes yet. Tap a highlight to add a note.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            highlights.forEach { highlight ->
                val start = highlight.startOffset.coerceIn(0, tafseerText.length)
                val end = highlight.endOffset.coerceIn(start, tafseerText.length)
                val snippet = if (start < end) tafseerText.substring(start, end) else ""

                Surface(
                    onClick = { onHighlightTapped(highlight) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                text = if (snippet.length > 80) snippet.take(80) + "..." else snippet,
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

// ── Floating Highlight Controls ─────────────────────────────────────────────────

@Composable
private fun TafseerHighlightControls(
    isHighlightMode: Boolean,
    onToggleHighlightMode: () -> Unit,
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    noteCount: Int,
    onNoteButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onToggleHighlightMode, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isHighlightMode) "Disable highlighting" else "Enable highlighting",
                    tint = if (isHighlightMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = isHighlightMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    highlightColors.forEach { (hex, name) ->
                        val isSelected = hex == selectedColor
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseColor(hex))
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                                    else Modifier
                                )
                                .clickable { onColorSelected(hex) }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "$name selected",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onNoteButtonClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (noteCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = noteCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
    }
    }
}
