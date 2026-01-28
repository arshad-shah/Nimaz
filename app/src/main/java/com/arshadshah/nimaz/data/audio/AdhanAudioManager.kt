package com.arshadshah.nimaz.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

/**
 * Represents the current download state for an adhan sound.
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

@Singleton
class AdhanAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<AdhanSound?>(null)
    val currentlyPlaying: StateFlow<AdhanSound?> = _currentlyPlaying.asStateFlow()

    private val _downloadState = MutableStateFlow<Map<AdhanSound, DownloadState>>(emptyMap())
    val downloadState: StateFlow<Map<AdhanSound, DownloadState>> = _downloadState.asStateFlow()

    private val adhanDir: File
        get() = File(context.filesDir, "adhan").also { it.mkdirs() }

    /**
     * Checks if the adhan sound is downloaded.
     * @param isFajr If true, checks for the Fajr variant.
     */
    fun isDownloaded(sound: AdhanSound, isFajr: Boolean = false): Boolean {
        val fileName = sound.getFileName(isFajr)
        return File(adhanDir, fileName).exists()
    }

    /**
     * Checks if both regular and Fajr variants are downloaded.
     */
    fun isFullyDownloaded(sound: AdhanSound): Boolean {
        return isDownloaded(sound, false) && isDownloaded(sound, true)
    }

    fun getAdhanUri(sound: AdhanSound, isFajr: Boolean = false): Uri? {
        val fileName = sound.getFileName(isFajr)
        val file = File(adhanDir, fileName)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    /**
     * Preview an adhan sound.
     * @param isFajr If true, plays the Fajr variant.
     */
    fun preview(sound: AdhanSound, isFajr: Boolean = false) {
        stopPreview()
        val fileName = sound.getFileName(isFajr)
        val file = File(adhanDir, fileName)

        if (!file.exists()) {
            _currentlyPlaying.value = null
            _isPlaying.value = false
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentlyPlaying.value = null
                }
                setOnErrorListener { _, what, extra ->
                    _isPlaying.value = false
                    _currentlyPlaying.value = null
                    true
                }
                prepare()
                start()
            }
            _isPlaying.value = true
            _currentlyPlaying.value = sound
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
            _currentlyPlaying.value = null
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPreview() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentlyPlaying.value = null
    }

    /**
     * Downloads an adhan sound from its CDN URL.
     * For SIMPLE_BEEP, generates the sound programmatically.
     * @param isFajr If true, downloads the Fajr variant.
     */
    suspend fun downloadAdhan(sound: AdhanSound, isFajr: Boolean = false): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = sound.getFileName(isFajr)
                val downloadUrl = sound.getDownloadUrl(isFajr)
                val outputFile = File(adhanDir, fileName)

                if (outputFile.exists()) {
                    updateDownloadState(sound, DownloadState.Completed)
                    return@withContext true
                }

                // Handle simple beep separately - generate it programmatically
                if (sound == AdhanSound.SIMPLE_BEEP) {
                    return@withContext generateBeepSound(outputFile)
                }

                // Download from CDN
                if (downloadUrl.isEmpty()) {
                    updateDownloadState(sound, DownloadState.Failed("No download URL available"))
                    return@withContext false
                }

                updateDownloadState(sound, DownloadState.Downloading(0))

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true

                try {
                    connection.connect()
                    val responseCode = connection.responseCode

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        updateDownloadState(sound, DownloadState.Failed("HTTP $responseCode"))
                        return@withContext false
                    }

                    val fileLength = connection.contentLength
                    val tempFile = File(adhanDir, "${fileName}.tmp")

                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                if (fileLength > 0) {
                                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                                    updateDownloadState(sound, DownloadState.Downloading(progress))
                                }
                            }
                        }
                    }

                    // Rename temp file to final file
                    if (tempFile.renameTo(outputFile)) {
                        updateDownloadState(sound, DownloadState.Completed)
                        true
                    } else {
                        tempFile.delete()
                        updateDownloadState(sound, DownloadState.Failed("Failed to save file"))
                        false
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateDownloadState(sound, DownloadState.Failed(e.message ?: "Download failed"))
                false
            }
        }
    }

    /**
     * Downloads both regular and Fajr variants of an adhan sound.
     */
    suspend fun downloadAdhanWithFajr(sound: AdhanSound): Boolean {
        val regularSuccess = downloadAdhan(sound, isFajr = false)
        val fajrSuccess = downloadAdhan(sound, isFajr = true)
        return regularSuccess && fajrSuccess
    }

    /**
     * Generates a simple beep sound and saves it as a WAV file.
     * MediaPlayer can play WAV files regardless of the file extension.
     */
    private fun generateBeepSound(outputFile: File): Boolean {
        return try {
            updateDownloadState(AdhanSound.SIMPLE_BEEP, DownloadState.Downloading(0))

            val sampleRate = 44100
            val durationMs = 1500 // 1.5 second beep
            val frequency = 800.0 // Hz
            val numSamples = (sampleRate * durationMs) / 1000

            // Generate sine wave samples with fade in/out
            val samples = ShortArray(numSamples)
            val fadeLength = numSamples / 10 // 10% fade

            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                var amplitude = sin(angle) * Short.MAX_VALUE * 0.7

                // Apply fade in
                if (i < fadeLength) {
                    amplitude *= i.toDouble() / fadeLength
                }
                // Apply fade out
                if (i > numSamples - fadeLength) {
                    amplitude *= (numSamples - i).toDouble() / fadeLength
                }

                samples[i] = amplitude.toInt().toShort()
            }

            updateDownloadState(AdhanSound.SIMPLE_BEEP, DownloadState.Downloading(50))

            // Write directly to output file (WAV format, MediaPlayer handles it fine)
            writeWavFile(outputFile, samples, sampleRate)

            updateDownloadState(AdhanSound.SIMPLE_BEEP, DownloadState.Completed)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            updateDownloadState(AdhanSound.SIMPLE_BEEP, DownloadState.Failed(e.message ?: "Generation failed"))
            false
        }
    }

    /**
     * Writes audio samples to a WAV file.
     */
    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val dataSize = samples.size * 2

        FileOutputStream(file).use { fos ->
            // RIFF header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(36 + dataSize))
            fos.write("WAVE".toByteArray())

            // fmt subchunk
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16)) // Subchunk1Size for PCM
            fos.write(shortToBytes(1)) // AudioFormat (1 = PCM)
            fos.write(shortToBytes(numChannels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes((numChannels * bitsPerSample / 8).toShort())) // BlockAlign
            fos.write(shortToBytes(bitsPerSample.toShort()))

            // data subchunk
            fos.write("data".toByteArray())
            fos.write(intToBytes(dataSize))

            // Write samples
            for (sample in samples) {
                fos.write(shortToBytes(sample))
            }
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    private fun updateDownloadState(sound: AdhanSound, state: DownloadState) {
        _downloadState.value = _downloadState.value.toMutableMap().apply {
            this[sound] = state
        }
    }

    /**
     * Deletes a downloaded adhan sound.
     * @param isFajr If true, deletes the Fajr variant.
     */
    fun deleteAdhan(sound: AdhanSound, isFajr: Boolean = false): Boolean {
        val fileName = sound.getFileName(isFajr)
        val file = File(adhanDir, fileName)
        val deleted = file.delete()
        if (deleted && !isDownloaded(sound, !isFajr)) {
            // Only reset state if both variants are deleted
            updateDownloadState(sound, DownloadState.Idle)
        }
        return deleted
    }

    /**
     * Deletes both regular and Fajr variants of an adhan sound.
     */
    fun deleteAdhanFully(sound: AdhanSound): Boolean {
        val regularDeleted = deleteAdhan(sound, false)
        val fajrDeleted = deleteAdhan(sound, true)
        updateDownloadState(sound, DownloadState.Idle)
        return regularDeleted || fajrDeleted
    }

    /**
     * Gets the file size of a downloaded adhan in bytes, or null if not downloaded.
     * @param isFajr If true, gets the size of the Fajr variant.
     */
    fun getDownloadedSize(sound: AdhanSound, isFajr: Boolean = false): Long? {
        val fileName = sound.getFileName(isFajr)
        val file = File(adhanDir, fileName)
        return if (file.exists()) file.length() else null
    }

    /**
     * Gets the total size of both regular and Fajr variants in bytes.
     */
    fun getTotalDownloadedSize(sound: AdhanSound): Long {
        val regularSize = getDownloadedSize(sound, false) ?: 0L
        val fajrSize = getDownloadedSize(sound, true) ?: 0L
        return regularSize + fajrSize
    }

    /**
     * Play adhan for a prayer notification via the foreground service.
     * This ensures playback works even when the app is closed.
     * @param isFajr If true, plays the Fajr variant (includes "prayer is better than sleep").
     * @param prayerName The name of the prayer for the notification.
     */
    fun playAdhanForNotification(sound: AdhanSound, isFajr: Boolean = false, prayerName: String = "Prayer") {
        AdhanPlaybackService.playAdhan(
            context = context,
            adhanSound = sound,
            isFajr = isFajr,
            prayerName = prayerName
        )
    }

    /**
     * Stop any adhan playing from a notification via the foreground service.
     */
    fun stopNotificationAdhan() {
        AdhanPlaybackService.stopAdhan(context)
    }
}
