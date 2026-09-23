package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arshadshah.nimaz.domain.model.PrayerAudio
import com.arshadshah.nimaz.domain.model.PrayerAudioState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Screen-scoped spoken-word playback. Never auto-advances the prayer lesson. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PrayerAudioManager @Inject constructor(@ApplicationContext private val context: Context) {
    private var player: ExoPlayer? = null
    private val mutableState = MutableStateFlow(PrayerAudioState())
    val state = mutableState.asStateFlow()

    fun toggle(clip: PrayerAudio) {
        if (state.value.current == clip) { stop(); return }
        val p = player ?: ExoPlayer.Builder(context).build().also { created ->
            player = created
            created.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            created.setHandleAudioBecomingNoisy(true)
            created.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> mutableState.update { it.copy(loading = true) }
                        Player.STATE_READY -> mutableState.update { it.copy(loading = false) }
                        Player.STATE_ENDED -> stop()
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    created.stop()
                    mutableState.value = PrayerAudioState(failed = true)
                }
            })
        }
        mutableState.value = PrayerAudioState(clip, loading = true)
        p.setMediaItems(urls(clip).map { MediaItem.fromUri(it) })
        p.prepare()
        p.play()
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        mutableState.value = PrayerAudioState()
    }

    fun release() {
        player?.release()
        player = null
        mutableState.value = PrayerAudioState()
    }

    companion object {
        internal fun urls(clip: PrayerAudio): List<String> = when (clip) {
            PrayerAudio.FATIHAH -> (1..7).map { "https://everyayah.com/data/Husary_128kbps/001%03d.mp3".format(java.util.Locale.ROOT, it) }
            // The lesson includes basmalah before Al-Ikhlas; play it explicitly once.
            PrayerAudio.IKHLAS -> listOf("https://everyayah.com/data/Husary_128kbps/001001.mp3") +
                (1..4).map { "https://everyayah.com/data/Husary_128kbps/112%03d.mp3".format(java.util.Locale.ROOT, it) }
            else -> listOf("https://www.hisnmuslim.com/audio/ar/${when (clip) {
                PrayerAudio.RUKU -> 33
                PrayerAudio.SUJUD -> 41
                PrayerAudio.SITTING -> 48
                PrayerAudio.TASHAHHUD -> 52
                PrayerAudio.SALAWAT -> 53
                else -> error("Quran playlists handled above")
            }}.mp3")
        }
    }
}
