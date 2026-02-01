package com.arshadshah.nimaz.domain.model

data class TafseerText(
    val id: Long,
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val tafseerId: String,
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
