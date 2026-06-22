package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.DuaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class DuaUseCases(
    val getAllCategories: GetAllCategoriesUseCase,
    val getCategoryById: GetCategoryByIdUseCase,
    val getDuaById: GetDuaByIdUseCase,
    val getDuasByCategory: GetDuasByCategoryUseCase,
    val getDuasByOccasion: GetDuasByOccasionUseCase,
    val getFavoriteDuas: GetFavoriteDuasUseCase,
    val getProgressForDate: GetProgressForDateUseCase,
    val isDuaFavorite: IsDuaFavoriteUseCase,
    val searchDuas: SearchDuasUseCase,
    val toggleFavorite: ToggleDuaFavoriteUseCase
)

class GetAllCategoriesUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(): Flow<List<DuaCategory>> = repository.getAllCategories()
}

class GetCategoryByIdUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(categoryId: String): DuaCategory? = repository.getCategoryById(categoryId)
}

class GetDuaByIdUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(duaId: String): Dua? = repository.getDuaById(duaId)
}

class GetDuasByCategoryUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(categoryId: String): Flow<List<Dua>> = repository.getDuasByCategory(categoryId)
}

class GetDuasByOccasionUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(occasion: DuaOccasion): Flow<List<Dua>> = repository.getDuasByOccasion(occasion)
}

class GetFavoriteDuasUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(): Flow<List<DuaBookmark>> = repository.getFavoriteDuas()
}

class GetProgressForDateUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(date: Long): Flow<List<DuaProgress>> = repository.getProgressForDate(date)
}

class IsDuaFavoriteUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(duaId: String): Flow<Boolean> = repository.isDuaFavorite(duaId)
}

class SearchDuasUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(query: String): Flow<List<DuaSearchResult>> = repository.searchDuas(query)
}

class ToggleDuaFavoriteUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(duaId: String, categoryId: String) = repository.toggleFavorite(duaId, categoryId)
}
