package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The Qur'an use cases: mostly delegations, and the three that are not.
 *
 * A delegating class has one interesting failure — reaching the wrong neighbour — and nothing
 * else catches it. `search` calling `getAllSurahs` still returns surahs; `byRevelationType`
 * calling `search` still compiles. Each is pinned to the repository call it is meant to make.
 *
 * The three with behaviour of their own:
 *
 * **[InsertQuranBookmarkUseCase]** does *not* pass the bookmark through. It unpacks it into the
 * five fields the repository takes, because `bookmarks` keys on `(kind, target_id)` and the
 * caller's `id` is meaningless on insert. A version that forwarded the whole object would carry
 * an id the table then has to ignore or, worse, honour.
 *
 * **[GetVerseOfTheDayUseCase]** maps a day onto a verse. The modulo is written the long way
 * round on purpose: Kotlin's `%` keeps the sign, so a negative `epochDay` — which is a real date
 * before 1970, and is what a device with a wrong clock hands it — would index at zero or below.
 *
 * **[GetAyahTranslationUseCase.forAyahs]** short-circuits an empty list rather than issuing an
 * `IN ()`, which is the query a topic with no citations would otherwise run.
 */
class QuranUseCasesTest {

    private val repository: QuranRepository = mockk(relaxed = true)

    private fun surah(number: Int, name: String) = Surah(
        number = number,
        nameArabic = "سورة",
        nameEnglish = name,
        nameTransliteration = name,
        revelationType = RevelationType.MECCAN,
        ayahCount = 7,
        orderInMushaf = number,
        startPage = number,
    )

    private fun ayah(id: Int, translation: String? = null) = Ayah(
        id = id,
        surahNumber = 1,
        ayahNumber = id,
        textArabic = "نص",
        textSimple = "nass",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
    )

    private fun bookmark(ayahId: Int, note: String?, colour: String?) = QuranBookmark(
        id = 77,
        ayahId = ayahId,
        surahNumber = 2,
        ayahNumber = 255,
        note = note,
        color = colour,
        createdAt = 0,
        updatedAt = 0,
    )

    // ---- The surah list, three ways ----

    @Test
    fun `the surah list, a revelation filter and a search are three different queries`() = runTest {
        every { repository.getAllSurahs() } returns flowOf(listOf(surah(1, "all")))
        every { repository.getSurahsByRevelationType(any()) } returns flowOf(listOf(surah(2, "by type")))
        every { repository.searchSurahs(any()) } returns flowOf(listOf(surah(3, "searched")))

        val useCase = GetSurahListUseCase(repository)

        assertThat(useCase().first().map { it.nameEnglish }).containsExactly("all")
        assertThat(useCase.byRevelationType(RevelationType.MECCAN).first().map { it.nameEnglish })
            .containsExactly("by type")
        assertThat(useCase.search("kahf").first().map { it.nameEnglish }).containsExactly("searched")
    }

    @Test
    fun `a surah search passes the query through untouched`() = runTest {
        every { repository.searchSurahs(any()) } returns flowOf(emptyList())

        GetSurahListUseCase(repository).search("  Al-Kahf  ").first()

        verify { repository.searchSurahs("  Al-Kahf  ") }
    }

    // ---- Verses ----

    @Test
    fun `one verse by id, and many by ids, are different reads`() = runTest {
        coEvery { repository.getAyahById(5) } returns ayah(5)
        coEvery { repository.getAyahsByIds(any()) } returns listOf(ayah(5), ayah(6))

        val useCase = GetAyahByIdUseCase(repository)

        assertThat(useCase(5)?.id).isEqualTo(5)
        assertThat(useCase.forIds(listOf(5, 6)).keys).containsExactly(5, 6)
    }

    @Test
    fun `a batched lookup is keyed by ayah id so a caller can index it`() = runTest {
        coEvery { repository.getAyahsByIds(any()) } returns listOf(ayah(11), ayah(12))

        val byId = GetAyahByIdUseCase(repository).forIds(listOf(11, 12))

        assertThat(byId[11]?.id).isEqualTo(11)
        assertThat(byId[12]?.id).isEqualTo(12)
    }

