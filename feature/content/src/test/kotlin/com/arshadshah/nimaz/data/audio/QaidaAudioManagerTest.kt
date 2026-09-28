package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import io.mockk.every
import io.mockk.mockk

/** Verified-cache transport; no bundled or unverified file fallback. */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class QaidaAudioManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = mockk<QaidaLessonAudioStore>()
    private lateinit var manager: QaidaAudioManager

    @Before
    fun setUp() {
        every { store.resolve(any()) } answers {
            File(context.cacheDir, "${firstArg<String>()}.mp3").apply { writeBytes(ByteArray(64) { 1 }) }
        }
        manager = QaidaAudioManager(context, store)
    }

    @After
    fun tearDown() {
        manager.release()
    }

    /**
     * The player the manager built, read back off its own private field.
     *
     * The queue is the only place the resolved URI is observable — `resolveUri` and
     * `mediaItemFor` are private and nothing else exposes what a key resolved to. This reaches
     * one field of one class in this module rather than into `ExoPlayer`'s internals, so a
     * media3 bump cannot break it.
     */
    private fun player(): ExoPlayer {
        val field = QaidaAudioManager::class.java.getDeclaredField("player")
        field.isAccessible = true
        return field.get(manager) as ExoPlayer
    }

    private fun queuedUri(index: Int = 0): String =
        player().getMediaItemAt(index).localConfiguration?.uri.toString()

    // ---- source resolution ------------------------------------------------------------

    @Test
    fun `a missing recording reports unavailable without queuing imaginary bundled audio`() {
        every { store.resolve("missing") } returns null
        manager.play("missing")
        assertThat(manager.state.value.error).isEqualTo("audio_not_downloaded")
        assertThat(manager.state.value.currentKey).isNull()
    }

    @Test
    fun `only the verified cache resolver supplies playback files`() {
        manager.play("l1_alif")
        assertThat(queuedUri()).endsWith("l1_alif.mp3")
        assertThat(queuedUri()).doesNotContain("android_asset")
    }

    @Test
    fun `refreshing a lesson changes the file used by the same audio key`() {
        manager.play("l1_alif")
        val replacement = File(context.cacheDir, "replacement.mp3").apply { writeBytes(ByteArray(64)) }
        every { store.resolve("l1_alif") } returns replacement
        manager.play("l1_alif")
        assertThat(queuedUri()).endsWith("replacement.mp3")
    }

    @Test
    fun `a missing clip refuses the whole sequence`() {
        every { store.resolve("missing") } returns null
        manager.playSequence(listOf("l1_alif", "missing"))
        assertThat(manager.state.value.error).isEqualTo("audio_not_downloaded")
    }

    // ---- the transport ----------------------------------------------------------------

    @Test
    fun `playing a token makes it the current key and marks it loading`() {
        manager.play("l1_alif")

        assertThat(manager.state.value.currentKey).isEqualTo("l1_alif")
        assertThat(manager.state.value.isLoading).isTrue()
        assertThat(manager.state.value.error).isNull()
    }

    @Test
    fun `a blank key is refused rather than queued`() {
        manager.play("")

        assertThat(manager.state.value.currentKey).isNull()
        assertThat(manager.state.value.isLoading).isFalse()
    }

    @Test
    fun `a later tap replaces the earlier one`() {
        manager.play("l1_alif")
        manager.play("l1_baa")

        assertThat(manager.state.value.currentKey).isEqualTo("l1_baa")
        assertThat(player().mediaItemCount).isEqualTo(1)
    }

    @Test
    fun `a whole line queues every clip and starts on the first`() {
        manager.playSequence(listOf("l1_alif", "l1_baa", "l1_taa"))

        assertThat(manager.state.value.currentKey).isEqualTo("l1_alif")
        assertThat(player().mediaItemCount).isEqualTo(3)
        assertThat(queuedUri(2)).endsWith("l1_taa.mp3")
    }

    @Test
    fun `a line of one clip is played as a single tap`() {
        // It delegates to `play`, which matters beyond tidiness: a one-item playlist reports
        // its end through a different arm of the listener than a single item does.
        manager.playSequence(listOf("only"))

        assertThat(manager.state.value.currentKey).isEqualTo("only")
        assertThat(player().mediaItemCount).isEqualTo(1)
    }

    @Test
    fun `a line of nothing never builds a player`() {
        manager.playSequence(emptyList())

        assertThat(manager.state.value.currentKey).isNull()
    }

    @Test
    fun `blank keys are dropped from a line rather than queued as silence`() {
        manager.playSequence(listOf("l1_alif", "", "l1_taa"))

        assertThat(player().mediaItemCount).isEqualTo(2)
        assertThat(manager.state.value.currentKey).isEqualTo("l1_alif")
        assertThat(queuedUri(1)).endsWith("l1_taa.mp3")
    }

    @Test
    fun `a line whose keys are all blank is refused outright`() {
        manager.playSequence(listOf("", "   "))

        assertThat(manager.state.value.currentKey).isNull()
    }

    @Test
    fun `a line that filters down to one clip is still played`() {
        // The filter runs before the size check, so `["", "one"]` is a single tap and not a
        // refusal — the arm an early `size` check on the raw list would get wrong.
        manager.playSequence(listOf("", "one"))

        assertThat(manager.state.value.currentKey).isEqualTo("one")
        assertThat(player().mediaItemCount).isEqualTo(1)
    }

    @Test
    fun `stopping returns to idle and clears the queue`() {
        manager.playSequence(listOf("l1_alif", "l1_baa"))

        manager.stop()

        assertThat(manager.state.value).isEqualTo(QaidaAudioState())
        assertThat(player().mediaItemCount).isEqualTo(0)
    }

    @Test
    fun `stopping before anything played is safe`() {
        // `QaidaReaderViewModel.selectLesson` stops audio on every lesson change, including
        // the first — when there is no player at all.
        manager.stop()

        assertThat(manager.state.value).isEqualTo(QaidaAudioState())
    }

    @Test
    fun `releasing twice is safe`() {
        manager.play("l1_alif")

        manager.release()
        manager.release()

        assertThat(manager.state.value).isEqualTo(QaidaAudioState())
    }

    @Test
    fun `playing again after a release builds a fresh player`() {
        manager.play("l1_alif")
        manager.release()

        manager.play("l1_baa")

        assertThat(manager.state.value.currentKey).isEqualTo("l1_baa")
        assertThat(player().mediaItemCount).isEqualTo(1)
    }
}
