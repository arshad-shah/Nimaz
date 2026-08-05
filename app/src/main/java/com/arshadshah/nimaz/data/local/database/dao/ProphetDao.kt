package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProphetDao {
    @Query("SELECT * FROM prophets ORDER BY display_order ASC")
    fun getAllProphets(): Flow<List<ProphetEntity>>

    @Query("SELECT * FROM prophets WHERE id = :id")
    suspend fun getProphetById(id: Int): ProphetEntity?

    /**
     * The favourited entries, from ids the user's database supplied.
     *
     * This was `INNER JOIN prophet_bookmarks`, which cannot work now that a person's marks
     * live in their own database: SQLite will not join across two, and Room could not verify
     * it if it did. Two reads and an `IN` says the same thing honestly.
     */
    @Query("SELECT * FROM prophets WHERE id IN (:ids) ORDER BY display_order ASC")
    fun getByIds(ids: List<Int>): Flow<List<ProphetEntity>>
}
