package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.R
import androidx.compose.ui.res.stringResource

/**
 * Combined reading-position + audio-control bar.
 *
 * The thin bar at the top reflects the reader's position in the current surah
 * (driven by scroll when idle, by the playing ayah when audio is active —
 * they stay in sync via the auto-scroll-to-playing-ayah effect on the screen).
 * Hitting play starts audio from the currently displayed ayah.
 */
@Composable
internal fun AudioBottomBar(
    isAudioActive: Boolean,
    isPlaying: Boolean,
    isDownloading: Boolean,
    isPreparing: Boolean,
    downloadProgress: Float,
    downloadedCount: Int,
    totalToDownload: Int,
    surahName: String,
    currentAyahInSurah: Int,
    totalAyahsInSurah: Int,
    pageNumber: Int,
    juzNumber: Int,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readingProgress = if (totalAyahsInSurah > 0) {
        (currentAyahInSurah.toFloat() / totalAyahsInSurah).coerceIn(0f, 1f)
    } else 0f

    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = {
                    if (isPreparing && totalToDownload > 0) downloadProgress else readingProgress
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (isPreparing) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(36.dp)) {
                    if (isDownloading || isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play from current ayah",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (isPreparing && totalToDownload > 0) {
                        Text(
                            text = "Downloading $downloadedCount / $totalToDownload…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = surahName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PositionChip(text = "Ayah $currentAyahInSurah / $totalAyahsInSurah")
                            PositionChip(text = "p. $pageNumber")
                            PositionChip(text = "Juz $juzNumber")
                        }
                    }
                }

                if (isAudioActive || isPreparing) {
                    IconButton(onClick = onStopClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_stop_audio),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Idle - mid surah")
@Composable
internal fun AudioBottomBarIdlePreview() {
    NimazTheme {
        AudioBottomBar(
            isAudioActive = false,
            isPlaying = false,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            surahName = "Al-Baqarah",
            currentAyahInSurah = 47,
            totalAyahsInSurah = 286,
            pageNumber = 8,
            juzNumber = 1,
            onPlayClick = {},
            onStopClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Playing")
@Composable
internal fun AudioBottomBarPlayingPreview() {
    NimazTheme {
        AudioBottomBar(
            isAudioActive = true,
            isPlaying = true,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            surahName = "Al-Fatihah",
            currentAyahInSurah = 4,
            totalAyahsInSurah = 7,
            pageNumber = 1,
            juzNumber = 1,
            onPlayClick = {},
            onStopClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Downloading")
@Composable
internal fun AudioBottomBarDownloadingPreview() {
    NimazTheme {
        AudioBottomBar(
            isAudioActive = true,
            isPlaying = false,
            isDownloading = true,
            isPreparing = true,
            downloadProgress = 0.65f,
            downloadedCount = 5,
            totalToDownload = 7,
            surahName = "Al-Baqarah",
            currentAyahInSurah = 1,
            totalAyahsInSurah = 286,
            pageNumber = 2,
            juzNumber = 1,
            onPlayClick = {},
            onStopClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "End of long surah")
@Composable
internal fun AudioBottomBarEndPreview() {
    NimazTheme {
        AudioBottomBar(
            isAudioActive = false,
            isPlaying = false,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            surahName = "Al-Baqarah",
            currentAyahInSurah = 286,
            totalAyahsInSurah = 286,
            pageNumber = 49,
            juzNumber = 3,
            onPlayClick = {},
            onStopClick = {}
        )
    }
}
