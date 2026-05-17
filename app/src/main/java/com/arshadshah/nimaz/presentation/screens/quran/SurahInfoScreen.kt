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

    // The audio control bar should only reflect playback that belongs to THIS
    // surah. If a different surah is playing/paused in the background, this
    // screen should still show the Listen button so the user can start playback
    // for the surah they are looking at.
    val isAudioForThisSurah = audioState.isActive && audioState.currentSurahNumber == surahNumber

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomActions(
                isAudioActive = isAudioForThisSurah,
                isPlaying = isAudioForThisSurah && audioState.isPlaying,
                isDownloading = isAudioForThisSurah && audioState.isDownloading,
                isPreparing = isAudioForThisSurah && audioState.isPreparing,
                downloadProgress = if (isAudioForThisSurah) audioState.downloadProgress else 0f,
                downloadedCount = if (isAudioForThisSurah) audioState.downloadedCount else 0,
                totalToDownload = if (isAudioForThisSurah) audioState.totalToDownload else 0,
                currentAyah = if (isAudioForThisSurah) audioState.currentAyahIndex + 1 else 0,
                totalAyahs = if (isAudioForThisSurah) audioState.totalAyahs else 0,
                surahProgress = if (isAudioForThisSurah) audioState.surahProgress else 0f,
                onPlayAudio = {
                    // Always (re)start playback for this surah. If a different
                    // surah is currently playing, playSurahFromInfo replaces it.
                    viewModel.onEvent(QuranEvent.PlaySurahFromInfo(surahNumber))
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
