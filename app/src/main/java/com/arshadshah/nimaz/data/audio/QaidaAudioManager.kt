package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.ConcurrentHashMap
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
 * Source resolution honours sub-issue B's delivery decision: the 2 MB clip pack
 * ships **bundled** in `assets/qaida/audio/`, so a key resolves directly to an
 * `android_asset` URI (which Media3's `DefaultDataSource` routes to its
 * `AssetDataSource`). A downloaded/drop-in override under
 * `filesDir/qaida_audio/` is honoured first, so the same engine also serves the
 * on-demand delivery mode without code changes — and works fully offline once a
 * clip is on disk either way.
 *
 * A single reused [ExoPlayer] means rapid taps cancel/replace cleanly: each
 * [play] simply swaps the current [MediaItem], and resolved items are cached so
 * repeat taps are instant.
 */
@UnstableApi
@Singleton
class QaidaAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null

    private val _state = MutableStateFlow(QaidaAudioState())
    val state: StateFlow<QaidaAudioState> = _state.asStateFlow()

    // Resolved MediaItems keyed by audio_key, so repeated taps never rebuild
    // them — the whole point is instant replay.
    private val mediaItemCache = ConcurrentHashMap<String, MediaItem>()

    // The ordered keys currently queued, so media-item transitions can report
    // the right currentKey when playing a whole line via playSequence().
    private var sequenceKeys: List<String> = emptyList()

    @OptIn(UnstableApi::class)
    private fun getOrCreatePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            player = newPlayer
            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> _state.update { it.copy(isLoading = false) }
                        Player.STATE_ENDED -> {
                            // Whole clip / sequence finished playing.
                            if (!newPlayer.hasNextMediaItem()) {
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
        val item = mediaItemFor(audioKey)
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
        val items = clean.map { mediaItemFor(it) }
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
        mediaItemCache.clear()
        _state.update { QaidaAudioState() }
    }

    private fun mediaItemFor(audioKey: String): MediaItem =
        mediaItemCache.getOrPut(audioKey) {
            MediaItem.Builder()
                .setMediaId(audioKey)
                .setUri(resolveUri(audioKey))
                .build()
        }

    /**
     * Resolve a token's `audio_key` to a playable URI. A downloaded/drop-in clip
     * under `filesDir/qaida_audio/` wins (the on-demand delivery mode), otherwise
     * the bundled asset is used (the shipped default). Both are local, so either
     * way playback is offline and instant.
     */
    private fun resolveUri(audioKey: String): String {
        val downloaded = File(File(context.filesDir, DOWNLOAD_DIR), "$audioKey.mp3")
        return if (downloaded.exists() && downloaded.length() > 0) {
            downloaded.toURI().toString()
        } else {
            "$ASSET_AUDIO_URI_PREFIX$audioKey.mp3"
        }
    }

    companion object {
        private const val ASSET_AUDIO_URI_PREFIX = "file:///android_asset/qaida/audio/"
        private const val DOWNLOAD_DIR = "qaida_audio"
    }
}
