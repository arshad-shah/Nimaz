package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.ReadingProgress
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.DropdownListItem
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.quran.CompactSurahCard
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuranScreen(
    bookmarks: State<List<LocalAya>>,
    suraList: State<ArrayList<LocalSurah>>,
    favorites: State<List<LocalAya>>,
    notes: State<List<LocalAya>>,

    // ADD THESE NEW PARAMETERS:
    readingProgress: State<List<ReadingProgress>>,

    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    handleEvents: (QuranViewModel.AyaEvent) -> Unit,

    // ADD THESE NEW CALLBACKS:
    onDeleteReadingProgress: (ReadingProgress) -> Unit,
    onClearAllProgress: () -> Unit,
    khatamState: State<QuranViewModel.KhatamState>,
    onKhatamEvent: (QuranViewModel.AyaEvent) -> Unit,
    onNavigateToStartKhatam: () -> Unit,
    onNavigateToEditKhatam: (Long) -> Unit,

    isLoading: State<Boolean>,
) {
    val context = LocalContext.current
    val translation = remember {
        when (PrivateSharedPreferences(context)
            .getData(AppConstants.TRANSLATION_LANGUAGE, "English")) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    var dialogState by remember { mutableStateOf<DialogState?>(null) }
    var showClearProgressDialog by remember { mutableStateOf(false) }

    // Refresh all data when screen becomes visible
    LaunchedEffect(Unit) {
        handleEvents(QuranViewModel.AyaEvent.getBookmarks)
        handleEvents(QuranViewModel.AyaEvent.getFavorites)
        handleEvents(QuranViewModel.AyaEvent.getNotes)
        // Refresh khatam data to ensure we have the latest state
        onKhatamEvent(QuranViewModel.AyaEvent.RefreshKhatamData)
    }

    // UPDATED: Add new sections for reading progress and quick jumps
    val sections = listOf(
        SectionData("Continue Reading", emptyList(), DeleteType.READING_PROGRESS),
        SectionData("Bookmarks", bookmarks.value, DeleteType.BOOKMARK),
        SectionData("Favorites", favorites.value, DeleteType.FAVORITE),
        SectionData("Notes", notes.value, DeleteType.NOTE)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        item {
            KhatamSection(
                khatamState = khatamState.value,
                onEvent = onKhatamEvent,
                onNavigateToAyatScreen = onNavigateToAyatScreen,
                onNavigateToStartKhatam = onNavigateToStartKhatam,
                onNavigateToEditKhatam = onNavigateToEditKhatam,
                translation = translation
            )
        }
        // READING PROGRESS SECTION
        item {
            ReadingProgressSection(
                readingProgress = readingProgress.value,
                suraList = suraList.value,
                onNavigateToAyatScreen = onNavigateToAyatScreen,
                onDeleteProgress = onDeleteReadingProgress,
                onClearAll = { showClearProgressDialog = true },
                translation = translation
            )
        }

        // EXISTING SECTIONS (Bookmarks, Favorites, Notes)
        items(sections.drop(1)) { section ->
            FeaturesDropDown(
                modifier = Modifier.padding(4.dp),
                label = section.title,
                items = section.items,
                dropDownItem = { aya ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                dialogState = DialogState(
                                    title = "Delete ${section.title}",
                                    message = "Are you sure you want to delete this ${
                                        section.title.dropLast(1)
                                    }?",
                                    item = aya,
                                    type = section.type
                                )
                            }
                            false
                        }
                    )

                    SwipeToDismissBox(
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        state = dismissState,
                        backgroundContent = {
                            SwipeBackground(dismissState = dismissState)
                        },
                        content = {
                            DropdownListItem(
                                item = aya,
                                onClick = { },
                                content = {
                                    val surah = suraList.value.find { it.number == aya.suraNumber }
                                    surah?.let {
                                        CompactSurahCard(
                                            surah = it,
                                            { _, _, _, _ ->
                                                onNavigateToAyatScreen(
                                                    it.number.toString(),
                                                    true,
                                                    translation,
                                                    aya.ayaNumberInSurah
                                                )
                                            },
                                            loading = isLoading.value
                                        )
                                    }
                                }
                            )
                        })
                }
            )
        }

        // FREQUENTLY READ SURAHS (unchanged)
        item {
            FeaturesDropDown(
                modifier = Modifier.padding(4.dp),
                label = "Frequently Read Surahs",
                items = getFrequentlyReadSurahs().toList(),
                showBadge = false,
                dropDownItem = { (_, details) ->
                    val surah = suraList.value.find { it.number == details.first.toInt() }
                    surah?.let {
                        CompactSurahCard(
                            surah = it,
                            { _, _, _, _ ->
                                onNavigateToAyatScreen(
                                    details.first,
                                    true,
                                    translation,
                                    details.second
                                )
                            },
                            loading = isLoading.value
                        )
                    }
                }
            )
        }
    }

    // EXISTING DELETE DIALOG
    dialogState?.let { state ->
        AlertDialogNimaz(
            title = state.title,
            contentToShow = {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            },
            onDismissRequest = { dialogState = null },
            contentHeight = 100.dp,
            confirmButtonText = "Yes",
            dismissButtonText = "No",
            onConfirm = {
                handleEvents(getDeleteEvent(state))
                dialogState = null
            },
            onDismiss = { dialogState = null },
            contentDescription = "Delete ${state.type.name}"
        )
    }

    // NEW: Clear all progress dialog
    if (showClearProgressDialog) {
        AlertDialogNimaz(
            title = "Clear All Progress",
            contentToShow = {
                Text(
                    text = "Are you sure you want to clear all reading progress? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            },
            onDismissRequest = { showClearProgressDialog = false },
            contentHeight = 120.dp,
            confirmButtonText = "Clear All",
            dismissButtonText = "Cancel",
            onConfirm = {
                onClearAllProgress()
                showClearProgressDialog = false
            },
            onDismiss = { showClearProgressDialog = false },
            contentDescription = "Clear all reading progress"
        )
    }
}

@Composable
fun KhatamSection(
    khatamState: QuranViewModel.KhatamState,
    onEvent: (QuranViewModel.AyaEvent) -> Unit,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    onNavigateToStartKhatam: () -> Unit,
    onNavigateToEditKhatam: (Long) -> Unit,
    translation: String
) {
    var isExpanded by remember { mutableStateOf(true) }

    ElevatedCard(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section - matching FeaturesDropDown style
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quran Khatam",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge showing active/count
                        if (khatamState.activeKhatam != null || khatamState.allKhatams.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (khatamState.activeKhatam != null) "Active" else "${khatamState.allKhatams.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }

            // Content Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        khatamState.activeKhatam?.let { khatam ->
                            ActiveKhatamCard(
                                khatam = khatam,
                                todayProgress = khatamState.todayProgress,
                                onContinueReading = {
                                    onNavigateToAyatScreen(
                                        khatam.currentSurah.toString(),
                                        true,
                                        translation,
                                        khatam.currentAya
                                    )
                                },
                                onComplete = {
                                    onEvent(QuranViewModel.AyaEvent.CompleteKhatam(khatam.id))
                                },
                                onPause = {
                                    onEvent(QuranViewModel.AyaEvent.PauseKhatam(khatam.id))
                                },
                                onResume = {
                                    onEvent(QuranViewModel.AyaEvent.ResumeKhatam(khatam.id))
                                },
                                onEdit = {
                                    onNavigateToEditKhatam(khatam.id)
                                },
                                onDelete = {
                                    onEvent(QuranViewModel.AyaEvent.ShowDeleteKhatamDialog(true, khatam))
                                }
                            )
                        } ?: run {
                            // No active khatam - show start button
                            StartNewKhatamCard(
                                onStartNew = onNavigateToStartKhatam
                            )
                        }

                        // Show completed khatams if any
                        if (khatamState.khatamHistory.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Completed (${khatamState.khatamHistory.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            khatamState.khatamHistory.take(3).forEach { khatam ->
                                CompletedKhatamItem(
                                    khatam = khatam,
                                    onDelete = {
                                        onEvent(QuranViewModel.AyaEvent.ShowDeleteKhatamDialog(true, khatam))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // Delete Confirmation Dialog
    if (khatamState.showDeleteDialog && khatamState.editingKhatam != null) {
        DeleteKhatamConfirmDialog(
            khatam = khatamState.editingKhatam,
            onDismiss = { onEvent(QuranViewModel.AyaEvent.ShowDeleteKhatamDialog(false, null)) },
            onConfirm = {
                onEvent(QuranViewModel.AyaEvent.DeleteKhatam(khatamState.editingKhatam.id))
                onEvent(QuranViewModel.AyaEvent.ShowDeleteKhatamDialog(false, null))
            }
        )
    }
}


@Composable
fun ActiveKhatamCard(
    khatam: KhatamSession,
    todayProgress: Int,
    onContinueReading: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = khatam.totalAyasRead.toFloat() / 6236f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = khatam.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Started: ${khatam.startDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    color = if (khatam.isActive) Color.Green.copy(alpha = 0.2f) else Color.Yellow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (khatam.isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (khatam.isActive) Color.Green else Color(0xFFB8860B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Surah ${khatam.currentSurah}, Aya ${khatam.currentAya}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            khatam.dailyTarget?.let { target ->
                Text(
                    text = "Today: $todayProgress / $target ayas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (todayProgress >= target) Color.Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress >= 0.99f) {
                    // Nearly complete - show complete button
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Complete", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    // Always show Continue Reading button
                    OutlinedButton(
                        onClick = onContinueReading,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Continue", style = MaterialTheme.typography.labelSmall)
                    }

                    // Pause/Resume toggle
                    OutlinedButton(
                        onClick = if (khatam.isActive) onPause else onResume,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (khatam.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            if (khatam.isActive) "Pause" else "Resume",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KhatamListItem(
    khatam: KhatamSession,
    onContinue: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val progress = khatam.totalAyasRead.toFloat() / 6236f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = khatam.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Started: ${khatam.startDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (khatam.isCompleted && khatam.completionDate != null) {
                        Text(
                            text = "Completed: ${khatam.completionDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                }

                Surface(
                    color = when {
                        khatam.isCompleted -> Color.Green.copy(alpha = 0.2f)
                        khatam.isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> Color.Yellow.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            khatam.isCompleted -> "✓"
                            khatam.isActive -> "Active"
                            else -> "Paused"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            khatam.isCompleted -> Color.Green
                            khatam.isActive -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFB8860B)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!khatam.isCompleted) {
                // Progress info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Surah ${khatam.currentSurah}, Aya ${khatam.currentAya}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = if (khatam.isActive) onPause else onResume,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (khatam.isActive) "Pause" else "Resume",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Read", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // For completed khatams, only show edit/delete buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StartNewKhatamCard(
    onStartNew: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartNew() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start New Khatam",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Begin your Quran reading journey",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Start",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CompletedKhatamItem(
    khatam: KhatamSession,
    onDelete: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = khatam.name,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = khatam.completionDate ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}


@Composable
fun DeleteKhatamConfirmDialog(
    khatam: KhatamSession,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialogNimaz(
        title = "Delete Khatam",
        contentDescription = "Delete Khatam Confirmation",
        onDismissRequest = onDismiss,
        contentHeight = 180.dp,
        confirmButtonText = "Delete",
        dismissButtonText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        contentToShow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Are you sure you want to delete \"${khatam.name}\"?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "This will permanently delete this Khatam and all its progress data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

// ============ NEW SECTION COMPONENTS ============

@Composable
private fun ReadingProgressSection(
    readingProgress: List<ReadingProgress>,
    suraList: List<LocalSurah>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    onDeleteProgress: (ReadingProgress) -> Unit,
    onClearAll: () -> Unit,
    translation: String
) {
    FeaturesDropDown(
        modifier = Modifier.padding(4.dp),
        label = "Continue Reading",
        items = readingProgress,
        showBadge = true,
        dropDownItem = { progress ->
            ReadingProgressCard(
                progress = progress,
                surah = suraList.find { it.number == progress.surahNumber },
                onContinueReading = {
                    onNavigateToAyatScreen(
                        progress.surahNumber.toString(),
                        true,
                        translation,
                        progress.lastReadAyaNumber
                    )
                },
                onDelete = { onDeleteProgress(progress) }
            )
        },
    )
}

// ============ CARD COMPONENTS ============

@Composable
private fun ReadingProgressCard(
    progress: ReadingProgress,
    surah: LocalSurah?,
    onContinueReading: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = surah?.englishNameTranslation ?: "Surah ${progress.surahNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Last read: Aya ${progress.lastReadAyaNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = "${progress.completionPercentage.toInt()}% completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete progress",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.completionPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinueReading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue Reading")
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// ============ UPDATED DATA CLASSES ============

private data class SectionData(
    val title: String,
    val items: List<LocalAya>,
    val type: DeleteType
)

// UPDATED: Add new delete types
private enum class DeleteType {
    BOOKMARK, FAVORITE, NOTE, READING_PROGRESS
}

private data class DialogState(
    val title: String,
    val message: String,
    val item: LocalAya,
    val type: DeleteType
)

// ============ UTILITY FUNCTIONS (unchanged) ============

private fun getDeleteEvent(state: DialogState): QuranViewModel.AyaEvent {
    val aya = state.item
    return when (state.type) {
        DeleteType.BOOKMARK -> QuranViewModel.AyaEvent.deleteBookmarkFromAya(
            aya.ayaNumberInSurah, aya.suraNumber, aya.ayaNumberInSurah
        )

        DeleteType.FAVORITE -> QuranViewModel.AyaEvent.deleteFavoriteFromAya(
            aya.ayaNumberInSurah, aya.suraNumber, aya.ayaNumberInSurah
        )

        DeleteType.NOTE -> QuranViewModel.AyaEvent.deleteNoteFromAya(
            aya.ayaNumberInSurah, aya.suraNumber, aya.ayaNumberInSurah
        )

        else -> QuranViewModel.AyaEvent.getBookmarks // fallback
    }
}

private fun getFrequentlyReadSurahs() = mapOf(
    "Al-Fatiha" to Triple("1", 1, "الْفَاتِحَة"),
    "Al-Baqarah" to Triple("2", 1, "الْبَقَرَة"),
    "Yaseen" to Triple("36", 1, "يس"),
    "Ar-Rahman" to Triple("55", 1, "الرَّحْمَٰن"),
    "Al-Mulk" to Triple("67", 1, "الْمُلْك"),
    "Al-Kawthar" to Triple("108", 1, "الْكَوْثَر"),
    "Al-Ikhlas" to Triple("112", 1, "الْإِخْلَاص"),
    "Al-Falaq" to Triple("113", 1, "الْفَلَق"),
    "An-Nas" to Triple("114", 1, "النَّاس"),
    "Al-Kahf" to Triple("18", 1, "الْكَهْف")
)