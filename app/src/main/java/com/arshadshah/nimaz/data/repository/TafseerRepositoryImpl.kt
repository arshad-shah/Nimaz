package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerTextEntity
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TafseerRepositoryImpl @Inject constructor(
    private val tafseerDao: TafseerDao
) : TafseerRepository {

    override suspend fun getTafseerForAyah(ayahId: Int, tafseerId: String): TafseerText? {
        return tafseerDao.getTafseerForAyah(ayahId, tafseerId)?.toDomain()
    }

    override fun getTafseerForSurah(surahNumber: Int, tafseerId: String): Flow<List<TafseerText>> {
        return tafseerDao.getTafseerForSurah(surahNumber, tafseerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHighlightsForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerHighlight>> {
        return tafseerDao.getHighlightsForAyah(ayahId, tafseerId).map { entities ->
            entities.map { it.toDomain() }
        }
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
        return tafseerDao.insertHighlight(
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
        tafseerDao.updateHighlight(
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
        tafseerDao.deleteHighlightById(highlightId)
    }

    override fun getNotesForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerNote>> {
        return tafseerDao.getNotesForAyah(ayahId, tafseerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addNote(ayahId: Int, tafseerId: String, text: String): Long {
        val now = System.currentTimeMillis()
        return tafseerDao.insertNote(
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
        tafseerDao.updateNote(
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
        tafseerDao.deleteNoteById(noteId)
    }

    override suspend fun exportAnnotations(): String {
        val highlights = tafseerDao.getAllHighlights().first()
        val notes = tafseerDao.getAllNotes().first()

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

    private fun TafseerTextEntity.toDomain() = TafseerText(
        id = id,
        ayahId = ayahId,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        tafseerId = tafseerId,
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
