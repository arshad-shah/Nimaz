package com.arshadshah.nimaz.data.audio

import com.arshadshah.nimaz.domain.model.AudioState
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactive playback state for the Qaida tap-to-hear engine.
 *
 * Deliberately tiny compared to [AudioState]: these are sub-second single-clip
 * taps, so there is no playlist position, duration, seek or notification state —
 * just which token is sounding and whether it is loading/playing.
 */
data class QaidaAudioState(
    /** The `audio_key` of the clip currently loaded/playing, or null when idle. */
    val currentKey: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Tap-to-hear playback for single Qaida tokens (epic #171, sub-issue F of #177).
 *
 * A stripped-down sibling of [QuranAudioManager]: it reuses the same Media3 /
 * ExoPlayer foundation but drops everything the Qaida does not need — no
 * playlist position tracking, no foreground service, no notification/MediaSession,
 * no CDN streaming. The clips are sub-second taps, not background listening.
 *
 * Only complete, content-matched lessons verified by QaidaLessonAudioStore are playable.
 */
@UnstableApi
@Singleton
class QaidaAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lessonAudio: QaidaLessonAudioStore = QaidaLessonAudioStore(context),
) {
    private var player: ExoPlayer? = null
    private var playbackSpeed = 1f

    private val _state = MutableStateFlow(QaidaAudioState())
    val state: StateFlow<QaidaAudioState> = _state.asStateFlow()

    // Resolved MediaItems keyed by audio_key, so repeated taps never rebuild
    // them — the whole point is instant replay.

    // The ordered keys currently queued, so media-item transitions can report
    // the right currentKey when playing a whole line via playSequence().
    private var sequenceKeys: List<String> = emptyList()

    private val _completions = MutableSharedFlow<String>(extraBufferCapacity = 32)

    /**
     * Keys whose clip **played to its end**, as it happens.
     *
     * `state.currentKey` cannot answer this: it goes null both when a clip finishes and when
     * [stop] cuts it off, and the difference is the whole question when playback is what
     * credits a learner's progress. Only a natural media-item transition
     * (`MEDIA_ITEM_TRANSITION_REASON_AUTO`) and a natural `STATE_ENDED` emit here — a tap
     * elsewhere, a lesson change or [stop] emit nothing.
     */
    val completions: SharedFlow<String> = _completions.asSharedFlow()

    @OptIn(UnstableApi::class)
    private fun getOrCreatePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            player = newPlayer
            newPlayer.setPlaybackSpeed(playbackSpeed)
            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> _state.update { it.copy(isLoading = false) }
                        Player.STATE_ENDED -> {
                            // Whole clip / sequence finished playing.
                            if (!newPlayer.hasNextMediaItem()) {
                                // The last item ran to its end, so it is heard. Emitted before
                                // clearing `currentKey`, which is the only place it is still
                                // known.
                                _state.value.currentKey?.let(_completions::tryEmit)
                                _state.update {
                                    it.copy(currentKey = null, isPlaying = false, isLoading = false)
                                }
                            }
                        }

                        Player.STATE_IDLE -> {
                            newPlayer.playerError?.let { err ->
                                _state.update {
                                    it.copy(
                                        isPlaying = false,
                                        isLoading = false,
                                        error = err.message ?: "Playback error"
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // AUTO means the previous item reached its end rather than being replaced,
                    // so that key — not this one — is what the learner just heard.
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        _state.value.currentKey?.let(_completions::tryEmit)
                    }
                    val key = mediaItem?.mediaId?.takeIf { it.isNotEmpty() }
                        ?: sequenceKeys.getOrNull(newPlayer.currentMediaItemIndex)
                    if (key != null) {
                        _state.update { it.copy(currentKey = key) }
                    }
                }
            })
        }
    }

    /**
     * Play the clip for a single token. Replaces anything currently playing, so
     * rapid taps always cancel cleanly and the latest tap wins.
     */
    fun play(audioKey: String) {
        if (audioKey.isBlank()) return
        sequenceKeys = listOf(audioKey)
        val item = mediaItemFor(audioKey) ?: run {
            stop()
            _state.value = QaidaAudioState(error = "audio_not_downloaded")
            return
        }
        val p = getOrCreatePlayer()
        _state.update { it.copy(currentKey = audioKey, isLoading = true, error = null) }
        p.setMediaItem(item)
        p.prepare()
        p.play()
    }

    /**
     * Play several tokens back-to-back — the "play whole line" affordance.
     * [currentKey] advances as each clip starts.
     */
    fun playSequence(keys: List<String>) {
        val clean = keys.filter { it.isNotBlank() }
        when (clean.size) {
            0 -> return
            1 -> {
                play(clean.first())
                return
            }
        }
        sequenceKeys = clean
        val items = clean.map { key ->
            mediaItemFor(key) ?: run {
                stop()
                _state.value = QaidaAudioState(error = "audio_not_downloaded")
                return
            }
        }
        val p = getOrCreatePlayer()
        _state.update { it.copy(currentKey = clean.first(), isLoading = true, error = null) }
        p.setMediaItems(items)
        p.prepare()
        p.play()
    }

    /** Stop playback and reset to idle, keeping the resolved-item cache warm. */
    fun stop() {
        sequenceKeys = emptyList()
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
        _state.update { QaidaAudioState() }
    }

    /** Fully tear down the player and caches (e.g. on process teardown). */
    fun release() {
        sequenceKeys = emptyList()
        player?.release()
        player = null
        _state.update { QaidaAudioState() }
    }

    /** Pitch-preserving slower playback; repeats use the same reviewed source clip. */
    fun setSlow(slow: Boolean) {
        playbackSpeed = if (slow) 0.75f else 1f
        player?.setPlaybackSpeed(playbackSpeed)
    }

    private fun mediaItemFor(audioKey: String): MediaItem? {
        val file = lessonAudio.resolve(audioKey) ?: return null
        // Resolve each time: a refreshed lesson can replace the clip for the same key.
        return MediaItem.Builder().setMediaId(audioKey).setUri(file.toURI().toString()).build()
    }
}
