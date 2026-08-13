package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.AyahWithText
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.SearchType
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The largest untested repository in the app — 900 lines, and the layer where content-database
 * rows become domain objects (audit §3.4, issue #476).
 *
 * The mapping half of it is not what these tests are for: a `toDomain()` that forgets a field
 * shows up the moment anyone opens the screen. What is covered here is the logic that fails
 * *silently* —
 *
 *  - bookmarks and favourites share one row with two flags, so toggling one can quietly destroy
 *    the other, and nothing in the UI would say so;
 *  - reading progress is read-modify-write on a single row, so a careless write loses a running
 *    total the user has been accumulating for months;
 *  - search has two implementations — the shipped index and a `LIKE` fallback for installs that
 *    predate it — and the whole point is that nothing above the repository can tell which ran;
 *  - the layout cache and the thematic-content probe are memoised, and a memo that re-queries is
 *    invisible except as a slow screen.
 */
class QuranRepositoryImplTest {

    private lateinit var quranDao: QuranDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var readingProgressDao: ReadingProgressDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: QuranRepositoryImpl

    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        quranDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        readingProgressDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)

        // No index unless a test says so: that is the state of an install that predates it.
        coEvery { searchIndex.isAvailable() } returns false
        every { quranDao.getAllSurahs() } returns flowOf(listOf(surahEntity()))

        repository = QuranRepositoryImpl(quranDao, bookmarkDao, readingProgressDao, searchIndex)
    }

    // ── one row, two flags ────────────────────────────────────────────────────
    //
    // The comment in the repository says it outright: "a verse can be bookmarked *and*
    // favourited: that was two tables and is now two flags on one row, which is why toggling
    // one must not disturb the other." These are that sentence, as tests.

    @Test
    fun `bookmarking a verse that is neither creates the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null

        repository.toggleBookmark(ayahId = 262, surahNumber = 2, ayahNumber = 255)

        val row = slot<BookmarkEntity>()
        coVerify { bookmarkDao.upsert(capture(row)) }
        assertThat(row.captured.bookmarked).isTrue()
        assertThat(row.captured.favourite).isFalse()
        assertThat(row.captured.targetId).isEqualTo(262)
    }

    @Test
    fun `un-bookmarking a verse that is also a favourite keeps the favourite`() = runTest {
        // The case the shared row exists to get right. Deleting here would silently drop a
        // favourite the user never touched.
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            bookmarkRow(bookmarked = true, favourite = true)

        repository.toggleBookmark(ayahId = 262, surahNumber = 2, ayahNumber = 255)

        coVerify { bookmarkDao.clearBookmark(BookmarkKind.AYAH, 262, any()) }
        coVerify(exactly = 0) { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `un-bookmarking a verse that is only bookmarked removes the row`() = runTest {
        // Nothing left on it, so leaving a row of two false flags behind would be litter.
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            bookmarkRow(bookmarked = true, favourite = false)

        repository.toggleBookmark(ayahId = 262, surahNumber = 2, ayahNumber = 255)

        coVerify { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `bookmarking a verse that is already a favourite sets the flag on the same row`() =
        runTest {
            coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
                bookmarkRow(bookmarked = false, favourite = true)

            repository.toggleBookmark(ayahId = 262, surahNumber = 2, ayahNumber = 255)

            val row = slot<BookmarkEntity>()
            coVerify { bookmarkDao.upsert(capture(row)) }
            assertThat(row.captured.bookmarked).isTrue()
            assertThat(row.captured.favourite).isTrue()
        }

    @Test
    fun `un-favouriting a verse that is also bookmarked keeps the bookmark`() = runTest {
        // The mirror image, and the one a copy-paste of the bookmark branch would get wrong.
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            bookmarkRow(bookmarked = true, favourite = true)

        repository.toggleFavorite(ayahId = 262, surahNumber = 2, ayahNumber = 255)

        coVerify { bookmarkDao.clearFavourite(BookmarkKind.AYAH, 262, any()) }
        coVerify(exactly = 0) { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `un-favouriting a verse that is only a favourite removes the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            bookmarkRow(bookmarked = false, favourite = true)

        repository.toggleFavorite(ayahId = 262, surahNumber = 2, ayahNumber = 255)

        coVerify { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `deleting a bookmark on a favourited verse clears the flag rather than the row`() =
        runTest {
            coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
                bookmarkRow(bookmarked = true, favourite = true)

            repository.deleteBookmark(262)

            coVerify { bookmarkDao.clearBookmark(BookmarkKind.AYAH, 262, any()) }
            coVerify(exactly = 0) { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
        }

    @Test
    fun `deleting a bookmark that does not exist writes nothing`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null

        repository.deleteBookmark(262)

        coVerify(exactly = 0) { bookmarkDao.delete(any(), any()) }
        coVerify(exactly = 0) { bookmarkDao.clearBookmark(any(), any(), any()) }
        coVerify(exactly = 0) { bookmarkDao.upsert(any()) }
    }

    @Test
    fun `adding a bookmark to a favourited verse preserves the favourite and its created date`() =
        runTest {
            val existing = bookmarkRow(bookmarked = false, favourite = true, createdAt = 1_000L)
            coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns existing

            repository.addBookmark(262, surahNumber = 2, ayahNumber = 255, note = "hi", color = "red")

            val row = slot<BookmarkEntity>()
            coVerify { bookmarkDao.upsert(capture(row)) }
            assertThat(row.captured.favourite).isTrue()
            // Re-bookmarking is not a new mark; the list is ordered by created_at, so
            // resetting it would silently reorder the user's bookmarks.
            assertThat(row.captured.createdAt).isEqualTo(1_000L)
            assertThat(row.captured.note).isEqualTo("hi")
        }

    // ── reading progress is read-modify-write on one row ──────────────────────

    @Test
    fun `moving the reading position keeps the running totals`() = runTest {
        // A months-old ayah count and khatma count live on the same row as the position, which
        // is written every time the reader scrolls.
        coEvery { readingProgressDao.get() } returns
            progressRow(totalAyahsRead = 4_120, currentKhatmaCount = 2)

        repository.updateReadingPosition(surah = 18, ayah = 10, page = 294, juz = 15)

        val row = slot<ReadingProgressEntity>()
        coVerify { readingProgressDao.upsert(capture(row)) }
        assertThat(row.captured.lastReadSurah).isEqualTo(18)
        assertThat(row.captured.totalAyahsRead).isEqualTo(4_120)
        assertThat(row.captured.currentKhatmaCount).isEqualTo(2)
    }

    @Test
    fun `the ayah count accumulates rather than replacing`() = runTest {
        coEvery { readingProgressDao.get() } returns progressRow(totalAyahsRead = 100)

        repository.incrementAyahsRead(7)

        val row = slot<ReadingProgressEntity>()
        coVerify { readingProgressDao.upsert(capture(row)) }
        assertThat(row.captured.totalAyahsRead).isEqualTo(107)
    }

    @Test
    fun `the first verse ever read seeds the row instead of failing`() = runTest {
        coEvery { readingProgressDao.get() } returns null

        repository.incrementAyahsRead(3)

        val row = slot<ReadingProgressEntity>()
        coVerify { readingProgressDao.upsert(capture(row)) }
        assertThat(row.captured.totalAyahsRead).isEqualTo(3)
        assertThat(row.captured.id).isEqualTo(1)
    }

    // ── two search implementations, one shape ─────────────────────────────────

    @Test
    fun `without an index, search falls back to LIKE over Arabic and translations`() = runTest {
        coEvery { searchIndex.isAvailable() } returns false
        every { quranDao.searchAyahsWithText("mercy") } returns flowOf(listOf(ayahWithText(262)))
        every {
            quranDao.searchTranslations("mercy", QuranTranslation.DEFAULT.id)
        } returns flowOf(listOf(translation(ayahId = 300, text = "the Merciful")))
        coEvery { quranDao.getAyahWithTextById(300) } returns ayahWithText(300)

        val results = repository.searchQuran("mercy", QuranTranslation.DEFAULT.id).first()

        assertThat(results.map { it.ayah.id }).containsExactly(262, 300)
        assertThat(results.map { it.searchType })
            .containsExactly(SearchType.ARABIC, SearchType.TRANSLATION)
        coVerify(exactly = 0) { searchIndex.refs(any(), any(), any(), any()) }
    }

    @Test
    fun `with an index, search goes through it and never runs the LIKE queries`() = runTest {
        // The reason the index exists: `LIKE` returns nothing for any Arabic query, because
        // the corpus is vocalised (audit §6.1).
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("الله", SearchKind.QURAN, null, any()) } returns listOf("262")
        coEvery { quranDao.getAyahsWithTextByIds(listOf(262)) } returns listOf(ayahWithText(262))

        val results = repository.searchQuran("الله", translatorId = null).first()

        assertThat(results.map { it.ayah.id }).containsExactly(262)
        every { quranDao.searchAyahsWithText(any()) } returns flowOf(emptyList())
        coVerify(exactly = 0) { quranDao.searchAyahsWithText(any()) }
    }

    @Test
    fun `an indexed translation search is narrowed to the reader's own translation`() = runTest {
        // All fifteen translations are in one index, so without the source filter a hit in
        // Bengali would surface for a reader who has Sahih International selected.
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs(any(), SearchKind.QURAN, null, any()) } returns emptyList()
        coEvery {
            searchIndex.refs("mercy", SearchKind.TRANSLATION, "ur_maududi", any())
        } returns listOf("300")
        coEvery { quranDao.getAyahsWithTextByIds(listOf(300)) } returns listOf(ayahWithText(300))
        coEvery {
            quranDao.getTranslationsByAyahIds(listOf(300), "ur_maududi")
        } returns listOf(translation(ayahId = 300, text = "رحم"))

        val results = repository.searchQuran("mercy", "ur_maududi").first()

        assertThat(results.map { it.ayah.id }).containsExactly(300)
        coVerify { searchIndex.refs("mercy", SearchKind.TRANSLATION, "ur_maududi", any()) }
    }

    @Test
    fun `a translator id this build does not know resolves to the default, not to no rows`() =
        runTest {
            // A stale preference — a translation removed from the catalogue — would otherwise
            // query for rows that cannot exist and return an empty list that reads as
            // "nothing matched".
            coEvery { searchIndex.isAvailable() } returns false
            every { quranDao.searchAyahsWithText(any()) } returns flowOf(emptyList())
            every { quranDao.searchTranslations(any(), any()) } returns flowOf(emptyList())

            repository.searchQuran("mercy", "a_translation_that_was_removed").first()

            coVerify { quranDao.searchTranslations("mercy", QuranTranslation.DEFAULT.id) }
        }

    @Test
    fun `a verse matched in both Arabic and translation is returned once`() = runTest {
        coEvery { searchIndex.isAvailable() } returns false
        every { quranDao.searchAyahsWithText("light") } returns flowOf(listOf(ayahWithText(262)))
        every { quranDao.searchTranslations("light", any()) } returns
            flowOf(listOf(translation(ayahId = 262, text = "light")))
        coEvery { quranDao.getAyahWithTextById(262) } returns ayahWithText(262)

        val results = repository.searchQuran("light", QuranTranslation.DEFAULT.id).first()

        assertThat(results).hasSize(1)
    }

    @Test
    fun `surah search uses the index where it exists, because LIKE cannot reach the Arabic name`() =
        runTest {
            // سورة الفاتحة is stored with its marks, and nobody types them.
            coEvery { searchIndex.isAvailable() } returns true
            coEvery { searchIndex.refs("fatiha", SearchKind.SURAH, null, any()) } returns
                listOf("1", "1", "not-a-number")
            coEvery { quranDao.getSurahsByNumbers(listOf(1)) } returns listOf(surahEntity())

            val results = repository.searchSurahs("fatiha").first()

            assertThat(results.map { it.number }).containsExactly(1)
            // Deduped and non-numeric refs dropped before the query, not after.
            coVerify { quranDao.getSurahsByNumbers(listOf(1)) }
        }

    @Test
    fun `surah search falls back to LIKE without an index`() = runTest {
        coEvery { searchIndex.isAvailable() } returns false
        every { quranDao.searchSurahs("fatiha") } returns flowOf(listOf(surahEntity()))

        val results = repository.searchSurahs("fatiha").first()

        assertThat(results.map { it.number }).containsExactly(1)
    }

    // ── memoised reads ────────────────────────────────────────────────────────

    @Test
    fun `a line-accurate edition's page ranges are read once and reused`() = runTest {
        // Several hundred immutable rows that both the Page tab and every page fetch consult.
        coEvery { quranDao.getLayoutPageAyahRanges(MushafScript.INDOPAK_16.name) } returns
            emptyList()

        repeat(3) { repository.getPageAyahRanges(MushafScript.INDOPAK_16) }

        coVerify(exactly = 1) { quranDao.getLayoutPageAyahRanges(MushafScript.INDOPAK_16.name) }
    }

    @Test
    fun `each edition is cached separately`() = runTest {
        // One cache keyed by script, not one cached list — INDOPAK_16 and INDOPAK_13 have
        // different page counts, so serving one edition's ranges for the other misplaces
        // every verse.
        coEvery { quranDao.getLayoutPageAyahRanges(any()) } returns emptyList()

        repository.getPageAyahRanges(MushafScript.INDOPAK_16)
        repository.getPageAyahRanges(MushafScript.INDOPAK_13)

        coVerify(exactly = 1) { quranDao.getLayoutPageAyahRanges(MushafScript.INDOPAK_16.name) }
        coVerify(exactly = 1) { quranDao.getLayoutPageAyahRanges(MushafScript.INDOPAK_13.name) }
    }

    @Test
    fun `a flowed edition does not use the layout table at all`() = runTest {
        coEvery { quranDao.getPageAyahRanges() } returns emptyList()

        repository.getPageAyahRanges(MushafScript.MADANI)

        coVerify { quranDao.getPageAyahRanges() }
        coVerify(exactly = 0) { quranDao.getLayoutPageAyahRanges(any()) }
    }

    @Test
    fun `a script with no stored text returns an empty page rather than querying`() = runTest {
        val layout = repository.getMushafPageLayout(page = 1, script = MushafScript.MADANI)

        assertThat(layout.page).isEqualTo(1)
        assertThat(layout.lines).isEmpty()
        coVerify(exactly = 0) { quranDao.getMushafLayoutByPage(any(), any(), any()) }
    }

    @Test
    fun `whether the artifact carries the thematic layer is decided once`() = runTest {
        // The content database is replaced wholesale by a release and never written at
        // runtime, so this cannot change while the process lives.
        coEvery { quranDao.countThemes() } returns 1
        coEvery { quranDao.countTopics() } returns 1

        repeat(3) { repository.hasThematicContent() }

        coVerify(atMost = 1) { quranDao.countThemes() }
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun bookmarkRow(
        bookmarked: Boolean,
        favourite: Boolean,
        createdAt: Long = now,
    ) = BookmarkEntity(
        kind = BookmarkKind.AYAH,
        targetId = 262,
        bookmarked = bookmarked,
        favourite = favourite,
        contextId = 2,
        ordinal = 255,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun progressRow(
        totalAyahsRead: Int = 0,
        currentKhatmaCount: Int = 0,
    ) = ReadingProgressEntity(
        id = 1,
        lastReadSurah = 1,
        lastReadAyah = 1,
        lastReadPage = 1,
        lastReadJuz = 1,
        totalAyahsRead = totalAyahsRead,
        currentKhatmaCount = currentKhatmaCount,
    )

    private fun surahEntity() = SurahEntity(
        id = 1,
        number = 1,
        nameArabic = "الفاتحة",
        nameEnglish = "The Opening",
        nameTransliteration = "Al-Fatihah",
        revelationType = "meccan",
        versesCount = 7,
        orderRevealed = 5,
        startPage = 1,
    )

    private fun ayahWithText(id: Int) = AyahWithText(
        ayah = AyahEntity(
            id = id,
            surahId = 1,
            numberInSurah = 1,
            numberGlobal = id,
            juz = 1,
            hizb = 1,
            page = 1,
        ),
        textUthmani = "بِسْمِ ٱللَّهِ",
        textSimple = "bismillah",
        sajdaKind = null,
        sajdaSequence = null,
    )

    private fun translation(ayahId: Int, text: String) = TranslationEntity(
        ayahId = ayahId,
        text = text,
        translatorId = QuranTranslation.DEFAULT.id,
    )
}
