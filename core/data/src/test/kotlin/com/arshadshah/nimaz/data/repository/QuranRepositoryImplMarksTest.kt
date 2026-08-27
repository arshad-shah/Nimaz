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
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.SearchType
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
 * The remaining arms of the two things in [QuranRepositoryImpl] that write.
 *
 * `toggleBookmark` and the reading position are read-modify-write against a single row, and
 * both have arms that only run for a user who has done two different things to the same verse
 * — or who has been reading for months. Neither failure shows on screen:
 *
 *  - toggling a bookmark off on a verse that is *also* favourited must clear the flag, not the
 *    row. Deleting it destroys a favourite the user made separately;
 *  - `updateReadingPosition` rewrites the row, and the running totals (`totalAyahsRead`,
 *    `currentKhatmaCount`) are not in its arguments. Reading them back off the existing row is
 *    the only thing standing between a page turn and a khatma counter reset to zero.
 *
 * The search arms below are the *translation* half, which the Arabic-only tests never reach:
 * both the index path and the `LIKE` path merge two result sets and de-duplicate, and a verse
 * that matches in both must appear once.
 */
class QuranRepositoryImplMarksTest {

    private lateinit var quranDao: QuranDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var readingProgressDao: ReadingProgressDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: QuranRepositoryImpl

