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

/**
 * The tap-to-hear engine behind the Qaida reader — its transport and its source resolution.
 *
 * **Source resolution is the part that decides whether a child hears anything.** A key resolves
 * to a bundled `android_asset` clip by default, but a drop-in file under
 * `filesDir/qaida_audio/` wins — that is the whole of the on-demand delivery mode, implemented
 * as one `if` and reachable through no setting. Two ways for it to be wrong and neither is
 * visible: ignore the override and every downloaded clip is dead weight; trust `exists()` alone
 * and a **truncated download** (a zero-length file that exists) plays silence forever, with no
 * way for the learner to recover and nothing on screen to say why.
 *
 * **The transport's guards are the rest.** A cell whose `audio_key` the content artifact does
 * not carry must be refused rather than queued — queuing `""` asks the data source for
 * `qaida/audio/.mp3`, which fails asynchronously and surfaces as a *playback error* on a tap
 * that should simply have done nothing. A line filters the same way, and a line that filters
 * down to one clip is played as a single tap rather than as a one-item playlist, because the
 * two report completions differently.
 *
 * **What is not covered here, and why.** The `Player.Listener` — `STATE_ENDED`, the
 * `MEDIA_ITEM_TRANSITION_REASON_AUTO` arm that decides which key was *heard*, and the error
 * arm — needs the player to actually decode something, and Robolectric has no media pipeline:
 * nothing ever leaves `STATE_IDLE`, so no callback fires. Reaching those bodies would mean
 * reflecting into `ExoPlayer`'s private listener set, which is a shape that breaks on a media3
 * bump and fails far from its cause. Its *consumer* is covered instead — `QaidaReaderViewModel`
 * credits a cell from `completions` and from nowhere else, and
 * `QaidaReaderViewModelTest` drives that flow directly with a fake manager.
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class QaidaAudioManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var manager: QaidaAudioManager

    @Before
    fun setUp() {
        manager = QaidaAudioManager(context)
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
    fun `a key with no downloaded clip resolves to the bundled asset`() {
        manager.play("l1_alif")

        assertThat(queuedUri()).isEqualTo("file:///android_asset/qaida/audio/l1_alif.mp3")
    }

    @Test
    fun `a downloaded clip wins over the bundled one`() {
        val dir = File(context.filesDir, "qaida_audio").apply { mkdirs() }
        File(dir, "l1_downloaded.mp3").writeBytes(ByteArray(64) { 1 })

        manager.play("l1_downloaded")

        assertThat(queuedUri()).contains("qaida_audio")
        assertThat(queuedUri()).doesNotContain("android_asset")
    }

    @Test
    fun `a truncated download is ignored in favour of the asset`() {
        // A zero-length file that exists. Trusting `exists()` alone plays silence forever.
        val dir = File(context.filesDir, "qaida_audio").apply { mkdirs() }
        File(dir, "l1_truncated.mp3").writeBytes(ByteArray(0))

        manager.play("l1_truncated")

        assertThat(queuedUri()).isEqualTo("file:///android_asset/qaida/audio/l1_truncated.mp3")
    }

    @Test
    fun `a replayed key reuses the resolved item rather than rebuilding it`() {
        // Instant replay is the whole point of the engine; the cache is what delivers it.
        manager.play("l1_alif")
        val first = player().getMediaItemAt(0)
        manager.play("l1_baa")
        manager.play("l1_alif")

        assertThat(player().getMediaItemAt(0)).isSameInstanceAs(first)
    }

    @Test
    fun `releasing drops the cache as well as the player`() {
        manager.play("l1_alif")
        val before = player().getMediaItemAt(0)

        manager.release()
        manager.play("l1_alif")

        assertThat(player().getMediaItemAt(0)).isNotSameInstanceAs(before)
        assertThat(queuedUri()).isEqualTo("file:///android_asset/qaida/audio/l1_alif.mp3")
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
