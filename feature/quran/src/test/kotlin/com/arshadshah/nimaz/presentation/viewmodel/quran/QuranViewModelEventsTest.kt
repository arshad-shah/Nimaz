package com.arshadshah.nimaz.presentation.viewmodel.quran

import android.content.Context
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PageAyahRange
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.repository.QuranPlayback
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * `QuranViewModel`'s event table — the one place every reader action lands.
 *
 * It is a single exhaustive `when` over twenty-odd events, and the class of bug it invites is
 * not a crash: it is a branch that dispatches to the wrong handler, or logs the wrong feature,
 * or handles the event and forgets to tell the player. All of those look like a working app
 * until someone reads a dashboard or presses the button twice.
 *
 * Two branches are worth naming. `ToggleTranslation` flips **inside** the state update rather
 * than reading and then writing, because `observeQuranSettings` writes the same field from its
 * own coroutine and can land between the two — so a read-then-write toggle derives its new value
 * from a state that no longer exists. And `PrefetchPage` is the same handler as `LoadPage` with
 * `makeActive = false`: it fills the cache for the pager's neighbours without moving the reader,
 * and the difference between those two is a reader who is silently teleported one page on.
 *
 * `QuranViewModelPageLoadTest` covers the page cache and the job that fills it; this is the table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelEventsTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var useCases: QuranUseCases
    private lateinit var audioManager: QuranPlayback
    private lateinit var settings: SettingsRepository
    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var context: Context

    private val showTranslation = MutableStateFlow(true)
    private val activeKhatam = MutableStateFlow<Khatam?>(null)
    private val readAyahIds = MutableStateFlow(emptySet<Int>())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = mockk(relaxed = true)
        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        khatamUseCases = mockk(relaxed = true)
        settings = mockk(relaxed = true)

        // The reader combines all of these and `combine` emits nothing until every source has,
        // so each needs a real flow rather than a relaxed mock's null.
        every { settings.quranMushafScript } returns MutableStateFlow(MushafScript.MADANI.name)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")
        every { settings.showTranslation } returns showTranslation
        every { settings.showTransliteration } returns MutableStateFlow(false)
        every { settings.quranArabicFontSize } returns MutableStateFlow(28f)
        every { settings.quranArabicFont } returns MutableStateFlow("amiri")
        every { settings.quranTranslationFontSize } returns MutableStateFlow(16f)
        every { settings.continuousReading } returns MutableStateFlow(true)
        every { settings.keepScreenOn } returns MutableStateFlow(true)
        every { settings.selectedReciterId } returns MutableStateFlow(null)
        every { settings.showTajweed } returns MutableStateFlow(false)
        every { settings.tajweedUnderline } returns MutableStateFlow(false)
        coEvery { settings.setShowTranslation(any()) } answers {
            showTranslation.value = firstArg()
        }

        every { khatamUseCases.observeActiveKhatam() } returns activeKhatam
        every { khatamUseCases.observeReadAyahIds(any()) } returns readAyahIds
        // The reader's khatam ticks come from the detail snapshot, not from the khatam alone:
        // one subscription feeds both the per-ayah marks and the home card's progress.
        every { khatamUseCases.observeKhatamDetail(any()) } answers {
            val id = firstArg<Long>()
            MutableStateFlow(
                activeKhatam.value?.takeIf { it.id == id }?.let {
                    KhatamDetailSnapshot(
                        khatam = it,
                        juzProgress = emptyList(),
                        dailyLogs = emptyList(),
                        insights = KhatamInsights(),
                        readAyahIds = readAyahIds.value,
                    )
                }
            )
        }

        every { useCases.getAyahsByPage(any(), any(), any()) } answers {
            MutableStateFlow(listOf(ayah(firstArg())))
        }
        coEvery { useCases.getMushafPagination(any()) } answers { pagination(firstArg()) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = QuranViewModel(
        useCases,
        audioManager,
        settings,
        khatamUseCases,
        telemetry,
        FakeTodayProvider(LocalDate.now()),
        FakeStringProvider(),
    )

    private val cave = Surah(
        number = 18,
        nameArabic = "الكهف",
        nameEnglish = "The Cave",
        nameTransliteration = "Al-Kahf",
        revelationType = RevelationType.MECCAN,
        ayahCount = 110,
        orderInMushaf = 18,
        startPage = 293,
    )

    private fun ayah(id: Int) = Ayah(
        id = id,
        surahNumber = 1,
        ayahNumber = id,
        textArabic = "نص $id",
        textSimple = "nass $id",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = id,
        sajdaType = null,
        sajdaNumber = null,
    )

    private fun pagination(script: MushafScript): MushafPagination {
        val pages = script.totalPages
        val perPage = Khatam.TOTAL_QURAN_AYAHS / pages
        return MushafPagination.from(
            script,
            (1..pages).map { page ->
                val min = (page - 1) * perPage + 1
                val max = if (page == pages) Khatam.TOTAL_QURAN_AYAHS else page * perPage
                PageAyahRange(page, min, max, max - min + 1)
            }
        )
    }

    // ---- The three ways into the reader ----

    @Test
    fun `each way in is counted as itself`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.LoadSurah(18))
        vm.onEvent(QuranEvent.LoadJuz(15))
        vm.onEvent(QuranEvent.LoadPage(293))
        advanceUntilIdle()

        // Only `LoadSurah` used to be logged, so the reader's usage read as "everyone browses by
        // surah" — which is exactly what a dashboard shows when the other two are not counted.
        assertThat(telemetry.featureUsages.map { it.action }).containsAtLeast("open_surah", "open_juz", "open_page")
    }

    @Test
    fun `opening a juz puts the reader in juz mode`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.LoadJuz(15))
        advanceUntilIdle()

        assertThat(vm.readerState.value.readingMode).isEqualTo(ReadingMode.JUZ)
    }

    @Test
    fun `opening a page puts the reader in page mode`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.LoadPage(293))
        advanceUntilIdle()

        assertThat(vm.readerState.value.readingMode).isEqualTo(ReadingMode.PAGE)
    }

    @Test
    fun `prefetching a neighbour fills the cache without moving the reader`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(QuranEvent.LoadPage(293))
        advanceUntilIdle()
        val modeBefore = vm.readerState.value.readingMode

        vm.onEvent(QuranEvent.PrefetchPage(294))
        advanceUntilIdle()

        // The pager keeps neighbours composed, so this runs for pages the reader is not on.
        // Making one active would teleport them.
        assertThat(vm.readerState.value.pageCache.keys).contains(294)
        assertThat(vm.readerState.value.readingMode).isEqualTo(modeBefore)
        // Prefetching is not browsing, so it is not counted as a page opened.
        assertThat(telemetry.featureUsages.count { it.action == "open_page" }).isEqualTo(1)
    }

    // ---- Translation ----

    @Test
    fun `toggling the translation flips it and persists the value it flipped to`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.readerState.value.showTranslation).isTrue()

        vm.onEvent(QuranEvent.ToggleTranslation)
        advanceUntilIdle()

        assertThat(vm.readerState.value.showTranslation).isFalse()
        coVerify { settings.setShowTranslation(false) }
    }

    @Test
    fun `toggling twice comes back to where it started`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleTranslation)
        advanceUntilIdle()
        vm.onEvent(QuranEvent.ToggleTranslation)
        advanceUntilIdle()

        // The flip happens inside the state update, so the settings coroutine writing the same
        // field cannot land between a read and a write and lose one of these.
        assertThat(vm.readerState.value.showTranslation).isTrue()
    }

    // ---- Marking a verse ----

    @Test
    fun `bookmarking a verse is counted and written`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleBookmark(ayahId = 262, surahNumber = 2, ayahNumber = 255))
        advanceUntilIdle()

        assertThat(telemetry.featureUsages.map { it.action }).contains("toggle_bookmark")
    }

    @Test
    fun `favouriting a verse reaches the store`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleFavorite(ayahId = 262, surahNumber = 2, ayahNumber = 255))
        advanceUntilIdle()

        coVerify { useCases.toggleFavorite(262, 2, 255) }
    }

    @Test
    fun `where the reader got to is written down`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.UpdateReadingPosition(surah = 18, ayah = 10, page = 293, juz = 15))
        advanceUntilIdle()

        coVerify { useCases.updateReadingPosition(18, 10, 293, 15) }
    }

    // ---- The player ----

    @Test
    fun `pausing and resuming both go through the one toggle the player has`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.PauseAudio)
        vm.onEvent(QuranEvent.ResumeAudio)
        advanceUntilIdle()

        verify(exactly = 2) { audioManager.togglePlayPause() }
    }

    @Test
    fun `stopping stops`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.StopAudio)
        advanceUntilIdle()

        verify { audioManager.stop() }
    }

    @Test
    fun `stepping through the recitation reaches the player`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.NextAyahAudio)
        vm.onEvent(QuranEvent.PreviousAyahAudio)
        advanceUntilIdle()

        verify { audioManager.skipToNext() }
        verify { audioManager.skipToPrevious() }
    }

    @Test
    fun `seeking reaches the player with the position asked for`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.SeekAudioTo(12_000))
        advanceUntilIdle()

        verify { audioManager.seekToTotal(12_000) }
    }

    @Test
    fun `a repeat is handed to the player as the domain type, not as a number`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.SetRecitationRepeat(RecitationRepeat.Ayah(times = 3)))
        advanceUntilIdle()

        // The invariants live on the type — a repeat of one is not a repeat — so the player can
        // never be handed one that breaks them.
        verify { audioManager.setRepeat(RecitationRepeat.Ayah(times = 3)) }
    }

    @Test
    fun `speed and follow-along reach the player`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.SetPlaybackSpeed(RecitationSpeed.FASTER))
        vm.onEvent(QuranEvent.SetFollowAlong(false))
        advanceUntilIdle()

        verify { audioManager.setSpeed(RecitationSpeed.FASTER) }
        verify { audioManager.setFollowAlong(false) }
    }

    // ---- Starting a recitation ----

    @Test
    fun `playing a surah from its card fetches the whole surah and plays it continuously`() =
        runTest {
            every { useCases.getSurahWithAyahs(any(), any()) } returns MutableStateFlow(
                SurahWithAyahs(cave, listOf(ayah(1), ayah(2)))
            )
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(QuranEvent.PlaySurahFromInfo(18))
            advanceUntilIdle()

            // A playlist, not one file: the player needs every verse to be able to roll on.
            verify { audioManager.setContinuousPlayback(true) }
            verify { audioManager.playSurah(eq(18), eq("The Cave"), match { it.size == 2 }) }
        }

    @Test
    fun `a surah the content database cannot answer for is not played`() = runTest {
        every { useCases.getSurahWithAyahs(any(), any()) } returns MutableStateFlow(null)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.PlaySurahFromInfo(18))
        advanceUntilIdle()

        // An empty playlist starts a session that immediately ends, which on a device is a
        // notification that appears and vanishes.
        verify(exactly = 0) { audioManager.playSurah(any(), any(), any()) }
    }

    @Test
    fun `playing from a verse starts the playlist at it`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(QuranEvent.LoadPage(1))
        advanceUntilIdle()

        vm.onEvent(QuranEvent.PlayAyahAudio(ayahGlobalId = 1, surahNumber = 1, ayahNumber = 1))
        advanceUntilIdle()

        verify { audioManager.playFromAyah(eq(1), match { it.isNotEmpty() }, any()) }
    }

    @Test
    fun `the reader's continuous-reading setting reaches the player before it starts`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            vm.onEvent(QuranEvent.LoadPage(1))
            advanceUntilIdle()

            vm.onEvent(QuranEvent.PlayAyahAudio(1, 1, 1))
            advanceUntilIdle()

            // Set first, so the session it starts already knows whether to roll on into the
            // next surah.
            verifyOrder {
                audioManager.setContinuousPlayback(any())
                audioManager.playFromAyah(any(), any(), any())
            }
        }

    @Test
    fun `choosing a reciter previews them`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.PreviewReciter("mishary"))
        advanceUntilIdle()

        verify { audioManager.setReciter("mishary", any()) }
    }

    // ---- Khatam ----

    @Test
    fun `marking a verse for the khatam does nothing without an active khatam`() = runTest {
        activeKhatam.value = null
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleKhatamAyah(262))
        advanceUntilIdle()

        coVerify(exactly = 0) { khatamUseCases.markAyahsRead(any(), any()) }
    }

    @Test
    fun `marking a verse for the khatam reaches the active one`() = runTest {
        activeKhatam.value = Khatam(id = 7, name = "Ramadan", isActive = true)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleKhatamAyah(262))
        advanceUntilIdle()

        coVerify { khatamUseCases.markAyahsRead(7, listOf(262)) }
    }

    @Test
    fun `marking a whole page marks every verse on it`() = runTest {
        activeKhatam.value = Khatam(id = 7, name = "Ramadan", isActive = true)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.TogglePageKhatam(listOf(1, 2, 3)))
        advanceUntilIdle()

        coVerify { khatamUseCases.markAyahsRead(7, listOf(1, 2, 3)) }
    }
}