    @Before
    fun setUp() {
        quranDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        readingProgressDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)
        coEvery { searchIndex.isAvailable() } returns false
        every { quranDao.getAllSurahs() } returns flowOf(listOf(surah()))
        repository = QuranRepositoryImpl(quranDao, bookmarkDao, readingProgressDao, searchIndex)
    }

    // ── the bookmark toggle ───────────────────────────────────────────────────

    @Test
    fun `toggling a bookmark on a verse with no row creates one`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleBookmark(262, surahNumber = 2, ayahNumber = 255)

        assertThat(saved.single().bookmarked).isTrue()
        assertThat(saved.single().favourite).isFalse()
        assertThat(saved.single().contextId).isEqualTo(2)
        assertThat(saved.single().ordinal).isEqualTo(255)
    }

    @Test
    fun `toggling a bookmark off on a favourited verse keeps the favourite`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(262, bookmarked = true, favourite = true)

        repository.toggleBookmark(262, 2, 255)

        coVerify { bookmarkDao.clearBookmark(BookmarkKind.AYAH, 262, any()) }
        coVerify(exactly = 0) { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `toggling a bookmark off on a verse that is nothing else removes the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(262, bookmarked = true, favourite = false)

        repository.toggleBookmark(262, 2, 255)

        coVerify { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `bookmarking a verse that is only favourited sets the flag on the same row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(262, bookmarked = false, favourite = true, createdAt = 7L)
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleBookmark(262, 2, 255)

        assertThat(saved.single().bookmarked).isTrue()
        assertThat(saved.single().favourite).isTrue()
        assertThat(saved.single().createdAt).isEqualTo(7L)
    }

    @Test
    fun `deleting a bookmark on a favourited verse clears the flag instead`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(262, bookmarked = true, favourite = true)

        repository.deleteBookmark(262)

        coVerify { bookmarkDao.clearBookmark(BookmarkKind.AYAH, 262, any()) }
    }

    @Test
    fun `deleting a bookmark on a verse with no row writes nothing`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null

        repository.deleteBookmark(262)

        coVerify(exactly = 0) { bookmarkDao.delete(any(), any()) }
        coVerify(exactly = 0) { bookmarkDao.clearBookmark(any(), any(), any()) }
    }

    @Test
    fun `un-favouriting a verse that is nothing else removes the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(262, bookmarked = false, favourite = true)

        repository.toggleFavorite(262, 2, 255)

        coVerify { bookmarkDao.delete(BookmarkKind.AYAH, 262) }
    }

    @Test
    fun `favouriting a verse with no row creates one that is not a bookmark`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.toggleFavorite(262, 2, 255)

        assertThat(saved.single().favourite).isTrue()
        assertThat(saved.single().bookmarked).isFalse()
    }

    // ── the reading position ──────────────────────────────────────────────────

    @Test
    fun `moving the reading position keeps the running totals`() = runTest {
        coEvery { readingProgressDao.get() } returns ReadingProgressEntity(
            id = 1, lastReadSurah = 1, lastReadAyah = 1, lastReadPage = 1, lastReadJuz = 1,
            totalAyahsRead = 4_000, currentKhatmaCount = 3, updatedAt = 1L,
        )
        val saved = mutableListOf<ReadingProgressEntity>()
        coEvery { readingProgressDao.upsert(capture(saved)) } returns Unit

        repository.updateReadingPosition(surah = 2, ayah = 255, page = 42, juz = 3)

        // Neither total is an argument, so a blind write resets a counter the user has been
        // accumulating for months.
        assertThat(saved.single().totalAyahsRead).isEqualTo(4_000)
        assertThat(saved.single().currentKhatmaCount).isEqualTo(3)
        assertThat(saved.single().lastReadPage).isEqualTo(42)
    }

    @Test
    fun `a first ever read starts both totals at zero rather than failing`() = runTest {
        coEvery { readingProgressDao.get() } returns null
        val saved = mutableListOf<ReadingProgressEntity>()
        coEvery { readingProgressDao.upsert(capture(saved)) } returns Unit

        repository.updateReadingPosition(1, 1, 1, 1)

        assertThat(saved.single().totalAyahsRead).isEqualTo(0)
        assertThat(saved.single().currentKhatmaCount).isEqualTo(0)
    }

    @Test
    fun `verses read are added to the running total, not written over it`() = runTest {
        coEvery { readingProgressDao.get() } returns ReadingProgressEntity(
            id = 1, lastReadSurah = 2, lastReadAyah = 255, lastReadPage = 42, lastReadJuz = 3,
            totalAyahsRead = 900, currentKhatmaCount = 2, updatedAt = 1L,
        )
        val saved = mutableListOf<ReadingProgressEntity>()
        coEvery { readingProgressDao.upsert(capture(saved)) } returns Unit

        repository.incrementAyahsRead(7)

        assertThat(saved.single().totalAyahsRead).isEqualTo(907)
        // The position is untouched: counting verses is not moving the bookmark.
        assertThat(saved.single().lastReadPage).isEqualTo(42)
    }

    @Test
    fun `counting verses on a device with no progress row starts one`() = runTest {
        coEvery { readingProgressDao.get() } returns null
        val saved = mutableListOf<ReadingProgressEntity>()
        coEvery { readingProgressDao.upsert(capture(saved)) } returns Unit

        repository.incrementAyahsRead(7)

        assertThat(saved.single().totalAyahsRead).isEqualTo(7)
        assertThat(saved.single().lastReadSurah).isEqualTo(1)
    }

    // ── search, the translation half ──────────────────────────────────────────

    @Test
    fun `without an index a translation search adds to the Arabic results`() = runTest {
        every { quranDao.searchAyahsWithText("mercy") } returns flowOf(listOf(ayah(1)))
        every {
            quranDao.searchTranslations("mercy", QuranTranslation.DEFAULT.id)
        } returns flowOf(listOf(translation(2, "the Merciful")))
        coEvery { quranDao.getAyahWithTextById(2) } returns ayah(2)

        val results = repository.searchQuran("mercy", QuranTranslation.DEFAULT.id).first()

        assertThat(results.map { it.ayah.id }).containsExactly(1, 2)
        assertThat(results.map { it.searchType })
            .containsExactly(SearchType.ARABIC, SearchType.TRANSLATION)
        assertThat(results.single { it.ayah.id == 2 }.matchedText).isEqualTo("the Merciful")
    }

    @Test
    fun `a verse matching in both Arabic and translation is listed once`() = runTest {
        every { quranDao.searchAyahsWithText("mercy") } returns flowOf(listOf(ayah(1)))
        every {
            quranDao.searchTranslations("mercy", QuranTranslation.DEFAULT.id)
        } returns flowOf(listOf(translation(1, "the Merciful")))
        coEvery { quranDao.getAyahWithTextById(1) } returns ayah(1)

        val results = repository.searchQuran("mercy", QuranTranslation.DEFAULT.id).first()

        assertThat(results.map { it.ayah.id }).containsExactly(1)
        // The Arabic hit wins, because it came first.
        assertThat(results.single().searchType).isEqualTo(SearchType.ARABIC)
    }

    @Test
    fun `a translation hit whose verse the corpus has lost is dropped, not rendered blank`() =
        runTest {
            every { quranDao.searchAyahsWithText(any()) } returns flowOf(emptyList())
            every {
                quranDao.searchTranslations("mercy", QuranTranslation.DEFAULT.id)
            } returns flowOf(listOf(translation(9_999, "orphan")))
            coEvery { quranDao.getAyahWithTextById(9_999) } returns null

            assertThat(repository.searchQuran("mercy", QuranTranslation.DEFAULT.id).first())
                .isEmpty()
        }

    @Test
    fun `a verse whose surah is not in the list is labelled by its surah id`() = runTest {
        every { quranDao.getAllSurahs() } returns flowOf(emptyList())
        every { quranDao.searchAyahsWithText("x") } returns flowOf(listOf(ayah(1)))

        assertThat(repository.searchQuran("x", null).first().single().surahName)
            .isEqualTo("Surah 1")
    }

    @Test
    fun `with an index the translation half is narrowed to the reader's own translation`() =
        runTest {
            coEvery { searchIndex.isAvailable() } returns true
            coEvery { searchIndex.refs("mercy", SearchKind.QURAN, null, any()) } returns
                listOf("1")
            coEvery {
                searchIndex.refs("mercy", SearchKind.TRANSLATION, QuranTranslation.DEFAULT.id, any())
            } returns listOf("2")
            coEvery { quranDao.getAyahsWithTextByIds(listOf(1)) } returns listOf(ayah(1))
            coEvery { quranDao.getAyahsWithTextByIds(listOf(2)) } returns listOf(ayah(2))
            coEvery {
                quranDao.getTranslationsByAyahIds(listOf(2), QuranTranslation.DEFAULT.id)
            } returns listOf(translation(2, "the Merciful"))

            val results = repository.searchQuran("mercy", QuranTranslation.DEFAULT.id).first()

            // A hit in the Bengali translation must not surface for a reader on Sahih
            // International: the index query carries the source.
            assertThat(results.map { it.ayah.id }).containsExactly(1, 2)
            assertThat(results.single { it.ayah.id == 2 }.matchedText).isEqualTo("the Merciful")
        }

    @Test
    fun `with an index and no translation selected only the Arabic half runs`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("mercy", SearchKind.QURAN, null, any()) } returns listOf("1")
        coEvery { quranDao.getAyahsWithTextByIds(listOf(1)) } returns listOf(ayah(1))

        assertThat(repository.searchQuran("mercy", null).first().map { it.ayah.id })
            .containsExactly(1)
        coVerify(exactly = 0) { quranDao.getTranslationsByAyahIds(any(), any()) }
    }

    @Test
    fun `an indexed translation hit whose verse row is missing is dropped`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("mercy", SearchKind.QURAN, null, any()) } returns emptyList()
        coEvery {
            searchIndex.refs("mercy", SearchKind.TRANSLATION, QuranTranslation.DEFAULT.id, any())
        } returns listOf("2")
        coEvery { quranDao.getAyahsWithTextByIds(emptyList()) } returns emptyList()
        coEvery { quranDao.getAyahsWithTextByIds(listOf(2)) } returns emptyList()
        coEvery {
            quranDao.getTranslationsByAyahIds(listOf(2), QuranTranslation.DEFAULT.id)
        } returns listOf(translation(2, "orphan"))

        assertThat(repository.searchQuran("mercy", QuranTranslation.DEFAULT.id).first()).isEmpty()
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun surah() = SurahEntity(
        id = 1, number = 1, nameArabic = "الفاتحة", nameEnglish = "The Opening",
        nameTransliteration = "Al-Fatihah", revelationType = "meccan", versesCount = 7,
        orderRevealed = 5, startPage = 1,
    )

    private fun ayah(id: Int) = AyahWithText(
        ayah = AyahEntity(
            id = id, surahId = 1, numberInSurah = id, numberGlobal = id,
            juz = 1, hizb = 1, page = 1,
        ),
        textUthmani = "بِسْمِ ٱللَّهِ",
        textSimple = "bismillah",
        sajdaKind = null,
        sajdaSequence = null,
    )

    private fun translation(ayahId: Int, text: String) = TranslationEntity(
        ayahId = ayahId, text = text, translatorId = QuranTranslation.DEFAULT.id,
    )

    private fun mark(
        targetId: Int,
        bookmarked: Boolean = true,
        favourite: Boolean = false,
        createdAt: Long = 1L,
    ) = BookmarkEntity(
        kind = BookmarkKind.AYAH,
        targetId = targetId,
        bookmarked = bookmarked,
        favourite = favourite,
        contextId = 2,
        ordinal = 255,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
