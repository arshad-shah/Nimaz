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
    val note: String
)
