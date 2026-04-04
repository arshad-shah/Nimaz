package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.InfoCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.HeroHeader
import com.arshadshah.nimaz.presentation.components.organisms.BottomActions
import com.arshadshah.nimaz.presentation.components.organisms.DetailGrid
import com.arshadshah.nimaz.presentation.components.organisms.ThemesList
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel

@Composable
fun SurahInfoScreen(
    surahNumber: Int,
    onNavigateBack: () -> Unit,
    onStartReading: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val homeState by viewModel.homeState.collectAsState()
    val surahInfo by viewModel.surahInfo.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val surah = homeState.surahs.find { it.number == surahNumber }

    LaunchedEffect(surahNumber) {
        viewModel.onEvent(QuranEvent.LoadSurahInfo(surahNumber))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomActions(
                isAudioActive = audioState.isActive,
                isPlaying = audioState.isPlaying,
                isDownloading = audioState.isDownloading,
                isPreparing = audioState.isPreparing,
                downloadProgress = audioState.downloadProgress,
                downloadedCount = audioState.downloadedCount,
                totalToDownload = audioState.totalToDownload,
                currentAyah = audioState.currentAyahIndex + 1,
                totalAyahs = audioState.totalAyahs,
                surahProgress = audioState.surahProgress,
                onPlayAudio = {
                    // Only start new playback if not already active
                    if (!audioState.isActive) {
                        viewModel.onEvent(QuranEvent.PlaySurahFromInfo(surahNumber))
                    }
                },
                onResumeAudio = { viewModel.onEvent(QuranEvent.ResumeAudio) },
                onPauseAudio = { viewModel.onEvent(QuranEvent.PauseAudio) },
                onStopAudio = { viewModel.onEvent(QuranEvent.StopAudio) },
                onStartReading = onStartReading
            )
        }
    ) { paddingValues ->
        if (surah != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Header with gradient
                HeroHeader(
                    surah = surah,
                    onNavigateBack = onNavigateBack
                )

                // Main Content
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // About This Surah
                    NimazSectionTitle(text = "About This Surah", uppercase = false)
                    InfoCard(
                        text = surahInfo?.description
                            ?: "This surah contains divine guidance and wisdom for believers."
                    )

                    // Details Grid
                    NimazSectionTitle(text = "Details", uppercase = false)
                    DetailGrid(surah = surah)

                    // Main Themes
                    NimazSectionTitle(text = "Main Themes", uppercase = false)
                    ThemesList(
                        themes = surahInfo?.themes
                            ?: listOf("Divine Guidance", "Worship", "Morality", "Remembrance")
                    )
                }
            }
        } else {
            // Loading or surah not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
