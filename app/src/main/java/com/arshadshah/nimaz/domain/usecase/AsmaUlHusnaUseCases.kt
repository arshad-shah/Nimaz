package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class AsmaUlHusnaUseCases(
    val getAllNames: GetAllAsmaUlHusnaUseCase,
    val getNameById: GetAsmaUlHusnaByIdUseCase,
    val searchNames: SearchAsmaUlHusnaUseCase,
    val toggleFavorite: ToggleAsmaUlHusnaFavoriteUseCase,
    val getFavorites: GetFavoriteAsmaUlHusnaUseCase
)

class GetAllAsmaUlHusnaUseCase @Inject constructor(private val repository: AsmaUlHusnaRepository) {
    operator fun invoke(): Flow<List<AsmaUlHusna>> = repository.getAllNames()
}

class GetAsmaUlHusnaByIdUseCase @Inject constructor(private val repository: AsmaUlHusnaRepository) {
    suspend operator fun invoke(id: Int): AsmaUlHusna? = repository.getNameById(id)
}

class SearchAsmaUlHusnaUseCase @Inject constructor(private val repository: AsmaUlHusnaRepository) {
    operator fun invoke(query: String): Flow<List<AsmaUlHusna>> = repository.searchNames(query)
}

class ToggleAsmaUlHusnaFavoriteUseCase @Inject constructor(private val repository: AsmaUlHusnaRepository) {
    suspend operator fun invoke(nameId: Int) = repository.toggleFavorite(nameId)
}

class GetFavoriteAsmaUlHusnaUseCase @Inject constructor(private val repository: AsmaUlHusnaRepository) {
    operator fun invoke(): Flow<List<AsmaUlHusna>> = repository.getFavoriteNames()
}
