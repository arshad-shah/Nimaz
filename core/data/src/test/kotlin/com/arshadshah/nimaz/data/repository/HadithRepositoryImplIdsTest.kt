package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.HadithChapterCount
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The identifier seam between the hadith screens and the hadith tables.
 *
 * The domain model carries **strings** — `Hadith.id`, `HadithBook.id`, `HadithChapter.id` — and
 * every table underneath is keyed by an **Int**. So every read in this repository parses, and
 * every parse has a failure arm that a route argument can reach: a deep link, a restored
 * back-stack entry, a bookmark to a book a later corpus renumbered.
 *
 * The two arms are deliberately different and the difference is the whole point:
 *
 *  - a lookup of *one specific thing* returns **null** — "no such hadith", which the screen
 *    renders as an empty state;
 *  - a *list* query falls back to **0** — an id no book has, so the flow is empty rather than
 *    absent, and a `LazyColumn` gets a list to be empty of.
 *
 * Get either wrong and the failure is a crash on a deep link (`toInt()` on "bukhari") or, worse,
 * book 0's hadiths silently rendered under another book's title.
 *
 * Chapters are the sharper case still: there is **no chapters table**. They are derived from
 * `GROUP BY chapter_id`, their ids are composite (`"{bookId}_{chapterId}"`), and the stored id
 * is 0-based while the number a reader sees is 1-based. An off-by-one here is not an error —
 * it is every chapter heading in the app being wrong by one.
 */
class HadithRepositoryImplIdsTest {

    private lateinit var hadithDao: HadithDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: HadithRepositoryImpl

