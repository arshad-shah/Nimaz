package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
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
 * The chapter search must survive a re-emission of the chapter list.
 *
 * `filteredChapters` was stored state, and `loadBook`'s Room collector rebuilt it as the whole
 * `chapters` list without consulting `searchQuery`. Any write that made the chapters flow
 * re-emit therefore wiped the user's search while the search field kept showing what they had
 * typed — the same defect as the tasbih category filter, in a second feature.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HadithViewModelChapterFilterTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var hadithUseCases: HadithUseCases
    private lateinit var settingsRepository: SettingsRepository

    private val chapters = MutableStateFlow(
        listOf(
            chapter("c1", "Book of Faith"),
            chapter("c2", "Book of Prayer"),
            chapter("c3", "Book of Fasting")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        hadithUseCases = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { hadithUseCases.getAllBooks() } returns flowOf(emptyList())
        every { hadithUseCases.getAllBookmarks() } returns flowOf(emptyList())
        coEvery { hadithUseCases.getHadithOfTheDay() } returns null
        coEvery { hadithUseCases.getBookById(any()) } returns null
        every { hadithUseCases.getChaptersByBook(any()) } returns chapters

        every { settingsRepository.hadithArabicFont } returns MutableStateFlow("amiri")
        every { settingsRepository.hadithArabicFontSize } returns MutableStateFlow(24f)
        every { settingsRepository.hadithTranslationFontSize } returns MutableStateFlow(16f)
        every { settingsRepository.hadithShowArabic } returns MutableStateFlow(true)
        every { settingsRepository.hadithShowTranslation } returns MutableStateFlow(true)
        every { settingsRepository.hadithShowGrade } returns MutableStateFlow(true)
        every { settingsRepository.hadithShowChain } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        HadithViewModel(hadithUseCases, settingsRepository, RecordingTelemetry())

    @Test
    fun `a chapter list re-emission keeps the active chapter search applied`() = runTest {
        val vm = viewModel()

        vm.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.SearchChapters("Prayer"))
        advanceUntilIdle()

        assertThat(vm.chaptersState.value.filteredChapters.map { it.id }).containsExactly("c2")

        // Any write to the chapters table re-emits the Room flow.
        chapters.value = chapters.value + chapter("c4", "Book of Zakat")
        advanceUntilIdle()

        assertThat(vm.chaptersState.value.searchQuery).isEqualTo("Prayer")
        assertThat(vm.chaptersState.value.filteredChapters.map { it.id }).containsExactly("c2")
    }

    @Test
    fun `a re-emission surfaces newly matching chapters`() = runTest {
        val vm = viewModel()

        vm.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.SearchChapters("Book of F"))
        advanceUntilIdle()
        assertThat(vm.chaptersState.value.filteredChapters.map { it.id })
            .containsExactly("c1", "c3")

        chapters.value = chapters.value + chapter("c5", "Book of Funerals")
        advanceUntilIdle()

        assertThat(vm.chaptersState.value.filteredChapters.map { it.id })
            .containsExactly("c1", "c3", "c5")
    }

    @Test
    fun `clearing the search shows every chapter again`() = runTest {
        val vm = viewModel()

        vm.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.SearchChapters("Prayer"))
        advanceUntilIdle()
        vm.onEvent(HadithEvent.ClearSearch)
        advanceUntilIdle()

        assertThat(vm.chaptersState.value.filteredChapters.map { it.id })
            .containsExactly("c1", "c2", "c3")
    }
}

private fun chapter(id: String, nameEnglish: String) = HadithChapter(
    id = id,
    bookId = "bukhari",
    chapterNumber = id.removePrefix("c").toInt(),
    nameArabic = "",
    nameEnglish = nameEnglish,
    hadithCount = 1,
    hadithStartNumber = 1,
    hadithEndNumber = 1
)
