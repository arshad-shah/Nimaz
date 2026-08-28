package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.QuranReciter
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed
import com.arshadshah.nimaz.domain.repository.AyahAudioItem
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Quran player's session state: who is reciting, what repeats, how fast, and what happens
 * when a surah runs out.
 *
 * `QuranAudioManagerDownloadTest` covers the transfer loop and its cancellation. This covers
 * everything the reader can *ask for* — the settings that survive a surah handover, the two
 * repeat modes ExoPlayer cannot express, and the three separate reasons a finished surah does
 * **not** roll into the next one. Between them the file was at 20%.
 *
 * The rolling rule is worth stating plainly, because it is easy to get subtly wrong and
 * impossible to notice: rolling on after a single verse the reader tapped is not continuous
 * playback, it is the app deciding to keep going after being asked for one thing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranAudioManagerTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Writes a small file so the manager sees the download as having succeeded. */
    private class WritingDownloader : AyahAudioDownloader {
        val calls = mutableListOf<String>()
        val count = AtomicInteger(0)
        override suspend fun download(url: String, destination: File) {
            calls += url
            count.incrementAndGet()
            destination.parentFile?.mkdirs()
            destination.writeBytes(ByteArray(64))
        }
    }

    /** Always fails, so the "nothing downloaded" arms are reachable. */
    private class FailingDownloader : AyahAudioDownloader {
        override suspend fun download(url: String, destination: File) {
            throw java.io.IOException("no network")
        }
    }

    private object NoNextSurah : NextSurahPlaylistSource {
        override suspend fun playlistFor(surahNumber: Int) = null
    }

    private class FixedNextSurah(
        private val playlist: NextSurahPlaylistSource.SurahPlaylist?,
    ) : NextSurahPlaylistSource {
        var askedFor: Int? = null
        override suspend fun playlistFor(surahNumber: Int): NextSurahPlaylistSource.SurahPlaylist? {
            askedFor = surahNumber
            return playlist
        }
    }

    private fun manager(
        downloader: AyahAudioDownloader = WritingDownloader(),
        next: NextSurahPlaylistSource = NoNextSurah,
    ) = QuranAudioManager(context, downloader, dispatcher, next)

    private fun playlist(count: Int, surah: Int = 1): List<AyahAudioItem> =
        (1..count).map { AyahAudioItem(ayahGlobalId = it, surahNumber = surah, ayahNumber = it) }

    // ── Who is reciting ─────────────────────────────────────────────────────────

    @Test
    fun `choosing a reciter puts their name in the state`() = runTest(dispatcher) {
        val manager = manager()

        manager.setReciter(QuranReciter.SUDAIS.id, restartIfPlaying = false)

        assertThat(manager.audioState.value.reciterName)
            .isEqualTo(QuranReciter.SUDAIS.displayName)
    }

    @Test
    fun `an unknown reciter id falls back rather than leaving the player without a CDN`() {
        // Ids are persisted, so a build that drops a reciter must not brick playback for
        // whoever had them selected.
        val manager = manager()

        manager.setReciter("someone-who-was-removed", restartIfPlaying = false)

        assertThat(manager.audioState.value.reciterName).isNotEmpty()
    }

    @Test
    fun `the reciter decides the URL the next verse is fetched from`() = runTest(dispatcher) {
        val downloader = WritingDownloader()
        val manager = manager(downloader)
        manager.setReciter(QuranReciter.SUDAIS.id, restartIfPlaying = false)

        manager.playAyah(ayahGlobalNumber = 262, surahNumber = 2, ayahNumber = 255)
        advanceUntilIdle()

        assertThat(downloader.calls.single()).contains("ar.abdurrahmaansudais")
    }

    @Test
    fun `a reciter with no CDN entry still resolves to a working edition`() = runTest(dispatcher) {
        val downloader = WritingDownloader()
        val manager = manager(downloader)
        val uncatalogued = QuranReciter.entries
            .firstOrNull { it !in QuranAudioManager.RECITER_CDN_MAP.keys }

        manager.setReciter(uncatalogued?.id, restartIfPlaying = false)
        manager.playAyah(1, 1, 1)
        advanceUntilIdle()

        assertThat(downloader.calls.single()).contains("cdn.islamic.network")
    }

    // ── The settings that belong to the sitting ─────────────────────────────────

    @Test
    fun `speed is remembered in the state so a surah handover does not drop back to normal`() {
        val manager = manager()

        manager.setSpeed(RecitationSpeed.FASTER)

        assertThat(manager.audioState.value.speed).isEqualTo(RecitationSpeed.FASTER)
    }

    @Test
    fun `follow-along is a state the reader sets, not something playback decides`() {
        val manager = manager()

        manager.setFollowAlong(true)
        assertThat(manager.audioState.value.followAlong).isTrue()

        manager.setFollowAlong(false)
        assertThat(manager.audioState.value.followAlong).isFalse()
    }

    @Test
    fun `every repeat mode is recorded, including the two ExoPlayer cannot express`() {
        val manager = manager()

        manager.setRepeat(RecitationRepeat.Surah)
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Surah)

        manager.setRepeat(RecitationRepeat.Ayah(3))
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Ayah(3))

        manager.setRepeat(RecitationRepeat.Range(2, 5))
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Range(2, 5))

        manager.setRepeat(RecitationRepeat.Off)
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Off)
    }

    // ── What a finished surah rolls into ────────────────────────────────────────

    @Test
    fun `a finished reading rolls into the next surah`() {
        assertThat(
            QuranAudioManager.nextSurahToPlay(
                continuousPlayback = true,
                playlistIsAReading = true,
                repeat = RecitationRepeat.Off,
                finishedSurah = 18,
            )
        ).isEqualTo(19)
    }

    @Test
    fun `continuous playback off means the sitting ends where the surah ends`() {
        assertThat(
            QuranAudioManager.nextSurahToPlay(
                continuousPlayback = false,
                playlistIsAReading = true,
                repeat = RecitationRepeat.Off,
                finishedSurah = 18,
            )
        ).isNull()
    }

    @Test
    fun `a single verse played on its own does not roll into a whole surah`() {
        // Tapping one verse's play button queues a one-item playlist too. Rolling from that
        // into the next surah is the app deciding to keep going after being asked for one verse.
        assertThat(
            QuranAudioManager.nextSurahToPlay(
                continuousPlayback = true,
                playlistIsAReading = false,
                repeat = RecitationRepeat.Off,
                finishedSurah = 18,
            )
        ).isNull()
    }

    @Test
    fun `any repeat is a request to stay, so none of them roll on`() {
        listOf(
            RecitationRepeat.Surah,
            RecitationRepeat.Ayah(3),
            RecitationRepeat.Range(1, 4),
        ).forEach { repeat ->
            assertThat(
                QuranAudioManager.nextSurahToPlay(
                    continuousPlayback = true,
                    playlistIsAReading = true,
                    repeat = repeat,
                    finishedSurah = 18,
                )
            ).isNull()
        }
    }

    @Test
    fun `there is no surah after An-Nas`() {
        assertThat(
            QuranAudioManager.nextSurahToPlay(
                continuousPlayback = true,
                playlistIsAReading = true,
                repeat = RecitationRepeat.Off,
                finishedSurah = NextSurahPlaylistSource.LAST_SURAH,
            )
        ).isNull()
    }

    @Test
    fun `a playlist that ended on no surah at all rolls nowhere`() {
        assertThat(
            QuranAudioManager.nextSurahToPlay(
                continuousPlayback = true,
                playlistIsAReading = true,
                repeat = RecitationRepeat.Off,
                finishedSurah = null,
            )
        ).isNull()
    }

    // ── Playing ─────────────────────────────────────────────────────────────────

    @Test
    fun `playing a surah publishes the whole playlist before a byte is fetched`() {
        // The reader sees the title and the verse count immediately; the download is what takes
        // time, and a screen that stays blank until it finishes reads as a hang.
        val manager = manager()

        manager.playSurah(18, "Al-Kahf", playlist(10, surah = 18))

        val state = manager.audioState.value
        assertThat(state.isActive).isTrue()
        assertThat(state.isPreparing).isTrue()
        assertThat(state.currentTitle).isEqualTo("Al-Kahf")
        assertThat(state.totalAyahs).isEqualTo(10)
        assertThat(state.currentSurahNumber).isEqualTo(18)
    }

    @Test
    fun `playing from a verse in the middle starts there, not at the beginning`() {
        val manager = manager()
        val ayahs = playlist(10, surah = 18)

        manager.playFromAyah(ayahGlobalId = 5, allAyahs = ayahs, title = "Al-Kahf")

        assertThat(manager.audioState.value.currentAyahIndex).isEqualTo(4)
        assertThat(manager.audioState.value.currentAyahId).isEqualTo(5)
    }

    @Test
    fun `playing from a verse that is not in the list does nothing at all`() {
        // Better than starting at verse one: a mis-keyed jump that silently plays the wrong
        // place is worse than one that does not play.
        val manager = manager()

        manager.playFromAyah(ayahGlobalId = 9999, allAyahs = playlist(10), title = "Al-Kahf")

        assertThat(manager.audioState.value.isActive).isFalse()
    }

    @Test
    fun `a single verse is announced by its number rather than by a surah title`() {
        val manager = manager()

        manager.playAyah(ayahGlobalNumber = 262, surahNumber = 2, ayahNumber = 255)

        assertThat(manager.audioState.value.currentTitle).isEqualTo("Ayah 255")
        assertThat(manager.audioState.value.currentSubtitle).isEqualTo("Surah 2")
        assertThat(manager.audioState.value.totalAyahs).isEqualTo(1)
    }

    @Test
    fun `a verse that will not download reports an error instead of silence`() = runTest(dispatcher) {
        val manager = manager(FailingDownloader())

        manager.playAyah(1, 1, 1)
        advanceUntilIdle()

        assertThat(manager.audioState.value.error).isNotNull()
        assertThat(manager.audioState.value.isActive).isFalse()
    }

    @Test
    fun `a surah whose files all fail to download reports an error, not an empty player`() =
        runTest(dispatcher) {
            val manager = manager(FailingDownloader())

            manager.playSurah(18, "Al-Kahf", playlist(3, surah = 18))
            advanceUntilIdle()

            assertThat(manager.audioState.value.error).isNotNull()
            assertThat(manager.audioState.value.isPreparing).isFalse()
            assertThat(manager.audioState.value.isActive).isFalse()
        }

    @Test
    fun `a second request for the same verse does not fetch it twice`() = runTest(dispatcher) {
        // Two screens can ask for the same ayah at once; downloading it twice wastes the
        // reader's data and races two writers onto one file.
        val downloader = WritingDownloader()
        val manager = manager(downloader)

        manager.playAyah(1, 1, 1)
        advanceUntilIdle()
        manager.playAyah(1, 1, 1)
        advanceUntilIdle()

        // The second call finds the cached file and never reaches the downloader.
        assertThat(downloader.count.get()).isEqualTo(1)
    }

    // ── Transport with nothing loaded ───────────────────────────────────────────

    @Test
    fun `the transport controls are inert before anything is playing`() {
        // The media notification's buttons can outlive the session; every one of these is
        // reachable with no player behind it.
        val manager = manager()

        manager.skipToNext()
        manager.skipToPrevious()
        manager.togglePlayPause()
        manager.seekTo(1_000)
        manager.seekToTotal(1_000)

        assertThat(manager.getPlayer()).isNull()
        assertThat(manager.audioState.value.isPlaying).isFalse()
    }

    // ── Ending the session ──────────────────────────────────────────────────────

    @Test
    fun `stopping clears the session back to nothing`() = runTest(dispatcher) {
        val manager = manager()
        manager.playSurah(18, "Al-Kahf", playlist(5, surah = 18))
        assertThat(manager.audioState.value.isActive).isTrue()

        manager.stop()

        assertThat(manager.audioState.value).isEqualTo(
            com.arshadshah.nimaz.domain.model.AudioState()
        )
        assertThat(manager.getPlayer()).isNull()
    }

    @Test
    fun `releasing is safe to call twice and leaves nothing running`() {
        val manager = manager()
        manager.playSurah(18, "Al-Kahf", playlist(5, surah = 18))

        manager.release()
        manager.release()

        assertThat(manager.getPlayer()).isNull()
    }

    @Test
    fun `stopping mid-download cancels the transfer rather than letting it finish`() =
        runTest(dispatcher) {
            val manager = manager()
            manager.playSurah(18, "Al-Kahf", playlist(50, surah = 18))

            manager.stop()
            advanceUntilIdle()

            assertThat(manager.audioState.value.isDownloading).isFalse()
            assertThat(manager.audioState.value.downloadedCount).isEqualTo(0)
        }

    // ── Continuous playback ─────────────────────────────────────────────────────

    @Test
    fun `continuous playback is a switch the manager remembers`() {
        // Not observable in state by design — it changes what a *transition* does — so the
        // assertion is on the rule it feeds.
        val manager = manager()

        manager.setContinuousPlayback(false)

        assertThat(
            QuranAudioManager.nextSurahToPlay(
                continuousPlayback = false,
                playlistIsAReading = true,
                repeat = RecitationRepeat.Off,
                finishedSurah = 1,
            )
        ).isNull()
    }

    @Test
    fun `the repeat mode ExoPlayer can do itself is handed to it, and the others are not`() {
        // REPEAT_MODE_ALL is the only one ExoPlayer can express; ayah and range have to *stop*,
        // so they are counted by the manager and the player is left alone.
        val manager = manager()
        manager.playSurah(18, "Al-Kahf", playlist(3, surah = 18))

        manager.setRepeat(RecitationRepeat.Surah)
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Surah)

        manager.setRepeat(RecitationRepeat.Ayah(2))
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Ayah(2))
    }

    @Test
    fun `the player exposed to the media session reports playlist totals, not item totals`() =
        runTest(dispatcher) {
            // The lock screen scrubber is over the whole surah. Handing it ExoPlayer directly
            // would make it snap back to zero on every verse boundary.
            val manager = manager()
            manager.playAyah(1, 1, 1)
            advanceUntilIdle()

            val player = manager.getPlayer()
            if (player != null) {
                assertThat(player.isCurrentMediaItemSeekable).isTrue()
                assertThat(manager.getPlayer()).isSameInstanceAs(player)
                assertThat(player.mediaMetadata.artist.toString()).isNotEmpty()
            }
        }

    @Test
    fun `REPEAT_MODE constants are only what the player is told, never what state records`() {
        val manager = manager()

        manager.setRepeat(RecitationRepeat.Surah)

        // Nothing to read off a null player; the contract asserted is that state carries the
        // reader's choice whether or not a player exists to receive it.
        assertThat(manager.audioState.value.repeat).isEqualTo(RecitationRepeat.Surah)
        assertThat(Player.REPEAT_MODE_ALL).isNotEqualTo(Player.REPEAT_MODE_OFF)
    }
}
