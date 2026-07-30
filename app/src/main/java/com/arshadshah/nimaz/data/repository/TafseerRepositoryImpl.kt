package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.database.entity.TafseerBlockEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TafseerRepositoryImpl @Inject constructor(
    private val tafseerDao: TafseerDao,
    /** Highlights and notes are the reader's, and live in the user's own database. */
    private val tafseerUserDao: TafseerUserDao,
    /** For resolving a surah:ayah span to the ids a highlight is keyed by. */
    private val quranDao: QuranDao
) : TafseerRepository {

    override suspend fun getTafseerForAyah(
        surahNumber: Int,
        ayahNumber: Int,
        tafseerId: String
    ): TafseerText? {
        return tafseerDao.getTafseerForAyah(surahNumber, ayahNumber, tafseerId)?.toDomain()
    }

    override fun getTafseerForSurah(surahNumber: Int, tafseerId: String): Flow<List<TafseerText>> {
        return tafseerDao.getTafseerForSurah(surahNumber, tafseerId).mapItems { it.toDomain() }
    }

    override fun getHighlightsForRange(
        surahNumber: Int,
        ayahStart: Int,
        ayahEnd: Int,
        tafseerId: String
    ): Flow<List<TafseerHighlight>> {
        // Two reads, because the verses and the highlights are in different databases now.
        return flow {
            val ayahIds = quranDao.getAyahIdsInRange(surahNumber, ayahStart, ayahEnd)
            if (ayahIds.isEmpty()) {
                emit(emptyList())
            } else {
                emitAll(
                    tafseerUserDao.getHighlightsForRange(tafseerId, ayahIds)
                        .mapItems { it.toDomain() }
                )
            }
        }
    }

    override fun getAllHighlights(): Flow<List<TafseerHighlight>> {
        return tafseerUserDao.getAllHighlights().mapItems { it.toDomain() }
    }

    override suspend fun addHighlight(
        ayahId: Int,
        tafseerId: String,
        startOffset: Int,
        endOffset: Int,
        color: String,
        note: String?
    ): Long {
        val now = System.currentTimeMillis()
        return tafseerUserDao.insertHighlight(
            TafseerHighlightEntity(
                ayahId = ayahId,
                tafseerId = tafseerId,
                startOffset = startOffset,
                endOffset = endOffset,
                color = color,
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateHighlight(highlight: TafseerHighlight) {
        tafseerUserDao.updateHighlight(
            TafseerHighlightEntity(
                id = highlight.id,
                ayahId = highlight.ayahId,
                tafseerId = highlight.tafseerId,
                startOffset = highlight.startOffset,
                endOffset = highlight.endOffset,
                color = highlight.color,
                note = highlight.note,
                createdAt = highlight.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteHighlight(highlightId: Long) {
        tafseerUserDao.deleteHighlightById(highlightId)
    }

    override fun getNotesForRange(
        surahNumber: Int,
        ayahStart: Int,
        ayahEnd: Int,
        tafseerId: String
    ): Flow<List<TafseerNote>> {
        return flow {
            val ayahIds = quranDao.getAyahIdsInRange(surahNumber, ayahStart, ayahEnd)
            if (ayahIds.isEmpty()) {
                emit(emptyList())
            } else {
                emitAll(
                    tafseerUserDao.getNotesForRange(tafseerId, ayahIds)
                        .mapItems { it.toDomain() }
                )
            }
        }
    }

    override suspend fun addNote(ayahId: Int, tafseerId: String, text: String): Long {
        val now = System.currentTimeMillis()
        return tafseerUserDao.insertNote(
            TafseerNoteEntity(
                ayahId = ayahId,
                tafseerId = tafseerId,
                text = text,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateNote(note: TafseerNote) {
        tafseerUserDao.updateNote(
            TafseerNoteEntity(
                id = note.id,
                ayahId = note.ayahId,
                tafseerId = note.tafseerId,
                text = note.text,
                createdAt = note.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteNote(noteId: Long) {
        tafseerUserDao.deleteNoteById(noteId)
    }

    override suspend fun exportAnnotations(): String {
        val highlights = tafseerUserDao.getAllHighlights().first()
        val notes = tafseerUserDao.getAllNotes().first()

        val json = JSONObject()

        val highlightsArray = JSONArray()
        for (h in highlights) {
            highlightsArray.put(JSONObject().apply {
                put("ayah_id", h.ayahId)
                put("tafseer_id", h.tafseerId)
                put("start_offset", h.startOffset)
                put("end_offset", h.endOffset)
                put("color", h.color)
                put("note", h.note ?: "")
                put("created_at", h.createdAt)
            })
        }
        json.put("highlights", highlightsArray)

        val notesArray = JSONArray()
        for (n in notes) {
            notesArray.put(JSONObject().apply {
                put("ayah_id", n.ayahId)
                put("tafseer_id", n.tafseerId)
                put("text", n.text)
                put("created_at", n.createdAt)
            })
        }
        json.put("notes", notesArray)

        return json.toString(2)
    }

    private fun TafseerBlockEntity.toDomain() = TafseerText(
        id = id,
        tafseerId = tafseerId,
        surahNumber = surahNumber,
        ayahStart = ayahStart,
        ayahEnd = ayahEnd,
        text = text
    )

    private fun TafseerHighlightEntity.toDomain() = TafseerHighlight(
        id = id,
        ayahId = ayahId,
        tafseerId = tafseerId,
        startOffset = startOffset,
        endOffset = endOffset,
        color = color,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TafseerNoteEntity.toDomain() = TafseerNote(
        id = id,
        ayahId = ayahId,
        tafseerId = tafseerId,
        text = text,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
