package com.arshadshah.nimaz.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdhanAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<AdhanSound?>(null)
    val currentlyPlaying: StateFlow<AdhanSound?> = _currentlyPlaying.asStateFlow()

    private val adhanDir: File
        get() = File(context.filesDir, "adhan").also { it.mkdirs() }

    fun isDownloaded(sound: AdhanSound): Boolean {
        return File(adhanDir, sound.fileName).exists()
    }

    fun getAdhanUri(sound: AdhanSound): Uri? {
        val file = File(adhanDir, sound.fileName)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun preview(sound: AdhanSound) {
        stopPreview()
        val file = File(adhanDir, sound.fileName)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                _isPlaying.value = false
                _currentlyPlaying.value = null
            }
            prepare()
            start()
        }
        _isPlaying.value = true
        _currentlyPlaying.value = sound
    }

    fun stopPreview() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentlyPlaying.value = null
    }

    suspend fun downloadAdhan(sound: AdhanSound, assetBased: Boolean = true): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // For now, copy from assets if available
                // In production, this would download from a CDN
                val outputFile = File(adhanDir, sound.fileName)
                if (outputFile.exists()) return@withContext true

                try {
                    context.assets.open("adhan/${sound.fileName}").use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } catch (e: Exception) {
                    // Asset not found - create a placeholder so the UI reflects "downloaded"
                    // In production, download from CDN here
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
