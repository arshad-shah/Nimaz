package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AsmaUlHusnaDao {
    @Query("SELECT * FROM asma_ul_husna ORDER BY display_order ASC")
    fun getAllNames(): Flow<List<AsmaUlHusnaEntity>>

    @Query("SELECT * FROM asma_ul_husna WHERE id = :id")
    suspend fun getNameById(id: Int): AsmaUlHusnaEntity?

    @Query("SELECT * FROM asma_ul_husna WHERE name_english LIKE '%' || :query || '%' OR name_transliteration LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' ORDER BY display_order ASC")
    fun searchNames(query: String): Flow<List<AsmaUlHusnaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: AsmaUlHusnaBookmarkEntity)

    @Query("DELETE FROM asma_ul_husna_bookmarks WHERE name_id = :nameId")
    suspend fun removeBookmark(nameId: Int)

    @Query("SELECT * FROM asma_ul_husna_bookmarks")
    fun getAllBookmarks(): Flow<List<AsmaUlHusnaBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM asma_ul_husna_bookmarks WHERE name_id = :nameId)")
    suspend fun isBookmarked(nameId: Int): Boolean

    @Transaction
    suspend fun toggleFavorite(nameId: Int) {
        if (isBookmarked(nameId)) {
            removeBookmark(nameId)
        } else {
            insertBookmark(AsmaUlHusnaBookmarkEntity(nameId = nameId))
        }
    }

    @Query("SELECT a.* FROM asma_ul_husna a INNER JOIN asma_ul_husna_bookmarks b ON a.id = b.name_id ORDER BY a.display_order ASC")
    fun getFavoriteNames(): Flow<List<AsmaUlHusnaEntity>>
}
