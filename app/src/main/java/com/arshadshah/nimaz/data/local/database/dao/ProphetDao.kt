package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.database.entity.ProphetBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProphetDao {
    @Query("SELECT * FROM prophets ORDER BY display_order ASC")
    fun getAllProphets(): Flow<List<ProphetEntity>>

    @Query("SELECT * FROM prophets WHERE id = :id")
    suspend fun getProphetById(id: Int): ProphetEntity?

    @Query("SELECT * FROM prophets WHERE name_english LIKE '%' || :query || '%' OR name_transliteration LIKE '%' || :query || '%' OR title_english LIKE '%' || :query || '%' ORDER BY display_order ASC")
    fun searchProphets(query: String): Flow<List<ProphetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: ProphetBookmarkEntity)

    @Query("DELETE FROM prophet_bookmarks WHERE prophet_id = :prophetId")
    suspend fun removeBookmark(prophetId: Int)

    @Query("SELECT * FROM prophet_bookmarks")
    fun getAllBookmarks(): Flow<List<ProphetBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM prophet_bookmarks WHERE prophet_id = :prophetId)")
    suspend fun isBookmarked(prophetId: Int): Boolean

    @Transaction
    suspend fun toggleFavorite(prophetId: Int) {
        if (isBookmarked(prophetId)) {
            removeBookmark(prophetId)
        } else {
            insertBookmark(ProphetBookmarkEntity(prophetId = prophetId))
        }
    }

    @Query("SELECT p.* FROM prophets p INNER JOIN prophet_bookmarks b ON p.id = b.prophet_id ORDER BY p.display_order ASC")
    fun getFavoriteProphets(): Flow<List<ProphetEntity>>
}
