package com.arshadshah.nimaz.data.local.qaida

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val qaidaJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Root of the bundled assets/qaida/qaida_content.json. [contentVersion] is
 * bumped whenever the Qaida content changes so existing installs re-seed on
 * update (see [QaidaContentSeeder]); fresh installs already carry the same
 * content in the prepopulated DB, so the first seed is a harmless one-time
 * refresh.
 */
@Serializable
data class QaidaJsonRoot(
    val contentVersion: Int = 1,
    val lessons: List<QaidaLessonDto> = emptyList(),
    val letters: List<QaidaLetterDto> = emptyList(),
    val lines: List<QaidaLineDto> = emptyList(),
    val cells: List<QaidaCellDto> = emptyList()
)

@Serializable
data class QaidaLessonDto(
    val id: Int,
    @SerialName("lesson_number") val lessonNumber: Int,
    @SerialName("title_english") val titleEnglish: String,
    @SerialName("title_arabic") val titleArabic: String,
    @SerialName("title_transliteration") val titleTransliteration: String,
    val description: String,
    @SerialName("concept_tags") val conceptTags: List<String> = emptyList(),
    val icon: String,
    @SerialName("display_order") val displayOrder: Int
)

@Serializable
data class QaidaLetterDto(
    val id: Int,
    @SerialName("letter_arabic") val letterArabic: String,
    @SerialName("name_arabic") val nameArabic: String,
    @SerialName("name_transliteration") val nameTransliteration: String,
    @SerialName("isolated_form") val isolatedForm: String,
    @SerialName("initial_form") val initialForm: String? = null,
    @SerialName("medial_form") val medialForm: String? = null,
    @SerialName("final_form") val finalForm: String? = null,
    @SerialName("is_connecting") val isConnecting: Boolean = false,
    @SerialName("makhraj_area") val makhrajArea: String,
    @SerialName("makhraj_detail") val makhrajDetail: String,
    @SerialName("phonetic_hint") val phoneticHint: String? = null,
    @SerialName("audio_key") val audioKey: String,
    @SerialName("display_order") val displayOrder: Int
)

@Serializable
data class QaidaLineDto(
    val id: Int,
    @SerialName("lesson_id") val lessonId: Int,
    @SerialName("line_number") val lineNumber: Int,
    @SerialName("line_type") val lineType: String,
    @SerialName("instruction_english") val instructionEnglish: String? = null,
    @SerialName("instruction_arabic") val instructionArabic: String? = null,
    @SerialName("display_order") val displayOrder: Int
)

@Serializable
data class QaidaCellDto(
    val id: Int,
    @SerialName("line_id") val lineId: Int,
    @SerialName("lesson_id") val lessonId: Int,
    val position: Int,
    @SerialName("text_arabic") val textArabic: String,
    val transliteration: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("audio_key") val audioKey: String,
    @SerialName("highlight_group") val highlightGroup: String? = null,
    @SerialName("letter_id") val letterId: Int? = null,
    val notes: String? = null
)
