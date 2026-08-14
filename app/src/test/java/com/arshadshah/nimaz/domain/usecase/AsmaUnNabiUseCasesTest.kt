package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AsmaUnNabiUseCasesTest {

    private lateinit var repo: FakeAsmaUnNabiRepository
    private lateinit var useCases: AsmaUnNabiUseCases

    private fun makeName(id: Int, english: String = "The Merciful") = AsmaUnNabi(
        id = id, number = id, nameArabic = "الرحيم", nameTransliteration = "Ar-Rahim",
        nameEnglish = english, meaning = "Merciful", explanation = "",
        source = "Quran", displayOrder = id, isFavorite = false
    )

    @Before
    fun setUp() {
        repo = FakeAsmaUnNabiRepository()
        useCases = AsmaUnNabiUseCases(
            getAllNames = GetAllAsmaUnNabiUseCase(repo),
            getNameById = GetAsmaUnNabiByIdUseCase(repo),
            toggleFavorite = ToggleAsmaUnNabiFavoriteUseCase(repo),
            getFavorites = GetFavoriteAsmaUnNabiUseCase(repo)
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
        repo.names.value = listOf(makeName(1, "The Compassionate"))

        val result = useCases.getNameById(1)

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("The Compassionate")
    }

    @Test
    fun `getNameById returns null when not found`() = runTest {
        repo.names.value = listOf(makeName(1))

        assertThat(useCases.getNameById(404)).isNull()
    }

    @Test
    fun `toggleFavorite adds name to favorites`() = runTest {
        repo.names.value = listOf(makeName(1))

        useCases.toggleFavorite(1)

        assertThat(repo.favorites).contains(1)
    }

    @Test
    fun `toggleFavorite removes already-favorited name`() = runTest {
        repo.names.value = listOf(makeName(1))
        repo.favorites.add(1)

        useCases.toggleFavorite(1)

        assertThat(repo.favorites).doesNotContain(1)
    }

    @Test
    fun `getFavorites returns only favorited names`() = runTest {
        repo.names.value = listOf(makeName(1), makeName(2), makeName(3))
        repo.favorites.addAll(listOf(1, 3))

        val result = useCases.getFavorites().first()

        assertThat(result.map { it.id }).containsExactly(1, 3)
    }

    private class FakeAsmaUnNabiRepository : AsmaUnNabiRepository {
        val names = MutableStateFlow<List<AsmaUnNabi>>(emptyList())
        val favorites = mutableSetOf<Int>()

        override fun getAllNames(): Flow<List<AsmaUnNabi>> = names

        override suspend fun getNameById(id: Int): AsmaUnNabi? =
            names.value.find { it.id == id }

        override fun getFavoriteNames(): Flow<List<AsmaUnNabi>> =
            MutableStateFlow(names.value.filter { it.id in favorites })

        override suspend fun toggleFavorite(nameId: Int) {
            if (nameId in favorites) favorites.remove(nameId) else favorites.add(nameId)
        }

        override suspend fun isFavorite(nameId: Int): Boolean = nameId in favorites
    }
}
