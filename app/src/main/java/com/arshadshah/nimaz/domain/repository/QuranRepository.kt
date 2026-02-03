package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.data.local.database.dao.PageAyahRange
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.model.Translator
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    // Surah operations
    fun getAllSurahs(): Flow<List<Surah>>
    suspend fun getSurahByNumber(surahNumber: Int): Surah?
    fun getSurahsByRevelationType(type: RevelationType): Flow<List<Surah>>
    fun searchSurahs(query: String): Flow<List<Surah>>

    // Ayah operations
    fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>>
    suspend fun getAyahById(ayahId: Int): Ayah?
    fun getAyahsByJuz(juzNumber: Int, translatorId: String? = null): Flow<List<Ayah>>
    fun getAyahsByPage(pageNumber: Int, translatorId: String? = null): Flow<List<Ayah>>
    fun getSajdaAyahs(): Flow<List<Ayah>>
    suspend fun getPageAyahRanges(): List<PageAyahRange>

    // Surah with Ayahs
    fun getSurahWithAyahs(surahNumber: Int, translatorId: String?): Flow<SurahWithAyahs?>

    // Translation operations
    suspend fun getAvailableTranslators(): List<Translator>
    fun getTranslationsForAyahs(ayahIds: List<Int>, translatorId: String): Flow<Map<Int, String>>

    // Search operations
    fun searchQuran(query: String, translatorId: String?): Flow<List<QuranSearchResult>>

    // Bookmark operations
    fun getAllBookmarks(): Flow<List<QuranBookmark>>
    suspend fun getBookmarkByAyahId(ayahId: Int): QuranBookmark?
    fun isAyahBookmarked(ayahId: Int): Flow<Boolean>
    suspend fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int)
    suspend fun addBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int, note: String?, color: String?)
    suspend fun updateBookmark(bookmark: QuranBookmark)
    suspend fun deleteBookmark(ayahId: Int)

    // Favorite operations
    fun getAllFavorites(): Flow<List<QuranFavorite>>
    fun getFavoriteAyahIds(): Flow<List<Int>>
    suspend fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int)

    // Reading progress
    fun getReadingProgress(): Flow<ReadingProgress?>
    suspend fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int)
    suspend fun incrementAyahsRead(count: Int)

    // Surah info
    suspend fun getSurahInfo(surahNumber: Int): SurahInfo?

    // Data initialization
    suspend fun initializeQuranData()
    suspend fun isDataInitialized(): Boolean
}
