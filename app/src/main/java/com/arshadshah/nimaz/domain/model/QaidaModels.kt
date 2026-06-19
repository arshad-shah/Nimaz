package com.arshadshah.nimaz.domain.model

/**
 * Domain models for the Noorani Qaida reader (epic #171, sub-issue D of #175).
 *
 * These are plain data classes fully decoupled from Room. The content hierarchy
 * mirrors the entity layer ([QaidaLesson] → [QaidaLine] → [QaidaCell]) with
 * [QaidaLetter] as a flat reference table, but string columns are lifted into
 * typed enums and `concept_tags` JSON is parsed into a [List]. `audio_key` is
 * resolved by the repository into a playable [QaidaCell.audioPath] /
 * [QaidaLetter.audioPath] per sub-issue B's bundled-asset delivery decision.
 */

/**
 * A Qaida lesson page. One of the 17 lessons of the Noorani Qaida progression.
 */
data class QaidaLesson(
    val id: Int,
    val lessonNumber: Int,
    val titleEnglish: String,
    val titleArabic: String,
    val titleTransliteration: String,
    val description: String,
    val conceptTags: List<String>,
    val icon: String,
    val displayOrder: Int
)

/**
 * A reference entry from the 29-letter table. Positional forms are null for
 * letters that do not connect in that position.
 */
data class QaidaLetter(
    val id: Int,
    val letterArabic: String,
    val nameArabic: String,
    val nameTransliteration: String,
    val isolatedForm: String,
    val initialForm: String?,
    val medialForm: String?,
    val finalForm: String?,
    val isConnecting: Boolean,
    val makhrajArea: MakhrajArea,
    val makhrajDetail: String,
    val phoneticHint: String?,
    val audioKey: String,
    val audioPath: String,
    val displayOrder: Int
)

/**
 * A printed row within a lesson page, carrying optional instruction text shown
 * above/around its [QaidaCell] tokens.
 */
data class QaidaLine(
    val id: Int,
    val lessonId: Int,
    val lineNumber: Int,
    val lineType: LineType,
    val instructionEnglish: String?,
    val instructionArabic: String?,
    val displayOrder: Int
)

/**
 * An individual tappable token (a letter, letter+harakah, syllable, or word).
 */
data class QaidaCell(
    val id: Int,
    val lineId: Int,
    val lessonId: Int,
    val position: Int,
    val textArabic: String,
    val transliteration: String,
    val tokenType: TokenType,
    val audioKey: String,
    val audioPath: String,
    val highlightGroup: String?,
    val letterId: Int?,
    val notes: String?
)

/**
 * A line together with its tappable cells, ordered by [QaidaCell.position].
 */
data class QaidaLineContent(
    val line: QaidaLine,
    val cells: List<QaidaCell>
)

/**
 * A full lesson page: the lesson plus its lines (ordered by display order),
 * each line with its ordered cells.
 */
data class QaidaLessonContent(
    val lesson: QaidaLesson,
    val lines: List<QaidaLineContent>
)

/**
 * Per-lesson user progress. Created empty and populated at runtime as the child
 * works through the lessons (progress-write logic lives in sub-issue E).
 */
data class QaidaLessonProgress(
    val lessonId: Int,
    val status: LessonStatus,
    val stars: Int,
    val lastCellId: Int?,
    val completedCells: Int,
    val totalCells: Int,
    val updatedAt: Long
)

/**
 * Fine-grained per-cell practice tracking. A cell becomes [isCompleted] (i.e.
 * "heard") the first time its audio is played; [heardCount] counts replays.
 */
data class QaidaCellProgress(
    val lessonId: Int,
    val cellId: Int,
    val heardCount: Int,
    val isCompleted: Boolean,
    val lastPracticedAt: Long
)

/**
 * Derived display state for a single lesson: the seeded [QaidaLesson] combined
 * with the learner's stored progress and the data-driven unlock rules. Unlike
 * [QaidaLessonProgress] (raw persisted data), [status] here is always the
 * authoritative value derived from gating + completion.
 */
data class QaidaLessonState(
    val lesson: QaidaLesson,
    val status: LessonStatus,
    val stars: Int,
    val completedCells: Int,
    val totalCells: Int,
    val completionFraction: Float,
    val lastCellId: Int?
)

/**
 * Whole-course rollup for a progress dashboard: every lesson's derived state
 * plus aggregate stats and the "continue where you left off" pointer.
 *
 * [nextLessonId] is the first unlocked-but-incomplete lesson (the global
 * resume pointer); null once every lesson is completed.
 */
data class QaidaCourseProgress(
    val lessons: List<QaidaLessonState>,
    val completedLessons: Int,
    val totalLessons: Int,
    val totalStars: Int,
    val maxStars: Int,
    val totalCellsHeard: Int,
    val overallFraction: Float,
    val nextLessonId: Int?
)

/**
 * The kind of token a [QaidaCell] represents.
 */
enum class TokenType {
    LETTER,
    HARAKAH,
    TANWEEN,
    SYLLABLE,
    WORD,
    MADD,
    LEEN,
    SUKOON,
    SHADDA,
    MUQATTAAT;

    companion object {
        fun fromString(value: String): TokenType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LETTER
        }
    }
}

/**
 * The role a [QaidaLine] plays on the lesson page.
 */
enum class LineType {
    HEADING,
    EXAMPLE,
    EXERCISE,
    PRACTICE;

    companion object {
        fun fromString(value: String): LineType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXAMPLE
        }
    }
}

/**
 * One of the five articulation regions (makharij) of a [QaidaLetter].
 */
enum class MakhrajArea {
    JAWF,
    HALQ,
    LISAN,
    SHAFATAIN,
    KHAYSHUM;

    companion object {
        fun fromString(value: String): MakhrajArea {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: JAWF
        }
    }
}

/**
 * The unlock/completion state of a lesson for the current learner.
 */
enum class LessonStatus {
    LOCKED,
    UNLOCKED,
    IN_PROGRESS,
    COMPLETED;

    companion object {
        fun fromString(value: String): LessonStatus {
            return when (value.uppercase()) {
                "LOCKED" -> LOCKED
                "UNLOCKED" -> UNLOCKED
                "IN_PROGRESS", "INPROGRESS" -> IN_PROGRESS
                "COMPLETED" -> COMPLETED
                else -> LOCKED
            }
        }
    }
}