    @Test
    fun `a page read defaults to the default script and no translator`() = runTest {
        every { repository.getAyahsByPage(any(), any(), any()) } returns flowOf(emptyList())

        GetAyahsByPageUseCase(repository)(pageNumber = 293).first()

        verify { repository.getAyahsByPage(293, null, MushafScript.DEFAULT) }
    }

    @Test
    fun `a page read carries the translator and script it was given`() = runTest {
        every { repository.getAyahsByPage(any(), any(), any()) } returns flowOf(emptyList())

        GetAyahsByPageUseCase(repository)(
            pageNumber = 1, translatorId = "sahih", script = MushafScript.INDOPAK_16,
        ).first()

        verify { repository.getAyahsByPage(1, "sahih", MushafScript.INDOPAK_16) }
    }

    @Test
    fun `a full-text search carries its optional translator`() = runTest {
        every { repository.searchQuran(any(), any()) } returns flowOf(emptyList())

        SearchQuranUseCase(repository)("mercy").first()
        SearchQuranUseCase(repository)("mercy", translatorId = "sahih").first()

        verify { repository.searchQuran("mercy", null) }
        verify { repository.searchQuran("mercy", "sahih") }
    }

    @Test
    fun `page ranges default to the default script`() = runTest {
        coEvery { repository.getPageAyahRanges(any()) } returns emptyList()

        GetPageAyahRangesUseCase(repository)()

        coVerify { repository.getPageAyahRanges(MushafScript.DEFAULT) }
    }

    // ---- Bookmarks and favourites ----

    @Test
    fun `inserting a bookmark unpacks it rather than forwarding the row`() = runTest {
        // The caller's id is meaningless on insert; the table keys on (kind, target_id).
        InsertQuranBookmarkUseCase(repository)(bookmark(2255, note = "the throne", colour = "#FDE68A"))

        coVerify {
            repository.addBookmark(
                ayahId = 2255,
                surahNumber = 2,
                ayahNumber = 255,
                note = "the throne",
                color = "#FDE68A",
            )
        }
    }

    @Test
    fun `a bookmark with neither note nor colour still inserts`() = runTest {
        InsertQuranBookmarkUseCase(repository)(bookmark(2255, note = null, colour = null))

        coVerify { repository.addBookmark(2255, 2, 255, null, null) }
    }

    @Test
    fun `updating a bookmark does forward the row, because now the id means something`() = runTest {
        val existing = bookmark(2255, note = "edited", colour = null)

        UpdateQuranBookmarkUseCase(repository)(existing)

        coVerify { repository.updateBookmark(existing) }
    }

    @Test
    fun `deleting a bookmark names the verse, not the row`() = runTest {
        DeleteQuranBookmarkUseCase(repository)(2255)

        coVerify { repository.deleteBookmark(2255) }
    }

    @Test
    fun `bookmarking and favouriting are different toggles`() = runTest {
        ToggleQuranBookmarkUseCase(repository)(2255, 2, 255)
        ToggleQuranFavoriteUseCase(repository)(2255, 2, 255)

        coVerify { repository.toggleBookmark(2255, 2, 255) }
        coVerify { repository.toggleFavorite(2255, 2, 255) }
    }

    // ---- Reading position ----

    @Test
    fun `a reading position carries all four coordinates`() = runTest {
        UpdateReadingPositionUseCase(repository)(surah = 18, ayah = 10, page = 294, juz = 15)

        coVerify { repository.updateReadingPosition(18, 10, 294, 15) }
    }

    @Test
    fun `the verses-read counter increments by what it was given`() = runTest {
        IncrementAyahsReadUseCase(repository)(7)

        coVerify { repository.incrementAyahsRead(7) }
    }

    // ---- The verse of the day ----

    private fun verseOfTheDay() =
        GetVerseOfTheDayUseCase(repository, GetAyahTranslationUseCase(repository))

    @Test
    fun `the same day gives the same verse`() = runTest {
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }

