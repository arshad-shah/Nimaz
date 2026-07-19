package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Floating "now playing" mini-player for the Quran reader.
 *
 * Presentation only — same inputs and the same [onPlayClick] / [onStopClick]
 * contract as before. The leading control is the play/pause button itself
 * (no album/art tile); the surah + position sit in the middle over a slim
 * progress track that reflects reading position (or download progress while
 * preparing); stop trails. A small equalizer animates while playing.
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
    val isBusy = isDownloading || isPreparing
    val shownProgress =
        if (isPreparing && totalToDownload > 0) downloadProgress else readingProgress

    NimazCard(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        style = NimazCardStyle.ELEVATED,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leading: the play/pause control itself.
            FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(48.dp)) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    NimazIcon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                        variant = NimazIconVariant.ON_ACCENT,
                        iconSize = 26.dp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                if (isPreparing && totalToDownload > 0) {
                    Text(
                        text = stringResource(
                            R.string.audio_downloading_short_format,
                            downloadedCount,
                            totalToDownload
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = surahName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPlaying) {
                            PlayingEqualizer(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NimazBadge(
                            text = stringResource(
                                R.string.audio_position_ayah_format,
                                currentAyahInSurah,
                                totalAyahsInSurah
                            ),
                            tone = NimazTone.ACCENT,
                            emphasis = NimazBadgeEmphasis.FILLED,
                            size = NimazBadgeSize.SMALL
                        )
                        NimazBadge(
                            text = stringResource(R.string.audio_position_juz_format, juzNumber),
                            tone = NimazTone.ACCENT,
                            emphasis = NimazBadgeEmphasis.OUTLINED,
                            size = NimazBadgeSize.SMALL
                        )
                        NimazBadge(
                            text = stringResource(R.string.audio_position_page_format, pageNumber),
                            tone = NimazTone.ACCENT,
                            emphasis = NimazBadgeEmphasis.OUTLINED,
                            size = NimazBadgeSize.SMALL
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { shownProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isPreparing) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (isAudioActive || isPreparing) {
                IconButton(onClick = onStopClick, modifier = Modifier.size(40.dp)) {
                    NimazIcon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_stop_audio),
                        variant = NimazIconVariant.MUTED,
                        size = NimazIconSize.MEDIUM
                    )
                }
            }
        }
    }
}

/** Three bars that bob while audio plays — a lightweight "now playing" cue. */
@Composable
private fun PlayingEqualizer(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val durations = listOf(420, 560, 480, 420, 560, 480, 420, 560, 480)
    Row(
        modifier = modifier.height(14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        durations.forEach { duration ->
            val fraction by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
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
