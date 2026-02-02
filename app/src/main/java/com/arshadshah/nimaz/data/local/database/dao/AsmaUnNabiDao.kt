package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AsmaUnNabiDao {
    @Query("SELECT * FROM asma_un_nabi ORDER BY display_order ASC")
    fun getAllNames(): Flow<List<AsmaUnNabiEntity>>

    @Query("SELECT * FROM asma_un_nabi WHERE id = :id")
    suspend fun getNameById(id: Int): AsmaUnNabiEntity?

    @Query("SELECT * FROM asma_un_nabi WHERE name_english LIKE '%' || :query || '%' OR name_transliteration LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' ORDER BY display_order ASC")
    fun searchNames(query: String): Flow<List<AsmaUnNabiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: AsmaUnNabiBookmarkEntity)

    @Query("DELETE FROM asma_un_nabi_bookmarks WHERE name_id = :nameId")
    suspend fun removeBookmark(nameId: Int)

    @Query("SELECT * FROM asma_un_nabi_bookmarks")
    fun getAllBookmarks(): Flow<List<AsmaUnNabiBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM asma_un_nabi_bookmarks WHERE name_id = :nameId)")
    suspend fun isBookmarked(nameId: Int): Boolean

    @Transaction
    suspend fun toggleFavorite(nameId: Int) {
        if (isBookmarked(nameId)) {
            removeBookmark(nameId)
        } else {
            insertBookmark(AsmaUnNabiBookmarkEntity(nameId = nameId))
        }
    }

    @Query("SELECT a.* FROM asma_un_nabi a INNER JOIN asma_un_nabi_bookmarks b ON a.id = b.name_id ORDER BY a.display_order ASC")
    fun getFavoriteNames(): Flow<List<AsmaUnNabiEntity>>
}
