package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.SavedKind
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
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
 * The two axes Saved filters on, and the fact that they are two.
 *
 * **What** something is (Qur'an, Hadith, Dua) and **how** it was marked (bookmarked, favourited,
 * annotated) are independent questions — "my notes on hadith" is a real one — and the screen
 * carries them in two different controls for that reason. A view model that treated them as one
 * would answer that question with everything, or with nothing, and either reads as a working
 * filter.
 *
 * `BookmarksViewModelTest` covers deletion, undo and the bookmark/favourite merge; this is the
 * filtering, the note edit and the counts the tabs are labelled with.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelAxesTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var quran: QuranUseCases
    private lateinit var hadith: HadithUseCases
    private lateinit var dua: DuaUseCases

    private val quranBookmarks = MutableStateFlow(emptyList<QuranBookmark>())
    private val quranFavourites = MutableStateFlow(emptyList<QuranFavorite>())
    private val hadithBookmarks = MutableStateFlow(emptyList<HadithBookmark>())
    private val duaBookmarks = MutableStateFlow(emptyList<DuaBookmark>())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        quran = mockk(relaxed = true)
        hadith = mockk(relaxed = true)
        dua = mockk(relaxed = true)
        every { quran.getBookmarks() } returns quranBookmarks
        every { quran.getFavorites() } returns quranFavourites
        every { hadith.getAllBookmarks() } returns hadithBookmarks
        every { dua.getAllBookmarks() } returns duaBookmarks
        coEvery { quran.updateBookmark(any()) } answers {
            val updated = firstArg<QuranBookmark>()
            quranBookmarks.value = quranBookmarks.value.map {
                if (it.ayahId == updated.ayahId) updated else it
            }
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = BookmarksViewModel(quran, hadith, dua, FakeStringProvider(), telemetry)

    private fun verse(ayahId: Int, note: String? = null) = QuranBookmark(
        id = ayahId.toLong(),
        ayahId = ayahId,
        surahNumber = 2,
        ayahNumber = ayahId,
        note = note,
        color = null,
        createdAt = ayahId.toLong(),
        updatedAt = ayahId.toLong(),
    )

    private fun favourite(ayahId: Int) = QuranFavorite(
        ayahId = ayahId,
        surahNumber = 2,
        ayahNumber = ayahId,
        createdAt = ayahId.toLong(),
    )

    private fun hadithMark(id: Int) = HadithBookmark(
        id = id.toLong(),
        hadithId = "hadith-$id",
        bookId = "bukhari",
        hadithNumber = id,
        note = null,
        color = null,
        createdAt = id.toLong(),
        updatedAt = id.toLong(),
    )

    private fun duaMark(id: Int) = DuaBookmark(
        id = id.toLong(),
        duaId = "dua-$id",
        categoryId = "morning",
        note = null,
        isFavorite = false,
        createdAt = id.toLong(),
        updatedAt = id.toLong(),
    )

    private fun seedOneOfEach() {
        quranBookmarks.value = listOf(verse(1), verse(2, note = "worth returning to"))
        quranFavourites.value = listOf(favourite(3))
        hadithBookmarks.value = listOf(hadithMark(10))
        duaBookmarks.value = listOf(duaMark(20))
    }

    // ---- What it is ----

    @Test
    fun `everything saved is listed when no corpus is chosen`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks.map { it.type })
            .containsExactly(
                BookmarkType.QURAN,
                BookmarkType.QURAN,
                BookmarkType.QURAN,
                BookmarkType.HADITH,
                BookmarkType.DUA,
            )
    }

    @Test
    fun `choosing a corpus narrows the list to it`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetFilter(BookmarkType.HADITH))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks.map { it.type })
            .containsExactly(BookmarkType.HADITH)
        assertThat(vm.bookmarksState.value.selectedFilter).isEqualTo(BookmarkType.HADITH)
    }

    @Test
    fun `clearing the corpus filter puts everything back`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetFilter(BookmarkType.HADITH))
        advanceUntilIdle()
        // The screen's menu offers "All" as its own row rather than making the chosen row a
        // toggle, so the way back is a null and not a repeat.
        vm.onEvent(BookmarksEvent.SetFilter(null))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.selectedFilter).isNull()
        assertThat(vm.bookmarksState.value.filteredBookmarks).hasSize(5)
    }

    // ---- How it was marked ----

    @Test
    fun `choosing a kind narrows to the rows marked that way`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetKind(SavedKind.NOTE))
        advanceUntilIdle()

        // A note was invisible before this axis existed unless you opened the row carrying it.
        assertThat(vm.bookmarksState.value.filteredBookmarks.map { it.note })
            .containsExactly("worth returning to")
    }

    @Test
    fun `a favourited verse is under FAVOURITE, and a bookmarked one is not`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetKind(SavedKind.FAVOURITE))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks.map { it.ayahNumber })
            .containsExactly(3)
    }

    @Test
    fun `the two axes narrow together rather than replacing one another`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetFilter(BookmarkType.QURAN))
        vm.onEvent(BookmarksEvent.SetKind(SavedKind.NOTE))
        advanceUntilIdle()

        // "My notes on the Qur'an" — the question the two controls exist to answer together.
        assertThat(vm.bookmarksState.value.filteredBookmarks.map { it.note })
            .containsExactly("worth returning to")
    }

    @Test
    fun `clearing the kind puts everything back, without disturbing the corpus`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetFilter(BookmarkType.QURAN))
        vm.onEvent(BookmarksEvent.SetKind(SavedKind.NOTE))
        advanceUntilIdle()
        vm.onEvent(BookmarksEvent.SetKind(null))
        advanceUntilIdle()

        // Independent axes: dropping one must not drop the other.
        assertThat(vm.bookmarksState.value.selectedKind).isNull()
        assertThat(vm.bookmarksState.value.selectedFilter).isEqualTo(BookmarkType.QURAN)
        assertThat(vm.bookmarksState.value.filteredBookmarks).hasSize(3)
    }

    // ---- The counts the tabs are labelled with ----

    @Test
    fun `the stats count each corpus and each kind`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        val stats = vm.statsState.value
        assertThat(stats.totalBookmarks).isEqualTo(5)
        assertThat(stats.quranCount).isEqualTo(3)
        assertThat(stats.hadithCount).isEqualTo(1)
        assertThat(stats.duaCount).isEqualTo(1)
        assertThat(stats.noteCount).isEqualTo(1)
        assertThat(stats.favouriteCount).isEqualTo(1)
    }

    @Test
    fun `the counts describe everything saved, not the filtered view`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetFilter(BookmarkType.HADITH))
        advanceUntilIdle()

        // The counts are what tell a reader there is nothing under Hadith *before* they tap it,
        // so a filter that rewrote them would make them useless for the thing they are for.
        assertThat(vm.statsState.value.totalBookmarks).isEqualTo(5)
    }

    // ---- Notes ----

    @Test
    fun `editing a note writes it and shows it`() = runTest {
        quranBookmarks.value = listOf(verse(1))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.EditNote(BookmarkType.QURAN.idFor(1), "a new note"))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.allBookmarks.single().note).isEqualTo("a new note")
    }

    @Test
    fun `a written note makes the row an annotated one`() = runTest {
        quranBookmarks.value = listOf(verse(1))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.EditNote(BookmarkType.QURAN.idFor(1), "a new note"))
        advanceUntilIdle()
        vm.onEvent(BookmarksEvent.SetKind(SavedKind.NOTE))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks).hasSize(1)
    }

    // ---- Retry ----

    @Test
    fun `retrying clears the error and asks again`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.Retry)
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.error).isNull()
    }

    @Test
    fun `dismissing a write error leaves the list where it was`() = runTest {
        seedOneOfEach()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.DismissWriteError)
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.writeError).isNull()
        assertThat(vm.bookmarksState.value.filteredBookmarks).hasSize(5)
    }
}
