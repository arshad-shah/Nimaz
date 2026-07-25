package com.arshadshah.nimaz.data.local.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val quranIndopakJson: Json = Json {
    ignoreUnknownKeys = true // the ayahs asset also carries a words[] array we don't need here
    isLenient = true
}

/**
 * One entry of the bundled assets/quran/ayahs_indopak.json — the full IndoPak-script
 * text of an ayah, keyed by the global ayah id (1-6236) that matches
 * [com.arshadshah.nimaz.data.local.database.entity.AyahEntity.id]. The source file also
 * carries a `words` array (the space-split tokens); it is ignored here because the layout
 * word positions index into `text_indopak.split(' ')`, which is identical.
 */
@Serializable
data class IndopakAyahDto(
    @SerialName("ayah_id") val ayahId: Int,
    @SerialName("text_indopak") val textIndopak: String
)

/**
 * One line-segment of the bundled assets/quran/mushaf_layout_indopak16.json — a contiguous
 * run of one ayah's words on a single printed line of the 16-line IndoPak Mushaf. Header
 * and basmalah rows carry only [surahId]; their [ayahId] and word positions are null.
 */
@Serializable
data class IndopakLayoutRowDto(
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("line_number") val lineNumber: Int,
    @SerialName("line_type") val lineType: String, // "ayah" | "surah_header" | "basmalah"
    @SerialName("surah_id") val surahId: Int,
    @SerialName("ayah_id") val ayahId: Int? = null, // global 1-6236
    @SerialName("first_word_position") val firstWordPosition: Int? = null,
    @SerialName("last_word_position") val lastWordPosition: Int? = null
)
