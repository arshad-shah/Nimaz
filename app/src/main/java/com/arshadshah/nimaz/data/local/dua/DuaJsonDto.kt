package com.arshadshah.nimaz.data.local.dua

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val duaJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Root of the bundled assets/duas/duas.json. [contentVersion] is bumped whenever
 * the dua content changes so existing installs re-seed on update (see
 * [DuaContentSeeder]); fresh installs already carry the same content in the
 * prepopulated DB, so the first seed is a harmless one-time refresh.
 */
@Serializable
data class DuaJsonRoot(
    val contentVersion: Int = 1,
    val categories: List<DuaCategoryDto> = emptyList(),
    val duas: List<DuaDto> = emptyList()
)

@Serializable
data class DuaCategoryDto(
    val id: Int,
    @SerialName("name_english") val nameEnglish: String,
    @SerialName("name_arabic") val nameArabic: String,
    val icon: String,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("dua_count") val duaCount: Int
)

@Serializable
data class DuaDto(
    val id: Int,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("title_english") val titleEnglish: String,
    @SerialName("title_arabic") val titleArabic: String,
    @SerialName("text_arabic") val textArabic: String,
    val transliteration: String,
    val translation: String,
    val source: String,
    val virtue: String? = null,
    @SerialName("repeat_count") val repeatCount: Int = 1,
    @SerialName("audio_file") val audioFile: String? = null,
    @SerialName("display_order") val displayOrder: Int
)
