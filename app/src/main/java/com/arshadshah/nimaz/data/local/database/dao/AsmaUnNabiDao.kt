package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
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

    /**
     * The favourited entries, from ids the user's database supplied.
     *
     * This was `INNER JOIN asma_un_nabi_bookmarks`, which cannot work now that a person's marks
     * live in their own database: SQLite will not join across two, and Room could not verify
     * it if it did. Two reads and an `IN` says the same thing honestly.
     */
    @Query("SELECT * FROM asma_un_nabi WHERE id IN (:ids) ORDER BY display_order ASC")
    fun getByIds(ids: List<Int>): Flow<List<AsmaUnNabiEntity>>
}
