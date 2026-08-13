package com.arshadshah.nimaz.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.dao.AYAH_WITH_TEXT_BODY
import com.arshadshah.nimaz.data.local.database.dao.AYAH_WITH_TEXT_VIEW_NAME
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The `ayah_with_text` view, and the three places its SQL is written down.
 *
 * The projection behind it used to be copied into eight `@Query` bodies, each carrying two range
 * joins and a subquery that re-grouped the whole `rukus` table. It is a view over precomputed
 * columns now, which moves a correctness risk: SQLite stores a view's defining statement
 * verbatim, Room compares that whole string on open, and the same string has to be written by
 * the `@DatabaseView` annotation, by `MIGRATION_24_25`, and by nimaz-data into the artifact. A
 * differing space in any of the three makes the database refuse to open — on a user's device,
 * at launch, with no other symptom. So the string is asserted, not trusted.
 */
@RunWith(RobolectricTestRunner::class)
class AyahWithTextViewTest {

    private lateinit var db: NimazDatabase
    private lateinit var dao: QuranDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.quranDao()
    }

    @After
    fun tearDown() = db.close()

    // --- the SQL the three copies have to agree on ------------------------------------------

    @Test
    fun `the assembled CREATE VIEW is the one Room exported`() {
        val exported = exportedSchema()
            .getJSONObject("database")
            .getJSONArray("views")
            .getJSONObject(0)

        assertThat(exported.getString("viewName")).isEqualTo(AYAH_WITH_TEXT_VIEW_NAME)
        // Room writes `${VIEW_NAME}` into the export and substitutes the name at generation time;
        // AYAH_WITH_TEXT_VIEW_SQL is that same substitution done once, for the migration to run.
        assertThat(AYAH_WITH_TEXT_VIEW_SQL).isEqualTo(
            exported.getString("createSql").replace("\${VIEW_NAME}", AYAH_WITH_TEXT_VIEW_NAME)
        )
    }

    @Test
    fun `the exported schema is the version the app declares`() {
        assertThat(exportedSchema().getJSONObject("database").getInt("version"))
            .isEqualTo(NIMAZ_DATABASE_VERSION)
    }

    @Test
    fun `the view Room created carries exactly the statement the migration would run`() {
        // Room built this database from the annotation. `MIGRATION_24_25` builds it from
        // AYAH_WITH_TEXT_VIEW_SQL. Reading it back out of sqlite_master is the only check that
        // covers the path an upgrading device actually takes.
        val stored = db.openHelper.readableDatabase.query(
            "SELECT sql FROM sqlite_master WHERE type = 'view' AND name = ?",
            arrayOf(AYAH_WITH_TEXT_VIEW_NAME)
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            cursor.getString(0)
        }
        assertThat(stored).isEqualTo(AYAH_WITH_TEXT_VIEW_SQL)
    }

    @Test
    fun `the body names no table the projection stopped joining`() {
        // The point of the change: the range joins and the regrouping subquery are gone.
        assertThat(AYAH_WITH_TEXT_BODY).doesNotContain("hizb_quarters")
        assertThat(AYAH_WITH_TEXT_BODY).doesNotContain("rukus")
        assertThat(AYAH_WITH_TEXT_BODY).doesNotContain("BETWEEN")
        assertThat(AYAH_WITH_TEXT_BODY).doesNotContain("GROUP BY")
    }

    // --- what the view returns --------------------------------------------------------------

    @Test
    fun `a verse reads its scripts, its prostration and its divisions in one row`() = runTest {
        seed()

        val fatihaVerse7 = dao.getAyahWithTextById(7)!!

        assertThat(fatihaVerse7.textUthmani).isEqualTo("uthmani-7")
        assertThat(fatihaVerse7.textSimple).isEqualTo("simple-7")
        // Al-Fātiḥah is a single rukūʿ over all seven verses, and it is the first of its surah.
        assertThat(fatihaVerse7.ayah.rukuNumber).isEqualTo(1)
        assertThat(fatihaVerse7.ayah.rukuEndAyahId).isEqualTo(7)
        assertThat(fatihaVerse7.ayah.rubNumber).isEqualTo(1)
        assertThat(fatihaVerse7.ayah.rubStartAyahId).isEqualTo(1)
    }

    @Test
    fun `the divisions are read straight off the row, not recomputed per query`() = runTest {
        seed()

        // Every read path goes through the same view, so they agree by construction — which is
        // the property the eight hand-copied projections could only have by inspection.
        val bySurah = dao.getAyahsWithTextBySurah(1).first().first { it.ayah.id == 7 }
        val byPage = dao.getAyahsWithTextByPage(1).first().first { it.ayah.id == 7 }
        val byRange = dao.getAyahsWithTextByRange(1, 7).first().first { it.ayah.id == 7 }
        val byIds = dao.getAyahsWithTextByIds(listOf(7)).single()
        val byJuz = dao.getAyahsWithTextByJuz(1).first().first { it.ayah.id == 7 }
        val byId = dao.getAyahWithTextById(7)!!

        assertThat(setOf(bySurah, byPage, byRange, byIds, byJuz, byId)).hasSize(1)
    }

    @Test
    fun `a verse with no prostration reads null rather than dropping out of the view`() = runTest {
        seed()

        // A LEFT JOIN, so verse 1 is present with no sajda — an INNER JOIN here would have
        // silently shortened every surah to its prostration verses.
        val verse1 = dao.getAyahWithTextById(1)!!
        assertThat(verse1.sajdaKind).isNull()
        assertThat(verse1.sajdaSequence).isNull()
        assertThat(dao.getAyahsWithTextBySurah(1).first()).hasSize(7)
    }

    @Test
    fun `the prostration query selects on the sajda the view exposes`() = runTest {
        seed()

        val sajdas = dao.getSajdaAyahsWithText().first()

        assertThat(sajdas.map { it.ayah.id }).containsExactly(7)
        assertThat(sajdas.single().sajdaKind).isEqualTo("recommended")
    }

    @Test
    fun `an unfilled division reads null, so the reader renders no marker`() = runTest {
        seed()
        // A device that upgraded before the matching artifact landed: the columns exist and are
        // empty. The contract is "no marker", never a wrong one.
        db.openHelper.writableDatabase.execSQL(
            "UPDATE ayahs SET ruku_number = NULL, ruku_end_ayah_id = NULL, " +
                "rub_number = NULL, rub_start_ayah_id = NULL"
        )

        val verse = dao.getAyahWithTextById(7)!!

        assertThat(verse.ayah.rukuNumber).isNull()
        assertThat(verse.ayah.rukuEndAyahId).isNull()
        assertThat(verse.ayah.rubNumber).isNull()
        assertThat(verse.ayah.rubStartAyahId).isNull()
        // The text still resolves — an absent division must not cost the verse its scripts.
        assertThat(verse.textUthmani).isEqualTo("uthmani-7")
    }

    // --- fixtures ---------------------------------------------------------------------------

    /** Al-Fātiḥah: seven verses, one rukūʿ, opening the first hizb quarter. */
    private suspend fun seed() {
        db.quranDao().insertSurahs(
            listOf(
                SurahEntity(
                    id = 1,
                    number = 1,
                    nameArabic = "الفاتحة",
                    nameEnglish = "The Opening",
                    nameTransliteration = "Al-Fatihah",
                    revelationType = "meccan",
                    versesCount = 7,
                    orderRevealed = 5,
                    startPage = 1
                )
            )
        )
        val db2 = db.openHelper.writableDatabase
        (1..7).forEach { id ->
            db2.execSQL(
                "INSERT INTO ayahs (id, surah_id, number_in_surah, number_global, juz, hizb, " +
                    "page, transliteration, text_tajweed, ruku_number, ruku_end_ayah_id, " +
                    "rub_number, rub_start_ayah_id) VALUES ($id, 1, $id, $id, 1, 1, 1, " +
                    "NULL, NULL, 1, 7, 1, 1)"
            )
            db2.execSQL(
                "INSERT INTO mushaf_ayah_texts (ayah_id, text_source, text) " +
                    "VALUES ($id, 'UTHMANI', 'uthmani-$id')"
            )
            db2.execSQL(
                "INSERT INTO mushaf_ayah_texts (ayah_id, text_source, text) " +
                    "VALUES ($id, 'SIMPLE', 'simple-$id')"
            )
        }
        db2.execSQL(
            "INSERT INTO sajdas (ayah_id, sequence, kind, upstream_kind) " +
                "VALUES (7, 1, 'recommended', NULL)"
        )
    }

    private fun exportedSchema(): JSONObject {
        val name = NimazDatabase::class.java.canonicalName
        val candidates = listOf(
            File("schemas/$name/$NIMAZ_DATABASE_VERSION.json"),
            File("app/schemas/$name/$NIMAZ_DATABASE_VERSION.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("no exported Room schema for v$NIMAZ_DATABASE_VERSION in $candidates")
        return JSONObject(file.readText())
    }
}
