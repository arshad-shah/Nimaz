package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.TafseerBookFrame
import com.arshadshah.nimaz.presentation.components.molecules.TafseerHighlightableText
import com.arshadshah.nimaz.presentation.components.molecules.TafseerNoteCard
import com.arshadshah.nimaz.presentation.components.molecules.TafseerOrnamentalDivider
import com.arshadshah.nimaz.presentation.components.molecules.highlightColors
import com.arshadshah.nimaz.presentation.components.molecules.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerPageContent(
    ayah: Ayah,
    tafseer: TafseerText?,
    highlights: List<TafseerHighlight>,
    notes: List<TafseerNote>,
    totalAyahs: Int,
    selectedSource: TafseerSource,
    onSourceSwitch: (TafseerSource) -> Unit,
    onHighlightCreated: (startOffset: Int, endOffset: Int, color: String) -> Unit,
    onHighlightDeleted: (highlightId: Long) -> Unit,
    onNoteAdded: (String) -> Unit,
    onNoteUpdated: (TafseerNote) -> Unit,
    onNoteDeleted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHighlightMode by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(highlightColors.first().first) }
    var showNotesSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp) // space for floating controls
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TafseerBookFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Source switcher
                    TafseerSourceSwitcher(
                        selectedSource = selectedSource,
                        onSourceSwitch = onSourceSwitch
                    )

                    TafseerOrnamentalDivider()

                    // 2. Arabic ayah text
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

                    // 3. Translation
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

                    // 4. Tafseer body
                    if (tafseer != null && tafseer.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TafseerHighlightableText(
                            text = tafseer.text,
                            highlights = highlights,
                            isHighlightMode = isHighlightMode,
                            selectedColor = selectedColor,
                            onHighlightCreated = onHighlightCreated,
                            onHighlightDeleted = onHighlightDeleted
                        )
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

            // 5. Page indicator
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ayah ${ayah.ayahNumber} of $totalAyahs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. Floating highlight controls
        TafseerHighlightControls(
            isHighlightMode = isHighlightMode,
            onToggleHighlightMode = { isHighlightMode = !isHighlightMode },
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it },
            noteCount = notes.size,
            onNoteButtonClick = { showNotesSheet = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // 7. Notes bottom sheet
    if (showNotesSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showNotesSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NotesSheetContent(
                notes = notes,
                onNoteAdded = onNoteAdded,
                onNoteUpdated = onNoteUpdated,
                onNoteDeleted = onNoteDeleted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun TafseerSourceSwitcher(
    selectedSource: TafseerSource,
    onSourceSwitch: (TafseerSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TafseerSource.entries.forEach { source ->
            FilterChip(
                selected = selectedSource == source,
                onClick = { onSourceSwitch(source) },
                label = {
                    Text(
                        text = source.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

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
            // Highlighter toggle
            IconButton(
                onClick = onToggleHighlightMode,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isHighlightMode) "Disable highlighting" else "Enable highlighting",
                    tint = if (isHighlightMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Color circles (animated show/hide)
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
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                    } else Modifier
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

            // Notes button
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onNoteButtonClick,
                    modifier = Modifier.size(40.dp)
                ) {
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

@Composable
private fun NotesSheetContent(
    notes: List<TafseerNote>,
    onNoteAdded: (String) -> Unit,
    onNoteUpdated: (TafseerNote) -> Unit,
    onNoteDeleted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddNote by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<TafseerNote?>(null) }
    var editNoteText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = { showAddNote = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Add Note")
            }
        }

        // Add note field
        if (showAddNote) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                label = { Text("Write your note...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    showAddNote = false
                    newNoteText = ""
                }) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        if (newNoteText.isNotBlank()) {
                            onNoteAdded(newNoteText)
                            newNoteText = ""
                            showAddNote = false
                        }
                    },
                    enabled = newNoteText.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }

        // Edit note inline
        editingNote?.let { note ->
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editNoteText,
                onValueChange = { editNoteText = it },
                label = { Text("Edit note...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    editingNote = null
                    editNoteText = ""
                }) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        if (editNoteText.isNotBlank()) {
                            onNoteUpdated(note.copy(text = editNoteText))
                            editingNote = null
                            editNoteText = ""
                        }
                    },
                    enabled = editNoteText.isNotBlank()
                ) {
                    Text("Update")
                }
            }
        }

        // Existing notes
        Spacer(modifier = Modifier.height(8.dp))
        notes.forEach { note ->
            TafseerNoteCard(
                note = note,
                onEdit = { n ->
                    editingNote = n
                    editNoteText = n.text
                },
                onDelete = onNoteDeleted
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notes.isEmpty() && !showAddNote) {
            Text(
                text = "No notes yet. Tap \"Add Note\" to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
