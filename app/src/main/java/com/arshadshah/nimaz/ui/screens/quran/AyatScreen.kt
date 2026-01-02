package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.constants.AppConstants.TAFSEER_SCREEN_ROUTE
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.components.quran.AyaItem
import com.arshadshah.nimaz.ui.components.quran.QuranBottomBar
import com.arshadshah.nimaz.ui.components.quran.SurahHeader
import com.arshadshah.nimaz.ui.components.quran.aya.components.QuranNavigationDialog
import com.arshadshah.nimaz.viewModel.AyatState
import com.arshadshah.nimaz.viewModel.AyatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AyatScreen(
    number: String,
    isSurah: String,
    language: String,
    scrollToAya: Int? = null,
    navController: NavHostController,
    viewModel: AyatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Load ayat data and khatam data
    LaunchedEffect(number, isSurah) {
        viewModel.handleEvent(
            AyatViewModel.AyatEvent.LoadAyat(
                number = number.toInt(),
                isSurah = isSurah.toBoolean()
            )
        )
    }

    // Scroll to specific aya if provided
    LaunchedEffect(scrollToAya) {
        scrollToAya?.let { target ->
            viewModel.handleEvent(AyatViewModel.AyatEvent.JumpToAya(target))
        }
    }

    // Load navigation data when surah is loaded
    LaunchedEffect(state.currentSurah) {
        if (state.currentSurah != null) {
            viewModel.handleEvent(AyatViewModel.AyatEvent.LoadNavigationData)
        }
    }

    // Debug: Log khatam state
    LaunchedEffect(state.activeKhatam) {
        Log.d("AyatScreen", "Active Khatam: ${state.activeKhatam?.name ?: "None"}")
        Log.d("AyatScreen", "Is Khatam Mode: ${state.isKhatamMode}")
    }

    AyatScreenContent(
        state = state,
        isSurah = isSurah.toBoolean(),
        onNavigateBack = { navController.popBackStack() },
        onNavigateToTafsir = { ayaNumber, surahNumber ->
            navController.navigate(
                TAFSEER_SCREEN_ROUTE
                    .replace("{surahNumber}", surahNumber.toString())
                    .replace("{ayaNumber}", ayaNumber.toString())
            )
        },
        onEvent = viewModel::handleEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyatScreenContent(
    state: AyatState,
    onNavigateBack: () -> Unit,
    isSurah: Boolean,
    onNavigateToTafsir: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    (if (isSurah) state.currentSurah?.englishName else state.currentJuz?.tname)?.let {
                        Text(text = it)
                    }
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ADD KHATAM PROGRESS BAR
                state.activeKhatam?.let { khatam ->
                    Log.d("AyatScreen", "Rendering Khatam Progress Bar for: ${khatam.name}")

                    val currentSurahNumber = state.currentSurah?.number ?: 1
                    val currentAyaNumber = getCurrentAyaNumber(state)

                    // Check if today's progress already covers current position
                    val todaySurah = state.khatamTodaySurah
                    val todayAya = state.khatamTodayAya
                    val alreadyLoggedToday = if (todaySurah != null && todayAya != null) {
                        when {
                            todaySurah > currentSurahNumber -> true
                            todaySurah < currentSurahNumber -> false
                            else -> todayAya >= currentAyaNumber
                        }
                    } else false

                    KhatamProgressBar(
                        khatam = khatam,
                        currentSurah = currentSurahNumber,
                        currentAya = currentAyaNumber,
                        isUpdating = state.isUpdatingKhatam,
                        alreadyLoggedToday = alreadyLoggedToday,
                        onMarkAsRead = { surah, aya ->
                            onEvent(AyatViewModel.AyatEvent.UpdateKhatamProgress(surah, aya))
                        }
                    )
                } ?: run {
                    Log.d("AyatScreen", "No active khatam - progress bar not shown")
                }

                // PAGE NAVIGATION CONTROLS (when in pagination mode)
                if (state.isPaginationMode && state.totalPages > 0) {
                    PageNavigationBar(
                        currentPage = state.currentPage,
                        totalPages = state.totalPages,
                        onPreviousPage = { onEvent(AyatViewModel.AyatEvent.PreviousPage) },
                        onNextPage = { onEvent(AyatViewModel.AyatEvent.NextPage) },
                        onJumpToPage = { page -> onEvent(AyatViewModel.AyatEvent.JumpToPage(page)) }
                    )
                }

                QuranBottomBar(
                    displaySettings = state.displaySettings,
                    onEvent = onEvent,
                    isPaginationMode = state.isPaginationMode,
                    onTogglePagination = { onEvent(AyatViewModel.AyatEvent.TogglePaginationMode) }
                )
            }
        },
        floatingActionButton = {
            // Only show navigation FAB for Surah view, not Juz
            if (isSurah) {
                FloatingActionButton(onClick = { onEvent(AyatViewModel.AyatEvent.ToggleNavigationPanel) }) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = "Quick Navigation")
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> PageLoading()
            state.error != null -> PageErrorState(message = state.error)
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content
                    AyatListContainer(
                        state = state,
                        contentPadding = padding,
                        onNavigateToTafsir = onNavigateToTafsir,
                        onEvent = onEvent,
                        onCurrentAyaChanged = { ayaIndex ->
                            onEvent(AyatViewModel.AyatEvent.UpdateCurrentAyaIndex(ayaIndex))
                        },
                    )

                    // Navigation dialog - only for Surah view
                    if (isSurah && state.showNavigationPanel) {
                        QuranNavigationDialog(
                            isVisible = state.showNavigationPanel,
                            currentSurah = state.currentSurah?.number ?: 1,
                            currentAya = getCurrentAyaNumber(state),
                            totalAyas = state.totalAyasInSurah,
                            onDismiss = { onEvent(AyatViewModel.AyatEvent.ToggleNavigationPanel) },
                            onJumpToAya = { onEvent(AyatViewModel.AyatEvent.JumpToAya(it)) },
                            onNextSurah = { onEvent(AyatViewModel.AyatEvent.NavigateToNextSurah) },
                            onPreviousSurah = { onEvent(AyatViewModel.AyatEvent.NavigateToPreviousSurah) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KhatamProgressBar(
    khatam: KhatamSession,
    currentSurah: Int,
    currentAya: Int,
    isUpdating: Boolean,
    alreadyLoggedToday: Boolean,
    onMarkAsRead: (Int, Int) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (khatam.isActive)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main progress row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (khatam.isActive) "📖" else "⏸️",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = khatam.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(khatam.totalAyasRead.toFloat() / 6236f * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Show last marked position
                        Text(
                            text = "• Last: ${khatam.currentSurah}:${khatam.currentAya}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )

                        if (alreadyLoggedToday) {
                            Text(
                                text = "• ✓ Today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!khatam.isActive) {
                        Text(
                            text = "⏸️ Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }

                    // Show current position if different from last marked
                    if (currentSurah != khatam.currentSurah || currentAya != khatam.currentAya) {
                        Text(
                            text = "Now: ${currentSurah}:${currentAya}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    } else {
                        // Primary action button
                        Button(
                            onClick = { onMarkAsRead(currentSurah, currentAya) },
                            modifier = Modifier.size(width = 110.dp, height = 32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            enabled = !alreadyLoggedToday,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (alreadyLoggedToday)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (alreadyLoggedToday) Icons.Default.Check else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (alreadyLoggedToday) "Marked" else "Mark",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Options toggle button
                        if (!alreadyLoggedToday) {
                            OutlinedButton(
                                onClick = { showOptions = !showOptions },
                                modifier = Modifier.size(width = 110.dp, height = 28.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showOptions) "Hide" else "Options",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Icon(
                                    if (showOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expandable options
            AnimatedVisibility(visible = showOptions && !isUpdating && !alreadyLoggedToday) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Mark reading from last position (${khatam.currentSurah}:${khatam.currentAya}) to:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Mark current aya
                            OutlinedButton(
                                onClick = {
                                    onMarkAsRead(currentSurah, currentAya)
                                    showOptions = false
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("This Aya", style = MaterialTheme.typography.labelSmall)
                                    Text("${currentSurah}:${currentAya}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Mark end of page (estimate ~15 ayas per page)
                            val pageEndAya = (currentAya + 15).coerceAtMost(getSurahAyaCount(currentSurah))
                            OutlinedButton(
                                onClick = {
                                    onMarkAsRead(currentSurah, pageEndAya)
                                    showOptions = false
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(6.dp),
                                enabled = pageEndAya > currentAya
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Page End", style = MaterialTheme.typography.labelSmall)
                                    Text("~${currentSurah}:${pageEndAya}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Mark end of surah
                            val surahEnd = getSurahAyaCount(currentSurah)
                            OutlinedButton(
                                onClick = {
                                    onMarkAsRead(currentSurah, surahEnd)
                                    showOptions = false
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(6.dp),
                                enabled = surahEnd > currentAya
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Surah End", style = MaterialTheme.typography.labelSmall)
                                    Text("${currentSurah}:${surahEnd}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to get surah aya count
private fun getSurahAyaCount(surahNumber: Int): Int {
    val surahAyaCounts = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,   // 1-10
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,    // 11-20
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,       // 21-30
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,         // 31-40
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,          // 41-50
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,          // 51-60
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,          // 61-70
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,          // 71-80
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,          // 81-90
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,               // 91-100
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,                   // 101-110
        5, 4, 5, 6                                        // 111-114
    )
    return if (surahNumber in 1..114) surahAyaCounts[surahNumber - 1] else 0
}

@Composable
fun PageNavigationBar(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onJumpToPage: (Int) -> Unit
) {
    var showPageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            OutlinedButton(
                onClick = onPreviousPage,
                enabled = currentPage > 1,
                modifier = Modifier.size(width = 90.dp, height = 36.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous Page",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Prev", style = MaterialTheme.typography.labelSmall)
            }

            // Page indicator (clickable to show jump dialog)
            Surface(
                onClick = { showPageDialog = true },
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Page",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "$currentPage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "/ $totalPages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Next button
            OutlinedButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages,
                modifier = Modifier.size(width = 90.dp, height = 36.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Next", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Page",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Page jump dialog
    if (showPageDialog) {
        PageJumpDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            onDismiss = { showPageDialog = false },
            onJumpToPage = { page ->
                onJumpToPage(page)
                showPageDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageJumpDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJumpToPage: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentPage.toString()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Jump to Page", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enter page number (1-$totalPages):",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    label = { Text("Page") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick jump buttons
                Text("Quick jump:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { pageInput = "1" },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("First", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { pageInput = (totalPages / 2).toString() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Middle", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { pageInput = totalPages.toString() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Last", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    pageInput.toIntOrNull()?.let { page ->
                        if (page in 1..totalPages) {
                            onJumpToPage(page)
                        }
                    }
                }
            ) {
                Text("Jump")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============ FIXED AYAT LIST CONTAINER WITH DEBOUNCING ============

@Composable
fun AyatListContainer(
    state: AyatState,
    contentPadding: PaddingValues,
    onNavigateToTafsir: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    onCurrentAyaChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // CRITICAL FIX: Track when we're in the middle of a programmatic jump
    var isJumping by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf(0L) }

    // FIXED: Debounced scroll tracking - ignores updates during jumps
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                // CRITICAL: Only update if we're not in the middle of a programmatic jump
                if (!isJumping && firstVisibleIndex >= 0 && firstVisibleIndex < state.ayatList.size) {
                    val currentTime = System.currentTimeMillis()
                    // Debounce: only update if at least 2 seconds have passed since last update
                    if (currentTime - lastUpdateTime < 2000) {
                        if (BuildConfig.DEBUG) {
                            Log.d("ScrollTracking", "Debounced scroll event")
                        }
                        return@collect
                    }

                    lastUpdateTime = currentTime
                    val currentAya = state.ayatList[firstVisibleIndex]

                    if (BuildConfig.DEBUG) {
                        Log.d(
                            "ScrollTracking",
                            "Organic scroll - Index: $firstVisibleIndex, Aya: ${currentAya.ayaNumberInSurah}"
                        )
                    }

                    // Update the current index
                    onCurrentAyaChanged(firstVisibleIndex)

                    // Only update reading progress for actual Quran ayas (not Bismillah)
                    if (currentAya.ayaNumberInSurah > 0) {
                        onEvent(AyatViewModel.AyatEvent.UpdateReadingProgress(currentAya.ayaNumberInSurah))
                    }
                } else if (isJumping) {
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            "ScrollTracking",
                            "Ignoring scroll event during jump - Index: $firstVisibleIndex"
                        )
                    }
                }
            }
    }

    // FIXED: Handle programmatic scrolling with proper debouncing
    LaunchedEffect(state.currentAyaIndex) {
        if (state.currentAyaIndex >= 0 &&
            state.currentAyaIndex < state.ayatList.size
        ) {

            val visibleRange = listState.layoutInfo.visibleItemsInfo
            val isCurrentItemVisible = visibleRange.any { it.index == state.currentAyaIndex }

            if (!isCurrentItemVisible) {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "ScrollTracking",
                        "Starting programmatic scroll to index: ${state.currentAyaIndex}"
                    )
                }

                // Set jumping flag to prevent interference
                isJumping = true

                try {
                    listState.animateScrollToItem(state.currentAyaIndex)

                    // CRITICAL: Wait for scroll animation to complete before re-enabling tracking
                    delay(800) // Give enough time for scroll animation

                    if (BuildConfig.DEBUG) {
                        Log.d(
                            "ScrollTracking",
                            "Programmatic scroll completed to index: ${state.currentAyaIndex}"
                        )
                    }
                } finally {
                    // Re-enable scroll tracking
                    isJumping = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = state.ayatList,
                key = { _, aya -> aya.ayaNumberInQuran }
            ) { _, aya ->

                if (isSpecialAya(aya)) {
                    onEvent(AyatViewModel.AyatEvent.GetSurahById(aya.suraNumber))
                    SurahHeader(surah = state.currentSurah!!)
                }
                AyaItem(
                    aya = aya,
                    displaySettings = state.displaySettings,
                    audioState = state.audioState,
                    onTafseerClick = onNavigateToTafsir,
                    onEvent = onEvent
                )
            }
        }
    }
}

// ============ HELPER FUNCTIONS ============

private fun isSpecialAya(aya: LocalAya): Boolean {
    return aya.ayaNumberInQuran == 0 ||
            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾" ||
            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ" ||
            (aya.suraNumber == 9 && aya.ayaNumberInSurah == 1)
}

// FIXED: Handle Bismillah correctly
private fun getCurrentAyaNumber(state: AyatState): Int {
    return if (state.ayatList.isNotEmpty() &&
        state.currentAyaIndex >= 0 &&
        state.currentAyaIndex < state.ayatList.size
    ) {

        val currentAya = state.ayatList[state.currentAyaIndex]

        // Handle Bismillah case - if we're on Bismillah (0), show as aya 1
        when (currentAya.ayaNumberInSurah) {
            0 -> 1 // Bismillah should be displayed as aya 1 in navigation
            else -> currentAya.ayaNumberInSurah
        }
    } else {
        1
    }
}
