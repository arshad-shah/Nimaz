package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HadithViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: HadithRepository
    private lateinit var viewModel: HadithViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getAllBooks() } returns flowOf(emptyList())
        every { repository.getAllBookmarks() } returns flowOf(emptyList())
        coEvery { repository.getHadithOfTheDay() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HadithViewModel(repository)

    private fun book(id: String, nameEnglish: String) = HadithBook(
        id = id, nameArabic = "", nameEnglish = nameEnglish, authorName = "",
        authorArabic = "", totalHadiths = 0, totalChapters = 0,
        description = null, displayOrder = 0
    )

    private fun chapter(id: String, nameEnglish: String, nameArabic: String = "") = HadithChapter(
        id = id, bookId = "b1", chapterNumber = 1, nameArabic = nameArabic,
        nameEnglish = nameEnglish, hadithCount = 0, hadithStartNumber = 1, hadithEndNumber = 1
    )

    // ── Init ────────────────────────────────────────────────────────

    @Test
    fun `init loads books and finishes loading`() = runTest {
        every { repository.getAllBooks() } returns flowOf(listOf(book("1", "Bukhari")))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.collectionState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.books).hasSize(1)
    }

    // ── Loading a book / chapter ────────────────────────────────────

    @Test
    fun `LoadBook populates book and its chapters as the filtered list`() = runTest {
        val b = book("bukhari", "Sahih Bukhari")
        val chapters = listOf(chapter("c1", "Revelation"), chapter("c2", "Belief"))
        coEvery { repository.getBookById("bukhari") } returns b
        every { repository.getChaptersByBook("bukhari") } returns flowOf(chapters)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()

        val state = viewModel.chaptersState.value
        assertThat(state.book).isEqualTo(b)
        assertThat(state.chapters).hasSize(2)
        assertThat(state.filteredChapters).hasSize(2)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `LoadChapter resets the current hadith index to zero`() = runTest {
        coEvery { repository.getChapterById("c1") } returns chapter("c1", "Revelation")
        every { repository.getHadithsByChapter("c1") } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.NavigateToHadith(5))
        viewModel.onEvent(HadithEvent.LoadChapter("c1"))
        advanceUntilIdle()

        assertThat(viewModel.readerState.value.currentHadithIndex).isEqualTo(0)
        assertThat(viewModel.readerState.value.isLoading).isFalse()
    }

    // ── Search ──────────────────────────────────────────────────────

    @Test
    fun `Search with a blank query resets search and skips the repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.Search(""))
        advanceUntilIdle()

        assertThat(viewModel.searchState.value.query).isEmpty()
        coVerify(exactly = 0) { repository.searchHadiths(any()) }
    }

    @Test
    fun `Search collects results and records the query`() = runTest {
        every { repository.searchHadiths("intention") } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.Search("intention"))
        advanceUntilIdle()

        assertThat(viewModel.searchState.value.query).isEqualTo("intention")
        assertThat(viewModel.searchState.value.isSearching).isFalse()
        coVerify { repository.searchHadiths("intention") }
    }

    @Test
    fun `SearchInBook records the selected book id`() = runTest {
        every { repository.searchHadithsInBook("bukhari", "fasting") } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.SearchInBook("bukhari", "fasting"))
        advanceUntilIdle()

        assertThat(viewModel.searchState.value.selectedBookId).isEqualTo("bukhari")
        coVerify { repository.searchHadithsInBook("bukhari", "fasting") }
    }

    @Test
    fun `SearchChapters filters the loaded chapters by name`() = runTest {
        val b = book("bukhari", "Sahih Bukhari")
        val chapters = listOf(chapter("c1", "Revelation"), chapter("c2", "Belief"))
        coEvery { repository.getBookById("bukhari") } returns b
        every { repository.getChaptersByBook("bukhari") } returns flowOf(chapters)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.SearchChapters("belief"))

        val state = viewModel.chaptersState.value
        assertThat(state.searchQuery).isEqualTo("belief")
        assertThat(state.filteredChapters.map { it.id }).containsExactly("c2")
    }

    @Test
    fun `ClearSearch restores all chapters`() = runTest {
        val chapters = listOf(chapter("c1", "Revelation"), chapter("c2", "Belief"))
        coEvery { repository.getBookById("bukhari") } returns book("bukhari", "Sahih Bukhari")
        every { repository.getChaptersByBook("bukhari") } returns flowOf(chapters)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.SearchChapters("belief"))
        assertThat(viewModel.chaptersState.value.filteredChapters).hasSize(1)

        viewModel.onEvent(HadithEvent.ClearSearch)
        assertThat(viewModel.chaptersState.value.filteredChapters).hasSize(2)
        assertThat(viewModel.chaptersState.value.searchQuery).isEmpty()
    }

    // ── Reader navigation & display ─────────────────────────────────

    @Test
    fun `NavigateToHadith updates the current index`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.NavigateToHadith(7))
        assertThat(viewModel.readerState.value.currentHadithIndex).isEqualTo(7)
    }

    @Test
    fun `ToggleArabic flips the showArabic flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.readerState.value.showArabic).isTrue()
        viewModel.onEvent(HadithEvent.ToggleArabic)
        assertThat(viewModel.readerState.value.showArabic).isFalse()
    }

    // ── Bookmarks ───────────────────────────────────────────────────

    @Test
    fun `ToggleBookmark delegates to the repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HadithEvent.ToggleBookmark("h1", "bukhari", 1))
        advanceUntilIdle()

        coVerify { repository.toggleBookmark("h1", "bukhari", 1) }
    }
}
