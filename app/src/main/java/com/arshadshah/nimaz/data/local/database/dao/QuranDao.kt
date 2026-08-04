package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Embedded
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.AyahThemeEntity
import com.arshadshah.nimaz.data.local.database.entity.HizbQuarterEntity
import com.arshadshah.nimaz.data.local.database.entity.JuzEntity
import com.arshadshah.nimaz.data.local.database.entity.ManzilEntity
import com.arshadshah.nimaz.data.local.database.entity.PageEntity
import com.arshadshah.nimaz.data.local.database.entity.RukuEntity
import com.arshadshah.nimaz.data.local.database.entity.SajdaEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahStructureEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafAyahTextEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutLineEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranTopicAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranTopicEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahInfoEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahOverviewEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahOverviewSectionEntity
import com.arshadshah.nimaz.data.local.database.entity.TopicWithSurahCount
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

data class PageAyahRangeRow(
    val page: Int,
    val minAyahId: Int,
    val maxAyahId: Int,
    val ayahCount: Int
)

/**
 * One line-segment of a line-accurate layout for a page, joined with the segment's glyph
 * text and ayah so the data layer can reconstruct words without a second query. Column names
 * are aliased in [QuranDao.getMushafLayoutByPage] to match these field names.
 *
 * [text] / [ayahNumberInSurah] are null for surah-header and basmalah rows (their [ayahId]
 * is null, so neither LEFT JOIN contributes anything).
 */
data class MushafLayoutLineRow(
    val page: Int,
    val line: Int,
    val lineType: String, // "ayah" | "surah_header" | "basmalah"
    val surahId: Int,
    val ayahId: Int?,
    val firstWordPosition: Int?,
    val lastWordPosition: Int?,
    val text: String?,
    val ayahNumberInSurah: Int?
)

@Dao
interface QuranDao {
    // Surah operations
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :surahNumber")
    suspend fun getSurahByNumber(surahNumber: Int): SurahEntity?

    @Query("SELECT * FROM surahs WHERE id = :surahId")
    suspend fun getSurahById(surahId: Int): SurahEntity?

    @Query("SELECT * FROM surahs WHERE revelation_type = :type ORDER BY number ASC")
    fun getSurahsByRevelationType(type: String): Flow<List<SurahEntity>>

