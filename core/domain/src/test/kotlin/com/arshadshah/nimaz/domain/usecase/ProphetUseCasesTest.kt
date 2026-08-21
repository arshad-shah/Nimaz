package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ProphetUseCasesTest {

    private lateinit var repo: FakeProphetRepository
    private lateinit var useCases: ProphetUseCases

    private val prophet1 = Prophet(
        id = 1, number = 1, nameArabic = "آدم", nameEnglish = "Adam",
        nameTransliteration = "Adam", titleArabic = "", titleEnglish = "The Father of Humanity",
        storySummary = "First prophet", keyLessons = listOf("Repentance"),
        quranMentions = listOf("2:31"), era = "Primordial", lineage = "",
        yearsLived = "930", placeOfPreaching = "Earth", miracles = emptyList(),
        displayOrder = 1, isFavorite = false
    )
    private val prophet2 = prophet1.copy(id = 2, number = 2, nameEnglish = "Idris",
        nameArabic = "إدريس", nameTransliteration = "Idris", displayOrder = 2)

    @Before
    fun setUp() {
        repo = FakeProphetRepository()
        useCases = ProphetUseCases(
            getAllProphets = GetAllProphetsUseCase(repo),
            getProphetById = GetProphetByIdUseCase(repo),
            toggleFavorite = ToggleProphetFavoriteUseCase(repo),
            getFavorites = GetFavoriteProphetsUseCase(repo)
        )
    }

    @Test
    fun `getAllProphets returns all prophets`() = runTest {
        repo.prophets.value = listOf(prophet1, prophet2)

        val result = useCases.getAllProphets().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].nameEnglish).isEqualTo("Adam")
        assertThat(result[1].nameEnglish).isEqualTo("Idris")
    }

    @Test
    fun `getProphetById returns correct prophet`() = runTest {
        repo.prophets.value = listOf(prophet1, prophet2)

        val result = useCases.getProphetById(1)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1)
        assertThat(result.nameEnglish).isEqualTo("Adam")
    }

    @Test
    fun `getProphetById returns null for unknown id`() = runTest {
        repo.prophets.value = listOf(prophet1)

        val result = useCases.getProphetById(999)

        assertThat(result).isNull()
    }

    @Test
    fun `toggleFavorite marks prophet as favorite`() = runTest {
        repo.prophets.value = listOf(prophet1)

        useCases.toggleFavorite(prophet1.id)

        assertThat(repo.favorites).contains(prophet1.id)
    }

    @Test
    fun `getFavorites returns only favorited prophets`() = runTest {
        repo.prophets.value = listOf(prophet1, prophet2)
        repo.favorites.add(prophet1.id)

        val result = useCases.getFavorites().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(prophet1.id)
    }

    @Test
    fun `getFavorites returns empty when no favorites`() = runTest {
        repo.prophets.value = listOf(prophet1, prophet2)

        val result = useCases.getFavorites().first()

        assertThat(result).isEmpty()
    }

    private class FakeProphetRepository : ProphetRepository {
        val prophets = MutableStateFlow<List<Prophet>>(emptyList())
        val favorites = mutableSetOf<Int>()

        override fun getAllProphets(): Flow<List<Prophet>> = prophets

        override suspend fun getProphetById(id: Int): Prophet? =
            prophets.value.find { it.id == id }

        override fun getFavoriteProphets(): Flow<List<Prophet>> =
            MutableStateFlow(prophets.value.filter { it.id in favorites })

        override suspend fun toggleFavorite(prophetId: Int) {
            if (prophetId in favorites) favorites.remove(prophetId)
            else favorites.add(prophetId)
        }

        override suspend fun isFavorite(prophetId: Int): Boolean = prophetId in favorites
    }
}
