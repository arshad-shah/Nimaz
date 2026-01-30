package com.arshadshah.nimaz.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class AudioState(
    val isPlaying: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentAyahId: Int = 0,
    // Total playlist duration and position (across all ayahs)
    val duration: Long = 0L,
    val position: Long = 0L,
    val currentTitle: String = "",
    val currentSubtitle: String? = null,
    val reciterName: String = "Mishary Rashid Alafasy",
    val isActive: Boolean = false,
    val error: String? = null,
    // Playlist progress for surah-level tracking
    val currentAyahIndex: Int = 0,
    val totalAyahs: Int = 0,
    // Download progress for batch downloads
    val downloadedCount: Int = 0,
    val totalToDownload: Int = 0,
    val isPreparing: Boolean = false
) {
    // Calculate surah progress as percentage (0.0 to 1.0)
    val surahProgress: Float
        get() = if (duration > 0) position.toFloat() / duration else 0f
}

@Singleton
class QuranAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var positionTrackingJob: Job? = null
    private var downloadJob: Job? = null

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // Reciter CDN ID and bitrate - dynamically set from preferences
    private var reciterCdnId = "ar.alafasy" // Default: Mishary Rashid Alafasy
    private var reciterBitrate = 128 // Default bitrate

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
        // CDN identifiers and bitrates from https://api.alquran.cloud/v1/edition?format=audio&type=versebyverse
        // Pair: (cdnId, bitrate) - some reciters only have 64kbps, others have 128kbps
        val RECITER_CDN_MAP = mapOf(
            "alafasy" to Pair("ar.alafasy", 128),
            "mishary" to Pair("ar.alafasy", 128),
            "sudais" to Pair("ar.abdurrahmaansudais", 64),
            "abdulbasit" to Pair("ar.abdulsamad", 64),
            "muaiqly" to Pair("ar.mahermuaiqly", 128),
            "maher" to Pair("ar.mahermuaiqly", 128),
            "hussary" to Pair("ar.husary", 128),
            "minshawi" to Pair("ar.minshawi", 128),
            "ajamy" to Pair("ar.ahmedajamy", 128),
            "shuraim" to Pair("ar.saoodshuraym", 64),
            "hudhaify" to Pair("ar.hudhaify", 128),
            "ayyoub" to Pair("ar.muhammadayyoub", 128),
            "jibreel" to Pair("ar.muhammadjibreel", 128),
            "shaatree" to Pair("ar.shaatree", 128),
            "basfar" to Pair("ar.abdullahbasfar", 64)
        )
    }

    fun setReciter(reciterId: String?) {
        val (cdnId, bitrate) = RECITER_CDN_MAP[reciterId] ?: Pair("ar.alafasy", 128)
        reciterCdnId = cdnId
        reciterBitrate = bitrate
        _audioState.update {
            it.copy(reciterName = getReciterDisplayName(reciterId))
        }
    }

    private fun getReciterDisplayName(reciterId: String?): String {
        return when (reciterId) {
            "alafasy", "mishary" -> "Mishary Rashid Alafasy"
            "sudais" -> "Abdul Rahman Al-Sudais"
            "abdulbasit" -> "Abdul Basit Abdul Samad"
            "muaiqly", "maher" -> "Maher Al-Muaiqly"
            "hussary" -> "Mahmoud Khalil Al-Hussary"
            "minshawi" -> "Muhammad Siddiq Al-Minshawi"
            "ajamy" -> "Ahmed Al-Ajamy"
            "shuraim" -> "Saud Al-Shuraim"
            "hudhaify" -> "Ali Al-Hudhaify"
            "ayyoub" -> "Muhammad Ayyoub"
            "jibreel" -> "Muhammad Jibreel"
            "shaatree" -> "Abu Bakr Al-Shaatree"
            "basfar" -> "Abdullah Basfar"
            else -> "Mishary Rashid Alafasy"
        }
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
                                        currentAyahId = 0
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
                    // This is called when transitioning to next ayah - gapless!
                    val newIndex = newPlayer.currentMediaItemIndex
                    if (newIndex >= 0 && newIndex < ayahPlaylist.size) {
                        currentPlaylistIndex = newIndex
                        val item = ayahPlaylist[newIndex]
                        // Build dynamic title: "Surah Name - Ayah X"
                        val dynamicTitle = if (playlistTitle.isNotEmpty()) {
                            "$playlistTitle - Ayah ${item.ayahNumber}"
                        } else {
                            "Ayah ${item.ayahNumber}"
                        }
                        _audioState.update {
                            it.copy(
                                currentAyahId = item.ayahGlobalId,
                                currentAyahIndex = newIndex,
                                currentTitle = dynamicTitle,
                                currentSubtitle = "Ayah ${item.ayahNumber} of ${ayahPlaylist.size}"
                                // Don't reset position — total position tracking handles it
                            )
                        }
                    }
                }
            })
        }
    }

    private fun startPositionTracking() {
        positionTrackingJob?.cancel()
        positionTrackingJob = scope.launch {
            while (true) {
                delay(100) // More frequent updates for smoother progress
                val p = player ?: break
                if (p.isPlaying) {
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
    private suspend fun downloadAllAyahs(ayahs: List<AyahAudioItem>): List<File> {
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

        // Download files with parallel downloads (limit concurrency to avoid overwhelming network)
        val downloadedCount = java.util.concurrent.atomic.AtomicInteger(0)

        withContext(Dispatchers.IO) {
            // Use chunked parallel downloads - 5 concurrent downloads at a time
            toDownload.chunked(5).forEach { chunk ->
                ensureActive() // Bail out if user cancelled
                val jobs = chunk.map { (ayah, file) ->
                    scope.launch(Dispatchers.IO) {
                        val url = "https://cdn.islamic.network/quran/audio/$reciterBitrate/$reciterCdnId/${ayah.ayahGlobalId}.mp3"
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
                jobs.forEach { it.join() }
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
                error = null,
                totalAyahs = ayahs.size,
                currentAyahIndex = startIndex,
                currentAyahId = ayahs.getOrNull(startIndex)?.ayahGlobalId ?: 0
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
                        } catch (_: Exception) {
                            0L
                        } finally {
                            retriever.release()
                        }
                    }
                }
                precomputedDurations = durations

                // Build media items with metadata for each ayah
                val mediaItems = validFiles.map { (originalIndex, file) ->
                    val ayah = ayahs[originalIndex]
                    val ayahTitle = if (title.isNotEmpty()) {
                        "$title - Ayah ${ayah.ayahNumber}"
                    } else {
                        "Ayah ${ayah.ayahNumber}"
                    }
                    MediaItem.Builder()
                        .setUri(file.toURI().toString())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(ayahTitle)
                                .setArtist(_audioState.value.reciterName)
                                .setAlbumTitle(title.ifEmpty { "Quran" })
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
                currentAyahIndex = 0
            )
        }

        // Start the foreground service for media notification
        QuranAudioService.start(context)

        scope.launch {
            val audioFile = getCachedFile("ayah_${ayahGlobalNumber}.mp3")
            if (!audioFile.exists()) {
                _audioState.update { it.copy(isDownloading = true) }
                val url = "https://cdn.islamic.network/quran/audio/$reciterBitrate/$reciterCdnId/${ayahGlobalNumber}.mp3"
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
                return manager.computeTotalDuration().takeIf { it > 0 } ?: super.getContentDuration()
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
            withContext(Dispatchers.IO) {
                var lastException: Exception? = null
                val maxRetries = 2
                for (attempt in 0..maxRetries) {
                    ensureActive() // Bail out if cancelled
                    try {
                        val connection = URL(url).openConnection()
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000

                        connection.getInputStream().use { input ->
                            destination.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                }
                            }
                        }
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
                lastException?.let { destination.delete() }
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
