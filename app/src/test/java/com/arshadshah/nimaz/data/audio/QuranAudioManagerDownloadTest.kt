package com.arshadshah.nimaz.data.audio

import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.domain.repository.AyahAudioItem
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * The download path, and specifically that cancelling it cancels it.
 *
 * This is the regression test for the defect fixed in #468. The per-file jobs were started with
 * `scope.launch` inside a `withContext`, which made them **siblings** of `downloadJob` rather
 * than children — so `downloadJob.cancel()` stopped the waiting and left every transfer running,
 * still writing `downloadedCount` and `downloadProgress` into the shared `AudioState`. Switching
 * surah mid-download let the old surah's progress overwrite the new one's and then jump
 * backwards.
 *
 * Nothing could have caught it. `QuranAudioManager` had no tests, and it could not have any while
 * the download reached straight for `URL.openConnection()` — hence [AyahAudioDownloader].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class QuranAudioManagerDownloadTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // The manager's own scope is Dispatchers.Main; the test dispatcher has to own it or the
        // state updates land on a thread the test cannot advance.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Records every call and never returns until released — a stand-in for a slow connection. */
    private class BlockingDownloader : AyahAudioDownloader {
        val started = AtomicInteger(0)
        val completed = AtomicInteger(0)
        private val gate = CompletableDeferred<Unit>()

        override suspend fun download(url: String, destination: File) {
            started.incrementAndGet()
            gate.await()
            destination.writeBytes(ByteArray(16))
            completed.incrementAndGet()
        }

        fun release() = gate.complete(Unit)
    }

    /** Completes after a delay, so progress can be observed advancing. */
    private class SlowDownloader(private val millis: Long) : AyahAudioDownloader {
        val completed = AtomicInteger(0)

        override suspend fun download(url: String, destination: File) {
            delay(millis)
            destination.writeBytes(ByteArray(16))
            completed.incrementAndGet()
        }
    }

    /** Nothing follows the surah under test — the download path never reaches the advance. */
    private object NoNextSurah : NextSurahPlaylistSource {
        override suspend fun playlistFor(surahNumber: Int): NextSurahPlaylistSource.SurahPlaylist? =
            null
    }

    private fun playlist(count: Int): List<AyahAudioItem> =
        (1..count).map { AyahAudioItem(ayahGlobalId = it, surahNumber = 1, ayahNumber = it) }

    @Test
    fun `cancelling the download stops further progress writes`() = runTest(dispatcher) {
        val downloader = SlowDownloader(millis = 1_000)
        val manager = QuranAudioManager(context, downloader, dispatcher, NoNextSurah)

        val job = launch { manager.downloadAllAyahs(playlist(20)) }

        // Let a few files land so progress is genuinely in flight, not merely queued.
        advanceTimeBy(2_500)
        val progressAtCancel = manager.audioState.value.downloadedCount
        assertThat(progressAtCancel).isGreaterThan(0)

        job.cancel()
        advanceUntilIdle()

        // The point of the fix: no write may land after the cancel. Before it, the in-flight
        // transfers carried on and kept incrementing this.
        assertThat(manager.audioState.value.downloadedCount).isEqualTo(progressAtCancel)
    }

    @Test
    fun `cancelling the download stops the transfers themselves`() = runTest(dispatcher) {
        val downloader = SlowDownloader(millis = 1_000)
        val manager = QuranAudioManager(context, downloader, dispatcher, NoNextSurah)

        val job = launch { manager.downloadAllAyahs(playlist(20)) }
        advanceTimeBy(2_500)
        val completedAtCancel = downloader.completed.get()

        job.cancel()
        advanceUntilIdle()

        // Not just "progress stopped being reported" — the work stopped. A sibling job would
        // have run to completion here and only its reporting would have looked cancelled.
        assertThat(downloader.completed.get()).isEqualTo(completedAtCancel)
    }

    /**
     * Five at a time, not five-then-wait-for-the-slowest. The old shape was `chunked(5)` followed
     * by `join()`, so one slow file idled four connections until it finished.
     */
    @Test
    fun `at most five downloads are in flight at once`() = runTest(dispatcher) {
        val downloader = BlockingDownloader()
        val manager = QuranAudioManager(context, downloader, dispatcher, NoNextSurah)

        val job = launch { manager.downloadAllAyahs(playlist(20)) }
        advanceUntilIdle()

        assertThat(downloader.started.get()).isEqualTo(5)

        downloader.release()
        job.cancel()
    }

    @Test
    fun `an empty playlist reports nothing to download`() = runTest(dispatcher) {
        val manager = QuranAudioManager(context, SlowDownloader(millis = 0), dispatcher, NoNextSurah)

        manager.downloadAllAyahs(emptyList())
        advanceUntilIdle()

        assertThat(manager.audioState.value.isDownloading).isFalse()
    }

    @Test
    fun `a completed download reports full progress`() = runTest(dispatcher) {
        val downloader = SlowDownloader(millis = 10)
        val manager = QuranAudioManager(context, downloader, dispatcher, NoNextSurah)

        manager.downloadAllAyahs(playlist(3))
        advanceUntilIdle()

        assertThat(downloader.completed.get()).isEqualTo(3)
        assertThat(manager.audioState.value.isDownloading).isFalse()
        assertThat(manager.audioState.value.downloadProgress).isEqualTo(1f)
    }
}
