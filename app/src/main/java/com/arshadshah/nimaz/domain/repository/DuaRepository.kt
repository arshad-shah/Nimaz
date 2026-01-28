package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import kotlinx.coroutines.flow.Flow

interface DuaRepository {
    // Category operations
    fun getAllCategories(): Flow<List<DuaCategory>>
    suspend fun getCategoryById(categoryId: String): DuaCategory?

    // Dua operations
    fun getDuasByCategory(categoryId: String): Flow<List<Dua>>
    suspend fun getDuaById(duaId: String): Dua?
    fun getDuasByOccasion(occasion: DuaOccasion): Flow<List<Dua>>

    // Search operations
    fun searchDuas(query: String): Flow<List<DuaSearchResult>>

    // Bookmark operations
    fun getAllBookmarks(): Flow<List<DuaBookmark>>
    fun getFavoriteDuas(): Flow<List<DuaBookmark>>
    suspend fun getBookmarkByDuaId(duaId: String): DuaBookmark?
    fun isDuaBookmarked(duaId: String): Flow<Boolean>
    fun isDuaFavorite(duaId: String): Flow<Boolean>
    suspend fun toggleFavorite(duaId: String, categoryId: String)
    suspend fun updateBookmark(bookmark: DuaBookmark)
    suspend fun deleteBookmark(duaId: String)

    // Progress operations
    suspend fun getProgressForDuaOnDate(duaId: String, date: Long): DuaProgress?
    fun getProgressForDate(date: Long): Flow<List<DuaProgress>>
    fun getProgressHistoryForDua(duaId: String): Flow<List<DuaProgress>>
    suspend fun incrementDuaProgress(duaId: String, date: Long, targetCount: Int)
    suspend fun decrementDuaProgress(duaId: String, date: Long)

    // Data initialization
    suspend fun initializeDuaData()
    suspend fun isDataInitialized(): Boolean
}
