@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel

import android.content.Context
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.PageAyahRange
import io.mockk.every
import io.mockk.mockk
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

/**
 * Page-mode loading in [QuranViewModel].
 *
 * The reader's pager keeps the settled page *and* its neighbours composed, so several pages
 * ask for their content in the same frame. These loads used to share the one `contentJob`
 * that the surah/juz loaders use, so each request cancelled the one before it and only the
 * last page requested in a frame ever reached `pageCache` — the losers rendered as a blank
 * Mushaf frame. It showed up on the ayah-flow editions (Madani) only, because the
 * line-accurate IndoPak layouts render from `mushafPageLayoutCache`, which was never
 * loaded through a shared job.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelPageLoadTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var audioManager: QuranAudioManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var context: Context

    private val mushafScript = MutableStateFlow(MushafScript.MADANI.name)
    private val translatorId = MutableStateFlow("sahih_international")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        khatamUseCases = mockk(relaxed = true)
        context = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // The reader combines all of these, and `combine` emits nothing until every source
        // has, so each needs a real flow — defaults chosen to match QuranReaderUiState's, so
        // the first emission is a no-op rather than a settings *change*.
        every { settingsRepository.quranMushafScript } returns mushafScript
        every { settingsRepository.quranTranslatorId } returns translatorId
        every { settingsRepository.showTranslation } returns MutableStateFlow(true)
        every { settingsRepository.showTransliteration } returns MutableStateFlow(false)
        every { settingsRepository.quranArabicFontSize } returns MutableStateFlow(28f)
        every { settingsRepository.quranArabicFont } returns MutableStateFlow("amiri")
        every { settingsRepository.quranTranslationFontSize } returns MutableStateFlow(16f)
        every { settingsRepository.continuousReading } returns MutableStateFlow(true)
        every { settingsRepository.keepScreenOn } returns MutableStateFlow(true)
        every { settingsRepository.selectedReciterId } returns MutableStateFlow(null)
        every { settingsRepository.showTajweed } returns MutableStateFlow(false)
        every { settingsRepository.tajweedUnderline } returns MutableStateFlow(false)

        // Page N holds one ayah with id N — enough to tell the pages apart in the cache.
        // Backed by a StateFlow per page rather than `flowOf`, because the real source is a
        // Room flow: it never completes, and it re-emits when the underlying rows change.
        // Publishing into one is how a test observes whether a page is still subscribed.
        every { useCases.getAyahsByPage(any(), any(), any()) } answers {
            pageContent(firstArg())
        }

        // Real paginations, not a relaxed mock: switching edition re-resolves the reader's
        // *position* through these, so a mock returning 0 would hide what is under test.
        coEvery { useCases.getMushafPagination(any()) } answers { paginationFor(firstArg()) }
    }

    /** [pages] equal pages spanning the whole Quran — enough to map a position across. */
    private fun paginationFor(script: MushafScript): MushafPagination {
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

    private val pageContent = mutableMapOf<Int, MutableStateFlow<List<Ayah>>>()

    private fun pageContent(page: Int): MutableStateFlow<List<Ayah>> =
        pageContent.getOrPut(page) { MutableStateFlow(listOf(ayahOnPage(page))) }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        QuranViewModel(useCases, audioManager, settingsRepository, khatamUseCases, context)

    private fun ayahOnPage(page: Int) = Ayah(
        id = page,
        surahNumber = 1,
        ayahNumber = page,
        textArabic = "ayah on page $page",
        textSimple = "ayah on page $page",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = page,
        sajdaType = null,
        sajdaNumber = null,
    )

    @Test
    fun `a page and its prefetched neighbours all reach the cache`() = runTest(dispatcher) {
        val viewModel = viewModel()

        // What one settled frame of the pager issues: the current page, then the neighbours
        // it keeps composed on either side.
        viewModel.onEvent(QuranEvent.LoadPage(2))
        viewModel.onEvent(QuranEvent.PrefetchPage(1))
        viewModel.onEvent(QuranEvent.PrefetchPage(3))
        advanceUntilIdle()

        assertThat(viewModel.readerState.value.pageCache.keys).containsExactly(1, 2, 3)
    }

    @Test
    fun `prefetching a neighbour does not make it the page the reader is on`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onEvent(QuranEvent.LoadPage(2))
            viewModel.onEvent(QuranEvent.PrefetchPage(3))
            advanceUntilIdle()

            // `ayahs` drives the saved reading position and the audio bar, so a page the user
            // has not swiped to must not land there.
            assertThat(viewModel.readerState.value.ayahs.map { it.id }).containsExactly(2)
        }

    @Test
    fun `repeated requests for the same page fetch it once`() = runTest(dispatcher) {
        val viewModel = viewModel()

        // The pager re-issues a page's prefetch whenever it re-enters composition; the
        // in-flight collector is enough, and re-launching would drop its emissions.
        viewModel.onEvent(QuranEvent.PrefetchPage(7))
        viewModel.onEvent(QuranEvent.PrefetchPage(7))
        viewModel.onEvent(QuranEvent.LoadPage(7))
        advanceUntilIdle()

        assertThat(viewModel.readerState.value.pageCache[7]?.map { it.id }).containsExactly(7)
    }

    @Test
    fun `reading on leaves the pages swiped past cached but no longer subscribed`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            // Page content comes from Room flows, which never complete, so a collector that
            // is not dropped stays subscribed for the life of the ViewModel. Reading on must
            // drop the ones left far behind — while keeping what they already fetched, so
            // swiping back stays instant.
            for (page in 1..20) {
                viewModel.onEvent(QuranEvent.LoadPage(page))
                advanceUntilIdle()
            }

            pageContent(1).value = listOf(ayahOnPage(1).copy(textArabic = "changed"))
            pageContent(19).value = listOf(ayahOnPage(19).copy(textArabic = "changed"))
            advanceUntilIdle()

            val cache = viewModel.readerState.value.pageCache
            // Page 1 is nineteen pages behind: cached, but no longer listening.
            assertThat(cache[1]?.single()?.textArabic).isEqualTo("ayah on page 1")
            // Page 19 neighbours the one being read, so it still tracks its source.
            assertThat(cache[19]?.single()?.textArabic).isEqualTo("changed")
        }

    @Test
    fun `switching Mushaf script clears the page cache and re-fetches the current page`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onEvent(QuranEvent.LoadPage(4))
            viewModel.onEvent(QuranEvent.PrefetchPage(5))
            advanceUntilIdle()
            assertThat(viewModel.readerState.value.pageCache.keys).containsExactly(4, 5)

            // A different edition repaginates the Quran, so page N no longer holds the same
            // ayahs: the stale neighbour must go, and the reader must come back on the page
            // carrying the text they were on rather than on the same integer.
            val madani = paginationFor(MushafScript.MADANI)
            val indopak = paginationFor(MushafScript.INDOPAK_16)
            val expected = indopak.pageMatching(4, madani)!!
            assertThat(expected).isNotEqualTo(4)

            mushafScript.value = MushafScript.INDOPAK_16.name
            advanceUntilIdle()

            val state = viewModel.readerState.value
            assertThat(state.mushafScript).isEqualTo(MushafScript.INDOPAK_16)
            assertThat(state.pageCache.keys).containsExactly(expected)
        }
}
