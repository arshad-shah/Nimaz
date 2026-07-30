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
    @Query("SELECT * FROM tafseer_highlights WHERE ayah_id = :ayahId AND tafseer_id = :tafseerId ORDER BY start_offset ASC")
    fun getHighlightsForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerHighlightEntity>>

    // Highlights/notes are still keyed by the single ayah they were made on, but a
    // block spans a range, so the reader needs every highlight/note whose ayah
    // falls inside the currently-displayed block — not just the current ayah's own.
    @Query(
        """
        SELECT h.* FROM tafseer_highlights h
        INNER JOIN ayahs a ON a.id = h.ayah_id
        WHERE h.tafseer_id = :tafseerId AND a.surah_id = :surahNumber
          AND a.number_in_surah BETWEEN :ayahStart AND :ayahEnd
        ORDER BY h.start_offset ASC
        """
    )
    fun getHighlightsForRange(
        surahNumber: Int,
        ayahStart: Int,
        ayahEnd: Int,
        tafseerId: String
    ): Flow<List<TafseerHighlightEntity>>

    @Query("SELECT * FROM tafseer_highlights ORDER BY created_at DESC")
    fun getAllHighlights(): Flow<List<TafseerHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: TafseerHighlightEntity): Long

    @Update
    suspend fun updateHighlight(highlight: TafseerHighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: TafseerHighlightEntity)

    @Query("DELETE FROM tafseer_highlights WHERE id = :highlightId")
    suspend fun deleteHighlightById(highlightId: Long)

    // Note operations
    @Query("SELECT * FROM tafseer_notes WHERE ayah_id = :ayahId AND tafseer_id = :tafseerId ORDER BY created_at DESC")
    fun getNotesForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerNoteEntity>>

    @Query(
        """
        SELECT n.* FROM tafseer_notes n
        INNER JOIN ayahs a ON a.id = n.ayah_id
        WHERE n.tafseer_id = :tafseerId AND a.surah_id = :surahNumber
          AND a.number_in_surah BETWEEN :ayahStart AND :ayahEnd
        ORDER BY n.created_at DESC
        """
    )
    fun getNotesForRange(
        surahNumber: Int,
        ayahStart: Int,
        ayahEnd: Int,
        tafseerId: String
    ): Flow<List<TafseerNoteEntity>>

    @Query("SELECT * FROM tafseer_notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<TafseerNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TafseerNoteEntity): Long

    @Update
    suspend fun updateNote(note: TafseerNoteEntity)

    @Delete
    suspend fun deleteNote(note: TafseerNoteEntity)

    @Query("DELETE FROM tafseer_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM tafseer_highlights")
    suspend fun deleteAllHighlights()

    @Query("DELETE FROM tafseer_notes")
    suspend fun deleteAllNotes()

    // Sync export queries
    @Query("SELECT * FROM tafseer_highlights ORDER BY created_at DESC")
    suspend fun getAllHighlightsSync(): List<TafseerHighlightEntity>

    @Query("SELECT * FROM tafseer_notes ORDER BY created_at DESC")
    suspend fun getAllNotesSync(): List<TafseerNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<TafseerHighlightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<TafseerNoteEntity>)

    @Transaction
    suspend fun deleteAllUserData() {
        deleteAllHighlights()
        deleteAllNotes()
    }
}
