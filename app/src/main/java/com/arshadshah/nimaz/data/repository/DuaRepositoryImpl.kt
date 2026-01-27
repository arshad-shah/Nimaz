package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaProgressEntity
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.DuaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuaRepositoryImpl @Inject constructor(
    private val duaDao: DuaDao
) : DuaRepository {

    override fun getAllCategories(): Flow<List<DuaCategory>> {
        return duaDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(categoryId: String): DuaCategory? {
        return duaDao.getCategoryById(categoryId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getDuasByCategory(categoryId: String): Flow<List<Dua>> {
        return duaDao.getDuasByCategory(categoryId.toIntOrNull() ?: 0).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDuaById(duaId: String): Dua? {
        return duaDao.getDuaById(duaId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getDuasByOccasion(occasion: DuaOccasion): Flow<List<Dua>> {
        // Since there's no occasion column in the database, return empty list
        return duaDao.searchDuas(occasion.name.lowercase()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchDuas(query: String): Flow<List<DuaSearchResult>> {
        return duaDao.searchDuas(query).map { entities ->
            entities.map { dua ->
                DuaSearchResult(
                    dua = dua.toDomain(),
                    categoryName = "",
                    matchedText = dua.translation
                )
            }
        }
    }

    override fun getAllBookmarks(): Flow<List<DuaBookmark>> {
        return duaDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteDuas(): Flow<List<DuaBookmark>> {
        return duaDao.getFavoriteDuas().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookmarkByDuaId(duaId: String): DuaBookmark? {
        return duaDao.getBookmarkByDuaId(duaId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun isDuaBookmarked(duaId: String): Flow<Boolean> {
        return duaDao.isDuaBookmarked(duaId.toIntOrNull() ?: 0)
    }

    override fun isDuaFavorite(duaId: String): Flow<Boolean> {
        return duaDao.isDuaFavorite(duaId.toIntOrNull() ?: 0)
    }

    override suspend fun toggleFavorite(duaId: String, categoryId: String) {
        duaDao.toggleFavorite(
            duaId.toIntOrNull() ?: return,
            categoryId.toIntOrNull() ?: return
        )
    }

    override suspend fun updateBookmark(bookmark: DuaBookmark) {
        duaDao.updateBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(duaId: String) {
        duaDao.deleteBookmarkByDuaId(duaId.toIntOrNull() ?: return)
    }

    override suspend fun getProgressForDuaOnDate(duaId: String, date: Long): DuaProgress? {
        return duaDao.getProgressForDuaOnDate(duaId.toIntOrNull() ?: return null, date)?.toDomain()
    }

    override fun getProgressForDate(date: Long): Flow<List<DuaProgress>> {
        return duaDao.getProgressForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProgressHistoryForDua(duaId: String): Flow<List<DuaProgress>> {
        return duaDao.getProgressHistoryForDua(duaId.toIntOrNull() ?: 0).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun incrementDuaProgress(duaId: String, date: Long, targetCount: Int) {
        duaDao.incrementDuaProgress(duaId.toIntOrNull() ?: return, date, targetCount)
    }

    override suspend fun initializeDuaData() {
        // Data is pre-populated in the database
    }

    override suspend fun isDataInitialized(): Boolean {
        return duaDao.getAllCategories().first().isNotEmpty()
    }

    // Mapping functions
    private fun DuaCategoryEntity.toDomain(): DuaCategory {
        return DuaCategory(
            id = id.toString(),
            nameArabic = nameArabic,
            nameEnglish = nameEnglish,
            description = null,
            iconName = icon,
            displayOrder = displayOrder,
            duaCount = duaCount
        )
    }

    private fun DuaEntity.toDomain(): Dua {
        return Dua(
            id = id.toString(),
            categoryId = categoryId.toString(),
            titleArabic = titleArabic,
            titleEnglish = titleEnglish,
            textArabic = textArabic,
            textTransliteration = transliteration,
            textEnglish = translation,
            reference = source,
            occasion = null,
            benefits = virtue,
            repeatCount = repeatCount,
            audioUrl = audioFile,
            displayOrder = displayOrder
        )
    }

    private fun DuaBookmarkEntity.toDomain(): DuaBookmark {
        return DuaBookmark(
            id = id,
            duaId = duaId.toString(),
            categoryId = categoryId.toString(),
            note = note,
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun DuaBookmark.toEntity(): DuaBookmarkEntity {
        return DuaBookmarkEntity(
            id = id,
            duaId = duaId.toIntOrNull() ?: 0,
            categoryId = categoryId.toIntOrNull() ?: 0,
            note = note,
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun DuaProgressEntity.toDomain(): DuaProgress {
        return DuaProgress(
            id = id,
            duaId = duaId.toString(),
            date = date,
            completedCount = completedCount,
            targetCount = targetCount,
            isCompleted = isCompleted,
            createdAt = createdAt
        )
    }
}
