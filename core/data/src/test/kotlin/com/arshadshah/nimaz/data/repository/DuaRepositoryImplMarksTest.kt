package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressEntity
import com.arshadshah.nimaz.data.local.user.ProgressKind
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The dua marks and counts, and the string→Int seam they cross.
 *
 * A dua is favourited, bookmarked *and* counted, and all three now live in two consolidated
 * tables keyed by `kind`. The failures this pins are the ones nothing above the repository
 * could report:
 *
 *  - **un-favouriting must not take the bookmark with it.** They share one row, so clearing the
 *    row rather than the flag silently loses a plain bookmark the user made separately;
 *  - **every id is a string in the domain and an Int in the table.** A `toIntOrNull()` whose
 *    failure arm is wrong is either a crash on a deep link or, in the list case, category 0's
 *    duas rendered under another category's title;
 *  - **a write against an unparseable id must do nothing** — not write against target 0, which
 *    would attach a favourite to whatever dua happens to be there.
 */
class DuaRepositoryImplMarksTest {

    private lateinit var duaDao: DuaDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var progressDao: ProgressDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: DuaRepositoryImpl

    @Before
    fun setUp() {
        duaDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        progressDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)
        coEvery { searchIndex.isAvailable() } returns false
        every { duaDao.getAllCategories() } returns flowOf(listOf(category(1)))
        repository = DuaRepositoryImpl(duaDao, bookmarkDao, progressDao, searchIndex)
    }

    // ── one row, two flags ────────────────────────────────────────────────────

    @Test
    fun `favouriting a dua that is not marked at all creates the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.DUA, 5) } returns null
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleFavorite(duaId = "5", categoryId = "1")

        assertThat(saved.single().favourite).isTrue()
        assertThat(saved.single().bookmarked).isTrue()
        assertThat(saved.single().contextId).isEqualTo(1)
    }

    @Test
    fun `un-favouriting a dua that is also bookmarked keeps the bookmark`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.DUA, 5) } returns
            mark(5, bookmarked = true, favourite = true)

        repository.toggleFavorite("5", "1")

        // Clearing the flag, not the row: the plain bookmark was a separate act.
        coVerify { bookmarkDao.clearFavourite(BookmarkKind.DUA, 5, any()) }
        coVerify(exactly = 0) { bookmarkDao.delete(BookmarkKind.DUA, 5) }
    }

    @Test
    fun `un-favouriting a dua that is only favourited removes the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.DUA, 5) } returns
            mark(5, bookmarked = false, favourite = true)

        repository.toggleFavorite("5", "1")

        coVerify { bookmarkDao.delete(BookmarkKind.DUA, 5) }
    }

    @Test
    fun `favouriting a dua that is already bookmarked sets the flag on the same row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.DUA, 5) } returns
            mark(5, bookmarked = true, favourite = false, note = "keep me")
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleFavorite("5", "1")

        assertThat(saved.single().favourite).isTrue()
        assertThat(saved.single().bookmarked).isTrue()
        assertThat(saved.single().note).isEqualTo("keep me")
    }

    @Test
    fun `favouriting a dua whose id is not a number writes nothing at all`() = runTest {
        repository.toggleFavorite("morning-adhkar", "1")

        // Falling back to target 0 would favourite whatever dua happens to hold that id.
        coVerify(exactly = 0) { bookmarkDao.find(any(), any()) }
        coVerify(exactly = 0) { bookmarkDao.upsert(any()) }
    }

    @Test
    fun `a bookmark reads back with its category and favourite flag`() = runTest {
        every { bookmarkDao.bookmarks(BookmarkKind.DUA) } returns
            flowOf(listOf(mark(5, favourite = true, note = "n", contextId = 3)))

        val bookmark = repository.getAllBookmarks().first().single()

        assertThat(bookmark.duaId).isEqualTo("5")
        assertThat(bookmark.categoryId).isEqualTo("3")
        assertThat(bookmark.isFavorite).isTrue()
        assertThat(bookmark.note).isEqualTo("n")
    }

    @Test
    fun `a bookmark row with no category still reads back`() = runTest {
        every { bookmarkDao.favourites(BookmarkKind.DUA) } returns
            flowOf(listOf(mark(5, contextId = null)))

        assertThat(repository.getFavoriteDuas().first().single().categoryId).isEqualTo("0")
    }

    @Test
    fun `looking up a bookmark by an unparseable id finds none`() = runTest {
        assertThat(repository.getBookmarkByDuaId("morning")).isNull()

        coVerify(exactly = 0) { bookmarkDao.find(any(), any()) }
    }

    @Test
    fun `looking up a bookmark by a numeric id reads the consolidated row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.DUA, 5) } returns mark(5)

        assertThat(repository.getBookmarkByDuaId("5")!!.duaId).isEqualTo("5")
    }

    @Test
    fun `the bookmarked and favourite flags are observed independently`() = runTest {
        every { bookmarkDao.observeIsBookmarked(BookmarkKind.DUA, 5) } returns flowOf(true)
        every { bookmarkDao.observeIsFavourite(BookmarkKind.DUA, 5) } returns flowOf(false)

        assertThat(repository.isDuaBookmarked("5").first()).isTrue()
        assertThat(repository.isDuaFavorite("5").first()).isFalse()
    }

    @Test
    fun `an unparseable id observes dua zero rather than crashing`() = runTest {
        every { bookmarkDao.observeIsBookmarked(BookmarkKind.DUA, 0) } returns flowOf(false)
        every { bookmarkDao.observeIsFavourite(BookmarkKind.DUA, 0) } returns flowOf(false)

        assertThat(repository.isDuaBookmarked("morning").first()).isFalse()
        assertThat(repository.isDuaFavorite("morning").first()).isFalse()
    }

    @Test
    fun `inserting and updating a bookmark both write one consolidated row`() = runTest {
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit
        val bookmark = DuaBookmark(
            id = 0, duaId = "5", categoryId = "1", note = "n",
            isFavorite = true, createdAt = 1L, updatedAt = 2L,
        )

        repository.insertBookmark(bookmark)
        repository.updateBookmark(bookmark.copy(note = "edited"))

        assertThat(saved.map { it.kind }.toSet()).containsExactly(BookmarkKind.DUA)
        assertThat(saved.map { it.targetId }.toSet()).containsExactly(5)
        assertThat(saved.first().favourite).isTrue()
        assertThat(saved.last().note).isEqualTo("edited")
    }

    @Test
    fun `a bookmark whose ids are not numbers writes against dua zero and no category`() =
        runTest {
            val saved = mutableListOf<BookmarkEntity>()
            coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

            repository.insertBookmark(
                DuaBookmark(
                    id = 0, duaId = "morning", categoryId = "adhkar", note = null,
                    isFavorite = false, createdAt = 1L, updatedAt = 1L,
                )
            )

            assertThat(saved.single().targetId).isEqualTo(0)
            assertThat(saved.single().contextId).isNull()
        }

    @Test
    fun `deleting a bookmark by an unparseable id deletes nothing`() = runTest {
        repository.deleteBookmark("morning")
        repository.deleteBookmark("5")

        coVerify(exactly = 1) { bookmarkDao.delete(BookmarkKind.DUA, 5) }
    }

    // ── daily counts ──────────────────────────────────────────────────────────

    @Test
    fun `a count for a day reads back with its target`() = runTest {
        coEvery { progressDao.find(ProgressKind.DUA, 5, 900L) } returns
            progress(targetId = 5, date = 900L, completed = 3, total = 7)

        val recorded = repository.getProgressForDuaOnDate("5", 900L)!!

        assertThat(recorded.duaId).isEqualTo("5")
        assertThat(recorded.completedCount).isEqualTo(3)
        assertThat(recorded.targetCount).isEqualTo(7)
        assertThat(recorded.isCompleted).isFalse()
    }

    @Test
    fun `a count with no recorded target reports zero rather than dropping the row`() = runTest {
        coEvery { progressDao.find(ProgressKind.DUA, 5, 900L) } returns
            progress(targetId = 5, date = 900L, completed = 1, total = null)

        assertThat(repository.getProgressForDuaOnDate("5", 900L)!!.targetCount).isEqualTo(0)
    }

    @Test
    fun `a count for an unparseable dua is not looked up`() = runTest {
        assertThat(repository.getProgressForDuaOnDate("morning", 900L)).isNull()

        coVerify(exactly = 0) { progressDao.find(any(), any(), any()) }
    }

    @Test
    fun `a day's counts come from the dua kind only`() = runTest {
        every { progressDao.onDate(ProgressKind.DUA, 900L) } returns
            flowOf(listOf(progress(targetId = 5, date = 900L)))

        assertThat(repository.getProgressForDate(900L).first().map { it.duaId })
            .containsExactly("5")
    }

    @Test
    fun `a dua's history is filtered to that dua`() = runTest {
        every { progressDao.ofKind(ProgressKind.DUA) } returns flowOf(
            listOf(
                progress(targetId = 5, date = 900L),
                progress(targetId = 6, date = 900L),
            )
        )

        assertThat(repository.getProgressHistoryForDua("5").first().map { it.duaId })
            .containsExactly("5")
    }

    @Test
    fun `an unparseable id's history is dua zero's, which is empty`() = runTest {
        every { progressDao.ofKind(ProgressKind.DUA) } returns
            flowOf(listOf(progress(targetId = 5, date = 900L)))

        assertThat(repository.getProgressHistoryForDua("morning").first()).isEmpty()
    }

    @Test
    fun `counting up and down goes through the shared progress table`() = runTest {
        repository.incrementDuaProgress("5", 900L, targetCount = 7)
        repository.decrementDuaProgress("5", 900L)

        coVerify { progressDao.increment(ProgressKind.DUA, 5, 900L, 7) }
        coVerify { progressDao.decrement(ProgressKind.DUA, 5, 900L) }
    }

    @Test
    fun `counting against an unparseable dua does nothing`() = runTest {
        repository.incrementDuaProgress("morning", 900L, 7)
        repository.decrementDuaProgress("morning", 900L)

        coVerify(exactly = 0) { progressDao.increment(any(), any(), any(), any()) }
        coVerify(exactly = 0) { progressDao.decrement(any(), any(), any()) }
    }

    // ── reads ─────────────────────────────────────────────────────────────────

    @Test
    fun `a category reads back with its icon and count`() = runTest {
        val category = repository.getAllCategories().first().single()

        assertThat(category.id).isEqualTo("1")
        assertThat(category.nameEnglish).isEqualTo("Morning & Evening")
        assertThat(category.iconName).isEqualTo("sunrise")
        assertThat(category.duaCount).isEqualTo(11)
    }

    @Test
    fun `an unparseable category id opens no category and lists no duas`() = runTest {
        every { duaDao.getDuasByCategory(0) } returns flowOf(emptyList())

        assertThat(repository.getCategoryById("adhkar")).isNull()
        assertThat(repository.getDuasByCategoryOnce("adhkar")).isEmpty()
        assertThat(repository.getDuasByCategory("adhkar").first()).isEmpty()
        coVerify(exactly = 0) { duaDao.getCategoryById(any()) }
        coVerify(exactly = 0) { duaDao.getDuasByCategoryOnce(any()) }
    }

    @Test
    fun `a category's duas are read once for a caller that does not want a flow`() = runTest {
        coEvery { duaDao.getDuasByCategoryOnce(1) } returns listOf(dua(5))

        assertThat(repository.getDuasByCategoryOnce("1").map { it.id }).containsExactly("5")
    }

    @Test
    fun `a dua reads back with everything the screen renders`() = runTest {
        coEvery { duaDao.getDuaById(5) } returns dua(5)

        val read = repository.getDuaById("5")!!

        assertThat(read.categoryId).isEqualTo("1")
        assertThat(read.textTransliteration).isEqualTo("Bismillah")
        assertThat(read.benefits).isEqualTo("Protection")
        assertThat(read.repeatCount).isEqualTo(3)
        assertThat(read.reference).isEqualTo("Abu Dawud")
    }

    @Test
    fun `an unparseable dua id opens nothing`() = runTest {
        assertThat(repository.getDuaById("morning")).isNull()
        assertThat(repository.getDuasByIds(listOf("a", "b"))).isEmpty()
        coVerify(exactly = 0) { duaDao.getDuaById(any()) }
        coVerify(exactly = 0) { duaDao.getDuasByIds(any()) }
    }

    @Test
    fun `a list of ids drops the ones that are not numbers`() = runTest {
        coEvery { duaDao.getDuasByIds(listOf(5)) } returns listOf(dua(5))

        assertThat(repository.getDuasByIds(listOf("5", "morning")).map { it.id })
            .containsExactly("5")
    }

    @Test
    fun `an occasion is searched for by name because there is no occasion column`() = runTest {
        every { duaDao.searchDuas("traveling") } returns flowOf(listOf(dua(5)))

        assertThat(repository.getDuasByOccasion(DuaOccasion.TRAVELING).first()).hasSize(1)
    }

    @Test
    fun `without an index a dua search scans and names each category`() = runTest {
        coEvery { searchIndex.isAvailable() } returns false
        every { duaDao.searchDuas("protection") } returns flowOf(listOf(dua(5)))

        val result = repository.searchDuas("protection").first().single()

        assertThat(result.categoryName).isEqualTo("Morning & Evening")
        assertThat(result.matchedText).isEqualTo("In the name of God")
    }

    @Test
    fun `with an index the scan is not run at all`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("بسم", SearchKind.DUA, null, any()) } returns
            listOf("5", "not-an-id")
        coEvery { duaDao.getDuasByIds(listOf(5)) } returns listOf(dua(5))

        assertThat(repository.searchDuas("بسم").first()).hasSize(1)
        // The Arabic is vocalised and the transliteration full of macrons: `LIKE` reaches
        // neither, and the index folds both.
        io.mockk.verify(exactly = 0) { duaDao.searchDuas(any()) }
    }

    @Test
    fun `a dua whose category is not in the catalogue is labelled with nothing`() = runTest {
        every { duaDao.getAllCategories() } returns flowOf(emptyList())
        every { duaDao.searchDuas(any()) } returns flowOf(listOf(dua(5, categoryId = 42)))

        assertThat(repository.searchDuas("x").first().single().categoryName).isEmpty()
    }

    @Test
    fun `an artifact with no categories counts as uninitialised`() = runTest {
        assertThat(repository.isDataInitialized()).isTrue()

        every { duaDao.getAllCategories() } returns flowOf(emptyList())
        assertThat(repository.isDataInitialized()).isFalse()
    }

    @Test
    fun `the database flow is built when it is collected, not when it is asked for`() = runTest {
        // Callers hold these flows without collecting them, so building the DB flow eagerly
        // would query on construction.
        val flow = repository.getAllCategories()

        io.mockk.verify(exactly = 0) { duaDao.getAllCategories() }
        assertThat(flow.first()).hasSize(1)
        io.mockk.verify { duaDao.getAllCategories() }
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun category(id: Int) = DuaCategoryEntity(
        id = id,
        nameEnglish = "Morning & Evening",
        nameArabic = "أذكار الصباح والمساء",
        icon = "sunrise",
        displayOrder = 1,
        duaCount = 11,
    )

    private fun dua(id: Int, categoryId: Int = 1) = DuaEntity(
        id = id,
        categoryId = categoryId,
        titleEnglish = "Bismillah",
        titleArabic = "بسم الله",
        textArabic = "بِسْمِ ٱللَّهِ",
        transliteration = "Bismillah",
        translation = "In the name of God",
        source = "Abu Dawud",
        virtue = "Protection",
        repeatCount = 3,
        audioFile = null,
        displayOrder = 1,
    )

    private fun mark(
        targetId: Int,
        bookmarked: Boolean = true,
        favourite: Boolean = false,
        note: String? = null,
        contextId: Int? = 1,
    ) = BookmarkEntity(
        kind = BookmarkKind.DUA,
        targetId = targetId,
        bookmarked = bookmarked,
        favourite = favourite,
        note = note,
        contextId = contextId,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun progress(
        targetId: Int,
        date: Long,
        completed: Int = 0,
        total: Int? = 7,
    ) = ProgressEntity(
        kind = ProgressKind.DUA,
        targetId = targetId,
        date = date,
        completed = completed,
        total = total,
        isCompleted = false,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
