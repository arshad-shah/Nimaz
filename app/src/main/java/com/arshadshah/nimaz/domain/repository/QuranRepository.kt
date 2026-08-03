package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PageAyahRange
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
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
    /**
     * The ayahs printed on [pageNumber] of [script]'s edition. Ayah-flow editions (Madani)
     * come from the `ayahs.page` column; line-accurate editions are resolved through their
     * own pagination, so the reader, the page info bar and khatam page marking all act on
     * the ayahs actually on the rendered page (#325).
     */
    fun getAyahsByPage(
        pageNumber: Int,
        translatorId: String? = null,
        script: MushafScript = MushafScript.DEFAULT
    ): Flow<List<Ayah>>

    fun getSajdaAyahs(): Flow<List<Ayah>>

    /** [script]'s page→ayah mapping, ordered by page. Empty when the edition has no data. */
    suspend fun getPageAyahRanges(script: MushafScript = MushafScript.DEFAULT): List<PageAyahRange>

    /**
     * [script]'s line-accurate layout of [page], grouped by printed line. Returns an empty
     * [MushafPageLayout] for an ayah-flow edition or a page with no layout data. Triggers
     * that edition's one-time seeding on first use.
     */
    suspend fun getMushafPageLayout(
        page: Int,
        script: MushafScript = MushafScript.DEFAULT
    ): MushafPageLayout

    // Surah with Ayahs
    fun getSurahWithAyahs(surahNumber: Int, translatorId: String?): Flow<SurahWithAyahs?>

    // Translation operations
    /** Every translation the app ships, in catalogue order. */
    suspend fun getAvailableTranslators(): List<Translator>
    fun getTranslationsForAyahs(ayahIds: List<Int>, translatorId: String): Flow<Map<Int, String>>

    // Search operations
    fun searchQuran(query: String, translatorId: String?): Flow<List<QuranSearchResult>>

    // Bookmark operations
    fun getAllBookmarks(): Flow<List<QuranBookmark>>
    suspend fun getBookmarkByAyahId(ayahId: Int): QuranBookmark?
    fun isAyahBookmarked(ayahId: Int): Flow<Boolean>
    suspend fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int)
    suspend fun addBookmark(
        ayahId: Int,
        surahNumber: Int,
        ayahNumber: Int,
        note: String?,
        color: String?
    )

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

    /**
     * How many rukūʿ each surah is divided into, keyed by surah number.
     *
     * From `surah_structure`, which answers it in one row per surah rather than a scan of
     * `rukus`. Empty on a device whose structure table has not been filled yet, so callers
     * must treat a missing surah as "unknown" and show nothing.
     */
    fun getSurahRukuCounts(): Flow<Map<Int, Int>>

    /**
     * The thematic layer (schemaVersion 24). Every method here answers "nothing" rather than
     * throwing on a device whose artifact predates it — the tables exist from the migration,
     * the rows arrive with the artifact, and the gap between the two is a normal state.
     */
    suspend fun getSurahOverview(surahNumber: Int): SurahOverview?
    suspend fun getThemesForSurah(surahNumber: Int): List<AyahTheme>
    suspend fun getThemeForAyah(surahNumber: Int, ayahNumber: Int): AyahTheme?
    suspend fun getTopicTreeRoots(tree: TopicTree): List<QuranTopic>
    suspend fun getTopicDetail(topicId: Int, tree: TopicTree): TopicDetail?
    suspend fun getTopicsForAyah(ayahId: Int): List<QuranTopic>
    suspend fun searchTopics(query: String, limit: Int = 60): List<QuranTopic>

    /** Whether this install's artifact actually carries the thematic layer at all. */
    suspend fun hasThematicContent(): Boolean

    // Data initialization
    suspend fun initializeQuranData()
    suspend fun isDataInitialized(): Boolean
}
