package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.domain.model.Prophet
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SearchNamesUseCaseTest {

    private lateinit var asmaUlHusnaUseCases: AsmaUlHusnaUseCases
    private lateinit var asmaUnNabiUseCases: AsmaUnNabiUseCases
    private lateinit var prophetUseCases: ProphetUseCases
    private lateinit var useCase: SearchNamesUseCase

    private fun makeAsmaUlHusna(id: Int, nameEnglish: String) = AsmaUlHusna(
        id = id, number = id, nameArabic = "الله", nameTransliteration = "Allah",
        nameEnglish = nameEnglish, meaning = "The Divine", explanation = "",
        benefits = "", quranReferences = emptyList(), usageInDua = "",
        displayOrder = id, isFavorite = false
    )

    private fun makeAsmaUnNabi(id: Int, nameEnglish: String) = AsmaUnNabi(
        id = id, number = id, nameArabic = "محمد", nameTransliteration = "Muhammad",
        nameEnglish = nameEnglish, meaning = "The Praised", explanation = "",
        source = "Quran", displayOrder = id, isFavorite = false
    )

    private fun makeProphet(id: Int, nameEnglish: String, titleEnglish: String = "") = Prophet(
        id = id, number = id, nameArabic = "إبراهيم", nameEnglish = nameEnglish,
        nameTransliteration = nameEnglish, titleArabic = "", titleEnglish = titleEnglish,
        storySummary = "", keyLessons = emptyList(), quranMentions = emptyList(),
        era = "", lineage = "", yearsLived = "", placeOfPreaching = "",
        miracles = emptyList(), displayOrder = id, isFavorite = false
    )

    @Before
    fun setUp() {
        val asmaUlHusnaRepo = mockk<com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository>()
        val asmaUnNabiRepo = mockk<com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository>()
        val prophetRepo = mockk<com.arshadshah.nimaz.domain.repository.ProphetRepository>()

        asmaUlHusnaUseCases = AsmaUlHusnaUseCases(
            getAllNames = GetAllAsmaUlHusnaUseCase(asmaUlHusnaRepo),
            getNameById = GetAsmaUlHusnaByIdUseCase(asmaUlHusnaRepo),
            toggleFavorite = ToggleAsmaUlHusnaFavoriteUseCase(asmaUlHusnaRepo),
            getFavorites = GetFavoriteAsmaUlHusnaUseCase(asmaUlHusnaRepo)
        )
        asmaUnNabiUseCases = AsmaUnNabiUseCases(
            getAllNames = GetAllAsmaUnNabiUseCase(asmaUnNabiRepo),
            getNameById = GetAsmaUnNabiByIdUseCase(asmaUnNabiRepo),
            toggleFavorite = ToggleAsmaUnNabiFavoriteUseCase(asmaUnNabiRepo),
            getFavorites = GetFavoriteAsmaUnNabiUseCase(asmaUnNabiRepo)
        )
        prophetUseCases = ProphetUseCases(
            getAllProphets = GetAllProphetsUseCase(prophetRepo),
            getProphetById = GetProphetByIdUseCase(prophetRepo),
            toggleFavorite = ToggleProphetFavoriteUseCase(prophetRepo),
            getFavorites = GetFavoriteProphetsUseCase(prophetRepo)
        )

        every { asmaUlHusnaRepo.getAllNames() } returns flowOf(
            listOf(
                makeAsmaUlHusna(1, "The Merciful"),
                makeAsmaUlHusna(2, "The King")
            )
        )
        every { asmaUnNabiRepo.getAllNames() } returns flowOf(
            listOf(
                makeAsmaUnNabi(1, "The Praised"),
                makeAsmaUnNabi(2, "The Trustworthy")
            )
        )
        every { prophetRepo.getAllProphets() } returns flowOf(
            listOf(
                makeProphet(1, "Ibrahim", "The Friend of Allah"),
                makeProphet(2, "Musa", "The One Who Speaks to Allah")
            )
        )

        useCase = SearchNamesUseCase(asmaUlHusnaUseCases, asmaUnNabiUseCases, prophetUseCases)
    }

    @Test
    fun `blank query returns empty list`() = runTest {
        val result = useCase("")
        assertThat(result).isEmpty()
    }

    @Test
    fun `whitespace-only query returns empty list`() = runTest {
        val result = useCase("   ")
        assertThat(result).isEmpty()
    }

    @Test
    fun `query matching AsmaUlHusna english name returns results with correct catalog`() = runTest {
        val result = useCase("Merciful")

        assertThat(result).hasSize(1)
        assertThat(result[0].catalog).isEqualTo(NameCatalog.ASMA_UL_HUSNA)
        assertThat(result[0].english).isEqualTo("The Merciful")
    }

    @Test
    fun `query matching AsmaUnNabi name returns results with correct catalog`() = runTest {
        val result = useCase("Trustworthy")

        assertThat(result).hasSize(1)
        assertThat(result[0].catalog).isEqualTo(NameCatalog.ASMA_UN_NABI)
        assertThat(result[0].english).isEqualTo("The Trustworthy")
    }

    @Test
    fun `query matching prophet name returns results with correct catalog`() = runTest {
        val result = useCase("Ibrahim")

        assertThat(result).hasSize(1)
        assertThat(result[0].catalog).isEqualTo(NameCatalog.PROPHETS)
        assertThat(result[0].english).isEqualTo("Ibrahim")
    }

    @Test
    fun `query matching across multiple catalogs returns all matches`() = runTest {
        // "The" appears in all catalogs
        val result = useCase("The")

        assertThat(result.size).isAtLeast(3)
        val catalogs = result.map { it.catalog }.toSet()
        assertThat(catalogs).containsAtLeast(
            NameCatalog.ASMA_UL_HUSNA,
            NameCatalog.ASMA_UN_NABI,
            NameCatalog.PROPHETS
        )
    }

    @Test
    fun `query with no matches returns empty list`() = runTest {
        val result = useCase("xyznotexist123")
        assertThat(result).isEmpty()
    }
}
