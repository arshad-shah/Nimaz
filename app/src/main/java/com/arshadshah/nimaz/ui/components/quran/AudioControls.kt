package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.viewModel.AudioState
import com.arshadshah.nimaz.viewModel.AyatViewModel

@Composable
fun AudioControls(
    aya: LocalAya,
    audioState: AudioState,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val isCurrentlyPlayingAya =
        audioState.currentPlayingAya?.ayaNumberInQuran == aya.ayaNumberInQuran
    val hasAudioFile = aya.audioFileLocation.isNotEmpty()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(48.dp)
    ) {
        // Download Button (shown when no audio file is available)
        if (!hasAudioFile) {
            IconButton(
                onClick = { onEvent(AyatViewModel.AyatEvent.DownloadAudio(aya)) },
                enabled = !loading && !audioState.isDownloading
            ) {
                if (audioState.isDownloading && audioState.currentPlayingAya?.ayaNumberInQuran == aya.ayaNumberInQuran) {
                    CircularProgressIndicator(
                        progress = { audioState.downloadProgress },
                        modifier = Modifier.size(24.dp),
                        trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download audio",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            return@AudioControls
        }

        // Play/Pause Button
        IconButton(
            onClick = {
                if (isCurrentlyPlayingAya && audioState.isPlaying) {
                    onEvent(AyatViewModel.AyatEvent.PauseAudio)
                } else {
                    onEvent(AyatViewModel.AyatEvent.PlayAudio(aya))
                }
            },
            enabled = !loading && !audioState.isDownloading
        ) {
            Icon(
                imageVector = if (isCurrentlyPlayingAya && audioState.isPlaying)
                    Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isCurrentlyPlayingAya && audioState.isPlaying)
                    "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Stop Button (only shown when audio is playing or paused)
        if (isCurrentlyPlayingAya && (audioState.isPlaying || audioState.isPaused)) {
            IconButton(
                onClick = { onEvent(AyatViewModel.AyatEvent.StopAudio) },
                enabled = !loading
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Download Progress Indicator
        if (audioState.isDownloading && audioState.currentPlayingAya?.ayaNumberInQuran == aya.ayaNumberInQuran) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                CircularProgressIndicator(
                    progress = { audioState.downloadProgress },
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                )
            }
        }
    }
}