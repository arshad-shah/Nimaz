package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class AsmaUnNabiUseCases(
    val getAllNames: GetAllAsmaUnNabiUseCase,
    val getNameById: GetAsmaUnNabiByIdUseCase,
    val toggleFavorite: ToggleAsmaUnNabiFavoriteUseCase,
    val getFavorites: GetFavoriteAsmaUnNabiUseCase
)

class GetAllAsmaUnNabiUseCase @Inject constructor(private val repository: AsmaUnNabiRepository) {
    operator fun invoke(): Flow<List<AsmaUnNabi>> = repository.getAllNames()
}

class GetAsmaUnNabiByIdUseCase @Inject constructor(private val repository: AsmaUnNabiRepository) {
    suspend operator fun invoke(id: Int): AsmaUnNabi? = repository.getNameById(id)
}

class ToggleAsmaUnNabiFavoriteUseCase @Inject constructor(private val repository: AsmaUnNabiRepository) {
    suspend operator fun invoke(nameId: Int) = repository.toggleFavorite(nameId)
}

class GetFavoriteAsmaUnNabiUseCase @Inject constructor(private val repository: AsmaUnNabiRepository) {
    operator fun invoke(): Flow<List<AsmaUnNabi>> = repository.getFavoriteNames()
}
