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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
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
    val error: String? = null
)

@Singleton
class QuranAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var positionTrackingJob: Job? = null

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // Reciter CDN ID and bitrate - dynamically set from preferences
    private var reciterCdnId = "ar.alafasy" // Default: Mishary Rashid Alafasy
    private var reciterBitrate = 128 // Default bitrate

    // Monotonically increasing generation counter. Each time we start a new track,
    // we bump this. The STATE_ENDED handler captures the generation at the time the
    // track started; if it doesn't match the current value, it means we've already
    // moved on and the callback is stale.
    @Volatile
    private var playbackGeneration: Int = 0

    // Sequential playback state
    private var ayahPlaylist: List<AyahAudioItem> = emptyList()
    private var currentPlaylistIndex: Int = -1
    private val failedAyahs = mutableSetOf<Int>()

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

    private fun getOrCreatePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            player = newPlayer
            // Track the generation at the time of listener registration
            var listenerGeneration = playbackGeneration

            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            // Sync this listener's generation to the current one
                            listenerGeneration = playbackGeneration
                            _audioState.update {
                                it.copy(
                                    duration = newPlayer.duration,
                                    isDownloading = false
                                )
                            }
                            startPositionTracking()
                        }
                        Player.STATE_ENDED -> {
                            _audioState.update { it.copy(isPlaying = false) }
                            // Only auto-advance if this callback belongs to the current generation.
                            // If generation has moved on, this is a stale callback from stop().
                            if (listenerGeneration == playbackGeneration) {
                                if (currentPlaylistIndex >= 0 && currentPlaylistIndex < ayahPlaylist.size - 1) {
                                    playNextInPlaylist()
                                } else {
                                    // Playlist finished
                                    _audioState.update { it.copy(isActive = false, currentAyahId = 0) }
                                }
                            }
                        }
                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _audioState.update { it.copy(isPlaying = isPlaying) }
                }
            })
        }
    }

    private fun startPositionTracking() {
        positionTrackingJob?.cancel()
        positionTrackingJob = scope.launch {
            while (true) {
                delay(500)
                val p = player ?: break
                if (p.isPlaying) {
                    _audioState.update { it.copy(position = p.currentPosition) }
                }
            }
        }
    }

    /**
     * Play all ayahs sequentially, starting from the given list and index.
     * Each ayah is highlighted as it plays, then auto-advances to the next.
     */
    fun playAyahsSequentially(ayahs: List<AyahAudioItem>, startIndex: Int = 0, title: String = "") {
        failedAyahs.clear() // Clear failed ayahs at start of new playlist
        ayahPlaylist = ayahs
        currentPlaylistIndex = startIndex - 1 // will be incremented by playNextInPlaylist
        _audioState.update {
            it.copy(
                isActive = true,
                currentTitle = title,
                error = null
            )
        }
        playNextInPlaylist()
    }

    private fun playNextInPlaylist() {
        currentPlaylistIndex++

        // Skip any previously failed ayahs
        while (currentPlaylistIndex < ayahPlaylist.size) {
            if (ayahPlaylist[currentPlaylistIndex].ayahGlobalId in failedAyahs) {
                currentPlaylistIndex++
                continue
            }
            break
        }

        if (currentPlaylistIndex >= ayahPlaylist.size) {
            // Done - playlist finished
            _audioState.update { it.copy(isActive = false, isPlaying = false, currentAyahId = 0) }
            failedAyahs.clear()
            return
        }

        val item = ayahPlaylist[currentPlaylistIndex]
        scope.launch {
            _audioState.update {
                it.copy(
                    isDownloading = true,
                    isActive = true,
                    error = null,
                    currentAyahId = item.ayahGlobalId,
                    currentSubtitle = "Surah ${item.surahNumber}, Ayah ${item.ayahNumber}"
                )
            }

            val audioFile = getCachedFile("ayah_${item.ayahGlobalId}.mp3")
            if (!audioFile.exists()) {
                // Try downloading with 1 retry
                val downloaded = downloadFileWithRetry(
                    url = "https://cdn.islamic.network/quran/audio/$reciterBitrate/$reciterCdnId/${item.ayahGlobalId}.mp3",
                    destination = audioFile
                )
                if (!downloaded) {
                    // Mark as failed and skip to next ayah instead of stopping
                    failedAyahs.add(item.ayahGlobalId)
                    _audioState.update {
                        it.copy(
                            isDownloading = false,
                            error = "Skipped Ayah ${item.ayahNumber} (download failed)"
                        )
                    }
                    // Continue to next ayah
                    playNextInPlaylist()
                    return@launch
                }
            }

            if (audioFile.exists()) {
                playFile(audioFile)
            } else {
                // Mark as failed and skip to next ayah
                failedAyahs.add(item.ayahGlobalId)
                _audioState.update {
                    it.copy(
                        isDownloading = false,
                        error = "Skipped Ayah ${item.ayahNumber} (file unavailable)"
                    )
                }
                playNextInPlaylist()
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
        currentPlaylistIndex = -1
        _audioState.update {
            it.copy(
                isActive = true,
                currentTitle = "Ayah $ayahNumber",
                currentSubtitle = "Surah $surahNumber"
            )
        }
        playNextInPlaylist()
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

    @OptIn(UnstableApi::class)
    private fun playFile(file: File) {
        val exoPlayer = getOrCreatePlayer()
        // Bump generation so any pending STATE_ENDED from the old track is ignored
        playbackGeneration++
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
            p.play()
        }
    }

    fun stop() {
        playbackGeneration++ // Invalidate any pending callbacks
        player?.stop()
        player?.clearMediaItems()
        positionTrackingJob?.cancel()
        ayahPlaylist = emptyList()
        currentPlaylistIndex = -1
        failedAyahs.clear()
        _audioState.update { AudioState() }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    private fun getCachedFile(filename: String): File {
        val dir = File(context.filesDir, "quran_audio/$reciterCdnId")
        dir.mkdirs()
        return File(dir, filename)
    }

    private suspend fun downloadFile(url: String, destination: File) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                val totalSize = connection.contentLength.toLong()
                var downloaded = 0L

                connection.getInputStream().use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalSize > 0) {
                                _audioState.update {
                                    it.copy(downloadProgress = downloaded.toFloat() / totalSize)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                destination.delete()
                _audioState.update { it.copy(isDownloading = false) }
            }
        }
    }

    private suspend fun downloadFileWithRetry(url: String, destination: File, maxRetries: Int = 1): Boolean {
        repeat(maxRetries + 1) { attempt ->
            downloadFile(url, destination)
            if (destination.exists() && destination.length() > 0) {
                return true
            }
            if (attempt < maxRetries) {
                kotlinx.coroutines.delay(1000) // Wait 1s before retry
            }
        }
        return false
    }

    fun release() {
        positionTrackingJob?.cancel()
        player?.release()
        player = null
        _audioState.update { AudioState() }
    }
}
