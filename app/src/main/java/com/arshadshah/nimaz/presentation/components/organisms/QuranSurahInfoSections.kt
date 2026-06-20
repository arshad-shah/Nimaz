package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
internal fun DetailGrid(
    surah: Surah,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailCard(
                icon = Icons.Default.LocationOn,
                label = "Revelation",
                value = if (surah.revelationType == RevelationType.MECCAN) "Makkah" else "Madinah",
                modifier = Modifier.weight(1f)
            )
            DetailCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Juz",
                value = surah.juzStart.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailCard(
                icon = Icons.Default.FormatListNumbered,
                label = "Order",
                value = "${surah.orderInMushaf} in Mushaf",
                modifier = Modifier.weight(1f)
            )
            DetailCard(
                icon = Icons.Default.Book,
                label = "Verses",
                value = "${surah.numberOfAyahs} ayahs",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetailCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(15.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF737373)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemesList(
    themes: List<String>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { theme ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = theme,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
                // Listen button - only shows when audio is NOT active
                if (!isAudioActive) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onPlayAudio)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Listen",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Read button (primary action)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF14B8A6), Color(0xFF0F766E))
                            )
                        )
                        .clickable(onClick = onStartReading)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Reading",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahAudioControlBar(
    isPlaying: Boolean,
    isDownloading: Boolean,
    isPreparing: Boolean,
    downloadProgress: Float,
    downloadedCount: Int,
    totalToDownload: Int,
    currentAyah: Int,
    totalAyahs: Int,
    surahProgress: Float,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        // Show download progress bar when preparing/downloading
        if (isPreparing && totalToDownload > 0) {
            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            // Surah progress bar
            LinearProgressIndicator(
                progress = { surahProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio info with surah progress or download status
            Column(modifier = Modifier.weight(1f)) {
                if (isPreparing && totalToDownload > 0) {
                    // Show download status
                    Text(
                        text = "Preparing Audio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Downloading $downloadedCount of $totalToDownload ayahs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${(downloadProgress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                } else {
                    // Show playback status
                    Text(
                        text = if (isPlaying) "Now Playing" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Ayah $currentAyah of $totalAyahs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${(surahProgress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Play/Pause button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading || isPreparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Resume",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stop button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                    .clickable(onClick = onStopClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_stop),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private val sampleSurah = Surah(
    number = 1,
    nameArabic = "\u0627\u0644\u0641\u0627\u062A\u062D\u0629",
    nameEnglish = "Al-Fatihah",
    nameTransliteration = "The Opening",
    revelationType = RevelationType.MECCAN,
    ayahCount = 7,
    juzStart = 1,
    orderInMushaf = 5,
    startPage = 1
)

@Preview(showBackground = true)
@Composable
private fun DetailGridPreview() {
    NimazTheme {
        DetailGrid(surah = sampleSurah)
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemesListPreview() {
    NimazTheme {
        ThemesList(
            themes = listOf("Praise", "Guidance", "Mercy", "Worship", "Supplication")
        )
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
