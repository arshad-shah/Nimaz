package com.arshadshah.nimaz.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.arshadshah.nimaz.core.di.IoDispatcher
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.QuranReciter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed
import javax.inject.Inject
import javax.inject.Singleton

data class AudioState(
    val isPlaying: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentAyahId: Int = 0,
    val currentSurahNumber: Int = 0,
    // Total playlist duration and position (across all ayahs)
    val duration: Long = 0L,
    val position: Long = 0L,
    val currentTitle: String = "",
    val currentSubtitle: String? = null,
    val reciterName: String = QuranReciter.DEFAULT.displayName,
    val isActive: Boolean = false,
    val error: String? = null,
    // Playlist progress for surah-level tracking
    val currentAyahIndex: Int = 0,
    val totalAyahs: Int = 0,
    // Download progress for batch downloads
    val downloadedCount: Int = 0,
    val totalToDownload: Int = 0,
    val isPreparing: Boolean = false,
    /** What to go back and say again — off, a verse N times, a range, or the whole surah. */
    val repeat: RecitationRepeat = RecitationRepeat.Off,
    /** Playback rate. Deliberately not persisted — see [QuranAudioManager.setSpeed]. */
    val speed: RecitationSpeed = RecitationSpeed.DEFAULT,
    /**
     * Whether the reader follows the recitation: scrolling the verse list, or turning the
     * mushaf page, to keep the verse being recited on screen.
     */
    val followAlong: Boolean = false,
) {
    // Calculate surah progress as percentage (0.0 to 1.0)
    val surahProgress: Float
        get() = if (duration > 0) position.toFloat() / duration else 0f
}

