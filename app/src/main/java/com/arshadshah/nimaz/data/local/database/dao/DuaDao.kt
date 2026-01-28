package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.DuaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuaDao {
    // Category operations
    @Query("SELECT * FROM dua_categories ORDER BY display_order ASC")
    fun getAllCategories(): Flow<List<DuaCategoryEntity>>

    @Query("SELECT * FROM dua_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): DuaCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<DuaCategoryEntity>)

    // Dua operations
    @Query("SELECT * FROM duas WHERE category_id = :categoryId ORDER BY display_order ASC")
    fun getDuasByCategory(categoryId: Int): Flow<List<DuaEntity>>

    @Query("SELECT * FROM duas WHERE id = :duaId")
    suspend fun getDuaById(duaId: Int): DuaEntity?

    @Query("SELECT * FROM duas WHERE title_english LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%'")
    fun searchDuas(query: String): Flow<List<DuaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuas(duas: List<DuaEntity>)

    // Bookmark operations
    @Query("SELECT * FROM dua_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<DuaBookmarkEntity>>

    @Query("SELECT * FROM dua_bookmarks WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteDuas(): Flow<List<DuaBookmarkEntity>>

    @Query("SELECT * FROM dua_bookmarks WHERE duaId = :duaId LIMIT 1")
    suspend fun getBookmarkByDuaId(duaId: Int): DuaBookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM dua_bookmarks WHERE duaId = :duaId)")
    fun isDuaBookmarked(duaId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM dua_bookmarks WHERE duaId = :duaId AND isFavorite = 1)")
    fun isDuaFavorite(duaId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: DuaBookmarkEntity)

    @Query("DELETE FROM dua_bookmarks WHERE duaId = :duaId")
    suspend fun deleteBookmarkByDuaId(duaId: Int)

    @Update
    suspend fun updateBookmark(bookmark: DuaBookmarkEntity)

    @Transaction
    suspend fun toggleFavorite(duaId: Int, categoryId: Int) {
        val existing = getBookmarkByDuaId(duaId)
        if (existing != null) {
            updateBookmark(existing.copy(isFavorite = !existing.isFavorite, updatedAt = System.currentTimeMillis()))
        } else {
            insertBookmark(
                DuaBookmarkEntity(
                    duaId = duaId,
                    categoryId = categoryId,
                    note = null,
                    isFavorite = true
                )
            )
        }
    }

    // Progress operations
    @Query("SELECT * FROM dua_progress WHERE duaId = :duaId AND date = :date LIMIT 1")
    suspend fun getProgressForDuaOnDate(duaId: Int, date: Long): DuaProgressEntity?

    @Query("SELECT * FROM dua_progress WHERE date = :date")
    fun getProgressForDate(date: Long): Flow<List<DuaProgressEntity>>

    @Query("SELECT * FROM dua_progress WHERE duaId = :duaId ORDER BY date DESC")
    fun getProgressHistoryForDua(duaId: Int): Flow<List<DuaProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DuaProgressEntity)

    @Update
    suspend fun updateProgress(progress: DuaProgressEntity)

    @Transaction
    suspend fun decrementDuaProgress(duaId: Int, date: Long) {
        val existing = getProgressForDuaOnDate(duaId, date)
        if (existing != null && existing.completedCount > 0) {
            val newCount = existing.completedCount - 1
            updateProgress(
                existing.copy(
                    completedCount = newCount,
                    isCompleted = newCount >= existing.targetCount
                )
            )
        }
    }

    @Transaction
    suspend fun incrementDuaProgress(duaId: Int, date: Long, targetCount: Int) {
        val existing = getProgressForDuaOnDate(duaId, date)
        if (existing != null) {
            val newCount = existing.completedCount + 1
            updateProgress(
                existing.copy(
                    completedCount = newCount,
                    isCompleted = newCount >= targetCount
                )
            )
        } else {
            insertProgress(
                DuaProgressEntity(
                    duaId = duaId,
                    date = date,
                    completedCount = 1,
                    targetCount = targetCount,
                    isCompleted = 1 >= targetCount
                )
            )
        }
    }
}
