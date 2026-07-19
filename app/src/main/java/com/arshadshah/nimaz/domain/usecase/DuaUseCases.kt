package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.DailyDuaSelection
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
    val toggleFavorite: ToggleDuaFavoriteUseCase,
    val getAllBookmarks: GetDuaBookmarksUseCase,
    val insertBookmark: InsertDuaBookmarkUseCase,
    val updateBookmark: UpdateDuaBookmarkUseCase,
    val deleteBookmark: DeleteDuaBookmarkUseCase,
    val getDailyDua: GetDailyDuaUseCase
)

class GetAllCategoriesUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(): Flow<List<DuaCategory>> = repository.getAllCategories()
}

class GetCategoryByIdUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(categoryId: String): DuaCategory? =
        repository.getCategoryById(categoryId)
}

class GetDuaByIdUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(duaId: String): Dua? = repository.getDuaById(duaId)
}

class GetDuasByCategoryUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(categoryId: String): Flow<List<Dua>> =
        repository.getDuasByCategory(categoryId)
}

class GetDuasByOccasionUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(occasion: DuaOccasion): Flow<List<Dua>> =
        repository.getDuasByOccasion(occasion)
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
    suspend operator fun invoke(duaId: String, categoryId: String) =
        repository.toggleFavorite(duaId, categoryId)
}

class GetDuaBookmarksUseCase @Inject constructor(private val repository: DuaRepository) {
    operator fun invoke(): Flow<List<DuaBookmark>> = repository.getAllBookmarks()
}

class InsertDuaBookmarkUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(bookmark: DuaBookmark) = repository.insertBookmark(bookmark)
}

class UpdateDuaBookmarkUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(bookmark: DuaBookmark) = repository.updateBookmark(bookmark)
}

class DeleteDuaBookmarkUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(duaId: String) = repository.deleteBookmark(duaId)
}

/**
 * "Dua of the day": picks the adhkar category appropriate to the time of day, then
 * rotates the specific dua within it daily. [hourOfDay] and [dayOfYear] are passed
 * in so the use case stays pure/testable.
 */
class GetDailyDuaUseCase @Inject constructor(private val repository: DuaRepository) {
    suspend operator fun invoke(hourOfDay: Int, dayOfYear: Int): DailyDuaSelection? {
        val categoryId = categoryIdForHour(hourOfDay)
        val category = repository.getCategoryById(categoryId) ?: return null
        val duas = repository.getDuasByCategoryOnce(categoryId)
        if (duas.isEmpty()) return null
        val index = (dayOfYear % duas.size).coerceIn(0, duas.size - 1)
        return DailyDuaSelection(
            dua = duas[index],
            categoryName = category.nameEnglish,
            categoryIcon = category.iconName
        )
    }

    // Adhkar category ids from the prepopulated `dua_categories` table.
    private fun categoryIdForHour(hour: Int): String = when (hour) {
        in 4..15 -> CATEGORY_MORNING
        in 16..20 -> CATEGORY_EVENING
        else -> CATEGORY_BEFORE_SLEEP
    }

    private companion object {
        const val CATEGORY_MORNING = "1"
        const val CATEGORY_EVENING = "2"
        const val CATEGORY_BEFORE_SLEEP = "5"
    }
}
