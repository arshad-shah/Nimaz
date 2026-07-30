package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.TafseerBlockEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TafseerDao {

    // Tafseer block queries — a block covers a contiguous ayah range, so the
    // per-ayah lookup matches on containment rather than equality.
    @Query(
        "SELECT * FROM tafseer_blocks WHERE tafseer_id = :tafseerId AND surah_number = :surahNumber " +
                "AND ayah_start <= :ayahNumber AND ayah_end >= :ayahNumber"
    )
    suspend fun getTafseerForAyah(surahNumber: Int, ayahNumber: Int, tafseerId: String): TafseerBlockEntity?

    @Query("SELECT * FROM tafseer_blocks WHERE surah_number = :surahNumber AND tafseer_id = :tafseerId ORDER BY ayah_start ASC")
    fun getTafseerForSurah(surahNumber: Int, tafseerId: String): Flow<List<TafseerBlockEntity>>

    @Query("SELECT * FROM tafseer_blocks WHERE text LIKE '%' || :query || '%' AND tafseer_id = :tafseerId")
    fun searchTafseer(query: String, tafseerId: String): Flow<List<TafseerBlockEntity>>

    // Highlight operations





}
