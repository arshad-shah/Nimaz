package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import kotlinx.coroutines.flow.Flow

@Dao
interface DuaDao {
    // Existing functions
    @Query("SELECT * FROM Dua WHERE chapter_id = :chapterId")
    suspend fun getDuasOfChapter(chapterId: Int): List<LocalDua>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapters(chapters: List<LocalChapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDuas(duas: List<LocalDua>)

    @Query("SELECT COUNT(*) FROM Chapter")
    suspend fun countChapters(): Int

    @Query("SELECT COUNT(*) FROM Dua")
    suspend fun countDuas(): Int

    @Query("SELECT * FROM Chapter WHERE category_id = :categoryId")
    suspend fun getChaptersByCategory(categoryId: Int): List<LocalChapter>

    // New functions for favorites
    @Query("SELECT * FROM Dua WHERE favourite = 1")
    suspend fun getFavoriteDuas(): List<LocalDua>

    @Query("SELECT * FROM Dua WHERE favourite = 1")
    fun getFavoriteDuasFlow(): Flow<List<LocalDua>>

    // Update favorite status
    @Update
    suspend fun updateDua(dua: LocalDua)

    // Search related queries
    @Query("SELECT * FROM Dua WHERE arabic_dua LIKE '%' || :query || '%' OR english_translation LIKE '%' || :query || '%' OR english_reference LIKE '%' || :query || '%'")
    suspend fun searchDuas(query: String): List<LocalDua>

    // Get specific dua by ID
    @Query("SELECT * FROM Dua WHERE _id = :duaId")
    suspend fun getDuaById(duaId: Int): LocalDua?

    // Get specific chapter by ID
    @Query("SELECT * FROM Chapter WHERE _id = :chapterId")
    suspend fun getChapterById(chapterId: Int): LocalChapter?

    // Get related duas (from same chapter)
    @Query("SELECT * FROM Dua WHERE chapter_id = :chapterId AND _id != :currentDuaId")
    suspend fun getRelatedDuas(chapterId: Int, currentDuaId: Int): List<LocalDua>

    // Batch operations
    @Query("DELETE FROM Dua")
    suspend fun deleteAllDuas()

    @Query("DELETE FROM Chapter")
    suspend fun deleteAllChapters()

    // Statistics queries
    @Query("SELECT COUNT(*) FROM Dua WHERE favourite = 1")
    suspend fun countFavoriteDuas(): Int

    @Query("SELECT COUNT(*) FROM Dua WHERE chapter_id = :chapterId")
    suspend fun countDuasInChapter(chapterId: Int): Int

    // Last accessed/modified queries
    @Query("SELECT * FROM Dua ORDER BY _id DESC LIMIT 1")
    suspend fun getLastAccessedDua(): LocalDua?

    // Bulk operations
    @Transaction
    suspend fun replaceDuas(duas: List<LocalDua>) {
        deleteAllDuas()
        saveDuas(duas)
    }

    @Transaction
    suspend fun replaceChapters(chapters: List<LocalChapter>) {
        deleteAllChapters()
        saveChapters(chapters)
    }

    // Advanced search with multiple criteria
    @Query(
        """
        SELECT * FROM Dua 
        WHERE (:chapterId IS NULL OR chapter_id = :chapterId)
        AND (:isFavorite IS NULL OR favourite = :isFavorite)
        AND (
            LOWER(arabic_dua) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(english_translation) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(english_reference) LIKE '%' || LOWER(:query) || '%'
        )
    """
    )
    suspend fun searchDuasAdvanced(
        query: String = "",
        chapterId: Int? = null,
        isFavorite: Int? = null
    ): List<LocalDua>

    // Get random dua
    @Query("SELECT * FROM Dua ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomDua(): LocalDua?

    // Get random favorite dua
    @Query("SELECT * FROM Dua WHERE favourite = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomFavoriteDua(): LocalDua?
}