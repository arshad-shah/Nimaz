package com.arshadshah.nimaz.domain.model

/**
 * A single commentary passage, covering the contiguous ayah range
 * [ayahStart]..[ayahEnd] within [surahNumber] — a range of one when the source
 * comments ayah-by-ayah, wider when it discusses several ayat as one block.
 */
data class TafseerText(
    val id: Long,
    val tafseerId: String,
    val surahNumber: Int,
    val ayahStart: Int,
    val ayahEnd: Int,
    val text: String
)

data class TafseerHighlight(
    val id: Long,
    val ayahId: Int,
    val tafseerId: String,
    val startOffset: Int,
    val endOffset: Int,
    val color: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class TafseerNote(
    val id: Long,
    val ayahId: Int,
    val tafseerId: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class TafseerSource(val id: String, val displayName: String) {
    IBN_KATHIR("ibn_kathir_en", "Ibn Kathir"),
    MAARIFUL_QURAN("maariful_quran_en", "Ma'arif al-Qur'an")
}

/**
 * A saved tafseer note resolved to its location, for the "My notes" list on the
 * Tafseer chapters page. Carries the surah/ayah so a tap can open the reader.
 */
data class TafseerNoteItem(
    val highlightId: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val sourceLabel: String,
    val color: String,
    val note: String,
    val tafseerId: String = TafseerSource.IBN_KATHIR.id,
    val createdAt: Long = 0,
    val quote: String? = null,
    val reflection: TafseerNote? = null,
    val highlight: TafseerHighlight? = null,
) {
    // The two tables have independent ids. Never key a mixed list by the numeric id alone.
    val key: String get() = if (reflection != null) "note:${reflection.id}" else "highlight:$highlightId"
}

fun TafseerNote.toNoteItem(surah: Int, ayah: Int) = TafseerNoteItem(
    highlightId = id, surahNumber = surah, ayahNumber = ayah,
    sourceLabel = TafseerSource.entries.firstOrNull { it.id == tafseerId }?.displayName ?: tafseerId,
    color = "", note = text, tafseerId = tafseerId, createdAt = createdAt, reflection = this,
)

fun TafseerHighlight.toNoteItem(surah: Int, ayah: Int, commentary: String?) = TafseerNoteItem(
    highlightId = id, surahNumber = surah, ayahNumber = ayah,
    sourceLabel = TafseerSource.entries.firstOrNull { it.id == tafseerId }?.displayName ?: tafseerId,
    color = color, note = note.orEmpty(), tafseerId = tafseerId, createdAt = createdAt,
    quote = commentary?.let { text ->
        val start = startOffset.coerceIn(0, text.length)
        val end = endOffset.coerceIn(start, text.length)
        text.substring(start, end).takeIf { it.isNotBlank() }
    }, highlight = this,
)
