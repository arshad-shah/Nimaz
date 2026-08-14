package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AsmaUlHusnaUseCasesTest {

    private lateinit var repo: FakeAsmaUlHusnaRepository
    private lateinit var useCases: AsmaUlHusnaUseCases

    private fun makeName(id: Int, arabic: String = "الله", english: String = "Allah") = AsmaUlHusna(
        id = id, number = id, nameArabic = arabic, nameTransliteration = "Allah",
        nameEnglish = english, meaning = "The One", explanation = "",
        benefits = "", quranReferences = emptyList(), usageInDua = "",
        displayOrder = id, isFavorite = false
    )

    @Before
    fun setUp() {
        repo = FakeAsmaUlHusnaRepository()
        useCases = AsmaUlHusnaUseCases(
            getAllNames = GetAllAsmaUlHusnaUseCase(repo),
            getNameById = GetAsmaUlHusnaByIdUseCase(repo),
            toggleFavorite = ToggleAsmaUlHusnaFavoriteUseCase(repo),
            getFavorites = GetFavoriteAsmaUlHusnaUseCase(repo)
        )
    }

    @Test
    fun `getAllNames returns all names`() = runTest {
        repo.names.value = listOf(makeName(1), makeName(2))

        val result = useCases.getAllNames().first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getNameById returns correct name`() = runTest {
        val name = makeName(1, english = "The Merciful")
        repo.names.value = listOf(name)

        val result = useCases.getNameById(1)

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("The Merciful")
    }

    @Test
    fun `getNameById returns null for unknown id`() = runTest {
        repo.names.value = listOf(makeName(1))

        assertThat(useCases.getNameById(999)).isNull()
    }

    @Test
    fun `toggleFavorite adds to favorites`() = runTest {
        repo.names.value = listOf(makeName(1))

        useCases.toggleFavorite(1)

        assertThat(repo.favorites).contains(1)
    }

    @Test
    fun `getFavorites returns favorited names`() = runTest {
        repo.names.value = listOf(makeName(1), makeName(2))
        repo.favorites.add(2)

        val result = useCases.getFavorites().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(2)
    }

    private class FakeAsmaUlHusnaRepository : AsmaUlHusnaRepository {
        val names = MutableStateFlow<List<AsmaUlHusna>>(emptyList())
        val favorites = mutableSetOf<Int>()

        override fun getAllNames(): Flow<List<AsmaUlHusna>> = names

        override suspend fun getNameById(id: Int): AsmaUlHusna? =
            names.value.find { it.id == id }

        override fun getFavoriteNames(): Flow<List<AsmaUlHusna>> =
            MutableStateFlow(names.value.filter { it.id in favorites })

        override suspend fun toggleFavorite(nameId: Int) {
            if (nameId in favorites) favorites.remove(nameId) else favorites.add(nameId)
        }

        override suspend fun isFavorite(nameId: Int): Boolean = nameId in favorites
    }
}
