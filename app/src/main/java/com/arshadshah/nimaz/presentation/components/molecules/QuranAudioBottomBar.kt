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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazPalette
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
 * Four rows, in the order a reader needs them: a full-bleed violet **download strip** (only
 * while fetching, so a tap on play with nothing cached visibly does something), **now playing**
 * with the transport — previous, play/pause, next, recitation settings — then the **seek rail**
 * between elapsed and remaining, and the **meta** line: reciter and speed on the left, the
 * repeat mode in the accent on the right.
 *
 * It draws nothing at all until audio is active or preparing. A player is a thing you summoned;
 * a permanent strip with a play button at the foot of the reader is furniture.
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
    // Nothing to show until there is something to control. The bar used to sit at the foot of
    // the reader permanently, offering a play button for audio nobody had asked for and eating
    // the bottom of every page — recitation starts from the ayah sheet's "Play here" or from
    // surah info's "Listen", and the player appears when it does.
    if (!isAudioActive && !isPreparing) return

    val isBusy = isDownloading || isPreparing

    NimazCard(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        style = NimazCardStyle.ELEVATED,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // The download strip is full-bleed across the top of the card, tinted violet — a
            // colour used nowhere else in the reader, so "audio is being fetched" is never
            // confused with progress through the surah (accent) or with a warning (amber).
            if (isPreparing && totalToDownload > 0) {
                DownloadStrip(
                    downloadedCount = downloadedCount,
                    totalToDownload = totalToDownload,
                    progress = downloadProgress,
                )
            }

            // Now playing on the left, transport on the right — the shape of a compact player,
            // and the shape of the prototype.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    Text(
                        text = listOfNotNull(
                            stringResource(
                                R.string.audio_position_ayah_format,
                                currentAyahInSurah,
                                totalAyahsInSurah
                            ),
                            stringResource(R.string.audio_position_juz_format, juzNumber),
                            stringResource(R.string.audio_position_page_format, pageNumber),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(onClick = onPreviousAyah, modifier = Modifier.size(38.dp)) {
                        NimazIcon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous_ayah_audio),
                            variant = NimazIconVariant.MUTED,
                            iconSize = 20.dp,
                        )
                    }
                    // The one big control. Filled and 46dp, because it is the button a reader
                    // hits without looking away from the page.
                    FilledIconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            NimazIcon(
                                imageVector = if (isPlaying) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (isPlaying) R.string.cd_pause else R.string.cd_play
                                ),
                                variant = NimazIconVariant.ON_ACCENT,
                                iconSize = 24.dp
                            )
                        }
                    }
                    IconButton(onClick = onNextAyah, modifier = Modifier.size(38.dp)) {
                        NimazIcon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.cd_next_ayah_audio),
                            variant = NimazIconVariant.MUTED,
                            iconSize = 20.dp,
                        )
                    }
                    // Tinted while a repeat mode is on, so a loop the reader set two sheets ago
                    // is visible without opening anything. Stop lives inside that sheet: a
                    // one-tap end-playback button beside next/previous is a mis-tap waiting to
                    // happen, and the bar disappearing is itself the confirmation.
                    IconButton(onClick = onExpand, modifier = Modifier.size(38.dp)) {
                        NimazIcon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.recitation_settings),
                            variant = if (repeatLabel != null) NimazIconVariant.PRIMARY
                            else NimazIconVariant.MUTED,
                            iconSize = 20.dp,
                        )
                    }
                }
            }

            // Elapsed · rail · remaining, on one line. The rail is the whole width between the
            // two clocks, which is what makes it draggable at all.
            if (durationMs > 0 && !isPreparing) {
                SeekRow(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = onSeek,
                )
            }

            MetaRow(
                reciterName = reciterName,
                speedLabel = speedLabel,
                repeatLabel = repeatLabel,
            )
        }
    }
}

/**
 * "Downloading 5 of 110", with its own progress rail, across the top of the card.
 *
 * Violet rather than the accent: this is the app fetching files, not the reader's progress
 * through the recitation, and the two rails would otherwise be the same colour a few
 * millimetres apart.
 */
@Composable
private fun DownloadStrip(downloadedCount: Int, totalToDownload: Int, progress: Float) {
    val violet = NimazPalette.Violet500
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(violet.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        NimazIcon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            tint = violet,
            iconSize = 14.dp,
        )
        Text(
            text = stringResource(R.string.audio_downloading, downloadedCount, totalToDownload),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = violet,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Drawn here rather than through NimazProgressTrack: the tone ramp has no violet, and
        // adding one for a single strip would put a colour in the shared vocabulary that
        // nothing else means.
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(violet.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(violet)
            )
        }
    }
}

/**
 * Elapsed · a draggable rail · remaining, on one row.
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ClockText(text = formatClock((fraction * durationMs).toLong()))
        Slider(
            value = fraction,
            onValueChange = { scrubbing = it },
            onValueChangeFinished = {
                scrubbing?.let { onSeek((it * durationMs).toLong()) }
                scrubbing = null
            },
            modifier = Modifier
                .weight(1f)
                .height(22.dp),
        )
        ClockText(
            text = "-" + formatClock(durationMs - (fraction * durationMs).toLong()),
            alignEnd = true,
        )
    }
}

/** Elapsed / remaining. Fixed width so the rail does not jog as the digits change. */
@Composable
private fun ClockText(text: String, alignEnd: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        maxLines = 1,
        modifier = Modifier.width(34.dp),
    )
}

/**
 * Who is reciting on the left, and — pushed to the right in the accent — the repeat mode.
 *
 * The repeat label earns the accent and the far edge because it is the one line here that
 * changes what happens next; the reciter and the speed only say what is already happening.
 */
@Composable
private fun MetaRow(reciterName: String, speedLabel: String?, repeatLabel: String?) {
    val left = listOfNotNull(
        reciterName.takeIf { it.isNotBlank() },
        speedLabel,
    )
    if (left.isEmpty() && repeatLabel == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = left.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.weight(1f))
        repeatLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
        )
    }
}
