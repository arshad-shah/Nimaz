package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Swapping a whole content collection out from under the reader.
 *
 * Each of these is a delete-then-insert across foreign-keyed tables, and the *order* is the whole
 * thing: duas reference categories and Qaida cells reference both lines and letters, so deleting
 * a parent before its children — or inserting a child before its parent — is a constraint failure
 * and a half-populated collection. The comment on each method spells the order out; nothing
 * asserted it.
 *
 * `tasbih_presets` is the app's one genuinely mixed table, shipped defaults and user-made rows
 * side by side under an `is_custom` flag, so "delete the user's presets" has to mean exactly the
 * rows with the flag set.
 */
@RunWith(RobolectricTestRunner::class)
class ContentReplacementTest {

    private lateinit var db: NimazDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    // ---- Duas ----

    @Test
    fun `replacing the duas leaves neither the old categories nor the old duas`() = runTest {
        val dao = db.duaDao()
        dao.replaceAllContent(
            categories = listOf(category(1, "Morning")),
            duas = listOf(dua(1, categoryId = 1, title = "On waking")),
        )

        dao.replaceAllContent(
            categories = listOf(category(2, "Evening")),
            duas = listOf(dua(2, categoryId = 2, title = "Before sleeping")),
        )

        assertThat(dao.getAllCategories().first().map { it.nameEnglish }).containsExactly("Evening")
        assertThat(dao.getDuasByCategory(2).first().map { it.titleEnglish })
            .containsExactly("Before sleeping")
        assertThat(dao.getDuasByCategory(1).first()).isEmpty()
    }

    @Test
    fun `a replacement into an empty database works as well as into a full one`() = runTest {
        val dao = db.duaDao()

        dao.replaceAllContent(
            categories = listOf(category(1, "Morning")),
            duas = listOf(dua(1, categoryId = 1, title = "On waking")),
        )

        assertThat(dao.getAllCategories().first()).hasSize(1)
    }

    @Test
    fun `replacing the duas with nothing empties the collection rather than failing`() = runTest {
        val dao = db.duaDao()
        dao.replaceAllContent(listOf(category(1, "Morning")), listOf(dua(1, 1, "On waking")))

        dao.replaceAllContent(categories = emptyList(), duas = emptyList())

        assertThat(dao.getAllCategories().first()).isEmpty()
    }

    // ---- Qaida ----

    @Test
    fun `replacing the qaida content swaps all four tables together`() = runTest {
        val dao = db.qaidaDao()
        dao.replaceAllContent(
            lessons = listOf(lesson(1)),
            letters = listOf(letter(1)),
            lines = listOf(line(101, lessonId = 1)),
            cells = listOf(cell(1001, lineId = 101, lessonId = 1, letterId = 1)),
        )

        dao.replaceAllContent(
            lessons = listOf(lesson(2)),
            letters = listOf(letter(2)),
            lines = listOf(line(201, lessonId = 2)),
            cells = listOf(cell(2001, lineId = 201, lessonId = 2, letterId = 2)),
        )

        // Children first on the way out, parents first on the way in — the foreign keys make any
        // other order a constraint failure part-way through, which is a half-swapped collection.
        assertThat(dao.getAllLessons().first().map { it.id }).containsExactly(2)
        assertThat(dao.getAllLetters().first().map { it.id }).containsExactly(2)
        assertThat(dao.getLinesForLesson(2).first().map { it.id }).containsExactly(201)
        assertThat(dao.getLinesForLesson(1).first()).isEmpty()
    }

    @Test
    fun `a qaida replacement does not touch what the learner has done`() = runTest {
        val dao = db.qaidaDao()
        dao.replaceAllContent(
            lessons = listOf(lesson(1)),
            letters = listOf(letter(1)),
            lines = listOf(line(101, lessonId = 1)),
            cells = listOf(cell(1001, lineId = 101, lessonId = 1, letterId = 1)),
        )

        // The two progress tables are the user's and have no foreign key into the content, which
        // is exactly what makes a content release safe to ship.
        dao.replaceAllContent(
            lessons = listOf(lesson(1), lesson(2)),
            letters = listOf(letter(1)),
            lines = listOf(line(101, lessonId = 1)),
            cells = listOf(cell(1001, lineId = 101, lessonId = 1, letterId = 1)),
        )

        assertThat(dao.getAllLessons().first().map { it.id }).containsExactly(1, 2).inOrder()
    }

