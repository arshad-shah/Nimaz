package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.QuickJump
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
    quickJumps: State<List<QuickJump>>,

    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    handleEvents: (QuranViewModel.AyaEvent) -> Unit,

    // ADD THESE NEW CALLBACKS:
    onDeleteQuickJump: (QuickJump) -> Unit,
    onDeleteReadingProgress: (ReadingProgress) -> Unit,
    onClearAllProgress: () -> Unit,
    khatamState: State<QuranViewModel.KhatamState>,
    onKhatamEvent: (QuranViewModel.AyaEvent) -> Unit,

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

    LaunchedEffect(Unit) {
        handleEvents(QuranViewModel.AyaEvent.getBookmarks)
        handleEvents(QuranViewModel.AyaEvent.getFavorites)
        handleEvents(QuranViewModel.AyaEvent.getNotes)
    }

    // UPDATED: Add new sections for reading progress and quick jumps
    val sections = listOf(
        SectionData("Continue Reading", emptyList(), DeleteType.READING_PROGRESS),
        SectionData("Quick Jumps", emptyList(), DeleteType.QUICK_JUMP),
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

        // QUICK JUMPS SECTION
        item {
            QuickJumpsSection(
                quickJumps = quickJumps.value,
                suraList = suraList.value,
                onNavigateToAyatScreen = onNavigateToAyatScreen,
                onDeleteQuickJump = onDeleteQuickJump,
                translation = translation
            )
        }

        // EXISTING SECTIONS (Bookmarks, Favorites, Notes)
        items(sections.drop(2)) { section ->
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

    if (khatamState.value.showKhatamDialog) {
        StartKhatamDialog(
            onDismiss = { onKhatamEvent(QuranViewModel.AyaEvent.ShowKhatamDialog(false)) },
            onStartKhatam = { name, targetDate, dailyTarget ->
                onKhatamEvent(
                    QuranViewModel.AyaEvent.StartNewKhatam(name, targetDate, dailyTarget)
                )
            }
        )
    }
}

@Composable
fun KhatamSection(
    khatamState: QuranViewModel.KhatamState,
    onEvent: (QuranViewModel.AyaEvent) -> Unit,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    translation: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quran Khatam",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                    }
                )
            } ?: run {
                // No active khatam - show start button
                StartNewKhatamCard(
                    onStartNew = {
                        onEvent(QuranViewModel.AyaEvent.ShowKhatamDialog(true))
                    }
                )
            }

            // Show completed khatams if any
            if (khatamState.khatamHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Completed Khatams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                khatamState.khatamHistory.take(3).forEach { khatam ->
                    CompletedKhatamItem(khatam = khatam)
                }
            }
        }
    }
}

@Composable
fun ActiveKhatamCard(
    khatam: KhatamSession,
    todayProgress: Int,
    onContinueReading: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val progress = khatam.totalAyasRead.toFloat() / 6236f

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = khatam.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Badge(
                    containerColor = if (khatam.isActive) Color.Green else Color.Yellow
                ) {
                    Text(
                        text = if (khatam.isActive) "Active" else "Paused",
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Surah ${khatam.currentSurah}, Aya ${khatam.currentAya}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            khatam.dailyTarget?.let { target ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Today: $todayProgress / $target ayas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (todayProgress >= target) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (progress >= 0.99f) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Complete Khatam")
                    }
                } else {
                    if (khatam.isActive) {
                        OutlinedButton(
                            onClick = onPause,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pause")
                        }
                    } else {
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resume")
                        }
                    }

                    Button(
                        onClick = onContinueReading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue Reading")
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartNew() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start New Khatam",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Begin tracking your complete Quran reading journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompletedKhatamItem(
    khatam: KhatamSession
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color.Green,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = khatam.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = khatam.completionDate ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StartKhatamDialog(
    onDismiss: () -> Unit,
    onStartKhatam: (String, String?, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hasTargetDate by remember { mutableStateOf(false) }
    var targetDate by remember { mutableStateOf("") }
    var hasDailyTarget by remember { mutableStateOf(false) }
    var dailyTarget by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Khatam") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Khatam Name") },
                    placeholder = { Text("e.g., Ramadan Khatam 2025") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasTargetDate,
                        onCheckedChange = { hasTargetDate = it }
                    )
                    Text("Set target completion date")
                }

                if (hasTargetDate) {
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("Target Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasDailyTarget,
                        onCheckedChange = { hasDailyTarget = it }
                    )
                    Text("Set daily aya target")
                }

                if (hasDailyTarget) {
                    OutlinedTextField(
                        value = dailyTarget,
                        onValueChange = { dailyTarget = it },
                        label = { Text("Daily Target") },
                        placeholder = { Text("e.g., 20 ayas per day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onStartKhatam(
                            name,
                            if (hasTargetDate && targetDate.isNotBlank()) targetDate else null,
                            if (hasDailyTarget && dailyTarget.isNotBlank()) dailyTarget.toIntOrNull() else null
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Start Khatam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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

@Composable
private fun QuickJumpsSection(
    quickJumps: List<QuickJump>,
    suraList: List<LocalSurah>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    onDeleteQuickJump: (QuickJump) -> Unit,
    translation: String
) {
    FeaturesDropDown(
        modifier = Modifier.padding(4.dp),
        label = "Quick Jumps",
        items = quickJumps,
        showBadge = true,
        dropDownItem = { quickJump ->
            QuickJumpCard(
                quickJump = quickJump,
                surah = suraList.find { it.number == quickJump.surahNumber },
                onJumpTo = {
                    onNavigateToAyatScreen(
                        quickJump.surahNumber.toString(),
                        true,
                        translation,
                        quickJump.ayaNumberInSurah
                    )
                },
                onDelete = { onDeleteQuickJump(quickJump) }
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
                progress = progress.completionPercentage / 100f,
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
private fun QuickJumpCard(
    quickJump: QuickJump,
    surah: LocalSurah?,
    onJumpTo: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(quickJump.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quickJump.name.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quickJump.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${surah?.englishNameTranslation ?: "Surah ${quickJump.surahNumber}"}, Aya ${quickJump.ayaNumberInSurah}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Actions
            Row {
                IconButton(onClick = onJumpTo) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "Jump to position",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    BOOKMARK, FAVORITE, NOTE, READING_PROGRESS, QUICK_JUMP
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