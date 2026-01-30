package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
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
        get() = if (totalAyahs > 0) {
            val completedAyahs = currentAyahIndex.toFloat()
            val currentAyahProgress = if (duration > 0) position.toFloat() / duration else 0f
            (completedAyahs + currentAyahProgress) / totalAyahs
        } else 0f
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
                            _audioState.update {
                                it.copy(
                                    duration = newPlayer.duration,
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
                        _audioState.update {
                            it.copy(
                                currentAyahId = item.ayahGlobalId,
                                currentAyahIndex = newIndex,
                                currentSubtitle = "Ayah ${item.ayahNumber} of ${ayahPlaylist.size}",
                                position = 0L // Reset position for new ayah
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
                    _audioState.update { it.copy(position = p.currentPosition) }
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
        player?.release()
        player = null

        ayahPlaylist = ayahs
        currentPlaylistIndex = startIndex

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

                // Build media items for all ayahs
                val mediaItems = validFiles.map { (_, file) ->
                    MediaItem.fromUri(file.toURI().toString())
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
                        it.copy(isPreparing = false)
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
        player?.release()
        player = null
        ayahPlaylist = emptyList()
        currentPlaylistIndex = -1
        downloadingFiles.clear()
        _audioState.update { AudioState() }
        // Stop the foreground service
        QuranAudioService.stop(context)
    }

    /**
     * Returns the current ExoPlayer instance, if one exists.
     * Used by QuranAudioService to bind a MediaSession.
     */
    fun getPlayer(): ExoPlayer? = player

    fun seekTo(position: Long) {
        player?.seekTo(position)
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
        player?.release()
        player = null
        downloadingFiles.clear()
        _audioState.update { AudioState() }
    }
}