    /** The surahs the shipped search index named, by number. See [getAyahsWithTextByIds]. */
    @Query("SELECT * FROM surahs WHERE number IN (:numbers) ORDER BY number ASC")
    suspend fun getSurahsByNumbers(numbers: List<Int>): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE name_english LIKE '%' || :query || '%' OR name_transliteration LIKE '%' || :query || '%'")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    // Ayah operations
    @Query("SELECT * FROM ayahs WHERE surah_id = :surahId ORDER BY number_in_surah ASC")
    fun getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE id = :ayahId")
    suspend fun getAyahById(ayahId: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE juz = :juzNumber ORDER BY id ASC")
    fun getAyahsByJuz(juzNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE page = :pageNumber ORDER BY id ASC")
    fun getAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>>

    /**
     * Ayahs spanning a global id range, in mushaf order. Used to fetch the content of a page
     * of an edition that does not paginate by the `ayahs.page` column — the 16-line IndoPak
     * mushaf, whose pagination lives in [MushafLayoutIndopak16Entity] (#325).
     */
    @Query("SELECT * FROM ayahs WHERE id BETWEEN :minAyahId AND :maxAyahId ORDER BY id ASC")
    fun getAyahsByIdRange(minAyahId: Int, maxAyahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT page, MIN(id) AS minAyahId, MAX(id) AS maxAyahId, COUNT(id) AS ayahCount FROM ayahs GROUP BY page ORDER BY page ASC")
    suspend fun getPageAyahRanges(): List<PageAyahRangeRow>

    /**
     * A line-accurate edition's page→ayah mapping, the counterpart of [getPageAyahRanges]
     * for editions that do not paginate by `ayahs.page` (#325). Header/basmalah rows carry a
     * null `ayah_id` and are excluded; a page's ayahs are counted distinctly because one
     * ayah can span several printed lines of the same page.
     */
    @Query(
        """
        SELECT page,
               MIN(ayah_id) AS minAyahId,
               MAX(ayah_id) AS maxAyahId,
               COUNT(DISTINCT ayah_id) AS ayahCount
        FROM mushaf_layout_lines
        WHERE script = :script AND ayah_id IS NOT NULL
        GROUP BY page
        ORDER BY page ASC
        """
    )
    suspend fun getLayoutPageAyahRanges(script: String): List<PageAyahRangeRow>

    // The prostration verses, in mushaf order. `sajda`/`sajda_type` were columns on all 6,236
    // rows to describe fifteen of them; since schemaVersion 22 they are the `sajdas` table.
    @Query(
        """
        SELECT a.* FROM ayahs a
        JOIN sajdas s ON s.ayah_id = a.id
        ORDER BY s.sequence ASC
        """
    )
    fun getSajdaAyahs(): Flow<List<AyahEntity>>

    /** The fifteen prostrations with their classification, which the ayah row no longer carries. */
    @Query("SELECT * FROM sajdas ORDER BY sequence ASC")
    fun getSajdas(): Flow<List<SajdaEntity>>

    @Query("SELECT * FROM sajdas WHERE ayah_id = :ayahId")
    suspend fun getSajdaForAyah(ayahId: Int): SajdaEntity?

    // Search every rendering rather than one column twice. The old query was
    // `text_uthmani LIKE … OR text_arabic LIKE …` over two columns that held identical bytes,
    // so it scanned the same 1.3 MB twice and could never match anything the other missed.
    // Now a match in any script — Uthmani, plain, either IndoPak — finds the verse, and DISTINCT
    // keeps a verse that matches in three of them from appearing three times.
    @Query(
        """
        SELECT DISTINCT a.* FROM ayahs a
        JOIN mushaf_ayah_texts t ON t.ayah_id = a.id
        WHERE t.text LIKE '%' || :query || '%'
        ORDER BY a.id ASC
        """
    )
    fun searchAyahs(query: String): Flow<List<AyahEntity>>


    // --- verses with their text (schemaVersion 22) ----------------------------------------

    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.surah_id = :surahId
        ORDER BY a.number_in_surah ASC
        """
    )
    fun getAyahsWithTextBySurah(surahId: Int): Flow<List<AyahWithText>>

    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.id = :ayahId
        """
    )
    suspend fun getAyahWithTextById(ayahId: Int): AyahWithText?

    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.id BETWEEN :minAyahId AND :maxAyahId
        ORDER BY a.id ASC
        """
    )
    fun getAyahsWithTextByRange(minAyahId: Int, maxAyahId: Int): Flow<List<AyahWithText>>

    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE sj.ayah_id IS NOT NULL
        ORDER BY sj.sequence ASC
        """
    )
    fun getSajdaAyahsWithText(): Flow<List<AyahWithText>>

    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.id IN (
            SELECT DISTINCT t.ayah_id FROM mushaf_ayah_texts t
            WHERE t.text LIKE '%' || :query || '%'
        )
        ORDER BY a.id ASC
        """
    )
    fun searchAyahsWithText(query: String): Flow<List<AyahWithText>>

    /**
     * The verses the shipped search index named, fetched in one go (#330).
     *
     * The index answers with ayah ids and nothing else, so this is how a hit becomes a
     * verse. It is a plain primary-key lookup — the searching already happened, against
     * a folded index that a `LIKE` could not have replaced at any speed.
     */
    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.id IN (:ayahIds)
        ORDER BY a.id ASC
        """
    )
    suspend fun getAyahsWithTextByIds(ayahIds: List<Int>): List<AyahWithText>


    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.page = :pageNumber
        ORDER BY a.id ASC
        """
    )
    fun getAyahsWithTextByPage(pageNumber: Int): Flow<List<AyahWithText>>

    @Query(
        """
        SELECT a.*,
               u.text AS text_uthmani,
               s.text AS text_simple,
               sj.kind AS sajda_kind,
               sj.sequence AS sajda_sequence,
               hq.number AS rub_number
        FROM ayahs a
        LEFT JOIN mushaf_ayah_texts u ON u.ayah_id = a.id AND u.text_source = 'UTHMANI'
        LEFT JOIN mushaf_ayah_texts s ON s.ayah_id = a.id AND s.text_source = 'SIMPLE'
        LEFT JOIN sajdas sj ON sj.ayah_id = a.id
        LEFT JOIN hizb_quarters hq ON a.id BETWEEN hq.start_ayah_id AND hq.end_ayah_id
        WHERE a.juz = :juzNumber
        ORDER BY a.id ASC
        """
    )
    fun getAyahsWithTextByJuz(juzNumber: Int): Flow<List<AyahWithText>>

    /**
     * The ids of a surah's verses within an ayah-number span.
     *
     * The bridge for anything the user's database keys by ayah id but selects by surah and
     * ayah number — highlights and notes on a commentary block, for instance. Those queries
     * used to `INNER JOIN ayahs`; the verses and the highlights are in different databases
     * now, so the span is resolved here and the ids travel as a parameter.
     */
    @Query(
        """
        SELECT id FROM ayahs
        WHERE surah_id = :surahNumber AND number_in_surah BETWEEN :ayahStart AND :ayahEnd
        ORDER BY number_in_surah ASC
        """
    )
    suspend fun getAyahIdsInRange(surahNumber: Int, ayahStart: Int, ayahEnd: Int): List<Int>

    /** A surah's verse ids, in order. */
    @Query("SELECT id FROM ayahs WHERE surah_id = :surahNumber ORDER BY id ASC")
    suspend fun getAyahIdsForSurah(surahNumber: Int): List<Int>

    /** Where a verse sits, for a khatam that knows only the id it has not read yet. */
    @Query(
        """
        SELECT surah_id AS surahId, number_in_surah AS numberInSurah, juz AS juz
        FROM ayahs WHERE id = :ayahId
        """
    )
    suspend fun getAyahLocation(ayahId: Int): AyahLocation?

    /** How many verses each juz holds. The other half of khatam progress. */
    @Query("SELECT juz AS juzNumber, COUNT(*) AS totalAyahs FROM ayahs GROUP BY juz ORDER BY juz ASC")
    suspend fun getJuzAyahTotals(): List<JuzAyahTotal>

    // --- the divisions of the mushaf (schemaVersion 22) ----------------------------------
    //
    // Each of these was previously a scan over `ayahs` with a MIN/MAX, which is why none of
    // them existed and there is no juz screen.

    @Query("SELECT * FROM juzs ORDER BY number ASC")
    fun getJuzs(): Flow<List<JuzEntity>>

    @Query("SELECT * FROM juzs WHERE :ayahId BETWEEN start_ayah_id AND end_ayah_id")
    suspend fun getJuzForAyah(ayahId: Int): JuzEntity?

    @Query("SELECT * FROM hizb_quarters WHERE juz_number = :juz ORDER BY number ASC")
    fun getHizbQuartersForJuz(juz: Int): Flow<List<HizbQuarterEntity>>

    @Query("SELECT * FROM hizb_quarters WHERE :ayahId BETWEEN start_ayah_id AND end_ayah_id")
    suspend fun getHizbQuarterForAyah(ayahId: Int): HizbQuarterEntity?

    @Query("SELECT * FROM manzils ORDER BY number ASC")
    fun getManzils(): Flow<List<ManzilEntity>>

    @Query("SELECT * FROM rukus WHERE surah_id = :surahId ORDER BY number ASC")
    fun getRukusForSurah(surahId: Int): Flow<List<RukuEntity>>

    @Query("SELECT * FROM rukus WHERE :ayahId BETWEEN start_ayah_id AND end_ayah_id")
    suspend fun getRukuForAyah(ayahId: Int): RukuEntity?

    @Query("SELECT * FROM pages WHERE number = :page")
    suspend fun getPageRange(page: Int): PageEntity?

    @Query("SELECT * FROM surah_structure WHERE surah_id = :surahId")
    suspend fun getSurahStructure(surahId: Int): SurahStructureEntity?

    @Query("SELECT * FROM surah_structure ORDER BY surah_id ASC")
    fun getAllSurahStructure(): Flow<List<SurahStructureEntity>>

    // Seeding for the divisions that are not derivable from `ayahs` on an upgrading device:
    // rukus, manzils and surah_structure have never been on a phone before.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRukus(rukus: List<RukuEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManzils(manzils: List<ManzilEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahStructure(rows: List<SurahStructureEntity>)

    @Query("SELECT COUNT(*) FROM rukus")
    suspend fun rukuCount(): Int

    @Query("SELECT COUNT(*) FROM surah_structure")
    suspend fun surahStructureCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    // Line-accurate Mushaf layouts arrive whole in the content artifact; the write side of
    // these tables went with MushafLayoutSeeder at versionCode 385 (docs/retirement.yaml).
    // The per-page read query for the renderer (getMushafLayoutByPage) is defined below.

    /**
     * All line-segments of one page of a line-accurate edition, ordered top-to-bottom and,
     * within a line, in reading order (insertion `id` preserves the source order). Each
     * segment is LEFT-JOINed to its glyph text and its ayah so both come back in one round
     * trip; header/basmalah rows have a null `ayah_id` and contribute no joined columns. The
     * data layer groups these by line into a MushafPageLayout.
     */
    @Query(
        """
        SELECT m.page AS page, m.line AS line, m.line_type AS lineType,
               m.surah_id AS surahId, m.ayah_id AS ayahId,
               m.first_word_position AS firstWordPosition,
               m.last_word_position AS lastWordPosition,
               t.text AS text, a.number_in_surah AS ayahNumberInSurah
        FROM mushaf_layout_lines m
        LEFT JOIN mushaf_ayah_texts t
               ON t.ayah_id = m.ayah_id AND t.text_source = :textSource
        LEFT JOIN ayahs a ON a.id = m.ayah_id
        WHERE m.script = :script AND m.page = :page
        ORDER BY m.line ASC, m.id ASC
        """
    )
    suspend fun getMushafLayoutByPage(
        script: String,
        textSource: String,
        page: Int
    ): List<MushafLayoutLineRow>

    // Translation operations
    @Query("SELECT * FROM translations WHERE ayah_id = :ayahId AND translator_id = :translatorId")
    suspend fun getTranslation(ayahId: Int, translatorId: String): TranslationEntity?

    @Query("SELECT * FROM translations WHERE ayah_id IN (:ayahIds) AND translator_id = :translatorId")
    fun getTranslationsForAyahs(
        ayahIds: List<Int>,
        translatorId: String
    ): Flow<List<TranslationEntity>>

    @Query("SELECT DISTINCT translator_id FROM translations")
    suspend fun getAvailableTranslatorIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<TranslationEntity>)

    // All 15 translations arrive in the content artifact; the per-translation write side went
    // with QuranTranslationSeeder at versionCode 385 (docs/retirement.yaml).

    @Query("SELECT * FROM translations WHERE text LIKE '%' || :query || '%' AND translator_id = :translatorId")
    fun searchTranslations(query: String, translatorId: String): Flow<List<TranslationEntity>>

    /** The translations the shipped search index named. See [getAyahsWithTextByIds]. */
    @Query(
        """
        SELECT * FROM translations
        WHERE ayah_id IN (:ayahIds) AND translator_id = :translatorId
        ORDER BY ayah_id ASC
        """
    )
    suspend fun getTranslationsByAyahIds(
        ayahIds: List<Int>,
        translatorId: String
    ): List<TranslationEntity>

    // Bookmark operations















    @Query("SELECT * FROM surah_info WHERE surahNumber = :surahNumber")
    suspend fun getSurahInfo(surahNumber: Int): SurahInfoEntity?

    // ---- Surah overview (long-form background) ----

    @Query("SELECT * FROM surah_overviews WHERE surah_number = :surahNumber")
    suspend fun getSurahOverview(surahNumber: Int): SurahOverviewEntity?

    @Query(
        "SELECT * FROM surah_overview_sections WHERE surah_number = :surahNumber " +
            "ORDER BY position ASC"
    )
    suspend fun getSurahOverviewSections(surahNumber: Int): List<SurahOverviewSectionEntity>

    // ---- Thematic passages ----

    @Query(
        "SELECT * FROM ayah_themes WHERE surah_number = :surahNumber " +
            "ORDER BY ayah_from ASC"
    )
    suspend fun getThemesForSurah(surahNumber: Int): List<AyahThemeEntity>

    /**
     * The passage an ayah falls in. Containment rides the `(surah_number, ayah_from)` primary
     * key, and the ranges never overlap, so at most one row can match — `LIMIT 1` states that
     * rather than relying on it.
     */
    @Query(
        "SELECT * FROM ayah_themes WHERE surah_number = :surahNumber " +
            "AND ayah_from <= :ayahNumber AND ayah_to >= :ayahNumber LIMIT 1"
    )
    suspend fun getThemeForAyah(surahNumber: Int, ayahNumber: Int): AyahThemeEntity?

    @Query("SELECT COUNT(*) FROM ayah_themes")
    suspend fun countThemes(): Int

    // ---- Topics ----

    @Query("SELECT * FROM quran_topics WHERE topic_id = :topicId")
    suspend fun getTopic(topicId: Int): QuranTopicEntity?

    @Query("SELECT * FROM quran_topics WHERE topic_id IN (:topicIds)")
    suspend fun getTopics(topicIds: List<Int>): List<QuranTopicEntity>

    /**
     * The roots of one of the three hierarchies. `parent` is the subject index (a root is a
     * topic with no `parent_id`); `thematic` and `ontology` are the two curated trees, whose
     * roots are members with a null parent of that kind — which is why membership is a column
     * and not inferred from the parent being null.
     */
    @Query(
        "SELECT * FROM quran_topics WHERE " +
            "(:tree = 'thematic' AND is_thematic = 1 AND thematic_parent_id IS NULL) OR " +
            "(:tree = 'ontology' AND is_ontology = 1 AND ontology_parent_id IS NULL) OR " +
            "(:tree = 'index' AND parent_id IS NULL) " +
            "ORDER BY ayah_count DESC, name ASC"
    )
    suspend fun getRootTopics(tree: String): List<QuranTopicEntity>

    @Query(
        "SELECT * FROM quran_topics WHERE " +
            "(:tree = 'thematic' AND thematic_parent_id = :parentId) OR " +
            "(:tree = 'ontology' AND ontology_parent_id = :parentId) OR " +
            "(:tree = 'index' AND parent_id = :parentId) " +
            "ORDER BY ayah_count DESC, name ASC"
    )
    suspend fun getChildTopics(tree: String, parentId: Int): List<QuranTopicEntity>

    /**
     * Every topic in [tree] that has at least one child.
     *
     * A tree row has to know whether it is a branch or a leaf *before* it is tapped — that is
     * what decides whether it gets a disclosure control — and asking per row would be 2,512
     * queries to draw one list. This is one query per tree over an indexed column, and the
     * answer is a few hundred ids that hold for the session.
     */
    @Query(
        "SELECT DISTINCT parent FROM (" +
            "SELECT thematic_parent_id AS parent FROM quran_topics WHERE :tree = 'thematic' " +
            "UNION ALL " +
            "SELECT ontology_parent_id AS parent FROM quran_topics WHERE :tree = 'ontology' " +
            "UNION ALL " +
            "SELECT parent_id AS parent FROM quran_topics WHERE :tree = 'index'" +
            ") WHERE parent IS NOT NULL"
    )
    suspend fun getBranchTopicIds(tree: String): List<Int>

    @Query(
        "SELECT * FROM quran_topic_ayahs WHERE topic_id = :topicId " +
            "ORDER BY ayah_id ASC"
    )
    suspend fun getTopicAyahs(topicId: Int): List<QuranTopicAyahEntity>

    /**
     * Every topic that cites this verse, busiest first. The index on `ayah_id` is what keeps
     * this off a scan of all 30,687 citations.
     */
    @Query(
        "SELECT t.* FROM quran_topics t " +
            "JOIN quran_topic_ayahs ta ON ta.topic_id = t.topic_id " +
            "WHERE ta.ayah_id = :ayahId ORDER BY t.ayah_count DESC, t.name ASC"
    )
    suspend fun getTopicsForAyah(ayahId: Int): List<QuranTopicEntity>

    /**
     * Every subject this surah's verses are cited under, weightiest *here* first.
     *
     * `verses_here` counts only the citations that fall inside this surah, and it is what the
     * list is ordered by: a reader on Al-Fatiha wants the seven verses' own subjects, not the
     * busiest subjects in the Qur'an that happen to touch it once. Ties fall back to the
     * global count, so between two subjects with one verse here the broader one leads.
     *
     * `surah_number` is not indexed — the table's index is on `ayah_id`, for the reverse
     * question — so this walks 30,687 rows. Once per surah, off the main thread, against a
     * table small enough that an index of its own would cost more in artifact size than it
     * saves here.
     */
    @Query(
        "SELECT t.*, COUNT(ta.ayah_id) AS verses_here FROM quran_topics t " +
            "JOIN quran_topic_ayahs ta ON ta.topic_id = t.topic_id " +
            "WHERE ta.surah_number = :surahNumber " +
            "GROUP BY t.topic_id " +
            "ORDER BY verses_here DESC, t.ayah_count DESC, t.name ASC"
    )
    suspend fun getTopicsForSurah(surahNumber: Int): List<TopicWithSurahCount>

    /**
     * How many subjects this surah touches.
     *
     * Asked by the surah-info screen, which needs the number to label a row and not the rows
     * themselves — loading a few hundred topics to display one integer is the query this
     * exists to avoid.
     */
    @Query(
        "SELECT COUNT(DISTINCT topic_id) FROM quran_topic_ayahs " +
            "WHERE surah_number = :surahNumber"
    )
    suspend fun countTopicsForSurah(surahNumber: Int): Int

    @Query(
        "SELECT * FROM quran_topics WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY ayah_count DESC, name ASC LIMIT :limit"
    )
    suspend fun searchTopicsByName(query: String, limit: Int): List<QuranTopicEntity>

    @Query("SELECT COUNT(*) FROM quran_topics")
    suspend fun countTopics(): Int

}

/**
 * A verse with everything the reader needs, resolved in one query.
 *
 * Since schemaVersion 22 a verse's text is not on its row: `mushaf_ayah_texts` holds one row per
 * (source, ayah). Reading a page therefore means a join rather than a column, and doing it per
 * verse would be 6,236 queries for a surah view. This is that join, once — both scripts, the
 * prostration if there is one, and the quarter the verse falls in.
 *
 * `rub_number` is the interesting one: `Ayah.rubNumber` used to be hard-coded to `0` with the
 * comment "Not available in database", because the quarter a verse belongs to could only be got
 * by scanning. It is a column of `hizb_quarters` now.
 */
data class AyahWithText(
    @Embedded val ayah: AyahEntity,
    @ColumnInfo(name = "text_uthmani") val textUthmani: String?,
    @ColumnInfo(name = "text_simple") val textSimple: String?,
    @ColumnInfo(name = "sajda_kind") val sajdaKind: String?,
    @ColumnInfo(name = "sajda_sequence") val sajdaSequence: Int?,
    @ColumnInfo(name = "rub_number") val rubNumber: Int?,
)

/** Where a verse sits in the mushaf. */
data class AyahLocation(
    val surahId: Int,
    val numberInSurah: Int,
    val juz: Int,
)

/** How many verses a juz holds. */
data class JuzAyahTotal(
    val juzNumber: Int,
    val totalAyahs: Int,
)
