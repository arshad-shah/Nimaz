package com.arshadshah.nimaz.data.local.user

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The copy out of the content database, which is the one piece of this split that can lose
 * somebody's data if it is wrong.
 *
 * A legacy file is built by hand here — the seven bookmark tables, the three progress
 * tables and one straight copy — because the point is to pin the *mapping*: which old
 * column became which new one, what happens to a verse that was both bookmarked and
 * favourited, and whether running twice does anything the second time.
 */
@RunWith(RobolectricTestRunner::class)
class LegacyUserDataImportTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var legacy: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NimazUserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        legacy = File.createTempFile("legacy-content", ".db")
        buildLegacy(legacy)
    }

    @After
    fun tearDown() {
        db.close()
        legacy.delete()
    }

    private fun buildLegacy(file: File) {
        val helper = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        helper.execSQL(
            "CREATE TABLE quran_bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, ayahId INTEGER NOT NULL, " +
                "surahNumber INTEGER NOT NULL, ayahNumber INTEGER NOT NULL, note TEXT, color TEXT, " +
                "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY, surahNumber INTEGER NOT NULL, " +
                "ayahNumber INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE hadith_bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, hadithId INTEGER NOT NULL, " +
                "bookId INTEGER NOT NULL, hadithNumber INTEGER NOT NULL, note TEXT, color TEXT, " +
                "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE dua_bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, duaId INTEGER NOT NULL, " +
                "categoryId INTEGER NOT NULL, note TEXT, isFavorite INTEGER NOT NULL, " +
                "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE prophet_bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, prophet_id INTEGER NOT NULL, " +
                "is_favorite INTEGER NOT NULL, created_at INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE dua_progress (id INTEGER PRIMARY KEY AUTOINCREMENT, duaId INTEGER NOT NULL, " +
                "date INTEGER NOT NULL, completedCount INTEGER NOT NULL, targetCount INTEGER NOT NULL, " +
                "isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE qaida_lesson_progress (lesson_id INTEGER PRIMARY KEY, status TEXT NOT NULL, " +
                "stars INTEGER NOT NULL, last_cell_id INTEGER, completed_cells INTEGER NOT NULL, " +
                "total_cells INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
        )
        helper.execSQL(
            "CREATE TABLE qaida_cell_progress (lesson_id INTEGER NOT NULL, cell_id INTEGER NOT NULL, " +
                "heard_count INTEGER NOT NULL, is_completed INTEGER NOT NULL, " +
                "last_practiced_at INTEGER NOT NULL, PRIMARY KEY(lesson_id, cell_id))"
        )
        helper.execSQL(
            "CREATE TABLE reading_progress (id INTEGER PRIMARY KEY, lastReadSurah INTEGER NOT NULL, " +
                "lastReadAyah INTEGER NOT NULL, lastReadPage INTEGER NOT NULL, lastReadJuz INTEGER NOT NULL, " +
                "totalAyahsRead INTEGER NOT NULL, currentKhatmaCount INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        )

        // 2:255 — bookmarked with a note *and* favourited. One row out, both flags set.
        helper.execSQL(
            "INSERT INTO quran_bookmarks (ayahId, surahNumber, ayahNumber, note, color, createdAt, updatedAt) " +
                "VALUES (262, 2, 255, 'Ayat al-Kursi', '#ffcc00', 100, 200)"
        )
        helper.execSQL(
            "INSERT INTO quran_favorites (ayahId, surahNumber, ayahNumber, createdAt, updatedAt) " +
                "VALUES (262, 2, 255, 100, 200)"
        )
        // 1:1 — favourited only.
        helper.execSQL(
            "INSERT INTO quran_favorites (ayahId, surahNumber, ayahNumber, createdAt, updatedAt) " +
                "VALUES (1, 1, 1, 300, 300)"
        )
        helper.execSQL(
            "INSERT INTO hadith_bookmarks (hadithId, bookId, hadithNumber, note, color, createdAt, updatedAt) " +
                "VALUES (77, 1, 12, 'niyyah', NULL, 400, 400)"
        )
        helper.execSQL(
            "INSERT INTO dua_bookmarks (duaId, categoryId, note, isFavorite, createdAt, updatedAt) " +
                "VALUES (9, 3, NULL, 1, 500, 500)"
        )
        helper.execSQL(
            "INSERT INTO prophet_bookmarks (prophet_id, is_favorite, created_at) VALUES (4, 1, 600)"
        )
        helper.execSQL(
            "INSERT INTO dua_progress (duaId, date, completedCount, targetCount, isCompleted, createdAt) " +
                "VALUES (9, 20260730, 3, 7, 0, 700)"
        )
        helper.execSQL(
            "INSERT INTO qaida_lesson_progress (lesson_id, status, stars, last_cell_id, completed_cells, " +
                "total_cells, updated_at) VALUES (2, 'COMPLETED', 3, 44, 10, 10, 800)"
        )
        helper.execSQL(
            "INSERT INTO qaida_cell_progress (lesson_id, cell_id, heard_count, is_completed, last_practiced_at) " +
                "VALUES (2, 44, 5, 1, 900)"
        )
        helper.execSQL(
            "INSERT INTO reading_progress (id, lastReadSurah, lastReadAyah, lastReadPage, lastReadJuz, " +
                "totalAyahsRead, currentKhatmaCount, updatedAt) VALUES (1, 18, 10, 294, 15, 1200, 2, 1000)"
        )
        helper.close()
    }

    private fun import() =
        LegacyUserDataImport.run(db.openHelper.writableDatabase, legacy.absolutePath)

    @Test
    fun `a verse that was bookmarked and favourited becomes one row with both flags`() = runTest {
        import()

        val row = db.bookmarkDao().find(BookmarkKind.AYAH, 262)
        assertThat(row).isNotNull()
        assertThat(row!!.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
        assertThat(row.note).isEqualTo("Ayat al-Kursi")
        assertThat(row.colour).isEqualTo("#ffcc00")
        assertThat(row.contextId).isEqualTo(2)     // surah
        assertThat(row.ordinal).isEqualTo(255)     // ayah in surah
    }

    @Test
    fun `a favourite with no bookmark keeps favourite alone`() = runTest {
        import()

        val row = db.bookmarkDao().find(BookmarkKind.AYAH, 1)
        assertThat(row).isNotNull()
        assertThat(row!!.favourite).isTrue()
        assertThat(row.bookmarked).isFalse()
    }

    @Test
    fun `every kind of bookmark arrives, with its context preserved`() = runTest {
        import()

        val hadith = db.bookmarkDao().find(BookmarkKind.HADITH, 77)!!
        assertThat(hadith.contextId).isEqualTo(1)   // book
        assertThat(hadith.ordinal).isEqualTo(12)    // number in book
        assertThat(hadith.note).isEqualTo("niyyah")

        val dua = db.bookmarkDao().find(BookmarkKind.DUA, 9)!!
        assertThat(dua.contextId).isEqualTo(3)      // category
        assertThat(dua.favourite).isTrue()

        assertThat(db.bookmarkDao().find(BookmarkKind.PROPHET, 4)!!.favourite).isTrue()
        assertThat(db.bookmarkDao().all()).hasSize(5)
    }

    @Test
    fun `the three progress tables land in one, keeping what each of them counted`() = runTest {
        import()

        val dua = db.progressDao().find(ProgressKind.DUA, 9, 20260730)!!
        assertThat(dua.completed).isEqualTo(3)
        assertThat(dua.total).isEqualTo(7)
        assertThat(dua.isCompleted).isFalse()

        val lesson = db.progressDao().find(ProgressKind.QAIDA_LESSON, 2)!!
        assertThat(lesson.state).isEqualTo("COMPLETED")
        assertThat(lesson.isCompleted).isTrue()
        assertThat(lesson.score).isEqualTo(3)
        assertThat(lesson.resumeId).isEqualTo(44)
        assertThat(lesson.completed).isEqualTo(10)
        assertThat(lesson.total).isEqualTo(10)

        val cell = db.progressDao().find(ProgressKind.QAIDA_CELL, 44)!!
        assertThat(cell.contextId).isEqualTo(2)     // its lesson
        assertThat(cell.completed).isEqualTo(5)     // times heard
        assertThat(cell.isCompleted).isTrue()
    }

    @Test
    fun `reading progress copies across unchanged`() {
        import()

        db.openHelper.readableDatabase.query(
            "SELECT lastReadSurah, lastReadAyah, lastReadPage, lastReadJuz, totalAyahsRead, " +
                "currentKhatmaCount FROM reading_progress"
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(18)
            assertThat(cursor.getInt(1)).isEqualTo(10)
            assertThat(cursor.getInt(2)).isEqualTo(294)
            assertThat(cursor.getInt(3)).isEqualTo(15)
            assertThat(cursor.getInt(4)).isEqualTo(1200)
            assertThat(cursor.getInt(5)).isEqualTo(2)
        }
    }

    @Test
    fun `running twice changes nothing`() = runTest {
        import()
        val first = db.bookmarkDao().all().sortedBy { it.targetId }
        val firstProgress = db.progressDao().all().size

        import()

        assertThat(db.bookmarkDao().all().sortedBy { it.targetId }).isEqualTo(first)
        assertThat(db.progressDao().all()).hasSize(firstProgress)
    }

    @Test
    fun `a legacy file that is missing tables copies what it has`() = runTest {
        val sparse = File.createTempFile("sparse", ".db")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(sparse, null).use { helper ->
            helper.execSQL(
                "CREATE TABLE prophet_bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "prophet_id INTEGER NOT NULL, is_favorite INTEGER NOT NULL, created_at INTEGER NOT NULL)"
            )
            helper.execSQL("INSERT INTO prophet_bookmarks (prophet_id, is_favorite, created_at) VALUES (1, 1, 5)")
        }

        LegacyUserDataImport.run(db.openHelper.writableDatabase, sparse.absolutePath)

        assertThat(db.bookmarkDao().all()).hasSize(1)
        sparse.delete()
    }

    @Test
    fun `an absent legacy file is not needed and not an error`() {
        val gone = File(legacy.parentFile, "does-not-exist.db")
        assertThat(LegacyUserDataImport.isNeeded(db.openHelper.writableDatabase, gone.absolutePath))
            .isFalse()
    }

    @Test
    fun `a database that already holds bookmarks is left alone`() = runTest {
        db.bookmarkDao().upsert(
            BookmarkEntity(
                kind = BookmarkKind.AYAH, targetId = 999, createdAt = 1, updatedAt = 1,
            )
        )
        assertThat(LegacyUserDataImport.isNeeded(db.openHelper.writableDatabase, legacy.absolutePath))
            .isFalse()
    }
}
