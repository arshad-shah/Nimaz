package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/** One figure in [SurahMetaStrip]: what it is, and what it says. */
internal data class SurahMetaStat(
    val icon: ImageVector,
    val label: String,
    val value: String,
)

/**
 * Where a surah sits in the Mushaf — its verse count, its juz and its opening page — as one
 * quiet strip under the cartouche.
 *
 * These were three elevated cards, which gave three small numbers the same visual weight as
 * the surah's name and the summary paragraph below it. They are reference figures you glance
 * at, not the point of the screen, so they now read as a single line: value over label,
 * divided rather than boxed, sharing one surface.
 */
@Composable
internal fun SurahMetaStrip(
    stats: List<SurahMetaStat>,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        tone = NimazTone.NEUTRAL,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEachIndexed { index, stat ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NimazIcon(
                            imageVector = stat.icon,
                            contentDescription = null,
                            variant = NimazIconVariant.MUTED,
                            iconSize = 14.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // A word, not a figure. "286" and "Madinah" cannot share a type size
                        // in a quarter-width column — at titleMedium the word wrapped to
                        // "Madin / ah" and shoved its own label out of line. Numbers keep the
                        // display size; anything longer steps down and stays on one line.
                        Text(
                            text = stat.value,
                            style = if (stat.value.length <= 4) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.labelLarge
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < stats.lastIndex) {
                    // A hairline rather than a gap: it separates the figures without
                    // making three boxes out of them again.
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SurahAudioControlBar(
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
                        text = stringResource(R.string.audio_preparing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(
                            R.string.audio_downloading_format,
                            downloadedCount,
                            totalToDownload
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.audio_percent_complete_format,
                            (downloadProgress * 100).toInt()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                } else {
                    // Show playback status
                    Text(
                        text = if (isPlaying) stringResource(R.string.audio_now_playing)
                        else stringResource(R.string.audio_paused),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(
                            R.string.audio_ayah_progress_format,
                            currentAyah,
                            totalAyahs
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.audio_percent_complete_format,
                            (surahProgress * 100).toInt()
                        ),
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
                    NimazIcon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Resume",
                        variant = NimazIconVariant.ON_ACCENT,
                        iconSize = 28.dp
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
                NimazIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_stop),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = NimazIconSize.MEDIUM
                )
            }
        }
    }
}

private val metaStripSample
    @Composable get() = listOf(
        SurahMetaStat(
            icon = Icons.Default.FormatListNumbered,
            label = stringResource(R.string.quran_verses_label),
            value = "286"
        ),
        SurahMetaStat(
            icon = Icons.Default.Layers,
            label = stringResource(R.string.quran_juz_label),
            value = "1"
        ),
        SurahMetaStat(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = stringResource(R.string.quran_page_label),
            value = "2"
        ),
    )

@Preview(showBackground = true, widthDp = 400, name = "Surah meta strip")
@Composable
private fun SurahMetaStripPreview() {
    NimazTheme {
        SurahMetaStrip(stats = metaStripSample, modifier = Modifier.padding(20.dp))
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    name = "Surah meta strip (dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun SurahMetaStripDarkPreview() {
    NimazTheme {
        SurahMetaStrip(stats = metaStripSample, modifier = Modifier.padding(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SurahAudioControlBarPreview() {
    NimazTheme {
        SurahAudioControlBar(
            isPlaying = true,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            currentAyah = 3,
            totalAyahs = 7,
            surahProgress = 0.43f,
            onPlayPauseClick = {},
            onStopClick = {}
        )
    }
}