        val first = verseOfTheDay()(epochDay = 20_000)
        val again = verseOfTheDay()(epochDay = 20_000)

        assertThat(first?.id).isEqualTo(again?.id)
    }

    @Test
    fun `the next day gives a different verse`() = runTest {
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }

        assertThat(verseOfTheDay()(epochDay = 20_000)?.id)
            .isNotEqualTo(verseOfTheDay()(epochDay = 20_001)?.id)
    }

    @Test
    fun `a date before 1970 still lands on a real verse`() = runTest {
        // Kotlin's % keeps the sign, so the naive form indexes at zero or below — reachable
        // from a device with a wrong clock, not just from a test.
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }

        listOf(-1L, -6236L, -20_000L).forEach { day ->
            val id = verseOfTheDay()(epochDay = day)?.id
            assertThat(id).isNotNull()
            assertThat(id!!).isAtLeast(1)
            assertThat(id).isAtMost(6236)
        }
    }

    @Test
    fun `the verse is asked for within the book`() = runTest {
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }

        (0L..40L).forEach { day ->
            val id = verseOfTheDay()(epochDay = day * 500)?.id!!
            assertThat(id).isAtLeast(1)
            assertThat(id).isAtMost(6236)
        }
    }

    @Test
    fun `no translator means the verse comes back untranslated`() = runTest {
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }

        assertThat(verseOfTheDay()(epochDay = 20_000)?.translation).isNull()
        coVerify(exactly = 0) { repository.getTranslationsForAyahs(any(), any()) }
    }

    @Test
    fun `a translator's text is folded into the verse`() = runTest {
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }
        coEvery { repository.getTranslationsForAyahs(any(), any()) } answers {
            flowOf(firstArg<List<Int>>().associateWith { "translated $it" })
        }

        val verse = verseOfTheDay()(epochDay = 20_000, translatorId = "sahih")

        assertThat(verse?.translation).isEqualTo("translated ${verse?.id}")
    }

    @Test
    fun `a translator the device does not have leaves the verse as it was`() = runTest {
        coEvery { repository.getAyahById(any()) } answers { ayah(firstArg()) }
        coEvery { repository.getTranslationsForAyahs(any(), any()) } returns flowOf(emptyMap())

        assertThat(verseOfTheDay()(epochDay = 20_000, translatorId = "missing")?.translation)
            .isNull()
    }

    @Test
    fun `a verse the content database does not have yields nothing`() = runTest {
        coEvery { repository.getAyahById(any()) } returns null

        assertThat(verseOfTheDay()(epochDay = 20_000)).isNull()
    }

    // ---- Translations in bulk ----

    @Test
    fun `asking for no translations runs no query`() = runTest {
        val result = GetAyahTranslationUseCase(repository).forAyahs(emptyList(), "sahih")

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { repository.getTranslationsForAyahs(any(), any()) }
    }

    @Test
    fun `many verses' translations come back in one read, keyed by id`() = runTest {
        coEvery { repository.getTranslationsForAyahs(any(), any()) } returns
            flowOf(mapOf(1 to "one", 2 to "two"))

        val result = GetAyahTranslationUseCase(repository).forAyahs(listOf(1, 2), "sahih")

        assertThat(result).containsExactly(1, "one", 2, "two")
        coVerify(exactly = 1) { repository.getTranslationsForAyahs(listOf(1, 2), "sahih") }
    }

    @Test
    fun `one verse's translation is picked out of the batch read`() = runTest {
        coEvery { repository.getTranslationsForAyahs(any(), any()) } returns flowOf(mapOf(9 to "nine"))

        assertThat(GetAyahTranslationUseCase(repository)(9, "sahih")).isEqualTo("nine")
    }

    @Test
    fun `a verse with no translation in that edition answers null`() = runTest {
        coEvery { repository.getTranslationsForAyahs(any(), any()) } returns flowOf(emptyMap())

        assertThat(GetAyahTranslationUseCase(repository)(9, "sahih")).isNull()
    }
}
