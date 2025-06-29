package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import com.arshadshah.nimaz.constants.AppConstants.TAFSEER_SCREEN_ROUTE
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.components.quran.AyaItem
import com.arshadshah.nimaz.ui.components.quran.QuranBottomBar
import com.arshadshah.nimaz.ui.components.quran.SurahHeader
import com.arshadshah.nimaz.ui.components.quran.aya.components.FloatingNavigationPanel
import com.arshadshah.nimaz.ui.components.quran.aya.components.QuickJumpDialog
import com.arshadshah.nimaz.viewModel.AyatState
import com.arshadshah.nimaz.viewModel.AyatViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(number, isSurah) {
        viewModel.handleEvent(
            AyatViewModel.AyatEvent.LoadAyat(
                number = number.toInt(),
                isSurah = isSurah.toBoolean(),
                language = language
            )
        )
    }

    // Load navigation data when surah is loaded
    LaunchedEffect(state.currentSurah) {
        if (state.currentSurah != null) {
            viewModel.handleEvent(AyatViewModel.AyatEvent.LoadNavigationData)
        }
    }

    AyatScreenContent(
        state = state,
        scrollToAya = scrollToAya,
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
    scrollToAya: Int?,
    onNavigateBack: () -> Unit,
    isSurah: Boolean,
    onNavigateToTafsir: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit
) {
    var showQuickJumps by remember { mutableStateOf(false) }

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
            // ADD KHATAM PROGRESS BAR
            state.activeKhatam?.let { khatam ->
                KhatamProgressBar(
                    khatam = khatam,
                    currentSurah = state.currentSurah?.number ?: 1,
                    currentAya = getCurrentAyaNumber(state),
                    onMarkAsRead = { surah, aya ->
                        onEvent(AyatViewModel.AyatEvent.UpdateKhatamProgress(surah, aya))
                    }
                )
            }
            QuranBottomBar(
                displaySettings = state.displaySettings,
                onEvent = onEvent
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onEvent(AyatViewModel.AyatEvent.ToggleNavigationPanel)
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Quick Navigation"
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> PageLoading()
            state.error != null -> PageErrorState(message = state.error)
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // FIXED: Main content with debounced scroll tracking
                    AyatListContainer(
                        state = state,
                        contentPadding = padding,
                        onNavigateToTafsir = onNavigateToTafsir,
                        onEvent = onEvent,
                        onCurrentAyaChanged = { ayaIndex ->
                            onEvent(AyatViewModel.AyatEvent.UpdateCurrentAyaIndex(ayaIndex))
                        }
                    )

                    // Navigation overlays
                    FloatingNavigationPanel(
                        isVisible = state.showNavigationPanel,
                        currentSurah = state.currentSurah?.number ?: 1,
                        currentAya = getCurrentAyaNumber(state),
                        totalAyas = state.totalAyasInSurah,
                        onDismiss = {
                            onEvent(AyatViewModel.AyatEvent.ToggleNavigationPanel)
                        },
                        onJumpToAya = { ayaNumber ->
                            onEvent(AyatViewModel.AyatEvent.JumpToAya(ayaNumber))
                        },
                        onShowBookmarks = { },
                        onShowSearch = { },
                        onShowQuickJumps = { showQuickJumps = true }
                    )

                    QuickJumpDialog(
                        isVisible = showQuickJumps,
                        quickJumps = state.quickJumps,
                        onJump = { quickJump ->
                            onEvent(AyatViewModel.AyatEvent.JumpToAya(quickJump.ayaNumberInSurah))
                            showQuickJumps = false
                        },
                        onAddNew = { name ->
                            onEvent(AyatViewModel.AyatEvent.AddQuickJump(name))
                        },
                        onDelete = { quickJump ->
                            onEvent(AyatViewModel.AyatEvent.DeleteQuickJump(quickJump))
                        },
                        onDismiss = { showQuickJumps = false }
                    )
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
    onMarkAsRead: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📖 ${khatam.name}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(khatam.totalAyasRead.toFloat() / 6236f * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = {
                    onMarkAsRead(currentSurah, currentAya)
                },
                modifier = Modifier.size(width = 100.dp, height = 32.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "Mark as Read",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ============ FIXED AYAT LIST CONTAINER WITH DEBOUNCING ============

@Composable
fun AyatListContainer(
    state: AyatState,
    contentPadding: PaddingValues,
    onNavigateToTafsir: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    onCurrentAyaChanged: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // CRITICAL FIX: Track when we're in the middle of a programmatic jump
    var isJumping by remember { mutableStateOf(false) }
    var lastProgrammaticIndex by remember { mutableStateOf(-1) }

    // FIXED: Debounced scroll tracking - ignores updates during jumps
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                // CRITICAL: Only update if we're not in the middle of a programmatic jump
                if (!isJumping && firstVisibleIndex >= 0 && firstVisibleIndex < state.ayatList.size) {
                    val currentAya = state.ayatList[firstVisibleIndex]

                    Log.d("ScrollTracking", "Organic scroll - Index: $firstVisibleIndex, Aya: ${currentAya.ayaNumberInSurah}")

                    // Update the current index
                    onCurrentAyaChanged(firstVisibleIndex)

                    // Only update reading progress for actual Quran ayas (not Bismillah)
                    if (currentAya.ayaNumberInSurah > 0) {
                        onEvent(AyatViewModel.AyatEvent.UpdateReadingProgress(currentAya.ayaNumberInSurah))
                    }
                } else if (isJumping) {
                    Log.d("ScrollTracking", "Ignoring scroll event during jump - Index: $firstVisibleIndex")
                }
            }
    }

    // FIXED: Handle programmatic scrolling with proper debouncing
    LaunchedEffect(state.currentAyaIndex) {
        if (state.currentAyaIndex >= 0 &&
            state.currentAyaIndex < state.ayatList.size &&
            state.currentAyaIndex != lastProgrammaticIndex) {

            val visibleRange = listState.layoutInfo.visibleItemsInfo
            val isCurrentItemVisible = visibleRange.any { it.index == state.currentAyaIndex }

            if (!isCurrentItemVisible) {
                Log.d("ScrollTracking", "Starting programmatic scroll to index: ${state.currentAyaIndex}")

                // Set jumping flag to prevent interference
                isJumping = true
                lastProgrammaticIndex = state.currentAyaIndex

                try {
                    listState.animateScrollToItem(state.currentAyaIndex)

                    // CRITICAL: Wait for scroll animation to complete before re-enabling tracking
                    delay(800) // Give enough time for scroll animation

                    Log.d("ScrollTracking", "Programmatic scroll completed to index: ${state.currentAyaIndex}")
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
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = state.ayatList,
                key = { index, aya -> aya.ayaNumberInQuran }
            ) { index, aya ->
                Log.d("AyatScreen", "Rendering aya: ${aya.ayaNumberInQuran} at index $index")
                if (isSpecialAya(aya)) {
                    Log.d("AyatScreen", "Skipping special aya: ${aya.ayaNumberInQuran}")
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
        state.currentAyaIndex < state.ayatList.size) {

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