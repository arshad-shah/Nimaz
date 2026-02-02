package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ProphetUseCases(
    val getAllProphets: GetAllProphetsUseCase,
    val getProphetById: GetProphetByIdUseCase,
    val searchProphets: SearchProphetsUseCase,
    val toggleFavorite: ToggleProphetFavoriteUseCase,
    val getFavorites: GetFavoriteProphetsUseCase
)

class GetAllProphetsUseCase @Inject constructor(private val repository: ProphetRepository) {
    operator fun invoke(): Flow<List<Prophet>> = repository.getAllProphets()
}

class GetProphetByIdUseCase @Inject constructor(private val repository: ProphetRepository) {
    suspend operator fun invoke(id: Int): Prophet? = repository.getProphetById(id)
}

class SearchProphetsUseCase @Inject constructor(private val repository: ProphetRepository) {
    operator fun invoke(query: String): Flow<List<Prophet>> = repository.searchProphets(query)
}

class ToggleProphetFavoriteUseCase @Inject constructor(private val repository: ProphetRepository) {
    suspend operator fun invoke(prophetId: Int) = repository.toggleFavorite(prophetId)
}

class GetFavoriteProphetsUseCase @Inject constructor(private val repository: ProphetRepository) {
    operator fun invoke(): Flow<List<Prophet>> = repository.getFavoriteProphets()
}
