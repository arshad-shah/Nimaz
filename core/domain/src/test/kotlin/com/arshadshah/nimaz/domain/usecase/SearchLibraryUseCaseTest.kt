package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.domain.model.NameSearchResult
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.SearchType
import com.arshadshah.nimaz.domain.repository.settings.FakeSearchSettings
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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

    private val searchNamesUC = mockk<SearchNamesUseCase>()

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
        coEvery { searchNamesUC.invoke(any()) } returns emptyList()

        useCase = searchingUnder(FakeSearchSettings())
    }

    /** The same use case reading a given set of stored preferences. */
    private fun searchingUnder(settings: FakeSearchSettings) = SearchLibraryUseCase(
        quranUseCases,
        hadithUseCases,
        duaUseCases,
        searchNamesUC,
        ObserveSearchPreferencesUseCase(settings),
    )

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

    // ── what the four settings actually change ────────────────────────────────

    @Test
    fun `EXACT strictness runs the phrase and nothing else`() = runTest {
        val exact = searchingUnder(
            FakeSearchSettings(strictness = MatchStrictness.EXACT.name)
        )

        exact("patience during hardship")

        verify(exactly = 1) { searchQuranUC.invoke("patience during hardship", any()) }
        verify(exactly = 0) { searchQuranUC.invoke("patience", any()) }
        verify(exactly = 0) { searchQuranUC.invoke("hardship", any()) }
    }

    @Test
    fun `a switched-off source is not queried at all`() = runTest {
        // Not merely filtered out of the results: the query is the expensive part, and the
        // point of the setting is that narrowing search also makes it faster.
        val quranOnly = searchingUnder(
            FakeSearchSettings(
                sources = ObserveSearchPreferencesUseCase.encode(setOf(LibrarySource.QURAN))
            )
        )

        quranOnly("patience")

        verify(exactly = 1) { searchQuranUC.invoke("patience", any()) }
        // Surah names are part of the Qur'an source, not a source of their own — "search the
        // Qur'an but not its surah names" is not a distinction worth a switch.
        verify(exactly = 1) { getSurahListUC.search("patience") }
        verify(exactly = 0) { searchHadithsUC.invoke(any()) }
        verify(exactly = 0) { searchDuasUC.invoke(any()) }
        coVerify(exactly = 0) { searchNamesUC.invoke(any()) }
    }

    @Test
    fun `the three name catalogues are one source, and reach the results`() = runTest {
        // The Names screen merged three destinations into one, so global search treats them as
        // one source too — one switch, one filter chip, one merged list.
        coEvery { searchNamesUC.invoke("rahman") } returns listOf(
            NameSearchResult(
                catalog = NameCatalog.ASMA_UL_HUSNA,
                id = 1,
                arabic = "الرحمن",
                transliteration = "Ar-Rahman",
                english = "The Most Merciful",
            ),
            NameSearchResult(
                catalog = NameCatalog.ASMA_UN_NABI,
                id = 7,
                arabic = "رحمة",
                transliteration = "Rahmatun lil-Alamin",
                english = "Mercy to the Worlds",
            ),
        )

        val results = useCase("rahman")

        assertThat(results.names.map { it.catalog })
            .containsExactly(NameCatalog.ASMA_UL_HUSNA, NameCatalog.ASMA_UN_NABI)
    }

    @Test
    fun `the per-source cap is the setting, not a constant`() = runTest {
        // The 180-results-for-الله report: three sources each capped at a hardcoded 60.
        val hits = (1..100).map { quranResult(2, it) }
        every { searchQuranUC.invoke("patience", any()) } returns flowOf(hits)

        val narrow = searchingUnder(FakeSearchSettings(resultsPerSource = 25))
        assertThat(narrow("patience").quran).hasSize(25)

        val wide = searchingUnder(FakeSearchSettings(resultsPerSource = 100))
        assertThat(wide("patience").quran).hasSize(100)
    }

    @Test
    fun `a preferences file that would return nothing still searches everything`() = runTest {
        // An empty stored source set is reachable by a downgrade or a restore; honouring it
        // literally would make every search look broken.
        val empty = searchingUnder(FakeSearchSettings(sources = "NOT_A_SOURCE"))

        empty("patience")

        verify(exactly = 1) { searchQuranUC.invoke("patience", any()) }
        verify(exactly = 1) { searchHadithsUC.invoke("patience") }
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
