package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.presentation.viewmodel.FakeStringProvider
import android.content.Context
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
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
 * `BookmarksViewModel` had **no tests and no error handling at all** — not one
 * `try`/`catch` or `CrashReporter` call in the file — while enriching every bookmark
 * with a per-row suspend query *inside* a Room collector. A missing content row after
 * a content-database replacement (a supported state per `docs/SUBSYSTEMS.md` §5)
 * killed the collector, pinned `isLoading` on for ever, and reported nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var quran: QuranUseCases
    private lateinit var hadith: HadithUseCases
    private lateinit var dua: DuaUseCases
    private lateinit var context: Context

    private val quranBookmarks = MutableStateFlow(emptyList<QuranBookmark>())

    private fun quranBookmark(ayahId: Int, created: Long) = QuranBookmark(
        id = ayahId.toLong(),
        ayahId = ayahId,
        surahNumber = 2,
        ayahNumber = ayahId,
        note = null,
        color = null,
        createdAt = created,
        updatedAt = created,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = mockk(relaxed = true)
        quran = mockk(relaxed = true)
        hadith = mockk(relaxed = true)
        dua = mockk(relaxed = true)
        every { quran.getBookmarks() } returns quranBookmarks
        // Saved merges bookmarks with favourites — one row in the store, two queries — so the
        // Qur'an load does not emit at all until both flows have.
        every { quran.getFavorites() } returns flowOf(emptyList<QuranFavorite>())
        every { hadith.getAllBookmarks() } returns flowOf(emptyList<HadithBookmark>())
        every { dua.getAllBookmarks() } returns flowOf(emptyList<DuaBookmark>())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = BookmarksViewModel(quran, hadith, dua, FakeStringProvider(), telemetry)

    @Test
    fun `a failing bookmark stream reports and stops loading instead of hanging`() = runTest {
        every { quran.getBookmarks() } returns flow {
            throw IllegalStateException("no such table: quran_bookmarks")
        }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.isLoading).isFalse()
        assertThat(vm.bookmarksState.value.error).isNotNull()
        assertThat(telemetry.errors.map { it.domain }).contains("bookmarks")
    }

    @Test
    fun `a delete that fails does not offer an undo for something still there`() = runTest {
        quranBookmarks.value = listOf(quranBookmark(1, 100))
        coEvery { quran.deleteBookmark(any()) } throws IllegalStateException("database is locked")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_1"))
        advanceUntilIdle()

        // The delete threw, so the bookmark is still there. Offering "Deleted — Undo"
        // would be the UI stating something untrue, and an undo tapped against it would
        // re-insert a row that was never removed.
        assertThat(vm.bookmarksState.value.recentlyDeleted).isNull()
        assertThat(telemetry.errors.map { it.domain }).contains("bookmarks")
    }

    @Test
    fun `two deletes in a row can both be undone`() = runTest {
        quranBookmarks.value = listOf(quranBookmark(1, 100), quranBookmark(2, 200))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_1"))
        advanceUntilIdle()
        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_2"))
        advanceUntilIdle()

        // pendingRestore was a single `var`, so the second delete overwrote the first
        // and bookmark 1 became unrecoverable — while the UI still showed a normal
        // single-item undo snackbar.
        vm.onEvent(BookmarksEvent.UndoDelete)
        advanceUntilIdle()
        vm.onEvent(BookmarksEvent.UndoDelete)
        advanceUntilIdle()

        coVerify { quran.insertBookmark(match { it.ayahId == 2 }) }
        coVerify { quran.insertBookmark(match { it.ayahId == 1 }) }
    }

    @Test
    fun `undo restores the most recent delete first`() = runTest {
        quranBookmarks.value = listOf(quranBookmark(1, 100), quranBookmark(2, 200))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_1"))
        advanceUntilIdle()
        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_2"))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.recentlyDeleted?.id).isEqualTo("quran_2")

        vm.onEvent(BookmarksEvent.UndoDelete)
        advanceUntilIdle()

        // The next undo target surfaces, so the snackbar can offer it.
        assertThat(vm.bookmarksState.value.recentlyDeleted?.id).isEqualTo("quran_1")
    }

    @Test
    fun `dismissing undo drops every pending restore, not just the newest`() = runTest {
        quranBookmarks.value = listOf(quranBookmark(1, 100), quranBookmark(2, 200))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_1"))
        vm.onEvent(BookmarksEvent.DeleteBookmark("quran_2"))
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.DismissUndo)
        advanceUntilIdle()
        vm.onEvent(BookmarksEvent.UndoDelete)
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.recentlyDeleted).isNull()
        coVerify(exactly = 0) { quran.insertBookmark(any()) }
    }

    @Test
    fun `enrichment is one batched query, not one per bookmark`() = runTest {
        val getAyahById = mockk<com.arshadshah.nimaz.domain.usecase.GetAyahByIdUseCase>(relaxed = true)
        coEvery { getAyahById.forIds(any()) } returns emptyMap()
        every { quran.getAyahById } returns getAyahById
        quranBookmarks.value = (1..50).map { quranBookmark(it, it.toLong()) }

        viewModel()
        advanceUntilIdle()

        // Was a suspend call per row inside the collector: 50 bookmarks meant 50
        // sequential round-trips on every re-emission, and clearing them all re-emitted
        // once per delete — O(N^2) on the most destructive action in the feature.
        coVerify(exactly = 1) { getAyahById.forIds(any()) }
        coVerify(exactly = 0) { getAyahById.invoke(any()) }
    }

    @Test
    fun `search filters across title, note and arabic text`() = runTest {
        quranBookmarks.value = listOf(quranBookmark(1, 100), quranBookmark(2, 200))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.SetSearchQuery("zzz-no-match"))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks).isEmpty()

        vm.onEvent(BookmarksEvent.SetSearchQuery(""))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks).hasSize(2)
    }

    @Test
    fun `sort order is applied and is not permanently newest-first`() = runTest {
        quranBookmarks.value = listOf(quranBookmark(1, 100), quranBookmark(2, 200))
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks.first().id).isEqualTo("quran_2")

        vm.onEvent(BookmarksEvent.SetSortOrder(BookmarkSortOrder.DATE_OLDEST))
        advanceUntilIdle()

        assertThat(vm.bookmarksState.value.filteredBookmarks.first().id).isEqualTo("quran_1")
    }
}
