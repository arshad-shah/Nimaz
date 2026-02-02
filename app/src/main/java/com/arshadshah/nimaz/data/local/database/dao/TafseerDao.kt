package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TafseerDao {

    // Tafseer text queries
    @Query("SELECT * FROM tafseer_texts WHERE ayah_id = :ayahId AND tafseer_id = :tafseerId")
    suspend fun getTafseerForAyah(ayahId: Int, tafseerId: String): TafseerTextEntity?

    @Query("SELECT * FROM tafseer_texts WHERE surah_number = :surahNumber AND tafseer_id = :tafseerId ORDER BY ayah_number ASC")
    fun getTafseerForSurah(surahNumber: Int, tafseerId: String): Flow<List<TafseerTextEntity>>

    @Query("SELECT * FROM tafseer_texts WHERE text LIKE '%' || :query || '%' AND tafseer_id = :tafseerId")
    fun searchTafseer(query: String, tafseerId: String): Flow<List<TafseerTextEntity>>

    // Highlight operations
    @Query("SELECT * FROM tafseer_highlights WHERE ayah_id = :ayahId AND tafseer_id = :tafseerId ORDER BY start_offset ASC")
    fun getHighlightsForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerHighlightEntity>>

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

    @Transaction
    suspend fun deleteAllUserData() {
        deleteAllHighlights()
        deleteAllNotes()
    }
}
