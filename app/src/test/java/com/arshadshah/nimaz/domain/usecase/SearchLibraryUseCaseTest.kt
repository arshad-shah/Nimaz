package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.SearchType
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SearchLibraryUseCaseTest {

    private val searchQuranUC = mockk<SearchQuranUseCase>()
    private val getSurahListUC = mockk<GetSurahListUseCase>()
    private val quranUseCases = mockk<QuranUseCases>()

    private val searchHadithsUC = mockk<SearchHadithsUseCase>()
    private val hadithUseCases = mockk<HadithUseCases>()

    private val searchDuasUC = mockk<SearchDuasUseCase>()
    private val duaUseCases = mockk<DuaUseCases>()

    private lateinit var useCase: SearchLibraryUseCase

    @Before
    fun setUp() {
        every { quranUseCases.searchQuran } returns searchQuranUC
        every { quranUseCases.getSurahList } returns getSurahListUC
        every { hadithUseCases.searchHadiths } returns searchHadithsUC
        every { duaUseCases.searchDuas } returns searchDuasUC

        // Default: nothing matches anywhere.
        every { searchQuranUC.invoke(any(), any()) } returns flowOf(emptyList())
        every { getSurahListUC.search(any()) } returns flowOf(emptyList())
        every { searchHadithsUC.invoke(any()) } returns flowOf(emptyList())
        every { searchDuasUC.invoke(any()) } returns flowOf(emptyList())

        useCase = SearchLibraryUseCase(quranUseCases, hadithUseCases, duaUseCases)
    }

    @Test
    fun `multi-word query finds per-word matches the phrase search misses`() = runTest {
        // The phrase "patience during hardship" never appears verbatim, but
        // individual words do — the old LIKE-the-whole-phrase search returned
        // nothing here.
        every { searchQuranUC.invoke("patience", any()) } returns
            flowOf(listOf(quranResult(2, 153)))
        every { searchQuranUC.invoke("hardship", any()) } returns
            flowOf(listOf(quranResult(94, 5)))

        val results = useCase("patience during hardship")

        assertThat(results.quran.map { it.ayah.surahNumber }).containsExactly(2, 94)
    }

    @Test
    fun `records matching more words rank higher`() = runTest {
        val both = quranResult(2, 153) // matches "patience" AND "prayer"
        val onlyPrayer = quranResult(4, 43)
        every { searchQuranUC.invoke("patience", any()) } returns flowOf(listOf(both))
        every { searchQuranUC.invoke("prayer", any()) } returns
            flowOf(listOf(onlyPrayer, both))

        val results = useCase("patience prayer")

        assertThat(results.quran.first().ayah.surahNumber).isEqualTo(2)
    }

    @Test
    fun `an exact phrase hit outranks any multi-word combination`() = runTest {
        val phraseHit = quranResult(1, 1)
        val wordHit = quranResult(2, 153)
        every { searchQuranUC.invoke("seek help", any()) } returns flowOf(listOf(phraseHit))
        every { searchQuranUC.invoke("seek", any()) } returns flowOf(listOf(wordHit))
        every { searchQuranUC.invoke("help", any()) } returns flowOf(listOf(wordHit))

        val results = useCase("seek help")

        assertThat(results.quran.first().ayah.surahNumber).isEqualTo(1)
    }

    @Test
    fun `single-word query searches only the phrase`() = runTest {
        useCase("patience")

        verify(exactly = 1) { searchQuranUC.invoke("patience", any()) }
        verify(exactly = 1) { searchHadithsUC.invoke("patience") }
        verify(exactly = 1) { searchDuasUC.invoke("patience") }
    }

    @Test
    fun `blank query returns empty without touching the database`() = runTest {
        val results = useCase("   ")

        assertThat(results.isEmpty).isTrue()
        verify(exactly = 0) { searchQuranUC.invoke(any(), any()) }
    }

    @Test
    fun `byTerms unions matches across terms and dedupes`() = runTest {
        val shared = quranResult(2, 153)
        every { searchQuranUC.invoke("patience", any()) } returns flowOf(listOf(shared))
        every { searchQuranUC.invoke("sabr", any()) } returns
            flowOf(listOf(shared, quranResult(39, 10)))

        val results = useCase.byTerms(listOf("patience", "sabr"))

        assertThat(results.quran).hasSize(2)
        // The record matched by both terms ranks first.
        assertThat(results.quran.first().ayah.surahNumber).isEqualTo(2)
    }

    @Test
    fun `tokenize drops stopwords and short words`() {
        assertThat(SearchLibraryUseCase.tokenize("what does the Quran say about patience"))
            .containsExactly("patience", "quran")
            .inOrder()
    }

    // ── model builders ────────────────────────────────────────────────────────

    private fun quranResult(surah: Int, ayahNumber: Int) = QuranSearchResult(
        ayah = Ayah(
            id = surah * 1000 + ayahNumber,
            surahNumber = surah,
            ayahNumber = ayahNumber,
            textArabic = "arabic",
            textSimple = "simple",
            juzNumber = 1,
            hizbNumber = 1,
            rubNumber = 1,
            pageNumber = 1,
            sajdaType = null,
            sajdaNumber = null,
            translation = "Be patient.",
        ),
        surahName = "Al-Baqarah",
        matchedText = "patience",
        searchType = SearchType.TRANSLATION,
    )
}
