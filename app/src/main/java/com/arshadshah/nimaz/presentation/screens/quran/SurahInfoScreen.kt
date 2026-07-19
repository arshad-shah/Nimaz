package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.InfoCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.DetailCard
import com.arshadshah.nimaz.presentation.components.molecules.SurahHeaderCartouche
import com.arshadshah.nimaz.presentation.components.organisms.BottomActions
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.ThemesList
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_home_surah_info),
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            )
        },
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
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Surah identity — the same cartouche the reader and list use
                SurahHeaderCartouche(
                    surah = surah,
                    showBismillah = false
                )

                // Stats row: Verses / Juz / Page
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCard(
                        icon = Icons.Default.FormatListNumbered,
                        label = stringResource(R.string.quran_verses_label),
                        value = surah.ayahCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    DetailCard(
                        icon = Icons.Default.Layers,
                        label = stringResource(R.string.quran_juz_label),
                        value = surah.juzStart.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    DetailCard(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = stringResource(R.string.quran_page_label),
                        value = surah.startPage.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // About This Surah
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NimazSectionTitle(text = stringResource(R.string.surah_info_about), uppercase = false)
                    InfoCard(
                        text = surahInfo?.description
                            ?: stringResource(R.string.surah_info_description_fallback)
                    )
                }

                // Main Themes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NimazSectionTitle(text = stringResource(R.string.surah_info_main_themes), uppercase = false)
                    ThemesList(
                        themes = surahInfo?.themes
                            ?: listOf(
                                stringResource(R.string.surah_info_theme_guidance),
                                stringResource(R.string.surah_info_theme_worship),
                                stringResource(R.string.surah_info_theme_morality),
                                stringResource(R.string.surah_info_theme_remembrance)
                            )
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