    @Before
    fun setUp() {
        hadithDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)
        coEvery { searchIndex.isAvailable() } returns false
        every { hadithDao.getAllBooks() } returns flowOf(listOf(book(1)))
        repository = HadithRepositoryImpl(hadithDao, bookmarkDao, searchIndex)
    }

    // ── a single thing: an unparseable id means "no such thing" ───────────────

    @Test
    fun `a book id that is not a number is no book at all`() = runTest {
        assertThat(repository.getBookById("bukhari")).isNull()

        // Never `toInt()`: a deep link carrying a slug would crash the screen it opened.
        coVerify(exactly = 0) { hadithDao.getBookById(any()) }
    }

    @Test
    fun `a book id that is a number is looked up`() = runTest {
        coEvery { hadithDao.getBookById(1) } returns book(1)

        assertThat(repository.getBookById("1")!!.nameEnglish).isEqualTo("Sahih al-Bukhari")
    }

    @Test
    fun `a hadith id that is not a number is no hadith at all`() = runTest {
        assertThat(repository.getHadithById("first")).isNull()
        assertThat(repository.getHadithByNumber("bukhari", 1)).isNull()

        coVerify(exactly = 0) { hadithDao.getHadithById(any()) }
        coVerify(exactly = 0) { hadithDao.getHadithByNumber(any(), any()) }
    }

    @Test
    fun `a hadith is looked up by its number within a book`() = runTest {
        coEvery { hadithDao.getHadithByNumber(1, 7) } returns hadith(id = 7)

        assertThat(repository.getHadithByNumber("1", 7)!!.id).isEqualTo("7")
    }

    @Test
    fun `a reference is a string all the way down and is not parsed`() = runTest {
        coEvery { hadithDao.getHadithByReference("bukhari:1") } returns hadith(id = 1)

        assertThat(repository.getHadithByReference("bukhari:1")!!.reference).isEqualTo("bukhari:1")
    }

    @Test
    fun `asking for a list of ids drops the ones that are not numbers`() = runTest {
        coEvery { hadithDao.getHadithsByIds(listOf(1, 3)) } returns listOf(hadith(1), hadith(3))

        assertThat(repository.getHadithsByIds(listOf("1", "not-an-id", "3")).map { it.id })
            .containsExactly("1", "3")
    }

    @Test
    fun `a list of ids none of which are numbers queries nothing`() = runTest {
        assertThat(repository.getHadithsByIds(listOf("a", "b"))).isEmpty()

        coVerify(exactly = 0) { hadithDao.getHadithsByIds(any()) }
    }

    // ── a list: an unparseable id means "empty", not "absent" ─────────────────

    @Test
    fun `an unparseable book id lists no hadiths rather than every hadith`() = runTest {
        every { hadithDao.getHadithsByBook(0) } returns flowOf(emptyList())

        assertThat(repository.getHadithsByBook("bukhari").first()).isEmpty()
        // Book 0 exists in no corpus, so the flow is empty — but it *is* a flow, which is what
        // a LazyColumn needs in order to be empty of anything.
        verifyBookQueried(0)
    }

    @Test
    fun `a bookmark list for an unparseable book falls back the same way`() = runTest {
        every { bookmarkDao.inContext(BookmarkKind.HADITH, 0) } returns flowOf(emptyList())

        assertThat(repository.getBookmarksByBook("bukhari").first()).isEmpty()
    }

    @Test
    fun `a bookmark flag for an unparseable hadith observes nothing rather than crashing`() =
        runTest {
            every { bookmarkDao.observeIsBookmarked(BookmarkKind.HADITH, 0) } returns flowOf(false)

            assertThat(repository.isHadithBookmarked("first").first()).isFalse()
        }

    // ── chapters, which are not a table ───────────────────────────────────────

    @Test
    fun `a stored chapter id is zero based and the number shown is one based`() = runTest {
        every { hadithDao.getChapterCountsForBook(1) } returns
            flowOf(listOf(HadithChapterCount(chapterId = 0, hadithCount = 7)))

        val chapter = repository.getChaptersByBook("1").first().single()

        // The raw id keeps loading working; the number is what the reader sees.
        assertThat(chapter.id).isEqualTo("1_0")
        assertThat(chapter.chapterNumber).isEqualTo(1)
        assertThat(chapter.nameEnglish).isEqualTo("Chapter 1")
        assertThat(chapter.nameArabic).isEqualTo("الباب 1")
        assertThat(chapter.hadithCount).isEqualTo(7)
    }

    @Test
    fun `a chapter's count is the real one, not a placeholder`() = runTest {
        every { hadithDao.getChapterCountsForBook(1) } returns flowOf(
            listOf(
                HadithChapterCount(chapterId = 0, hadithCount = 7),
                HadithChapterCount(chapterId = 1, hadithCount = 53),
            )
        )

        val counts = repository.getChaptersByBook("1").first().map { it.hadithCount }

        assertThat(counts).containsExactly(7, 53).inOrder()
    }

    @Test
    fun `a composite chapter id round trips back into its parts`() = runTest {
        val chapter = repository.getChapterById("3_11")!!

        assertThat(chapter.bookId).isEqualTo("3")
        assertThat(chapter.chapterNumber).isEqualTo(12)
        assertThat(chapter.id).isEqualTo("3_11")
    }

    @Test
    fun `a chapter id that is not composite opens nothing`() = runTest {
        assertThat(repository.getChapterById("11")).isNull()
        assertThat(repository.getChapterById("1_2_3")).isNull()
    }

    @Test
    fun `a composite chapter id whose chapter part is not a number opens nothing`() = runTest {
        assertThat(repository.getChapterById("1_intro")).isNull()
    }

    @Test
    fun `a chapter's hadiths are read by the raw chapter id, not the displayed number`() =
        runTest {
            every { hadithDao.getHadithsByChapter(11) } returns flowOf(listOf(hadith(1)))

            assertThat(repository.getHadithsByChapter("3_11").first()).hasSize(1)
        }

    @Test
    fun `a bare chapter id still resolves, for a caller that has only the number`() = runTest {
        every { hadithDao.getHadithsByChapter(11) } returns flowOf(listOf(hadith(1)))

        assertThat(repository.getHadithsByChapter("11").first()).hasSize(1)
    }

    @Test
    fun `a chapter id that resolves to nothing reads chapter zero rather than crashing`() =
        runTest {
            every { hadithDao.getHadithsByChapter(0) } returns flowOf(emptyList())

            assertThat(repository.getHadithsByChapter("intro").first()).isEmpty()
            assertThat(repository.getHadithsByChapter("1_intro").first()).isEmpty()
        }

    @Test
    fun `searching chapters matches the English name and the Arabic one`() = runTest {
        every { hadithDao.getChapterCountsForBook(1) } returns flowOf(
            listOf(
                HadithChapterCount(chapterId = 0, hadithCount = 1),
                HadithChapterCount(chapterId = 1, hadithCount = 1),
            )
        )

        assertThat(repository.searchChapters("1", "chapter 2").first().map { it.id })
            .containsExactly("1_1")
        assertThat(repository.searchChapters("1", "الباب 1").first().map { it.id })
            .containsExactly("1_0")
        assertThat(repository.searchChapters("1", "nothing").first()).isEmpty()
    }

    // ── hadith of the day ─────────────────────────────────────────────────────

    @Test
    fun `an install with no hadith corpus has no hadith of the day`() = runTest {
        coEvery { hadithDao.getHadithCount() } returns 0

        assertThat(repository.getHadithOfTheDay()).isNull()
        // The modulo would divide by zero; the guard is what stops the home screen crashing on
        // an install whose content artifact has not landed yet.
        coVerify(exactly = 0) { hadithDao.getHadithByOffset(any()) }
    }

    @Test
    fun `the hadith of the day is picked deterministically from the day of the year`() = runTest {
        coEvery { hadithDao.getHadithCount() } returns 34_532
        val offset = java.time.LocalDate.now().dayOfYear % 34_532
        coEvery { hadithDao.getHadithByOffset(offset) } returns hadith(id = 99)

        assertThat(repository.getHadithOfTheDay()!!.id).isEqualTo("99")
        // Twice in a day is the same hadith, or the home screen changes it on every scroll.
        assertThat(repository.getHadithOfTheDay()!!.id).isEqualTo("99")
    }

    @Test
    fun `a corpus smaller than the day of the year still resolves an offset in range`() =
        runTest {
            coEvery { hadithDao.getHadithCount() } returns 10
            coEvery { hadithDao.getHadithByOffset(any()) } answers {
                hadith(id = firstArg<Int>())
            }

            val id = repository.getHadithOfTheDay()!!.id.toInt()

            assertThat(id).isAtLeast(0)
            assertThat(id).isLessThan(10)
        }

    @Test
    fun `an offset past the end of the corpus reports nothing`() = runTest {
        coEvery { hadithDao.getHadithByOffset(999_999) } returns null

        assertThat(repository.getHadithByOffset(999_999)).isNull()
        assertThat(repository.getHadithCount()).isEqualTo(0)
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    fun `without an index the search scans, and names each book it found`() = runTest {
        coEvery { searchIndex.isAvailable() } returns false
        every { hadithDao.searchHadiths("intention") } returns flowOf(listOf(hadith(1)))

        val result = repository.searchHadiths("intention").first().single()

        assertThat(result.bookName).isEqualTo("Sahih al-Bukhari")
        assertThat(result.chapterName).isEqualTo("Chapter 1")
        assertThat(result.matchedText).isEqualTo("Actions are by intentions")
    }

    @Test
    fun `with an index the scan is not run at all`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("النية", SearchKind.HADITH, null, any()) } returns
            listOf("1", "not-an-id")
        coEvery { hadithDao.getHadithsByIds(listOf(1)) } returns listOf(hadith(1))

        assertThat(repository.searchHadiths("النية").first()).hasSize(1)
        // `text_arabic LIKE '%…%'` scans 36 MB and matches nothing for a vocalised matn.
        verifyNoScan()
    }

    @Test
    fun `a hadith whose book is not in the catalogue is labelled by its id`() = runTest {
        every { hadithDao.getAllBooks() } returns flowOf(emptyList())
        every { hadithDao.searchHadiths(any()) } returns flowOf(listOf(hadith(1, bookId = 42)))

        assertThat(repository.searchHadiths("x").first().single().bookName).isEqualTo("Book 42")
    }

    @Test
    fun `searching within a book scopes the query to that book`() = runTest {
        every { hadithDao.searchHadithsInBook(1, "intention") } returns flowOf(listOf(hadith(1)))

        assertThat(repository.searchHadithsInBook("1", "intention").first().single().bookName)
            .isEqualTo("Sahih al-Bukhari")
    }

    @Test
    fun `searching within an unparseable book searches book zero, which is empty`() = runTest {
        every { hadithDao.searchHadithsInBook(0, "x") } returns flowOf(emptyList())

        assertThat(repository.searchHadithsInBook("bukhari", "x").first()).isEmpty()
    }

    // ── grades ────────────────────────────────────────────────────────────────

    @Test
    fun `every grade is queried by the lowercase value the corpus stores`() = runTest {
        every { hadithDao.getHadithsByGrade(any()) } returns flowOf(emptyList())

        HadithGrade.entries.forEach { repository.getHadithsByGrade(it).first() }

        listOf("sahih", "hasan", "daif", "mawdu", "unknown").forEach {
            verifyGradeQueried(it)
        }
    }

    @Test
    fun `a hadith with a blank chain of narration has no chain rather than an empty one`() =
        runTest {
            coEvery { hadithDao.getHadithById(1) } returns hadith(1).copy(narratorChain = "   ")

            // The reader hides the section when there is no chain; an empty string would draw
            // a heading over nothing, and a guessed chain is never shown.
            assertThat(repository.getHadithById("1")!!.narratorChain).isNull()
        }

    @Test
    fun `a hadith with a curated chain keeps it`() = runTest {
        coEvery { hadithDao.getHadithById(1) } returns hadith(1).copy(narratorChain = "A > B")

        assertThat(repository.getHadithById("1")!!.narratorChain).isEqualTo("A > B")
    }

    // ── bookmarks ─────────────────────────────────────────────────────────────

    @Test
    fun `bookmarking a hadith files it under its book and number`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.HADITH, 7) } returns null
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleBookmark(hadithId = "7", bookId = "1", hadithNumber = 12)

        assertThat(saved.single().contextId).isEqualTo(1)
        assertThat(saved.single().ordinal).isEqualTo(12)
        assertThat(saved.single().bookmarked).isTrue()
    }

    @Test
    fun `un-bookmarking a hadith that is not favourited deletes the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.HADITH, 7) } returns
            mark(targetId = 7, bookmarked = true)

        repository.toggleBookmark("7", "1", 12)

        coVerify { bookmarkDao.delete(BookmarkKind.HADITH, 7) }
    }

    @Test
    fun `bookmarking a hadith that is only favourited keeps the favourite`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.HADITH, 7) } returns
            mark(targetId = 7, bookmarked = false, favourite = true, createdAt = 5L)
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleBookmark("7", "1", 12)

        assertThat(saved.single().favourite).isTrue()
        assertThat(saved.single().bookmarked).isTrue()
        // One row, one history: the favourite's creation time is the row's.
        assertThat(saved.single().createdAt).isEqualTo(5L)
    }

    @Test
    fun `toggling a bookmark on an unparseable hadith writes nothing`() = runTest {
        repository.toggleBookmark("first", "1", 1)
        repository.deleteBookmark("first")

        coVerify(exactly = 0) { bookmarkDao.upsert(any()) }
        coVerify(exactly = 0) { bookmarkDao.delete(any(), any()) }
    }

    @Test
    fun `a bookmark reads back with the book and number it was filed under`() = runTest {
        every { bookmarkDao.bookmarks(BookmarkKind.HADITH) } returns
            flowOf(listOf(mark(targetId = 7, contextId = 1, ordinal = 12, note = "n", colour = "c")))

        val bookmark = repository.getAllBookmarks().first().single()

        assertThat(bookmark.hadithId).isEqualTo("7")
        assertThat(bookmark.bookId).isEqualTo("1")
        assertThat(bookmark.hadithNumber).isEqualTo(12)
        assertThat(bookmark.note).isEqualTo("n")
        assertThat(bookmark.color).isEqualTo("c")
    }

    @Test
    fun `a bookmark row with no context still reads back`() = runTest {
        every { bookmarkDao.bookmarks(BookmarkKind.HADITH) } returns
            flowOf(listOf(mark(targetId = 7, contextId = null, ordinal = null)))

        val bookmark = repository.getAllBookmarks().first().single()

        assertThat(bookmark.bookId).isEqualTo("0")
        assertThat(bookmark.hadithNumber).isEqualTo(0)
    }

    @Test
    fun `looking up a bookmark by an unparseable hadith id finds none`() = runTest {
        assertThat(repository.getBookmarkByHadithId("first")).isNull()
    }

    @Test
    fun `looking up a bookmark by hadith id reads the consolidated row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.HADITH, 7) } returns mark(targetId = 7)

        assertThat(repository.getBookmarkByHadithId("7")!!.hadithId).isEqualTo("7")
    }

    @Test
    fun `inserting and updating a bookmark both write one consolidated row`() = runTest {
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit
        val bookmark = HadithBookmark(
            id = 0, hadithId = "7", bookId = "1", hadithNumber = 12,
            note = "n", color = "c", createdAt = 1L, updatedAt = 2L,
        )

        repository.insertBookmark(bookmark)
        repository.updateBookmark(bookmark.copy(note = "edited"))

        assertThat(saved).hasSize(2)
        assertThat(saved.map { it.kind }.toSet()).containsExactly(BookmarkKind.HADITH)
        assertThat(saved.map { it.targetId }.toSet()).containsExactly(7)
        assertThat(saved.last().note).isEqualTo("edited")
    }

    @Test
    fun `a bookmark whose hadith id is not a number writes against target zero`() = runTest {
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.insertBookmark(
            HadithBookmark(
                id = 0, hadithId = "first", bookId = "bukhari", hadithNumber = 1,
                note = null, color = null, createdAt = 1L, updatedAt = 1L,
            )
        )

        assertThat(saved.single().targetId).isEqualTo(0)
        assertThat(saved.single().contextId).isNull()
    }

    @Test
    fun `deleting a bookmark by a numeric id removes the row`() = runTest {
        repository.deleteBookmark("7")

        coVerify { bookmarkDao.delete(BookmarkKind.HADITH, 7) }
    }

    // ── initialisation ────────────────────────────────────────────────────────

    @Test
    fun `an artifact with books in it counts as initialised`() = runTest {
        repository.initializeHadithData()

        assertThat(repository.isDataInitialized()).isTrue()
    }

    @Test
    fun `an artifact with no books counts as uninitialised`() = runTest {
        every { hadithDao.getAllBooks() } returns flowOf(emptyList())

        assertThat(repository.isDataInitialized()).isFalse()
    }

    @Test
    fun `a book reads back with the counts the catalogue holds`() = runTest {
        val catalogue = repository.getAllBooks().first().single()

        assertThat(catalogue.id).isEqualTo("1")
        assertThat(catalogue.totalHadiths).isEqualTo(7563)
        assertThat(catalogue.authorName).isEqualTo("Al-Bukhari")
        assertThat(catalogue.displayOrder).isEqualTo(1)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun verifyBookQueried(id: Int) = verify { hadithDao.getHadithsByBook(id) }

    private fun verifyGradeQueried(grade: String) = verify { hadithDao.getHadithsByGrade(grade) }

    private fun verifyNoScan() = verify(exactly = 0) { hadithDao.searchHadiths(any()) }

    private fun book(id: Int) = HadithBookEntity(
        id = id, nameEnglish = "Sahih al-Bukhari", nameArabic = "البخاري",
        author = "Al-Bukhari", hadithCount = 7563, description = "Authentic collection",
        icon = "bukhari_icon",
    )

    private fun hadith(id: Int, bookId: Int = 1, chapterId: Int = 1) = HadithEntity(
        id = id, bookId = bookId, chapterId = chapterId, numberInBook = id,
        numberInChapter = id, textArabic = "إنما الأعمال بالنيات",
        textEnglish = "Actions are by intentions", narrator = "Umar",
        grade = "sahih", reference = "bukhari:1", narratorChain = null,
    )

    private fun mark(
        targetId: Int,
        bookmarked: Boolean = true,
        favourite: Boolean = false,
        note: String? = null,
        colour: String? = null,
        contextId: Int? = 1,
        ordinal: Int? = 12,
        createdAt: Long = 1L,
    ) = BookmarkEntity(
        kind = BookmarkKind.HADITH,
        targetId = targetId,
        bookmarked = bookmarked,
        favourite = favourite,
        note = note,
        colour = colour,
        contextId = contextId,
        ordinal = ordinal,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
