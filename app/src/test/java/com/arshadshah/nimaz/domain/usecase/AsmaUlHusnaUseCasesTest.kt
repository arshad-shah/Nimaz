package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
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
 * Unit tests for the Asma-ul-Husna use cases. Each is a thin delegation onto
 * [AsmaUlHusnaRepository]; these guard the wiring, in particular that
 * "favorites" reads from getFavoriteNames() rather than getAllNames() and that
 * the search query is forwarded unchanged.
 */
class AsmaUlHusnaUseCasesTest {

    private lateinit var repository: AsmaUlHusnaRepository

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    private fun name(id: Int, english: String = "Ar-Rahman", favorite: Boolean = false) =
        AsmaUlHusna(
            id = id, number = id, nameArabic = "الرحمن",
            nameTransliteration = "Ar-Rahman", nameEnglish = english,
            meaning = "The Most Gracious", explanation = "", benefits = "",
            quranReferences = emptyList(), usageInDua = "", displayOrder = id,
            isFavorite = favorite
        )

    @Test
    fun `getAllNames exposes the repository flow`() = runTest {
        val names = listOf(name(1), name(2))
        every { repository.getAllNames() } returns flowOf(names)

        assertThat(GetAllAsmaUlHusnaUseCase(repository)().first()).isEqualTo(names)
    }

    @Test
    fun `getNameById returns the repository value`() = runTest {
        coEvery { repository.getNameById(1) } returns name(1)
        assertThat(GetAsmaUlHusnaByIdUseCase(repository)(1)).isEqualTo(name(1))
    }

    @Test
    fun `searchNames forwards the query to the repository`() = runTest {
        val result = listOf(name(1))
        every { repository.searchNames("rahman") } returns flowOf(result)

        assertThat(SearchAsmaUlHusnaUseCase(repository)("rahman").first()).isEqualTo(result)
        verify { repository.searchNames("rahman") }
    }

    @Test
    fun `toggleFavorite delegates with the name id`() = runTest {
        ToggleAsmaUlHusnaFavoriteUseCase(repository)(7)
        coVerify { repository.toggleFavorite(7) }
    }

    @Test
    fun `getFavorites reads from getFavoriteNames not getAllNames`() = runTest {
        val favorites = listOf(name(1, favorite = true))
        every { repository.getFavoriteNames() } returns flowOf(favorites)

        assertThat(GetFavoriteAsmaUlHusnaUseCase(repository)().first()).isEqualTo(favorites)
        verify { repository.getFavoriteNames() }
        verify(exactly = 0) { repository.getAllNames() }
    }
}
