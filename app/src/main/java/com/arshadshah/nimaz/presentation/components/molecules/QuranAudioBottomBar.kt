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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressTrack
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The Qur'an reader's recitation player.
 *
 * It was a collapsed "now playing" strip: a play button, a surah name, a progress bar you could
 * look at but not touch. The manager has been able to seek across the whole surah since the
 * playlist work — `seekToTotal` — and has published position, duration and download counts all
 * along; the bar simply never offered any of it. Everything added here is state the manager
 * already keeps, except repeat and speed, which are dispatched as events like everything else.
 *
 * Three rows, in the order a reader needs them: **downloads** (only while fetching, so a tap on
 * play with nothing cached visibly does something), **now playing** with prev / play-pause /
 * next / expand, and the **seek rail** with elapsed and remaining.
 *
 * @param positionMs / [durationMs] whole-surah coordinates, the same ones [onSeek] speaks.
 * @param onExpand opens the recitation sheet — reciter, repeat, speed, follow-along.
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
    modifier: Modifier = Modifier,
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    reciterName: String = "",
    speedLabel: String? = null,
    repeatLabel: String? = null,
    onSeek: (Long) -> Unit = {},
    onNextAyah: () -> Unit = {},
    onPreviousAyah: () -> Unit = {},
    onExpand: () -> Unit = {},
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
                if (durationMs > 0 && !isPreparing) {
                    SeekRow(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeek = onSeek,
                    )
                } else {
                    // Nothing to scrub yet — a rail that cannot move is worse than a bar that
                    // says it is loading.
                    NimazProgressTrack(
                        progress = shownProgress,
                        tone = if (isPreparing) NimazTone.WARNING else NimazTone.ACCENT,
                        size = NimazProgressSize.THIN,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MetaRow(
                    reciterName = reciterName,
                    speedLabel = speedLabel,
                    repeatLabel = repeatLabel,
                )
            }

            // Prev / next step the playlist by one verse — the control a reader reaches for
            // when the reciter is one verse past where they were following.
            if (isAudioActive) {
                IconButton(onClick = onPreviousAyah, modifier = Modifier.size(36.dp)) {
                    NimazIcon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous_ayah_audio),
                        variant = NimazIconVariant.MUTED,
                        size = NimazIconSize.MEDIUM
                    )
                }
                IconButton(onClick = onNextAyah, modifier = Modifier.size(36.dp)) {
                    NimazIcon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_ayah_audio),
                        variant = NimazIconVariant.MUTED,
                        size = NimazIconSize.MEDIUM
                    )
                }
                IconButton(onClick = onExpand, modifier = Modifier.size(36.dp)) {
                    NimazIcon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.recitation_settings),
                        variant = NimazIconVariant.MUTED,
                        size = NimazIconSize.MEDIUM
                    )
                }
            }

            if (isAudioActive || isPreparing) {
                IconButton(onClick = onStopClick, modifier = Modifier.size(36.dp)) {
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

/**
 * Elapsed, a draggable rail, remaining.
 *
 * The rail speaks whole-surah milliseconds, the same coordinates `seekToTotal` takes, so a
 * reader dragging to the middle of a 286-verse surah lands in the middle of the *recitation*
 * rather than the middle of whichever file happens to be playing.
 */
@Composable
private fun SeekRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    // While a finger is down the rail follows the finger, not the player: publishing position
    // every 400 ms would otherwise yank the thumb back under it between drags.
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val fraction = scrubbing ?: (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    Column {
        Slider(
            value = fraction,
            onValueChange = { scrubbing = it },
            onValueChangeFinished = {
                scrubbing?.let { onSeek((it * durationMs).toLong()) }
                scrubbing = null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatClock((fraction * durationMs).toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatClock(durationMs - (fraction * durationMs).toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Reciter, and the two settings that are only worth naming when they are not the default. */
@Composable
private fun MetaRow(reciterName: String, speedLabel: String?, repeatLabel: String?) {
    val parts = listOfNotNull(
        reciterName.takeIf { it.isNotBlank() },
        speedLabel,
        repeatLabel,
    )
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp),
    )
}

/** `m:ss`, or `h:mm:ss` for the long surahs. Not a duration a locale formats differently. */
internal fun formatClock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val seconds = total % 60
    val minutes = (total / 60) % 60
    val hours = total / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
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