@UnstableApi
@Singleton
class QuranAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: AyahAudioDownloader,
    /**
     * Where file transfers run. Injected rather than `Dispatchers.IO` inline so a test can
     * substitute a test dispatcher — without it the download loop runs on a real thread pool
     * that `advanceUntilIdle()` cannot drive, and the cancellation behaviour #468 fixed cannot
     * be asserted at all.
     */
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var positionTrackingJob: Job? = null
    private var downloadJob: Job? = null

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // Reciter CDN ID and bitrate - dynamically set from preferences
    private var reciterCdnId = DEFAULT_CDN.first
    private var reciterBitrate = DEFAULT_CDN.second

    // Sequential playback state
    private var ayahPlaylist: List<AyahAudioItem> = emptyList()
    private var currentPlaylistIndex: Int = -1
    private var playlistTitle: String = "" // Surah/Juz name for dynamic titles

    // Pre-computed durations (ms) for each playlist item, obtained from MediaMetadataRetriever
    // before playback starts. This avoids relying on ExoPlayer's lazy timeline parsing.
    private var precomputedDurations: List<Long> = emptyList()

    // ForwardingPlayer that reports total playlist position/duration to MediaSession
    private var forwardingPlayer: ForwardingPlayer? = null

    // Track which files are currently being downloaded to avoid duplicate downloads
    private val downloadingFiles = ConcurrentHashMap<String, Boolean>()

    // Controls whether audio auto-advances to next ayah
    private var continuousPlayback: Boolean = true

    /**
     * Set whether audio should auto-advance to next ayah when current one ends.
     * When false, playback stops after the current ayah completes.
     */
    fun setContinuousPlayback(enabled: Boolean) {
        continuousPlayback = enabled
    }

    /**
     * Choose what gets repeated.
     *
     * [RecitationRepeat.Surah] is the only mode ExoPlayer can do by itself, as
     * `REPEAT_MODE_ALL` over the playlist. The other two are counted here, in
     * [onAyahCompleted], because both have to *stop*: an ayah repeat has to move on after N,
     * and a range has to come back to its start rather than loop a single item forever.
     * `REPEAT_MODE_ONE` can express neither.
     */
    fun setRepeat(repeat: RecitationRepeat) {
        ayahRepeatsDone = 0
        player?.repeatMode = if (repeat is RecitationRepeat.Surah) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
        _audioState.update { it.copy(repeat = repeat) }
    }

    /**
     * Set the playback rate.
     *
     * **Not persisted, deliberately.** A reader who slowed one difficult passage to follow the
     * madd does not want every session for the next year slowed; the setting belongs to the
     * sitting, not to the person.
     */
    fun setSpeed(speed: RecitationSpeed) {
        player?.setPlaybackSpeed(speed.multiplier)
        _audioState.update { it.copy(speed = speed) }
    }

    /** Whether the reader scrolls or turns pages to keep the recited verse visible. */
    fun setFollowAlong(enabled: Boolean) {
        _audioState.update { it.copy(followAlong = enabled) }
    }

    /**
     * How many times the current verse has been played under [RecitationRepeat.Ayah].
     *
     * Reset whenever the mode changes or the playlist moves on, so a reader who sets "3 times"
     * on verse 5 and then jumps to verse 40 gets three plays of 40, not the remainder of 5's.
     */
    private var ayahRepeatsDone = 0

    /**
     * The verse just finished — decide whether to say it again, jump back, or carry on.
     *
     * Called from the media-item transition listener, which is where ExoPlayer tells us an item
     * ended. Returns true when it has taken over the transition, so the caller leaves the
     * player where this put it.
     */
    private fun handleRepeatOnTransition(p: ExoPlayer, previousIndex: Int): Boolean {
        return when (val repeat = _audioState.value.repeat) {
            is RecitationRepeat.Ayah -> {
                ayahRepeatsDone += 1
                if (ayahRepeatsDone < repeat.times) {
                    p.seekTo(previousIndex, 0L)
                    true
                } else {
                    ayahRepeatsDone = 0
                    false
                }
            }

            is RecitationRepeat.Range -> {
                // The playlist is the surah's verses in order, so verse numbers index it.
                val fromIndex = (repeat.fromAyah - 1).coerceIn(0, ayahPlaylist.lastIndex)
                val toIndex = (repeat.toAyah - 1).coerceIn(0, ayahPlaylist.lastIndex)
                if (previousIndex >= toIndex) {
                    p.seekTo(fromIndex, 0L)
                    true
                } else {
                    false
                }
            }

            RecitationRepeat.Off, RecitationRepeat.Surah -> false
        }
    }

    /**
     * Compute total duration across all playlist items.
     * Uses pre-computed durations (from MediaMetadataRetriever) for accuracy,
     * since ExoPlayer lazily parses item durations and may report 0 for unloaded items.
     */
    private fun computeTotalDuration(): Long {
        if (precomputedDurations.isNotEmpty()) {
            return precomputedDurations.sum()
        }
        return 0L
    }

    /**
     * Compute the cumulative position across all playlist items.
     * = sum of pre-computed durations of items before currentIndex + current item position.
     */
    private fun computeTotalPosition(player: ExoPlayer): Long {
        val currentIndex = player.currentMediaItemIndex
        var cumulative = 0L
        if (precomputedDurations.isNotEmpty()) {
            for (i in 0 until currentIndex.coerceAtMost(precomputedDurations.size)) {
                cumulative += precomputedDurations[i]
            }
        }
        return cumulative + player.currentPosition
    }

    /**
     * Seek to a total playlist position by finding the right media item and offset.
     * Uses pre-computed durations for accurate item boundary calculation.
     */
    fun seekToTotal(totalPositionMs: Long) {
        val p = player ?: return
        if (precomputedDurations.isEmpty()) {
            p.seekTo(totalPositionMs)
            return
        }
        var cumulative = 0L
        for (i in precomputedDurations.indices) {
            val dur = precomputedDurations[i]
            if (dur <= 0) continue
            if (cumulative + dur > totalPositionMs) {
                val offset = totalPositionMs - cumulative
                p.seekTo(i, offset)
                return
            }
            cumulative += dur
        }
        // Past end — seek to last item at its end
        val lastIndex = precomputedDurations.size - 1
        if (lastIndex >= 0) {
            p.seekTo(lastIndex, precomputedDurations[lastIndex])
        }
    }

    companion object {
        /** How often playback position is republished. See [startPositionTracking]. */
        private const val POSITION_TICK_MS = 400L

        /** Concurrent ayah downloads. Enough to saturate a connection, few enough to share it. */
        private const val MAX_PARALLEL_DOWNLOADS = 5

        // CDN identifiers and bitrates from https://api.alquran.cloud/v1/edition?format=audio&type=versebyverse
        // Pair: (cdnId, bitrate) - some reciters only have 64kbps, others have 128kbps.
        //
        // Keyed by the QuranReciter catalogue rather than by raw id strings: the *who* lives in
        // the domain enum, and only the CDN wiring — which edition slug, at which bitrate — is a
        // data-layer concern. An id the catalogue resolves but this map has no entry for falls
        // back to the default edition rather than failing.
        val RECITER_CDN_MAP: Map<QuranReciter, Pair<String, Int>> = mapOf(
            QuranReciter.MISHARY to Pair("ar.alafasy", 128),
            QuranReciter.SUDAIS to Pair("ar.abdurrahmaansudais", 64),
            QuranReciter.ABDULBASIT to Pair("ar.abdulsamad", 64),
            QuranReciter.MAHER to Pair("ar.mahermuaiqly", 128),
            QuranReciter.HUSSARY to Pair("ar.husary", 128),
            QuranReciter.MINSHAWI to Pair("ar.minshawi", 128),
            QuranReciter.AJAMY to Pair("ar.ahmedajamy", 128),
            QuranReciter.SHURAIM to Pair("ar.saoodshuraym", 64),
            QuranReciter.HUDHAIFY to Pair("ar.hudhaify", 128),
            QuranReciter.AYYOUB to Pair("ar.muhammadayyoub", 128),
            QuranReciter.JIBREEL to Pair("ar.muhammadjibreel", 128),
            QuranReciter.SHAATREE to Pair("ar.shaatree", 128),
            QuranReciter.BASFAR to Pair("ar.abdullahbasfar", 64)
        )

        private val DEFAULT_CDN = Pair("ar.alafasy", 128)
    }

    /**
     * Choose who is reciting.
     *
     * The CDN id only decides the URL of the *next* file fetched, so a reciter chosen mid-surah
     * used to change nothing audible: the queued media items still pointed at the previous
     * reciter's files, and the reader had to stop and start again to hear the change. When
     * something is playing, the playlist is rebuilt at the verse being recited — the same thing
     * a stop-and-restart did, without making the reader do it.
     *
     * @param restartIfPlaying false for the reciter *preview*, which sets the CDN and then
     *   immediately queues its own one-verse playlist; rebuilding the old one first would
     *   download a whole surah under the new reciter and throw it away a frame later.
     */
    fun setReciter(reciterId: String?, restartIfPlaying: Boolean = true) {
        val reciter = QuranReciter.fromId(reciterId)
        val (cdnId, bitrate) = RECITER_CDN_MAP[reciter] ?: DEFAULT_CDN
        val changed = cdnId != reciterCdnId || bitrate != reciterBitrate
        reciterCdnId = cdnId
        reciterBitrate = bitrate
        _audioState.update {
            it.copy(reciterName = reciter.displayName)
        }

        // Only when there is something to re-cut. On a settings hydration at launch — the common
        // case — nothing is loaded and this is a no-op.
        if (!changed || !restartIfPlaying) return
        val playlist = ayahPlaylist
        if (!_audioState.value.isActive || playlist.isEmpty()) return
        val resumeIndex = _audioState.value.currentAyahIndex.coerceIn(0, playlist.lastIndex)
        playAyahsSequentially(playlist, startIndex = resumeIndex, title = playlistTitle)
    }

    data class AyahAudioItem(
        val ayahGlobalId: Int,
        val surahNumber: Int,
        val ayahNumber: Int
    )

    @OptIn(UnstableApi::class)
    private fun getOrCreatePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            player = newPlayer

            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            val totalDur = computeTotalDuration()
                            _audioState.update {
                                it.copy(
                                    duration = if (totalDur > 0) totalDur else newPlayer.duration,
                                    isDownloading = false,
                                    isPreparing = false
                                )
                            }
                            startPositionTracking()
                        }

                        Player.STATE_ENDED -> {
                            // Playlist has fully ended
                            if (!newPlayer.hasNextMediaItem()) {
                                _audioState.update {
                                    it.copy(
                                        isPlaying = false,
                                        isActive = false,
                                        currentAyahId = 0,
                                        currentSurahNumber = 0
                                    )
                                }
                            }
                        }

                        Player.STATE_BUFFERING -> {
                            // Could show buffering indicator if needed
                        }

                        Player.STATE_IDLE -> {
                            // Player went idle, possibly due to an error - reset for recovery
                            if (newPlayer.playerError != null) {
                                _audioState.update {
                                    it.copy(
                                        isPlaying = false,
                                        error = "Playback error: ${newPlayer.playerError?.message}"
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _audioState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Repeat first, and only on an *automatic* transition: a reader who taps
                    // next while "repeat 3 times" is on is asking to move on, and a repeat that
                    // fought the next button would be a control that ignores you.
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                        handleRepeatOnTransition(newPlayer, currentPlaylistIndex)
                    ) {
                        return
                    }
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        // Moving by hand starts the count again — see [ayahRepeatsDone].
                        ayahRepeatsDone = 0
                    }

                    // Honor "Continuous Reading" setting: pause when an ayah ends naturally
                    // so the user can advance manually. User-driven transitions (next/prev,
                    // seek, playlist change) are still allowed through.
                    if (!continuousPlayback && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        newPlayer.pause()
                    }

                    val newIndex = newPlayer.currentMediaItemIndex
                    if (newIndex >= 0 && newIndex < ayahPlaylist.size) {
                        currentPlaylistIndex = newIndex
                        val item = ayahPlaylist[newIndex]
                        val dynamicTitle = playlistTitle.ifEmpty { "Quran" }
                        _audioState.update {
                            it.copy(
                                currentAyahId = item.ayahGlobalId,
                                currentSurahNumber = item.surahNumber,
                                currentAyahIndex = newIndex,
                                currentTitle = dynamicTitle,
                                currentSubtitle = it.reciterName
                            )
                        }
                    }
                }
            })
        }
    }

    /**
     * Publish playback position while something is listening.
     *
     * This used to tick every 100 ms unconditionally — ten wake-ups a second, forever,
     * including with the screen off during background playback, and including while
     * *paused*, because `isPlaying` guarded only the state update and not the delay.
     * Each tick recomputes [computeTotalPosition] and [computeTotalDuration] across the
     * whole playlist, which is not free on a 286-ayah surah.
     *
     * Now 400 ms, and only while a collector is attached. A progress bar animating
     * between 400 ms ticks looks smoother than one snapping to 100 ms ones, so this is
     * not a quality trade.
     */
    private fun startPositionTracking() {
        positionTrackingJob?.cancel()
        positionTrackingJob = scope.launch {
            _audioState.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { hasCollectors ->
                    if (!hasCollectors) return@collectLatest
                    while (true) {
                        delay(POSITION_TICK_MS)
                        val p = player ?: break
                        if (!p.isPlaying) continue
                        val totalPos = computeTotalPosition(p)
                        val totalDur = computeTotalDuration()
                        _audioState.update {
                            it.copy(
                                position = totalPos,
                                duration = if (totalDur > 0) totalDur else it.duration
                            )
                        }
                    }
                }
        }
    }

    /**
     * Download all ayahs for the playlist in parallel, then start playback.
     * Shows download progress as files are downloaded.
     */
    @VisibleForTesting
    internal suspend fun downloadAllAyahs(ayahs: List<AyahAudioItem>): List<File> {
        val files = mutableListOf<File>()
        val toDownload = mutableListOf<Pair<AyahAudioItem, File>>()

        // First pass: check what needs downloading
        for (ayah in ayahs) {
            val audioFile = getCachedFile("ayah_${ayah.ayahGlobalId}.mp3")
            files.add(audioFile)
            if (!audioFile.exists() || audioFile.length() == 0L) {
                toDownload.add(ayah to audioFile)
            }
        }

        if (toDownload.isEmpty()) {
            return files
        }

        _audioState.update {
            it.copy(
                isDownloading = true,
                isPreparing = true,
                downloadedCount = 0,
                totalToDownload = toDownload.size
            )
        }

        val downloadedCount = java.util.concurrent.atomic.AtomicInteger(0)

        // `coroutineScope { launch { … } }`, not `scope.launch { … }`.
        //
        // These used to be started on the manager's own `scope`, which made them
        // *siblings* of `downloadJob` rather than children of it. So `downloadJob.cancel()`
        // stopped the waiting and left every download running: they kept writing
        // `downloadedCount` and `downloadProgress` into the shared audio state, so
        // switching surah mid-download let the old surah's progress overwrite the new
        // one's and then jump backwards. Children inherit cancellation; siblings do not.
        //
        // A Semaphore rather than `chunked(5) + join`: the old shape waited for the
        // slowest file in each group of five before starting the next group, so one slow
        // connection idled four others. This keeps five in flight at all times.
        withContext(ioDispatcher) {
            val slots = Semaphore(MAX_PARALLEL_DOWNLOADS)
            coroutineScope {
                toDownload.forEach { (ayah, file) ->
                    ensureActive() // Bail out if the user cancelled.
                    launch {
                        slots.withPermit {
                            val url =
                                "https://cdn.islamic.network/quran/audio/$reciterBitrate/$reciterCdnId/${ayah.ayahGlobalId}.mp3"
                            downloadFileSilent(url, file)
                            val count = downloadedCount.incrementAndGet()
                            _audioState.update {
                                it.copy(
                                    downloadedCount = count,
                                    downloadProgress = count.toFloat() / toDownload.size
                                )
                            }
                        }
                    }
                }
            }
        }

        _audioState.update {
            it.copy(
                isDownloading = false,
                downloadProgress = 1f
            )
        }

        return files
    }

    /**
     * Play all ayahs sequentially using ExoPlayer's gapless playlist feature.
     * Downloads all files first, then queues them for seamless playback.
     */
    @OptIn(UnstableApi::class)
    fun playAyahsSequentially(ayahs: List<AyahAudioItem>, startIndex: Int = 0, title: String = "") {
        // Cancel any ongoing download job
        downloadJob?.cancel()

        // Release any existing player to ensure a fresh start (avoids stale ENDED state)
        positionTrackingJob?.cancel()
        forwardingPlayer = null
        player?.release()
        player = null
        precomputedDurations = emptyList()

        ayahPlaylist = ayahs
        currentPlaylistIndex = startIndex
        playlistTitle = title

        _audioState.update {
            AudioState(
                isActive = true,
                isPreparing = true,
                currentTitle = title,
                reciterName = it.reciterName, // Preserve reciter set by setReciter()
                error = null,
                totalAyahs = ayahs.size,
                currentAyahIndex = startIndex,
                currentAyahId = ayahs.getOrNull(startIndex)?.ayahGlobalId ?: 0,
                currentSurahNumber = ayahs.getOrNull(startIndex)?.surahNumber ?: 0
            )
        }

        // Start the foreground service for media notification
        QuranAudioService.start(context)

        downloadJob = scope.launch {
            try {
                // Download all ayahs first
                val files = downloadAllAyahs(ayahs)

                // Filter out any files that failed to download
                val validFiles = files.mapIndexedNotNull { index, file ->
                    if (file.exists() && file.length() > 0) {
                        index to file
                    } else {
                        null
                    }
                }

                if (validFiles.isEmpty()) {
                    _audioState.update {
                        it.copy(
                            isPreparing = false,
                            isActive = false,
                            error = "Failed to download audio files"
                        )
                    }
                    return@launch
                }

                // Pre-compute durations from files using MediaMetadataRetriever.
                // This gives accurate total duration immediately, avoiding the issue where
                // ExoPlayer lazily parses items and reports 0 for unloaded ones.
                val durations = withContext(Dispatchers.IO) {
                    validFiles.map { (_, file) ->
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(file.absolutePath)
                            retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_DURATION
                            )?.toLongOrNull() ?: 0L
                        } catch (e: Exception) {
                            CrashReporter.recordException(e)
                            0L
                        } finally {
                            retriever.release()
                        }
                    }
                }
                precomputedDurations = durations

                // Build media items with metadata for each ayah
                val surahTitle = title.ifEmpty { "Quran" }
                val mediaItems = validFiles.map { (_, file) ->
                    MediaItem.Builder()
                        .setUri(file.toURI().toString())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(surahTitle)
                                .setArtist(_audioState.value.reciterName)
                                .setAlbumTitle(surahTitle)
                                .build()
                        )
                        .build()
                }

                // Find the adjusted start index after filtering
                val adjustedStartIndex = validFiles.indexOfFirst { it.first >= startIndex }
                    .takeIf { it >= 0 } ?: 0

                // Setup player with all media items for gapless playback
                withContext(Dispatchers.Main) {
                    val exoPlayer = getOrCreatePlayer()
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    exoPlayer.addMediaItems(mediaItems)
                    exoPlayer.seekTo(adjustedStartIndex, 0L)
                    exoPlayer.prepare()
                    exoPlayer.play()

                    _audioState.update {
                        it.copy(
                            isPreparing = false,
                            duration = durations.sum()
                        )
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                _audioState.update {
                    it.copy(
                        isPreparing = false,
                        isDownloading = false,
                        error = "Error preparing audio: ${e.message}"
                    )
                }
            }
        }
    }

    fun playSurah(surahNumber: Int, surahName: String, ayahs: List<AyahAudioItem>) {
        val title = surahName
        playAyahsSequentially(ayahs, 0, title)
    }

    fun playAyah(ayahGlobalNumber: Int, surahNumber: Int, ayahNumber: Int) {
        // Single ayah play -- creates a 1-item playlist
        val item = AyahAudioItem(ayahGlobalNumber, surahNumber, ayahNumber)
        ayahPlaylist = listOf(item)
        currentPlaylistIndex = 0
        _audioState.update {
            it.copy(
                isActive = true,
                currentTitle = "Ayah $ayahNumber",
                currentSubtitle = "Surah $surahNumber",
                totalAyahs = 1,
                currentAyahIndex = 0,
                currentAyahId = ayahGlobalNumber,
                currentSurahNumber = surahNumber
            )
        }

        // Start the foreground service for media notification
        QuranAudioService.start(context)

        scope.launch {
            val audioFile = getCachedFile("ayah_${ayahGlobalNumber}.mp3")
            if (!audioFile.exists()) {
                _audioState.update { it.copy(isDownloading = true) }
                val url =
                    "https://cdn.islamic.network/quran/audio/$reciterBitrate/$reciterCdnId/${ayahGlobalNumber}.mp3"
                downloadFileSilent(url, audioFile)
                _audioState.update { it.copy(isDownloading = false) }
            }

            if (audioFile.exists() && audioFile.length() > 0) {
                withContext(Dispatchers.Main) {
                    playFile(audioFile)
                }
            } else {
                _audioState.update {
                    it.copy(
                        isActive = false,
                        error = "Failed to download audio"
                    )
                }
                // Stop service if playback failed
                QuranAudioService.stop(context)
            }
        }
    }

    /**
     * Play all ayahs starting from a specific one in the list.
     */
    fun playFromAyah(ayahGlobalId: Int, allAyahs: List<AyahAudioItem>, title: String) {
        val startIndex = allAyahs.indexOfFirst { it.ayahGlobalId == ayahGlobalId }
        if (startIndex >= 0) {
            playAyahsSequentially(allAyahs, startIndex, title)
        }
    }

    /**
     * Skip to next ayah in the playlist.
     */
    fun skipToNext() {
        val p = player ?: return
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        }
    }

    /**
     * Skip to previous ayah in the playlist.
     */
    fun skipToPrevious() {
        val p = player ?: return
        if (p.hasPreviousMediaItem()) {
            p.seekToPreviousMediaItem()
        } else {
            // If at the beginning, restart current ayah
            p.seekTo(0)
        }
    }

    @OptIn(UnstableApi::class)
    private fun playFile(file: File) {
        val exoPlayer = getOrCreatePlayer()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        val mediaItem = MediaItem.fromUri(file.toURI().toString())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            when (p.playbackState) {
                Player.STATE_ENDED -> {
                    // Player finished — seek back to start and replay
                    if (p.mediaItemCount > 0) {
                        p.seekTo(0, 0L)
                        p.play()
                    }
                }

                Player.STATE_IDLE -> {
                    // Player is idle with no media — nothing to do
                }

                else -> {
                    p.play()
                }
            }
        }
    }

    fun stop() {
        downloadJob?.cancel()
        downloadJob = null
        positionTrackingJob?.cancel()
        // Release the player entirely so next playback gets a fresh instance
        forwardingPlayer = null
        player?.release()
        player = null
        ayahPlaylist = emptyList()
        currentPlaylistIndex = -1
        playlistTitle = ""
        precomputedDurations = emptyList()
        downloadingFiles.clear()
        _audioState.update { AudioState() }
        // Don't call QuranAudioService.stop() here — the service's state observer
        // detects isActive=false and calls stopSelf(). Sending a separate stop intent
        // caused a race condition: the async ACTION_STOP could arrive after a new
        // playback had already started, killing the new session.
    }

    /**
     * Returns a ForwardingPlayer that reports total playlist position/duration
     * and dynamic metadata from AudioState.
     * Used by QuranAudioService to bind a MediaSession for notification & lock screen.
     */
    @OptIn(UnstableApi::class)
    fun getPlayer(): Player? {
        val p = player ?: return null

        // Return cached forwarding player if it still wraps the same ExoPlayer
        val existing = forwardingPlayer
        if (existing != null && existing.wrappedPlayer === p) {
            return existing
        }

        val manager = this
        return object : ForwardingPlayer(p) {
            override fun getDuration(): Long {
                return manager.computeTotalDuration().takeIf { it > 0 } ?: super.getDuration()
            }

            override fun getCurrentPosition(): Long {
                return manager.computeTotalPosition(p)
            }

            override fun getContentPosition(): Long {
                return manager.computeTotalPosition(p)
            }

            override fun getContentDuration(): Long {
                return manager.computeTotalDuration().takeIf { it > 0 }
                    ?: super.getContentDuration()
            }

            override fun getBufferedPosition(): Long {
                val currentIndex = p.currentMediaItemIndex
                var cumulative = 0L
                for (i in 0 until currentIndex.coerceAtMost(manager.precomputedDurations.size)) {
                    cumulative += manager.precomputedDurations[i]
                }
                return cumulative + p.bufferedPosition
            }

            override fun getMediaMetadata(): MediaMetadata {
                // Return dynamic metadata built from current AudioState so that
                // MediaSession always reflects the currently playing ayah title.
                val state = manager._audioState.value
                return MediaMetadata.Builder()
                    .setTitle(state.currentTitle.ifEmpty { manager.playlistTitle })
                    .setArtist(state.reciterName)
                    .setAlbumTitle(manager.playlistTitle.ifEmpty { "Quran" })
                    .build()
            }

            override fun seekTo(positionMs: Long) {
                manager.seekToTotal(positionMs)
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                if (mediaItemIndex == 0 && p.mediaItemCount > 1) {
                    manager.seekToTotal(positionMs)
                } else {
                    super.seekTo(mediaItemIndex, positionMs)
                }
            }

            override fun isCurrentMediaItemSeekable(): Boolean = true
        }.also { forwardingPlayer = it }
    }

    fun seekTo(position: Long) {
        // Position is in total playlist coordinates — map to correct item + offset
        seekToTotal(position)
    }

    private fun getCachedFile(filename: String): File {
        val dir = File(context.filesDir, "quran_audio/$reciterCdnId")
        dir.mkdirs()
        return File(dir, filename)
    }

    /**
     * Download file without updating state (for batch downloads).
     * Includes retry logic and timeout for the wait loop.
     */
    private suspend fun downloadFileSilent(url: String, destination: File) {
        // Check if already downloading this file
        val key = destination.absolutePath
        if (downloadingFiles.putIfAbsent(key, true) != null) {
            // Already downloading, wait for it with timeout (max 60s)
            var waited = 0L
            while (downloadingFiles.containsKey(key) && waited < 60_000L) {
                delay(100)
                waited += 100
            }
            return
        }

        try {
            withContext(ioDispatcher) {
                var lastException: Exception? = null
                val maxRetries = 2
                for (attempt in 0..maxRetries) {
                    ensureActive() // Bail out if cancelled
                    try {
                        downloader.download(url, destination)
                        return@withContext // Success
                    } catch (e: Exception) {
                        destination.delete()
                        lastException = e
                        if (attempt < maxRetries) {
                            delay(1000L) // Wait 1s before retry
                        }
                    }
                }
                // All retries failed
                lastException?.let {
                    CrashReporter.recordException(it)
                    destination.delete()
                }
            }
        } finally {
            downloadingFiles.remove(key)
        }
    }

    fun release() {
        downloadJob?.cancel()
        positionTrackingJob?.cancel()
        forwardingPlayer = null
        player?.release()
        player = null
        playlistTitle = ""
        precomputedDurations = emptyList()
        downloadingFiles.clear()
        _audioState.update { AudioState() }
    }
}
