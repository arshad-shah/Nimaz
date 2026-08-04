package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
 * Where the hadith reader opens, and whether it stays there.
 *
 * #364 R3 calls `loadHadithByNumber` "latent today — the event is never emitted". It is not.
 * `UnifiedBookmark` carries `hadithBookId` and `hadithNumber` and **no hadith id**, so opening a
 * bookmarked hadith can only go through this path — and the nav graph, lacking a route for it,
 * passed `hadithNumber.toString()` into `Route.HadithReader`'s `hadithId` slot instead. See
 * `the bookmark path addresses a hadith by book and number` below for what that resolved to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HadithReaderTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var useCases: HadithUseCases
    private lateinit var settingsRepository: SettingsRepository

    /** Chapter 3 of book 1, whose composite key is `1_3`. */
    private val chapter = HadithChapter(
        id = "1_3",
        bookId = "1",
        chapterNumber = 3,
        nameArabic = "كتاب الصلاة",
        nameEnglish = "The Book of Prayer",
        hadithCount = 3,
        hadithStartNumber = 40,
        hadithEndNumber = 42,
    )

    private val hadiths = MutableStateFlow(
        listOf(hadith("501", 40), hadith("502", 41), hadith("503", 42)),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.hadithArabicFont } returns MutableStateFlow("amiri")
        every { settingsRepository.hadithArabicFontSize } returns MutableStateFlow(28f)
        every { settingsRepository.hadithTranslationFontSize } returns MutableStateFlow(16f)
        every { settingsRepository.hadithShowArabic } returns MutableStateFlow(true)
        every { settingsRepository.hadithShowTranslation } returns MutableStateFlow(true)
        every { settingsRepository.hadithShowGrade } returns MutableStateFlow(true)
        every { settingsRepository.hadithShowChain } returns MutableStateFlow(false)
        every { useCases.getHadithsByChapter("1_3") } returns hadiths
        coEvery { useCases.getChapterById("1_3") } returns chapter
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HadithViewModel(useCases, settingsRepository, telemetry)

    // R3 — opening a hadith by its number.

    @Test
    fun `opening a hadith by number lands on that hadith`() = runTest {
        coEvery { useCases.getHadithByNumber("1", 42) } returns hadith("503", 42)

        val vm = viewModel()
        vm.onEvent(HadithEvent.LoadHadithByNumber("1", 42))
        advanceUntilIdle()

        // The index was read out of `_readerState` on the line *after* `loadChapter` launched
        // its own coroutine, so it always saw the previous chapter's list. `indexOfFirst` was
        // always -1 and the branch that sets the index could never run: the reader opened at
        // hadith 40 of the right chapter, every time.
        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(2)
        assertThat(vm.readerState.value.hadiths.map { it.id }).containsExactly("501", "502", "503")
    }

    @Test
    fun `opening a hadith by number resolves its chapter header`() = runTest {
        coEvery { useCases.getHadithByNumber("1", 42) } returns hadith("503", 42)

        val vm = viewModel()
        vm.onEvent(HadithEvent.LoadHadithByNumber("1", 42))
        advanceUntilIdle()

        // `getChapterById` is keyed on the composite `bookId_chapterId`. This path passed the
        // bare `chapterId` — "3" — so the header resolved to null while its sibling
        // `loadHadithById`, three lines up, built the composite correctly.
        assertThat(vm.readerState.value.chapter).isEqualTo(chapter)
    }

    @Test
    fun `a hadith number that does not exist reports not found rather than opening chapter one`() =
        runTest {
            coEvery { useCases.getHadithByNumber("1", 9999) } returns null

            val vm = viewModel()
            vm.onEvent(HadithEvent.LoadHadithByNumber("1", 9999))
            advanceUntilIdle()

            assertThat(vm.readerState.value.isLoading).isFalse()
            assertThat(vm.readerState.value.error).isNotNull()
        }

    // R4 — a content refresh must not move the reader.

    @Test
    fun `a content refresh leaves the reader where it was`() = runTest {
        val vm = viewModel()
        vm.onEvent(HadithEvent.LoadChapter("1_3"))
        advanceUntilIdle()

        vm.onEvent(HadithEvent.NavigateToHadith(2))
        advanceUntilIdle()

        // `getHadithsByChapter` is a Room Flow: any write touching the table re-emits, and so
        // does a content-database swap. The loader set `currentHadithIndex = 0` inside that
        // collector, so a reader at hadith 42 was scrolled back to hadith 40 by a background
        // refresh — `HadithReaderScreen` drives its pager off this index.
        //
        // Bookmarking is the cheapest real trigger: it writes the row and Room re-emits the
        // chapter with the flag flipped. (A `toList()` copy would not do — the list compares
        // equal and `MutableStateFlow` would drop it.)
        hadiths.value = hadiths.value.map { it.copy(isBookmarked = true) }
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(2)
    }

    @Test
    fun `the anchor follows the hadith, not the index, when rows are inserted`() = runTest {
        val vm = viewModel()
        vm.onEvent(HadithEvent.LoadChapter("1_3"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.NavigateToHadith(2))
        advanceUntilIdle()

        // A refresh that prepends a row would keep a reader anchored by *index* on the same
        // number and a different hadith.
        hadiths.value = listOf(hadith("500", 39)) + hadiths.value
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(3)
        assertThat(vm.readerState.value.hadiths[vm.readerState.value.currentHadithIndex].id)
            .isEqualTo("503")
    }

    @Test
    fun `a chapter opened without a target starts at the top`() = runTest {
        val vm = viewModel()
        vm.onEvent(HadithEvent.LoadChapter("1_3"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(0)
        assertThat(vm.readerState.value.isLoading).isFalse()
    }

    // R15 — the grade filter must not leave a foreign header over its results.

    @Test
    fun `filtering by grade clears the chapter it came from`() = runTest {
        every { useCases.getHadithsByGrade(any()) } returns
            MutableStateFlow(listOf(hadith("900", 1)))

        val vm = viewModel()
        vm.onEvent(HadithEvent.LoadChapter("1_3"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.NavigateToHadith(2))
        advanceUntilIdle()

        vm.onEvent(HadithEvent.FilterByGrade(com.arshadshah.nimaz.domain.model.HadithGrade.SAHIH))
        advanceUntilIdle()

        // Results span every chapter, so the previous chapter's header and reading position
        // describe nothing. Left in place they would have rendered over the results the first
        // time any screen wired this up.
        assertThat(vm.readerState.value.chapter).isNull()
        assertThat(vm.readerState.value.currentHadithIndex).isEqualTo(0)
        assertThat(vm.readerState.value.hadiths.map { it.id }).containsExactly("900")
    }

    // The composite key, stated once.

    @Test
    fun `a hadith knows the key its chapter is stored under`() {
        assertThat(hadith("503", 42).chapterKey).isEqualTo("1_3")
    }

    private fun hadith(id: String, number: Int) = Hadith(
        id = id,
        bookId = "1",
        chapterId = "3",
        hadithNumber = number,
        hadithNumberInBook = number,
        textArabic = "نص",
        textEnglish = "text",
        narratorChain = null,
        narratorName = null,
        grade = null,
        gradeArabic = null,
        reference = null,
    )
}
