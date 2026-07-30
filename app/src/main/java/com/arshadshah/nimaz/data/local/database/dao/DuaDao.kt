package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
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

    @Query("SELECT COUNT(*) FROM dua_categories")
    suspend fun categoryCount(): Int

    @Query("DELETE FROM dua_categories")
    suspend fun deleteAllCategories()

    // Dua operations
    @Query("SELECT * FROM duas WHERE category_id = :categoryId ORDER BY display_order ASC")
    fun getDuasByCategory(categoryId: Int): Flow<List<DuaEntity>>

    // One-shot read used by the home screen's "Dua of the Moment" selection.
    @Query("SELECT * FROM duas WHERE category_id = :categoryId ORDER BY display_order ASC")
    suspend fun getDuasByCategoryOnce(categoryId: Int): List<DuaEntity>

    @Query("SELECT * FROM duas WHERE id = :duaId")
    suspend fun getDuaById(duaId: Int): DuaEntity?

    @Query("SELECT * FROM duas WHERE title_english LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%'")
    fun searchDuas(query: String): Flow<List<DuaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuas(duas: List<DuaEntity>)

    @Query("DELETE FROM duas")
    suspend fun deleteAllDuas()

    /**
     * Atomically replaces the dua content tables with [categories] and [duas].
     * Used by the content seeder so an interrupted refresh never leaves the
     * collection half-populated. Duas are deleted before categories to respect
     * the duas → dua_categories foreign key, then categories are inserted before
     * duas for the same reason. dua_bookmarks and dua_progress have no foreign
     * key to duas, so this never touches the user's saved data.
     */
    @Transaction
    suspend fun replaceAllContent(
        categories: List<DuaCategoryEntity>,
        duas: List<DuaEntity>
    ) {
        deleteAllDuas()
        deleteAllCategories()
        insertCategories(categories)
        insertDuas(duas)
    }

    // Bookmark operations










    // Progress operations







}
