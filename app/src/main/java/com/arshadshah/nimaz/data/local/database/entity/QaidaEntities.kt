package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entities backing the Noorani Qaida reader (epic #171, sub-issue C of #174).
 *
 * The content is hierarchical: [QaidaLessonEntity] → [QaidaLineEntity] →
 * [QaidaCellEntity], with [QaidaLetterEntity] acting as a flat reference table
 * linked from cells. The four content tables are seeded from the pre-packaged
 * database (filled by `nimaz-pro-data/scripts/generate_database.py`); the two
 * progress tables are user data, created empty and written at runtime.
 */

/**
 * A Qaida lesson page. One of the 17 lessons of the Noorani Qaida progression.
 */
@Entity(tableName = "qaida_lessons")
data class QaidaLessonEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "lesson_number")
    val lessonNumber: Int,
    @ColumnInfo(name = "title_english")
    val titleEnglish: String,
    @ColumnInfo(name = "title_arabic")
    val titleArabic: String,
    @ColumnInfo(name = "title_transliteration")
    val titleTransliteration: String,
    val description: String,
    // JSON-encoded list of concept tags (matches the prophets/asma JSON-as-TEXT convention).
    @ColumnInfo(name = "concept_tags")
    val conceptTags: String,
    val icon: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int
)

/**
 * The 29-letter reference table (28 letters + hamzah). Positional forms are
 * null for letters that do not connect in that position.
 */
@Entity(tableName = "qaida_letters")
data class QaidaLetterEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "letter_arabic")
    val letterArabic: String,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    @ColumnInfo(name = "name_transliteration")
    val nameTransliteration: String,
    @ColumnInfo(name = "isolated_form")
    val isolatedForm: String,
    @ColumnInfo(name = "initial_form")
    val initialForm: String?,
    @ColumnInfo(name = "medial_form")
    val medialForm: String?,
    @ColumnInfo(name = "final_form")
    val finalForm: String?,
    @ColumnInfo(name = "is_connecting")
    val isConnecting: Boolean,
    // One of the 5 articulation regions: JAWF | HALQ | LISAN | SHAFATAIN | KHAYSHUM.
    @ColumnInfo(name = "makhraj_area")
    val makhrajArea: String,
    @ColumnInfo(name = "makhraj_detail")
    val makhrajDetail: String,
    @ColumnInfo(name = "phonetic_hint")
    val phoneticHint: String?,
    @ColumnInfo(name = "audio_key")
    val audioKey: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int
)

/**
 * A printed row within a lesson page. Holds optional instruction text shown
 * above/around its [QaidaCellEntity] tokens.
 */
@Entity(
    tableName = "qaida_lines",
    foreignKeys = [
        ForeignKey(
            entity = QaidaLessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lesson_id"])]
)
data class QaidaLineEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    @ColumnInfo(name = "line_number")
    val lineNumber: Int,
    // HEADING | EXAMPLE | EXERCISE | PRACTICE
    @ColumnInfo(name = "line_type")
    val lineType: String,
    @ColumnInfo(name = "instruction_english")
    val instructionEnglish: String?,
    @ColumnInfo(name = "instruction_arabic")
    val instructionArabic: String?,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int
)

/**
 * An individual tappable token (a letter, letter+harakah, syllable, or word).
 * Carries `lesson_id` denormalized for fast per-lesson queries and an optional
 * `letter_id` linking back to the reference table.
 */
@Entity(
    tableName = "qaida_cells",
    foreignKeys = [
        ForeignKey(
            entity = QaidaLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["line_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QaidaLetterEntity::class,
            parentColumns = ["id"],
            childColumns = ["letter_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["line_id"]),
        Index(value = ["lesson_id"]),
        Index(value = ["letter_id"])
    ]
)
data class QaidaCellEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "line_id")
    val lineId: Int,
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    val position: Int,
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    val transliteration: String,
    // LETTER | HARAKAH | TANWEEN | SYLLABLE | WORD | MADD | LEEN | SUKOON | SHADDA | MUQATTAAT
    @ColumnInfo(name = "token_type")
    val tokenType: String,
    @ColumnInfo(name = "audio_key")
    val audioKey: String,
    @ColumnInfo(name = "highlight_group")
    val highlightGroup: String?,
    @ColumnInfo(name = "letter_id")
    val letterId: Int?,
    val notes: String?
)

/**
 * Per-lesson user progress. NOT seeded from the prepopulated DB — created empty
 * and populated at runtime as the child works through lessons.
 */
@Entity(tableName = "qaida_lesson_progress")
data class QaidaLessonProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    // LOCKED | UNLOCKED | IN_PROGRESS | COMPLETED
    val status: String,
    val stars: Int,
    @ColumnInfo(name = "last_cell_id")
    val lastCellId: Int?,
    @ColumnInfo(name = "completed_cells")
    val completedCells: Int,
    @ColumnInfo(name = "total_cells")
    val totalCells: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

/**
 * Optional fine-grained per-cell practice tracking. User data, created empty.
 */
@Entity(
    tableName = "qaida_cell_progress",
    primaryKeys = ["lesson_id", "cell_id"]
)
data class QaidaCellProgressEntity(
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    @ColumnInfo(name = "cell_id")
    val cellId: Int,
    @ColumnInfo(name = "heard_count")
    val heardCount: Int,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "last_practiced_at")
    val lastPracticedAt: Long
)
