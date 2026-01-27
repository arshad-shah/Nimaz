package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM ayahs WHERE sajda_type IS NOT NULL ORDER BY id ASC")
    fun getSajdaAyahs(): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE text_uthmani LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%'")
    fun searchAyahs(query: String): Flow<List<AyahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    // Translation operations
    @Query("SELECT * FROM translations WHERE ayah_id = :ayahId AND translator_id = :translatorId")
    suspend fun getTranslation(ayahId: Int, translatorId: String): TranslationEntity?

    @Query("SELECT * FROM translations WHERE ayah_id IN (:ayahIds) AND translator_id = :translatorId")
    fun getTranslationsForAyahs(ayahIds: List<Int>, translatorId: String): Flow<List<TranslationEntity>>

    @Query("SELECT DISTINCT translator_id FROM translations")
    suspend fun getAvailableTranslatorIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<TranslationEntity>)

    @Query("SELECT * FROM translations WHERE text LIKE '%' || :query || '%' AND translator_id = :translatorId")
    fun searchTranslations(query: String, translatorId: String): Flow<List<TranslationEntity>>

    // Bookmark operations
    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<QuranBookmarkEntity>>

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
    suspend fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE reading_progress SET totalAyahsRead = totalAyahsRead + :count, updatedAt = :timestamp WHERE id = 1")
    suspend fun incrementAyahsRead(count: Int, timestamp: Long = System.currentTimeMillis())

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
}