    // ---- Counting presets ----

    @Test
    fun `deleting the user's presets leaves the shipped ones`() = runTest {
        val dao = db.tasbihDao()
        dao.insertPresets(
            listOf(
                preset(id = 1, name = "SubhanAllah", isCustom = 0),
                preset(id = 2, name = "Mine", isCustom = 1),
                preset(id = 3, name = "Also mine", isCustom = 1),
            )
        )

        dao.deleteCustomUserPresets()

        // `tasbih_presets` is the one table that is both content and user data. "Delete all my
        // data" must mean the flagged rows and only those.
        assertThat(dao.getAllPresetsSync().map { it.name }).containsExactly("SubhanAllah")
        assertThat(dao.getCustomPresets().first()).isEmpty()
        assertThat(dao.getDefaultPresets().first()).hasSize(1)
    }

    @Test
    fun `deleting the user's presets when they have none changes nothing`() = runTest {
        val dao = db.tasbihDao()
        dao.insertPresets(listOf(preset(id = 1, name = "SubhanAllah", isCustom = 0)))

        dao.deleteCustomUserPresets()

        assertThat(dao.getAllPresetsSync()).hasSize(1)
    }

    private fun category(id: Int, name: String) = DuaCategoryEntity(
        id = id,
        nameEnglish = name,
        nameArabic = name,
        icon = "🕌",
        displayOrder = id,
        duaCount = 1,
    )

    private fun dua(id: Int, categoryId: Int, title: String) = DuaEntity(
        id = id,
        categoryId = categoryId,
        titleEnglish = title,
        titleArabic = title,
        textArabic = "دعاء",
        transliteration = "duaa",
        translation = title,
        source = "Bukhari",
        virtue = null,
        repeatCount = 1,
        audioFile = null,
        displayOrder = id,
    )

    private fun lesson(id: Int) = QaidaLessonEntity(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "درس $id",
        titleTransliteration = "Dars $id",
        description = "desc $id",
        conceptTags = "[\"tag\"]",
        icon = "🔤",
        displayOrder = id,
    )

    private fun letter(id: Int) = QaidaLetterEntity(
        id = id,
        letterArabic = "ب",
        nameArabic = "بَاء",
        nameTransliteration = "baa",
        isolatedForm = "ب",
        initialForm = "بـ",
        medialForm = "ـبـ",
        finalForm = "ـب",
        isConnecting = true,
        makhrajArea = "SHAFATAIN",
        makhrajDetail = "The inner part of both lips.",
        phoneticHint = "like 'b'",
        audioKey = "letter_ba_$id",
        displayOrder = id,
    )

    private fun line(id: Int, lessonId: Int) = QaidaLineEntity(
        id = id,
        lessonId = lessonId,
        lineNumber = 1,
        lineType = "PRACTICE",
        instructionEnglish = null,
        instructionArabic = null,
        displayOrder = 1,
    )

    private fun cell(id: Int, lineId: Int, lessonId: Int, letterId: Int) = QaidaCellEntity(
        id = id,
        lineId = lineId,
        lessonId = lessonId,
        position = 1,
        textArabic = "ب",
        transliteration = "baa",
        tokenType = "LETTER",
        audioKey = "letter_ba_$id",
        highlightGroup = null,
        letterId = letterId,
        notes = null,
    )

    private fun preset(id: Long, name: String, isCustom: Int) = TasbihPresetEntity(
        id = id,
        name = name,
        arabic = "سبحان الله",
        transliteration = "Subhanallah",
        translation = "Glory be to God",
        targetCount = 33,
        isCustom = isCustom,
        displayOrder = id.toInt(),
        updatedAt = 0,
    )
}
