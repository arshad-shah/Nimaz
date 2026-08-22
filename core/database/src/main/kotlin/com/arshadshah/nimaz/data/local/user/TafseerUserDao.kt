package com.arshadshah.nimaz.data.local.user

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

/**
 * A reader's own highlights and notes on the commentary.
 *
 * The two range reads used to `INNER JOIN ayahs` to turn a surah and an ayah span into the
 * ids a highlight is keyed by. They cannot now — the verses are in the content database and
 * these rows are not, and SQLite will not join across two of them. The span is resolved
 * first by [com.arshadshah.nimaz.data.local.database.dao.TafseerDao.getAyahIdsInRange] and
 * the ids come in as a parameter, which is one extra read and no loss of meaning.
 */
@Dao
interface TafseerUserDao {
    @Query("SELECT * FROM tafseer_highlights WHERE ayah_id = :ayahId AND tafseer_id = :tafseerId ORDER BY start_offset ASC")
    fun getHighlightsForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerHighlightEntity>>

    // Highlights/notes are still keyed by the single ayah they were made on, but a
    // block spans a range, so the reader needs every highlight/note whose ayah
    // falls inside the currently-displayed block — not just the current ayah's own.
    @Query(
        """
        SELECT * FROM tafseer_highlights
        WHERE tafseer_id = :tafseerId AND ayah_id IN (:ayahIds)
        ORDER BY start_offset ASC
        """
    )
    fun getHighlightsForRange(
        tafseerId: String,
        ayahIds: List<Int>,
    ): Flow<List<TafseerHighlightEntity>>
    @Query("SELECT * FROM tafseer_highlights ORDER BY created_at DESC")
    fun getAllHighlights(): Flow<List<TafseerHighlightEntity>>
    @Query("DELETE FROM tafseer_highlights WHERE id = :highlightId")
    suspend fun deleteHighlightById(highlightId: Long)

    // Note operations
    @Query("SELECT * FROM tafseer_notes WHERE ayah_id = :ayahId AND tafseer_id = :tafseerId ORDER BY created_at DESC")
    fun getNotesForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerNoteEntity>>
    @Query(
        """
        SELECT * FROM tafseer_notes
        WHERE tafseer_id = :tafseerId AND ayah_id IN (:ayahIds)
        ORDER BY ayah_id ASC
        """
    )
    fun getNotesForRange(
        tafseerId: String,
        ayahIds: List<Int>,
    ): Flow<List<TafseerNoteEntity>>
    @Query("SELECT * FROM tafseer_notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<TafseerNoteEntity>>
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
    suspend fun insertHighlight(highlight: TafseerHighlightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TafseerNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<TafseerHighlightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<TafseerNoteEntity>)

    @Update
    suspend fun updateHighlight(highlight: TafseerHighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: TafseerHighlightEntity)

    @Update
    suspend fun updateNote(note: TafseerNoteEntity)

    @Delete
    suspend fun deleteNote(note: TafseerNoteEntity)

    /** Everything this reader wrote on the commentary, in one transaction. */
    @Transaction
    suspend fun deleteAllUserData() {
        deleteAllHighlights()
        deleteAllNotes()
    }
}
