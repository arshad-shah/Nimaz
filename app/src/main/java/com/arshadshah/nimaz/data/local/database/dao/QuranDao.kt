package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafAyahTextEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutLineEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahInfoEntity
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

    @Query("SELECT * FROM ayahs WHERE sajda_type IS NOT NULL ORDER BY id ASC")
    fun getSajdaAyahs(): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE text_uthmani LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%'")
    fun searchAyahs(query: String): Flow<List<AyahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    // Line-accurate Mushaf layouts. Every edition's glyph text and layout ships as bundled
    // JSON assets and is seeded at runtime by MushafLayoutSeeder — the prepackaged DB is not
    // regenerated. These methods cover seeding + idempotency; the per-page read query for the
    // renderer (getMushafLayoutByPage) is defined further below.
    @Query("SELECT COUNT(*) FROM mushaf_layout_lines WHERE script = :script")
    suspend fun countLayoutLines(script: String): Int

    @Query("SELECT COUNT(*) FROM mushaf_ayah_texts WHERE text_source = :textSource")
    suspend fun countAyahTexts(textSource: String): Int

    @Query("DELETE FROM mushaf_layout_lines WHERE script = :script")
    suspend fun deleteLayoutLines(script: String)

    @Query("DELETE FROM mushaf_ayah_texts WHERE text_source = :textSource")
    suspend fun deleteAyahTexts(textSource: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayoutLines(rows: List<MushafLayoutLineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahTexts(rows: List<MushafAyahTextEntity>)

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

    /**
     * Atomically (re)seed one edition: replace that script's layout segments and its text
     * source's glyphs. Both writes are scoped by key, so seeding one edition never disturbs
     * another — and editions that share a text source simply rewrite identical rows.
     * Idempotent, so re-running after a version bump is safe.
     */
    @Transaction
    suspend fun replaceMushafLayout(
        script: String,
        textSource: String,
        texts: List<MushafAyahTextEntity>,
        layout: List<MushafLayoutLineEntity>
    ) {
        deleteLayoutLines(script)
        deleteAyahTexts(textSource)
        insertAyahTexts(texts)
        insertLayoutLines(layout)
    }

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

    // Per-translation seeding (QuranTranslationSeeder). Translations ship as bundled JSON
    // assets and are seeded lazily, one translator at a time, the first time that
    // translation is selected — the prepackaged DB only ever carried one of them.
    @Query("SELECT COUNT(*) FROM translations WHERE translator_id = :translatorId")
    suspend fun countTranslationsFor(translatorId: String): Int

    @Query("DELETE FROM translations WHERE translator_id = :translatorId")
    suspend fun deleteTranslationsFor(translatorId: String)

    /**
     * Atomically replace exactly one translator's verses. Scoped to [translatorId], so
     * seeding or re-seeding one translation never touches another — or any user data.
     *
     * The delete is what keeps this idempotent: `translations.id` is auto-generated, so
     * re-inserting without it would append a second copy of all 6,236 rows rather than
     * overwrite them.
     */
    @Transaction
    suspend fun replaceTranslation(translatorId: String, rows: List<TranslationEntity>) {
        deleteTranslationsFor(translatorId)
        insertTranslations(rows)
    }

    @Query("SELECT * FROM translations WHERE text LIKE '%' || :query || '%' AND translator_id = :translatorId")
    fun searchTranslations(query: String, translatorId: String): Flow<List<TranslationEntity>>

    // Bookmark operations
    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<QuranBookmarkEntity>>

    @Query("SELECT ayahId FROM quran_bookmarks")
    suspend fun getAllBookmarkIds(): List<Int>

    @Query("SELECT * FROM quran_bookmarks WHERE ayahId = :ayahId LIMIT 1")
    suspend fun getBookmarkByAyahId(ayahId: Int): QuranBookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM quran_bookmarks WHERE ayahId = :ayahId)")
    fun isAyahBookmarked(ayahId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: QuranBookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE ayahId = :ayahId")
    suspend fun deleteBookmarkByAyahId(ayahId: Int)

    @Update
    suspend fun updateBookmark(bookmark: QuranBookmarkEntity)

    // Reading progress operations
    @Query("SELECT * FROM reading_progress WHERE id = 1")
    fun getReadingProgress(): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingProgress(progress: ReadingProgressEntity)

    @Query("UPDATE reading_progress SET lastReadSurah = :surah, lastReadAyah = :ayah, lastReadPage = :page, lastReadJuz = :juz, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateReadingPosition(
        surah: Int,
        ayah: Int,
        page: Int,
        juz: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE reading_progress SET totalAyahsRead = totalAyahsRead + :count, updatedAt = :timestamp WHERE id = 1")
    suspend fun incrementAyahsRead(count: Int, timestamp: Long = System.currentTimeMillis())

    // Favorite operations
    @Query("SELECT * FROM quran_favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<QuranFavoriteEntity>>

    @Query("SELECT ayahId FROM quran_favorites")
    fun getFavoriteAyahIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM quran_favorites WHERE ayahId = :ayahId)")
    fun isAyahFavorite(ayahId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: QuranFavoriteEntity)

    @Query("DELETE FROM quran_favorites WHERE ayahId = :ayahId")
    suspend fun deleteFavorite(ayahId: Int)

    @Transaction
    suspend fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        val exists = getFavoriteByAyahId(ayahId)
        if (exists != null) {
            deleteFavorite(ayahId)
        } else {
            insertFavorite(
                QuranFavoriteEntity(
                    ayahId = ayahId,
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber
                )
            )
        }
    }

    @Query("SELECT * FROM quran_favorites WHERE ayahId = :ayahId LIMIT 1")
    suspend fun getFavoriteByAyahId(ayahId: Int): QuranFavoriteEntity?

    // Surah info operations
    @Query("SELECT * FROM surah_info WHERE surahNumber = :surahNumber")
    suspend fun getSurahInfo(surahNumber: Int): SurahInfoEntity?

    @Transaction
    suspend fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        val existing = getBookmarkByAyahId(ayahId)
        if (existing != null) {
            deleteBookmarkByAyahId(ayahId)
        } else {
            insertBookmark(
                QuranBookmarkEntity(
                    ayahId = ayahId,
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    note = null,
                    color = null
                )
            )
        }
    }

    @Transaction
    suspend fun deleteAllUserData() {
        deleteAllBookmarks()
        deleteAllFavorites()
        deleteAllReadingProgress()
    }

    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarksSync(): List<QuranBookmarkEntity>

    @Query("SELECT * FROM quran_favorites ORDER BY createdAt DESC")
    suspend fun getAllFavoritesSync(): List<QuranFavoriteEntity>

    @Query("SELECT * FROM reading_progress WHERE id = 1")
    suspend fun getReadingProgressSync(): ReadingProgressEntity?

    @Query("DELETE FROM quran_bookmarks")
    suspend fun deleteAllBookmarks()

    @Query("DELETE FROM quran_favorites")
    suspend fun deleteAllFavorites()

    @Query("DELETE FROM reading_progress")
    suspend fun deleteAllReadingProgress()
}
