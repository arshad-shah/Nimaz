package com.arshadshah.nimaz.data.local.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val mushafLayoutJson: Json = Json {
    ignoreUnknownKeys = true // the text asset also carries a words[] array we don't need here
    isLenient = true
}

/**
 * One entry of a bundled `assets/quran/mushaf/<text_source>_text.json` — the full glyph text
 * of an ayah in one script, keyed by the global ayah id (1-6236) that matches
 * [com.arshadshah.nimaz.data.local.database.entity.AyahEntity.id]. The source file also
 * carries a `words` array (the space-split tokens); it is ignored here because the layout's
 * word positions index into `text.split(' ')`, which is identical by construction — the
 * generator validates that round-trip.
 */
@Serializable
data class MushafAyahTextDto(
    @SerialName("ayah_id") val ayahId: Int,
    @SerialName("text") val text: String
)

/**
 * One line-segment of a bundled `assets/quran/mushaf/<script>_layout.json` — a contiguous run
 * of one ayah's words on a single printed line of that edition. Header and basmalah rows
 * carry only [surahId]; their [ayahId] and word positions are null.
 */
@Serializable
data class MushafLayoutRowDto(
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("line_number") val lineNumber: Int,
    @SerialName("line_type") val lineType: String, // "ayah" | "surah_header" | "basmalah"
    @SerialName("surah_id") val surahId: Int,
    @SerialName("ayah_id") val ayahId: Int? = null, // global 1-6236
    @SerialName("first_word_position") val firstWordPosition: Int? = null,
    @SerialName("last_word_position") val lastWordPosition: Int? = null
)
