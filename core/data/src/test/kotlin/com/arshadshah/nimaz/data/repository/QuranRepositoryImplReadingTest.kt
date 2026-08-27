package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.AyahWithText
import com.arshadshah.nimaz.data.local.database.dao.MushafLayoutLineRow
import com.arshadshah.nimaz.data.local.database.dao.PageAyahRangeRow
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahInfoEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahStructureEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SajdaType
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
 * The read paths of [QuranRepositoryImpl] — the ones a reader screen actually calls.
 *
 * These pin behaviour that a screen cannot report on its own:
 *
 *  - a **line-accurate** edition does not paginate by `ayahs.page`. It resolves the page's span
 *    through its own layout table, and the failure mode when it does not is not an error: it is
 *    the *Madani* page, silently, in an Indo-Pak reader (#325). An unknown span must emit an
 *    empty page rather than the wrong one;
 *  - the layout table is several hundred immutable rows consulted on every page turn, so it is
 *    memoised per edition — and a memo that re-queries is invisible except as a slow reader;
 *  - every verse read path stamps `isBookmarked` from the *user's* database, which lives in a
 *    different file from the content. A path that forgets shows a reader with no bookmarks and
 *    no error;
 *  - a translator id is normalised against the catalogue, so a stale preference resolves to the
 *    default rather than querying for rows that cannot exist;
 *  - the division markers (ʿayn, ۞) are drawn on the verse that *begins* or *ends* a division,
 *    not on every verse inside it, and the two conventions are opposite ways round.
 */
class QuranRepositoryImplReadingTest {

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
        coEvery { bookmarkDao.bookmarkedIds(BookmarkKind.AYAH) } returns emptyList()
        every { quranDao.getAllSurahs() } returns flowOf(listOf(surahEntity()))
        repository = QuranRepositoryImpl(quranDao, bookmarkDao, readingProgressDao, searchIndex)
    }

    // ── the surah list ────────────────────────────────────────────────────────

    @Test
    fun `a surah row becomes a surah with its revelation type resolved`() = runTest {
        val surah = repository.getAllSurahs().first().single()

        assertThat(surah.number).isEqualTo(1)
        assertThat(surah.nameEnglish).isEqualTo("The Opening")
        assertThat(surah.revelationType).isEqualTo(RevelationType.MECCAN)
        assertThat(surah.ayahCount).isEqualTo(7)
        assertThat(surah.startPage).isEqualTo(1)
    }

    @Test
    fun `a surah number the corpus does not hold reports none`() = runTest {
        coEvery { quranDao.getSurahByNumber(115) } returns null

        assertThat(repository.getSurahByNumber(115)).isNull()
    }

    @Test
    fun `a surah number the corpus holds comes back as a domain surah`() = runTest {
        coEvery { quranDao.getSurahByNumber(1) } returns surahEntity()

        assertThat(repository.getSurahByNumber(1)!!.nameTransliteration).isEqualTo("Al-Fatihah")
    }

    @Test
    fun `the revelation filter is queried with the stored lowercase value`() = runTest {
        every { quranDao.getSurahsByRevelationType("medinan") } returns
            flowOf(listOf(surahEntity().copy(number = 2, revelationType = "medinan")))

        val surahs = repository.getSurahsByRevelationType(RevelationType.MEDINAN).first()

        // The enum name is uppercase and matches no row in the content database.
        assertThat(surahs.single().revelationType).isEqualTo(RevelationType.MEDINAN)
    }

    // ── verses ────────────────────────────────────────────────────────────────

    @Test
    fun `a surah's verses are fetched by its row id, not by its number`() = runTest {
        // The surah's primary key and its printed number are not the same column.
        every { quranDao.getAllSurahs() } returns
            flowOf(listOf(surahEntity().copy(id = 42, number = 7)))
        every { quranDao.getAyahsWithTextBySurah(42) } returns flowOf(listOf(ayah(id = 954)))

        val ayahs = repository.getAyahsBySurah(7).first()

        assertThat(ayahs.map { it.id }).containsExactly(954)
    }

    @Test
    fun `a surah number with no row yields an empty reader rather than every verse`() = runTest {
        every { quranDao.getAllSurahs() } returns flowOf(listOf(surahEntity()))

        assertThat(repository.getAyahsBySurah(115).first()).isEmpty()
        verifyNoAyahFetch()
    }

    @Test
    fun `asking for no verse ids queries nothing`() = runTest {
        assertThat(repository.getAyahsByIds(emptyList())).isEmpty()

        coVerify(exactly = 0) { quranDao.getAyahsWithTextByIds(any()) }
    }

    @Test
    fun `verse ids come back as verses`() = runTest {
        coEvery { quranDao.getAyahsWithTextByIds(listOf(1, 2)) } returns
            listOf(ayah(id = 1), ayah(id = 2))

        assertThat(repository.getAyahsByIds(listOf(1, 2)).map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun `a verse id the corpus does not hold reports none`() = runTest {
        coEvery { quranDao.getAyahWithTextById(99999) } returns null

        assertThat(repository.getAyahById(99999)).isNull()
    }

    @Test
    fun `a verse carries the Uthmani text and a genuinely different simple text`() = runTest {
        coEvery { quranDao.getAyahWithTextById(1) } returns
            ayah(id = 1).copy(textUthmani = "بِسۡمِ", textSimple = "bism")

        val verse = repository.getAyahById(1)!!

        // These were byte-identical in all 6,236 rows before schemaVersion 22: a reader who chose
        // the plain script got the Uthmani one and could not tell.
        assertThat(verse.textArabic).isEqualTo("بِسۡمِ")
        assertThat(verse.textSimple).isEqualTo("bism")
    }

    @Test
    fun `a verse with no simple text falls back to the Uthmani one rather than to blank`() =
        runTest {
            coEvery { quranDao.getAyahWithTextById(1) } returns
                ayah(id = 1).copy(textUthmani = "بِسۡمِ", textSimple = null)

            assertThat(repository.getAyahById(1)!!.textSimple).isEqualTo("بِسۡمِ")
        }

    @Test
    fun `a division marker lands on the verse that closes a ruku and opens a quarter`() = runTest {
        coEvery { quranDao.getAyahWithTextById(5) } returns ayah(id = 5).let {
            it.copy(ayah = it.ayah.copy(rukuEndAyahId = 5, rubStartAyahId = 5, rubNumber = 3))
        }
        coEvery { quranDao.getAyahWithTextById(6) } returns ayah(id = 6).let {
            // Verse 6 falls *inside* the same ruku and quarter but begins/ends neither.
            it.copy(ayah = it.ayah.copy(rukuEndAyahId = 9, rubStartAyahId = 5, rubNumber = 3))
        }

        val marked = repository.getAyahById(5)!!
        val inside = repository.getAyahById(6)!!

        assertThat(marked.isRukuEnd).isTrue()
        assertThat(marked.isRubStart).isTrue()
        assertThat(inside.isRukuEnd).isFalse()
        assertThat(inside.isRubStart).isFalse()
        assertThat(inside.rubNumber).isEqualTo(3)
    }

    @Test
    fun `a verse with no quarter recorded reports zero rather than crashing`() = runTest {
        coEvery { quranDao.getAyahWithTextById(1) } returns ayah(id = 1)

        assertThat(repository.getAyahById(1)!!.rubNumber).isEqualTo(0)
    }

    @Test
    fun `a sajda verse carries its kind and its sequence`() = runTest {
        every { quranDao.getSajdaAyahsWithText() } returns flowOf(
            listOf(ayah(id = 1160).copy(sajdaKind = "recommended", sajdaSequence = 1))
        )

        val sajda = repository.getSajdaAyahs().first().single()

        assertThat(sajda.sajdaType).isEqualTo(SajdaType.fromString("recommended"))
        assertThat(sajda.sajdaNumber).isEqualTo(1)
    }

    // ── juz ───────────────────────────────────────────────────────────────────

    @Test
    fun `a juz read with no translator asks for no translations`() = runTest {
        every { quranDao.getAyahsWithTextByJuz(1) } returns flowOf(listOf(ayah(id = 1)))

        val verses = repository.getAyahsByJuz(1, translatorId = null).first()

        assertThat(verses.single().translation).isNull()
        coVerify(exactly = 0) { quranDao.getTranslationsForAyahs(any(), any()) }
    }

    @Test
    fun `a juz read with a translator attaches the translation to each verse`() = runTest {
        every { quranDao.getAyahsWithTextByJuz(1) } returns flowOf(listOf(ayah(id = 1)))
        every { quranDao.getTranslationsForAyahs(listOf(1), QuranTranslation.DEFAULT.id) } returns
            flowOf(listOf(translationRow(1, "In the name of God")))

        assertThat(repository.getAyahsByJuz(1, QuranTranslation.DEFAULT.id).first().single().translation)
            .isEqualTo("In the name of God")
    }

    @Test
    fun `a stale translator preference reads the default rather than nothing`() = runTest {
        every { quranDao.getAyahsWithTextByJuz(1) } returns flowOf(listOf(ayah(id = 1)))
        every { quranDao.getTranslationsForAyahs(listOf(1), QuranTranslation.DEFAULT.id) } returns
            flowOf(listOf(translationRow(1, "fallback")))

        val verses = repository.getAyahsByJuz(1, "a-translation-that-was-removed").first()

        assertThat(verses.single().translation).isEqualTo("fallback")
    }

    @Test
    fun `an empty juz does not query translations for an empty id list`() = runTest {
        every { quranDao.getAyahsWithTextByJuz(31) } returns flowOf(emptyList())

        assertThat(repository.getAyahsByJuz(31, QuranTranslation.DEFAULT.id).first()).isEmpty()
        coVerify(exactly = 0) { quranDao.getTranslationsForAyahs(any(), any()) }
    }

    @Test
    fun `a bookmarked verse is marked bookmarked when read through a juz`() = runTest {
        every { quranDao.getAyahsWithTextByJuz(1) } returns
            flowOf(listOf(ayah(id = 1), ayah(id = 2)))
        coEvery { bookmarkDao.bookmarkedIds(BookmarkKind.AYAH) } returns listOf(2)

        val verses = repository.getAyahsByJuz(1, translatorId = null).first()

        assertThat(verses.single { it.id == 1 }.isBookmarked).isFalse()
        assertThat(verses.single { it.id == 2 }.isBookmarked).isTrue()
    }

    // ── pages ─────────────────────────────────────────────────────────────────

    @Test
    fun `a flowed edition paginates by the page column`() = runTest {
        every { quranDao.getAyahsWithTextByPage(3) } returns flowOf(listOf(ayah(id = 20)))

        val verses = repository.getAyahsByPage(3, null, MushafScript.MADANI).first()

        assertThat(verses.map { it.id }).containsExactly(20)
        coVerify(exactly = 0) { quranDao.getLayoutPageAyahRanges(any()) }
    }

    @Test
    fun `a line accurate edition resolves the page through its own layout table`() = runTest {
        coEvery { quranDao.getLayoutPageAyahRanges("INDOPAK_16") } returns
            listOf(PageAyahRangeRow(page = 3, minAyahId = 30, maxAyahId = 40, ayahCount = 11))
        every { quranDao.getAyahsWithTextByRange(30, 40) } returns flowOf(listOf(ayah(id = 30)))

        val verses = repository.getAyahsByPage(3, null, MushafScript.INDOPAK_16).first()

        assertThat(verses.map { it.id }).containsExactly(30)
        // Never the `ayahs.page` column — that is the Madani pagination (#325).
        coVerify(exactly = 0) { quranDao.getAyahsWithTextByPage(any()) }
    }

    @Test
    fun `a page the layout table does not span comes back empty, not as the Madani page`() =
        runTest {
            coEvery { quranDao.getLayoutPageAyahRanges("INDOPAK_16") } returns
                listOf(PageAyahRangeRow(page = 1, minAyahId = 1, maxAyahId = 7, ayahCount = 7))

            assertThat(repository.getAyahsByPage(600, null, MushafScript.INDOPAK_16).first())
                .isEmpty()
            coVerify(exactly = 0) { quranDao.getAyahsWithTextByPage(any()) }
        }

    @Test
    fun `the layout table is read once per edition however many pages are turned`() = runTest {
        coEvery { quranDao.getLayoutPageAyahRanges("INDOPAK_16") } returns
            listOf(
                PageAyahRangeRow(page = 1, minAyahId = 1, maxAyahId = 7, ayahCount = 7),
                PageAyahRangeRow(page = 2, minAyahId = 8, maxAyahId = 20, ayahCount = 13),
            )
        every { quranDao.getAyahsWithTextByRange(any(), any()) } returns flowOf(emptyList())

        repository.getAyahsByPage(1, null, MushafScript.INDOPAK_16).first()
        repository.getAyahsByPage(2, null, MushafScript.INDOPAK_16).first()
        repository.getPageAyahRanges(MushafScript.INDOPAK_16)

        coVerify(exactly = 1) { quranDao.getLayoutPageAyahRanges("INDOPAK_16") }
    }

    @Test
    fun `each edition memoises its own layout rather than sharing one`() = runTest {
        coEvery { quranDao.getLayoutPageAyahRanges("INDOPAK_16") } returns
            listOf(PageAyahRangeRow(page = 1, minAyahId = 1, maxAyahId = 7, ayahCount = 7))
        coEvery { quranDao.getLayoutPageAyahRanges("INDOPAK_15") } returns
            listOf(PageAyahRangeRow(page = 1, minAyahId = 1, maxAyahId = 5, ayahCount = 5))

        val sixteen = repository.getPageAyahRanges(MushafScript.INDOPAK_16).single()
        val fifteen = repository.getPageAyahRanges(MushafScript.INDOPAK_15).single()

        assertThat(sixteen.maxAyahId).isEqualTo(7)
        assertThat(fifteen.maxAyahId).isEqualTo(5)
    }

    @Test
    fun `a flowed edition's page ranges come from the verse table, not the layout table`() =
        runTest {
            coEvery { quranDao.getPageAyahRanges() } returns
                listOf(PageAyahRangeRow(page = 1, minAyahId = 1, maxAyahId = 7, ayahCount = 7))

            assertThat(repository.getPageAyahRanges(MushafScript.MADANI).single().ayahCount)
                .isEqualTo(7)
            coVerify(exactly = 0) { quranDao.getLayoutPageAyahRanges(any()) }
        }

    @Test
    fun `an edition with no stored glyphs has no printed layout to render`() = runTest {
        // MADANI has no textSource, so there is nothing to group into lines.
        val layout = repository.getMushafPageLayout(1, MushafScript.MADANI)

        assertThat(layout.page).isEqualTo(1)
        assertThat(layout.lines).isEmpty()
        coVerify(exactly = 0) { quranDao.getMushafLayoutByPage(any(), any(), any()) }
    }

    @Test
    fun `a line accurate edition's page is grouped into printed lines`() = runTest {
        coEvery { quranDao.getMushafLayoutByPage("INDOPAK_16", "INDOPAK", 2) } returns listOf(
            layoutRow(line = 1, ayahId = 8, text = "الم ذلك", first = 0, last = 1),
            layoutRow(line = 2, ayahId = 8, text = "الم ذلك", first = 1, last = 1),
        )

        val layout = repository.getMushafPageLayout(2, MushafScript.INDOPAK_16)

        assertThat(layout.page).isEqualTo(2)
        assertThat(layout.lines).hasSize(2)
    }

    // ── a surah and its verses together ───────────────────────────────────────

    @Test
    fun `a surah number with no row opens nothing rather than an empty surah`() = runTest {
        assertThat(repository.getSurahWithAyahs(115, null).first()).isNull()
    }

    @Test
    fun `a surah opens with its verses, translations and bookmarks in one read`() = runTest {
        coEvery { quranDao.getSurahByNumber(1) } returns surahEntity()
        every { quranDao.getAyahsWithTextBySurah(1) } returns
            flowOf(listOf(ayah(id = 1), ayah(id = 2)))
        every {
            quranDao.getTranslationsForAyahs(listOf(1, 2), QuranTranslation.DEFAULT.id)
        } returns flowOf(listOf(translationRow(1, "In the name of God")))
        coEvery { bookmarkDao.bookmarkedIds(BookmarkKind.AYAH) } returns listOf(2)

        val opened = repository.getSurahWithAyahs(1, QuranTranslation.DEFAULT.id).first()!!

        assertThat(opened.surah.nameEnglish).isEqualTo("The Opening")
        assertThat(opened.ayahs.single { it.id == 1 }.translation).isEqualTo("In the name of God")
        // A verse the translation is missing for still renders — with no translation, not blank.
        assertThat(opened.ayahs.single { it.id == 2 }.translation).isNull()
        assertThat(opened.ayahs.single { it.id == 2 }.isBookmarked).isTrue()
    }

    @Test
    fun `a surah listed but with no row of its own opens with no verses`() = runTest {
        coEvery { quranDao.getSurahByNumber(1) } returns null

        val opened = repository.getSurahWithAyahs(1, null).first()!!

        assertThat(opened.ayahs).isEmpty()
    }

    // ── translations ──────────────────────────────────────────────────────────

    @Test
    fun `the translator list is the catalogue, not whichever rows happen to be present`() =
        runTest {
            val translators = repository.getAvailableTranslators()

            assertThat(translators).hasSize(QuranTranslation.entries.size)
            assertThat(translators.map { it.id })
                .containsExactlyElementsIn(QuranTranslation.entries.map { it.id })
            // A DISTINCT over the table would have no display name or language to report.
            assertThat(translators.first().name).isNotEmpty()
            assertThat(translators.first().languageCode).isNotEmpty()
            coVerify(exactly = 0) { quranDao.getAvailableTranslatorIds() }
        }

    @Test
    fun `translations come back keyed by verse id`() = runTest {
        every { quranDao.getTranslationsForAyahs(listOf(1, 2), QuranTranslation.DEFAULT.id) } returns
            flowOf(listOf(translationRow(1, "one"), translationRow(2, "two")))

        val byId = repository.getTranslationsForAyahs(listOf(1, 2), QuranTranslation.DEFAULT.id)
            .first()

        assertThat(byId).containsExactly(1, "one", 2, "two")
    }

    @Test
    fun `an unknown translator id reads the default catalogue entry`() = runTest {
        every { quranDao.getTranslationsForAyahs(listOf(1), QuranTranslation.DEFAULT.id) } returns
            flowOf(listOf(translationRow(1, "default")))

        assertThat(repository.getTranslationsForAyahs(listOf(1), "gone").first())
            .containsExactly(1, "default")
    }

    // ── bookmarks and favourites, on the read side ────────────────────────────

    @Test
    fun `the bookmark list carries the surah and verse the row was filed under`() = runTest {
        every { bookmarkDao.bookmarks(BookmarkKind.AYAH) } returns
            flowOf(listOf(mark(targetId = 262, contextId = 2, ordinal = 255)))

        val bookmark = repository.getAllBookmarks().first().single()

        assertThat(bookmark.ayahId).isEqualTo(262)
        assertThat(bookmark.surahNumber).isEqualTo(2)
        assertThat(bookmark.ayahNumber).isEqualTo(255)
    }

    @Test
    fun `a row with no context still reads back rather than being dropped`() = runTest {
        every { bookmarkDao.bookmarks(BookmarkKind.AYAH) } returns
            flowOf(listOf(mark(targetId = 1, contextId = null, ordinal = null)))

        val bookmark = repository.getAllBookmarks().first().single()

        assertThat(bookmark.surahNumber).isEqualTo(0)
        assertThat(bookmark.ayahNumber).isEqualTo(0)
    }

    @Test
    fun `a verse that is only favourited is not reported as bookmarked`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(targetId = 262, bookmarked = false, favourite = true)

        // One row, two flags: asking for the bookmark must not answer with the favourite.
        assertThat(repository.getBookmarkByAyahId(262)).isNull()
    }

    @Test
    fun `a bookmarked verse reports its note and colour`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(targetId = 262, bookmarked = true, note = "memorise", colour = "#FF0000")

        val bookmark = repository.getBookmarkByAyahId(262)!!

        assertThat(bookmark.note).isEqualTo("memorise")
        assertThat(bookmark.color).isEqualTo("#FF0000")
    }

    @Test
    fun `a verse with no row at all reports no bookmark`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null

        assertThat(repository.getBookmarkByAyahId(262)).isNull()
    }

    @Test
    fun `the bookmarked flag is observed rather than polled`() = runTest {
        every { bookmarkDao.observeIsBookmarked(BookmarkKind.AYAH, 262) } returns flowOf(true)

        assertThat(repository.isAyahBookmarked(262).first()).isTrue()
    }

    @Test
    fun `the favourites list and the favourite ids read the same rows`() = runTest {
        every { bookmarkDao.favourites(BookmarkKind.AYAH) } returns
            flowOf(listOf(mark(targetId = 262, contextId = 2, ordinal = 255, favourite = true)))

        assertThat(repository.getAllFavorites().first().single().ayahId).isEqualTo(262)
        assertThat(repository.getFavoriteAyahIds().first()).containsExactly(262)
    }

    @Test
    fun `updating a bookmark keeps a favourite on the same verse`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
            mark(targetId = 262, bookmarked = true, favourite = true)
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        repository.updateBookmark(
            repository.getBookmarkByAyahId(262)!!.copy(note = "edited", color = "#00FF00")
        )

        assertThat(saved.single().favourite).isTrue()
        assertThat(saved.single().bookmarked).isTrue()
        assertThat(saved.single().note).isEqualTo("edited")
        assertThat(saved.single().colour).isEqualTo("#00FF00")
    }

    @Test
    fun `adding a bookmark to a verse that has none keeps the row's original creation time`() =
        runTest {
            coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns
                mark(targetId = 262, bookmarked = false, favourite = true, createdAt = 111L)
            val saved = mutableListOf<BookmarkEntity>()
            coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

            repository.addBookmark(262, surahNumber = 2, ayahNumber = 255, note = null, color = null)

            // The verse was favourited months ago; bookmarking it now must not re-date the row.
            assertThat(saved.single().createdAt).isEqualTo(111L)
            assertThat(saved.single().favourite).isTrue()
            assertThat(saved.single().bookmarked).isTrue()
        }

    @Test
    fun `adding a bookmark to a verse with no row files it under its surah and number`() =
        runTest {
            coEvery { bookmarkDao.find(BookmarkKind.AYAH, 262) } returns null
            val saved = mutableListOf<BookmarkEntity>()
            coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

            repository.addBookmark(262, surahNumber = 2, ayahNumber = 255, note = "n", color = "c")

            assertThat(saved.single().contextId).isEqualTo(2)
            assertThat(saved.single().ordinal).isEqualTo(255)
            assertThat(saved.single().favourite).isFalse()
        }

    // ── reading progress ──────────────────────────────────────────────────────

    @Test
    fun `no progress row reports no progress rather than a zeroed one`() = runTest {
        every { readingProgressDao.observe() } returns flowOf(null)

        assertThat(repository.getReadingProgress().first()).isNull()
    }

    @Test
    fun `the progress row reads back as the position and the running totals`() = runTest {
        every { readingProgressDao.observe() } returns flowOf(
            com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity(
                id = 1,
                lastReadSurah = 2, lastReadAyah = 255, lastReadPage = 42, lastReadJuz = 3,
                totalAyahsRead = 900, currentKhatmaCount = 2, updatedAt = 7L,
            )
        )

        val progress = repository.getReadingProgress().first()!!

        assertThat(progress.lastReadSurah).isEqualTo(2)
        assertThat(progress.lastReadPage).isEqualTo(42)
        assertThat(progress.totalAyahsRead).isEqualTo(900)
        assertThat(progress.currentKhatmaCount).isEqualTo(2)
    }

    // ── surah info and structure ──────────────────────────────────────────────

    @Test
    fun `a surah's themes column is split into a list`() = runTest {
        coEvery { quranDao.getSurahInfo(1) } returns SurahInfoEntity(
            surahNumber = 1,
            description = "The opening chapter.",
            themes = "prayer, guidance ,mercy",
        )

        val info = repository.getSurahInfo(1)!!

        assertThat(info.themes).containsExactly("prayer", "guidance", "mercy").inOrder()
    }

    @Test
    fun `a surah with no info row reports none`() = runTest {
        coEvery { quranDao.getSurahInfo(115) } returns null

        assertThat(repository.getSurahInfo(115)).isNull()
    }

    @Test
    fun `the ruku counts come back keyed by surah`() = runTest {
        every { quranDao.getAllSurahStructure() } returns flowOf(
            listOf(structure(surahId = 1, rukuCount = 1), structure(surahId = 2, rukuCount = 40))
        )

        assertThat(repository.getSurahRukuCounts().first()).containsExactly(1, 1, 2, 40)
    }

    // ── initialisation ────────────────────────────────────────────────────────

    @Test
    fun `an artifact with surahs in it counts as initialised`() = runTest {
        repository.initializeQuranData()

        assertThat(repository.isDataInitialized()).isTrue()
    }

    @Test
    fun `an artifact with no surahs counts as uninitialised`() = runTest {
        every { quranDao.getAllSurahs() } returns flowOf(emptyList())

        assertThat(repository.isDataInitialized()).isFalse()
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun verifyNoAyahFetch() =
        coVerify(exactly = 0) { quranDao.getAyahsWithTextBySurah(any()) }

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

    private fun ayah(id: Int) = AyahWithText(
        ayah = AyahEntity(
            id = id,
            surahId = 1,
            numberInSurah = id,
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

    private fun translationRow(ayahId: Int, text: String) = TranslationEntity(
        ayahId = ayahId,
        text = text,
        translatorId = QuranTranslation.DEFAULT.id,
    )

    private fun mark(
        targetId: Int,
        bookmarked: Boolean = true,
        favourite: Boolean = false,
        note: String? = null,
        colour: String? = null,
        contextId: Int? = 2,
        ordinal: Int? = 255,
        createdAt: Long = 1L,
    ) = BookmarkEntity(
        kind = BookmarkKind.AYAH,
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

    private fun structure(surahId: Int, rukuCount: Int) = SurahStructureEntity(
        surahId = surahId,
        rukuCount = rukuCount,
        startAyahId = 1,
        endAyahId = 7,
        startPage = 1,
        endPage = 1,
        hasBasmalah = 1,
        revelationOrder = 5,
    )

    private fun layoutRow(line: Int, ayahId: Int, text: String, first: Int, last: Int) =
        MushafLayoutLineRow(
            page = 2,
            line = line,
            lineType = "ayah",
            surahId = 2,
            ayahId = ayahId,
            firstWordPosition = first,
            lastWordPosition = last,
            text = text,
            ayahNumberInSurah = 1,
        )
}
