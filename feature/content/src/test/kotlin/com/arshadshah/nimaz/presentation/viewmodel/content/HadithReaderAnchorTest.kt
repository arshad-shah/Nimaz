package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.repository.settings.HadithDisplaySettings
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The hadith reader's **anchor** — the one piece of state that survives a content refresh.
 *
 * `getHadithsByChapter` is a Room flow: any write touching the hadiths table re-emits, and so
 * does a whole content-database swap by `ContentArtifactInstaller`. The loader used to set
 * `currentHadithIndex = 0` inside that collector, on *every* emission — so a background content
 * refresh scrolled a reader at hadith 50 back to hadith 1, silently, mid-read.
 *
 * The anchor is held **by id rather than by index** so it survives a refresh that inserts or
 * reorders rows, which an index would not. That is the difference this file exists to hold: an
 * index-based anchor passes the "a refresh does not reset the reader" test and still moves the
 * reader when the content changes underneath it.
 *
 * The rest is `retryFailedLoads`, which re-runs **only** the surfaces that are actually failing.
 * A retry tapped in the reader must not also re-fetch the book list behind it, and there is
 * nothing on screen to say whether it did.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HadithReaderAnchorTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: HadithUseCases
    private lateinit var settings: HadithDisplaySettings

    private val chapterHadiths = MutableStateFlow<List<Hadith>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        settings = mockk(relaxed = true)

        every { settings.hadithArabicFont } returns flowOf("amiri")
        every { settings.hadithArabicFontSize } returns flowOf(24f)
        every { settings.hadithTranslationFontSize } returns flowOf(16f)
        every { settings.hadithShowArabic } returns flowOf(true)
        every { settings.hadithShowTranslation } returns flowOf(true)
        every { settings.hadithShowGrade } returns flowOf(true)
        every { settings.hadithShowChain } returns flowOf(true)

        every { useCases.getAllBooks() } returns flowOf(emptyList())
        every { useCases.getAllBookmarks() } returns flowOf(emptyList())
        every { useCases.getHadithsByChapter(any()) } returns chapterHadiths
        coEvery { useCases.getChapterById(any()) } returns chapter()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HadithViewModel(useCases, settings, RecordingTelemetry())

    @Test
    fun `a chapter opens at the top`() = runTest {
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(0)
        assertThat(vm.readerState.value.hadiths).hasSize(5)
    }

    @Test
    fun `a content refresh does not scroll the reader back to the first hadith`() = runTest {
        // The defect this exists for: a background content swap re-emits the chapter, and the
        // reader was at hadith 4.
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()

        vm.onEvent(HadithEvent.NavigateToHadith(3))
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(3)
    }

    @Test
    fun `the anchor follows the hadith, not the position it was at`() = runTest {
        // The reason the anchor is an id. A refresh that inserts a hadith above the reader
        // shifts every index by one; an index-based anchor would leave them on the hadith
        // before the one they were reading, which looks like a scroll glitch.
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.NavigateToHadith(3))

        // A newly published hadith arrives at the top of the chapter.
        chapterHadiths.value = listOf(hadith("h0", 0)) + (1..5).map { hadith("h$it", it) }
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(4)
        assertThat(vm.readerState.value.hadiths[4].id).isEqualTo("h4")
    }

    @Test
    fun `a refresh that removes the anchored hadith falls back to the top`() = runTest {
        // `takeIf { it >= 0 } ?: 0`. Without it the reader indexes at -1.
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.NavigateToHadith(3))

        chapterHadiths.value = listOf(hadith("h1", 1), hadith("h2", 2))
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(0)
    }

    @Test
    fun `opening a hadith by id lands on that hadith within its chapter`() = runTest {
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        coEvery { useCases.getHadithById("h3") } returns hadith("h3", 3)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(HadithEvent.LoadHadithById("h3"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(2)
    }

    @Test
    fun `a hadith id that is not in the collection is an answer, not a failure`() = runTest {
        // Nothing went wrong, so nothing is reported — but the reader is told, in their own
        // language rather than as the English literal this used to set.
        coEvery { useCases.getHadithById("ghost") } returns null
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(HadithEvent.LoadHadithById("ghost"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.error).isNotNull()
        assertThat(vm.readerState.value.isLoading).isFalse()
        assertThat(vm.readerState.value.hadiths).isEmpty()
    }

    @Test
    fun `opening a hadith by number resolves its chapter through the composite key`() = runTest {
        // `getChapterById` is keyed on `bookId_chapterId`; passing the raw chapter id resolved
        // the header to null for every hadith opened from a bookmark.
        val target = hadith("m9", 9, bookId = "muslim", chapterId = "3")
        chapterHadiths.value = listOf(hadith("m8", 8, bookId = "muslim", chapterId = "3"), target)
        coEvery { useCases.getHadithByNumber("muslim", 2564) } returns target
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(HadithEvent.LoadHadithByNumber("muslim", 2564))
        advanceUntilIdle()

        coVerify { useCases.getChapterById("muslim_3") }
        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(1)
    }

    @Test
    fun `browsing a grade clears the chapter it came from`() = runTest {
        // Swapping the list while leaving `chapter` set rendered a foreign chapter header over
        // the results, and `currentHadithIndex` still pointed into the old list.
        chapterHadiths.value = (1..5).map { hadith("h$it", it) }
        every { useCases.getHadithsByGrade(HadithGrade.SAHIH) } returns
            flowOf(listOf(hadith("s1", 1), hadith("s2", 2)))
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.NavigateToHadith(4))

        vm.onEvent(HadithEvent.FilterByGrade(HadithGrade.SAHIH))
        advanceUntilIdle()

        assertThat(vm.readerState.value.chapter).isNull()
        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(0)
        assertThat(vm.readerState.value.hadiths.map { it.id }).containsExactly("s1", "s2")
    }

    @Test
    fun `retry re-runs only the surface that is failing`() = runTest {
        // A retry tapped in the reader must not also re-fetch the book list behind it, and
        // nothing on screen would say whether it did.
        every { useCases.getHadithsByChapter("bukhari_9") } returns flow { throw IllegalStateException("disk") }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_9"))
        advanceUntilIdle()
        assertThat(vm.readerState.value.error).isNotNull()

        vm.onEvent(HadithEvent.Retry)
        advanceUntilIdle()

        // The book list never failed, so it is never re-fetched: once at init and no more.
        verifyBooksLoadedOnce()
    }

    @Test
    fun `retry does nothing at all when nothing is failing`() = runTest {
        chapterHadiths.value = listOf(hadith("h1", 1))
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()

        vm.onEvent(HadithEvent.Retry)
        advanceUntilIdle()

        verifyBooksLoadedOnce()
        assertThat(vm.readerState.value.error).isNull()
    }

    @Test
    fun `a failing book list is what a retry re-issues`() = runTest {
        every { useCases.getAllBooks() } returns flow { throw IllegalStateException("no table") }
        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.collectionState.value.error).isNotNull()

        vm.onEvent(HadithEvent.Retry)
        advanceUntilIdle()

        io.mockk.verify(exactly = 2) { useCases.getAllBooks() }
    }

    @Test
    fun `the font-size and Arabic events change only what they name`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(HadithEvent.SetFontSize(20f))
        vm.onEvent(HadithEvent.SetArabicFontSize(34f))
        vm.onEvent(HadithEvent.ToggleArabic)

        assertThat(vm.readerState.value.fontSize).isEqualTo(20f)
        assertThat(vm.readerState.value.arabicFontSize).isEqualTo(34f)
        assertThat(vm.readerState.value.showArabic).isFalse()
        assertThat(vm.readerState.value.showTranslation).isTrue()
    }

    @Test
    fun `clearing the search clears the chapter filter with it`() = runTest {
        // Two surfaces, one event: the search results and the chapter list's own query box.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(HadithEvent.ClearSearch)

        assertThat(vm.searchState.value.query).isEmpty()
        assertThat(vm.chaptersState.value.searchQuery).isEmpty()
    }

    @Test
    fun `bookmarking is a write and never replaces what is on screen`() = runTest {
        // Best-effort: blanking a perfectly readable hadith to report a failed star would be
        // the worse outcome.
        coEvery { useCases.toggleBookmark(any(), any(), any()) } throws IllegalStateException("db")
        chapterHadiths.value = listOf(hadith("h1", 1))
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(HadithEvent.LoadChapter("bukhari_1"))
        advanceUntilIdle()

        vm.onEvent(HadithEvent.ToggleBookmark("h1", "bukhari", 1))
        advanceUntilIdle()

        assertThat(vm.readerState.value.hadiths).hasSize(1)
        assertThat(vm.readerState.value.error).isNull()
    }

    @Test
    fun `the book list feeds the collection surface`() = runTest {
        every { useCases.getAllBooks() } returns flowOf(listOf(book()))
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.collectionState.value.books).hasSize(1)
        assertThat(vm.collectionState.value.isLoading).isFalse()
    }

    private fun verifyBooksLoadedOnce() {
        io.mockk.verify(exactly = 1) { useCases.getAllBooks() }
    }

    private fun book() = HadithBook(
        id = "bukhari",
        nameArabic = "صحيح البخاري",
        nameEnglish = "Sahih al-Bukhari",
        authorName = "Imam al-Bukhari",
        authorArabic = "البخاري",
        totalHadiths = 7563,
        totalChapters = 97,
        description = null,
        displayOrder = 1,
    )

    private fun chapter() = HadithChapter(
        id = "bukhari_1",
        bookId = "bukhari",
        chapterNumber = 1,
        nameArabic = "بدء الوحي",
        nameEnglish = "Revelation",
        hadithCount = 7,
        hadithStartNumber = 1,
        hadithEndNumber = 7,
    )

    private fun hadith(
        id: String,
        number: Int,
        bookId: String = "bukhari",
        chapterId: String = "1",
    ) = Hadith(
        id = id,
        bookId = bookId,
        chapterId = chapterId,
        hadithNumber = number,
        hadithNumberInBook = number,
        textArabic = "نص",
        textEnglish = "Text $number",
        narratorChain = null,
        narratorName = null,
        grade = HadithGrade.SAHIH,
        gradeArabic = null,
        reference = null,
    )
}
