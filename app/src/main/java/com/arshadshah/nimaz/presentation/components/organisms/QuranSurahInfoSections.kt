package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.SurahAudioControlBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
internal fun BottomActions(
    isAudioActive: Boolean,
    isPlaying: Boolean,
    isDownloading: Boolean,
    isPreparing: Boolean,
    downloadProgress: Float,
    downloadedCount: Int,
    totalToDownload: Int,
    currentAyah: Int,
    totalAyahs: Int,
    surahProgress: Float,
    onPlayAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onStartReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Audio control bar when playing (full-width, dedicated)
        if (isAudioActive) {
            SurahAudioControlBar(
                isPlaying = isPlaying,
                isDownloading = isDownloading,
                isPreparing = isPreparing,
                downloadProgress = downloadProgress,
                downloadedCount = downloadedCount,
                totalToDownload = totalToDownload,
                currentAyah = currentAyah,
                totalAyahs = totalAyahs,
                surahProgress = surahProgress,
                onPlayPauseClick = { if (isPlaying) onPauseAudio() else onResumeAudio() },
                onStopClick = onStopAudio
            )
        }

        // Main action buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Listen — secondary action, only shows when audio is NOT active
                if (!isAudioActive) {
                    NimazButton(
                        text = stringResource(R.string.listen),
                        onClick = onPlayAudio,
                        modifier = Modifier.weight(1f),
                        variant = NimazButtonVariant.TONAL,
                        size = NimazButtonSize.LARGE,
                        leadingIcon = Icons.Default.PlayArrow
                    )
                }

                // Read — primary action
                NimazButton(
                    text = stringResource(R.string.quran_home_start_reading),
                    onClick = onStartReading,
                    modifier = Modifier.weight(1f),
                    variant = NimazButtonVariant.FILLED,
                    size = NimazButtonSize.LARGE,
                    leadingIcon = Icons.AutoMirrored.Filled.MenuBook
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomActionsPreview() {
    NimazTheme {
        BottomActions(
            isAudioActive = false,
            isPlaying = false,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            currentAyah = 0,
            totalAyahs = 7,
            surahProgress = 0f,
            onPlayAudio = {},
            onResumeAudio = {},
            onPauseAudio = {},
            onStopAudio = {},
            onStartReading = {}
        )
    }
}
