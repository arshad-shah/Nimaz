package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Drive Media3's public callbacks to distinguish natural completion from interrupted audio. */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class QaidaPlaybackCallbacksTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val player = mockk<ExoPlayer>(relaxed = true)
    private val store = mockk<QaidaLessonAudioStore>()
    private val listener = slot<Player.Listener>()
    private lateinit var manager: QaidaAudioManager

    @Before fun setUp() {
        mockkConstructor(ExoPlayer.Builder::class)
        every { anyConstructed<ExoPlayer.Builder>().build() } returns player
        every { player.addListener(capture(listener)) } just Runs
        every { player.hasNextMediaItem() } returns false
        every { player.playerError } returns null
        every { player.currentMediaItemIndex } returns 0
        every { store.resolve(any()) } answers { File(context.cacheDir, "${firstArg<String>()}.mp3") }
        manager = QaidaAudioManager(context, store)
    }

    @After fun tearDown() {
        manager.release()
        unmockkConstructor(ExoPlayer.Builder::class)
    }

    @Test fun `natural transitions credit the finished key and final end clears playback`() = runTest {
        manager.completions.test {
            manager.playSequence(listOf("alif", "baa"))
            listener.captured.onPlaybackStateChanged(Player.STATE_READY)
            listener.captured.onIsPlayingChanged(true)
            assertThat(manager.state.value.isLoading).isFalse()
            assertThat(manager.state.value.isPlaying).isTrue()
            listener.captured.onMediaItemTransition(
                MediaItem.Builder().setMediaId("baa").build(), Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            )
            assertThat(awaitItem()).isEqualTo("alif")
            assertThat(manager.state.value.currentKey).isEqualTo("baa")
            listener.captured.onPlaybackStateChanged(Player.STATE_ENDED)
            assertThat(awaitItem()).isEqualTo("baa")
            assertThat(manager.state.value.currentKey).isNull()
            assertThat(manager.state.value.isPlaying).isFalse()
            listener.captured.onPlaybackStateChanged(Player.STATE_ENDED)
            expectNoEvents()
        }
    }

    @Test fun `seek stop and intermediate end never award completion`() = runTest {
        manager.completions.test {
            manager.playSequence(listOf("alif", "baa"))
            every { player.hasNextMediaItem() } returns true
            listener.captured.onPlaybackStateChanged(Player.STATE_ENDED)
            assertThat(manager.state.value.currentKey).isEqualTo("alif")
            listener.captured.onMediaItemTransition(
                MediaItem.Builder().setMediaId("baa").build(), Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
            )
            manager.stop()
            listener.captured.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            listener.captured.onPlaybackStateChanged(Player.STATE_BUFFERING)
            listener.captured.onIsPlayingChanged(false)
            assertThat(manager.state.value).isEqualTo(QaidaAudioState())
            expectNoEvents()
        }
    }

    @Test fun `missing media ids fall back to the queue and invalid indices leave current key alone`() {
        manager.playSequence(listOf("alif", "baa"))
        every { player.currentMediaItemIndex } returns 1
        listener.captured.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        assertThat(manager.state.value.currentKey).isEqualTo("baa")
        every { player.currentMediaItemIndex } returns 0
        listener.captured.onMediaItemTransition(MediaItem.EMPTY, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        assertThat(manager.state.value.currentKey).isEqualTo("alif")
        every { player.currentMediaItemIndex } returns 99
        listener.captured.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        assertThat(manager.state.value.currentKey).isEqualTo("alif")
    }

    @Test fun `idle without error preserves state and player errors use a safe fallback`() {
        manager.play("alif")
        listener.captured.onPlaybackStateChanged(Player.STATE_IDLE)
        assertThat(manager.state.value.error).isNull()
        for (message in listOf("broken clip", null)) {
            every { player.playerError } returns PlaybackException(message, null, PlaybackException.ERROR_CODE_UNSPECIFIED)
            listener.captured.onPlaybackStateChanged(Player.STATE_IDLE)
            assertThat(manager.state.value.error).isEqualTo(message ?: "Playback error")
            assertThat(manager.state.value.isLoading).isFalse()
            assertThat(manager.state.value.isPlaying).isFalse()
        }
    }

    @Test fun `slow speed applies before creation during playback and after release`() {
        manager.setSlow(true)
        manager.play("alif")
        verify { player.setPlaybackSpeed(0.75f) }
        manager.setSlow(false)
        verify { player.setPlaybackSpeed(1f) }
        manager.release()
        manager.setSlow(true)
        manager.play("baa")
        verify(exactly = 2) { player.setPlaybackSpeed(0.75f) }
    }
}
