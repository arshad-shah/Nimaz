package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.database.dao.PageAyahRangeRow
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.AyahWithText
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PageAyahRange
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.domain.model.SearchType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.model.Translator
import com.arshadshah.nimaz.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepositoryImpl @Inject constructor(
    private val quranDao: QuranDao,
    private val bookmarkDao: BookmarkDao,
    private val readingProgressDao: ReadingProgressDao,
    private val searchIndex: ContentSearchIndex
) : QuranRepository {

    /**
     * Normalises a translator id against the catalogue. Every read path that takes a
     * `translatorId` goes through here, so an id that is unknown — a stale preference, say —
     * resolves to [QuranTranslation.DEFAULT] rather than querying for rows that cannot exist.
     * Returns null for a null id so "no translation" stays a cheap no-op.
     *
     * All 15 translations arrive in the content artifact, so there is nothing to seed: this
     * used to double as the lazy seeding hook until `QuranTranslationSeeder` was retired at
     * versionCode 385 (`docs/retirement.yaml`).
     */
    private fun translationId(translatorId: String?): String? {
        if (translatorId == null) return null
        return QuranTranslation.fromId(translatorId).id
    }

    override fun getAllSurahs(): Flow<List<Surah>> {
        return quranDao.getAllSurahs().mapItems { it.toDomain() }
    }

    override suspend fun getSurahByNumber(surahNumber: Int): Surah? {
        return quranDao.getSurahByNumber(surahNumber)?.toDomain()
    }

    override fun getSurahsByRevelationType(type: RevelationType): Flow<List<Surah>> {
        val typeString = when (type) {
            RevelationType.MECCAN -> "meccan"
            RevelationType.MEDINAN -> "medinan"
        }
        return quranDao.getSurahsByRevelationType(typeString).mapItems { it.toDomain() }
    }

    override fun searchSurahs(query: String): Flow<List<Surah>> {
        // The index carries the Arabic name too, which the `LIKE` below never could reach:
        // سورة الفاتحة is stored with its marks and nobody types them.
        return flow {
            if (searchIndex.isAvailable()) {
                val numbers = searchIndex.refs(query, SearchKind.SURAH)
                    .mapNotNull(String::toIntOrNull)
                    .distinct()
                emit(quranDao.getSurahsByNumbers(numbers).map { it.toDomain() })
            } else {
                emitAll(quranDao.searchSurahs(query).mapItems { it.toDomain() })
            }
        }
    }

    override fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>> {
        // Get surah by number first to get the id
        return quranDao.getAllSurahs().map { surahs ->
            val surah = surahs.find { it.number == surahNumber }
            surah?.id
        }.combine(quranDao.getAllSurahs()) { surahId, _ ->
            surahId
        }.map { surahId ->
            if (surahId != null) {
                quranDao.getAyahsWithTextBySurah(surahId).first().map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getAyahById(ayahId: Int): Ayah? {
        return quranDao.getAyahWithTextById(ayahId)?.toDomain()
    }

    override fun getAyahsByJuz(juzNumber: Int, translatorId: String?): Flow<List<Ayah>> {
        return quranDao.getAyahsWithTextByJuz(juzNumber).map { entities ->
            // Fetch translations if translatorId is provided
            val translation = translationId(translatorId)
            val translationMap = if (translation != null && entities.isNotEmpty()) {
                val ayahIds = entities.map { it.ayah.id }
                quranDao.getTranslationsForAyahs(ayahIds, translation)
                    .first()
                    .associate { it.ayahId to it.text }
            } else {
                emptyMap()
            }
            // Fetch bookmark IDs to set isBookmarked correctly
            val bookmarkedIds = bookmarkDao.bookmarkedIds(BookmarkKind.AYAH).toSet()
            entities.map { entity ->
                entity.toDomain(translationMap[entity.ayah.id]).copy(
                    isBookmarked = entity.ayah.id in bookmarkedIds
                )
            }
        }
    }

    override fun getAyahsByPage(
        pageNumber: Int,
        translatorId: String?,
        script: MushafScript
    ): Flow<List<Ayah>> {
        val entityFlow = if (!script.isLineAccurate) {
            quranDao.getAyahsWithTextByPage(pageNumber)
        } else {
            // A line-accurate edition does not paginate by `ayahs.page`, so resolve the
            // page's span through its own layout table and fetch that id range (#325).
            // Emits an empty page rather than the unrelated Madani page when the span is
            // unknown.
            flow {
                val range = layoutRanges(script).firstOrNull { it.page == pageNumber }
                if (range == null) {
                    emit(emptyList())
                } else {
                    emitAll(quranDao.getAyahsWithTextByRange(range.minAyahId, range.maxAyahId))
                }
            }
        }
        return entityFlow.map { entities ->
            // Fetch translations if translatorId is provided
            val translation = translationId(translatorId)
            val translationMap = if (translation != null && entities.isNotEmpty()) {
                val ayahIds = entities.map { it.ayah.id }
                quranDao.getTranslationsForAyahs(ayahIds, translation)
                    .first()
                    .associate { it.ayahId to it.text }
            } else {
                emptyMap()
            }
            // Fetch bookmark IDs to set isBookmarked correctly
            val bookmarkedIds = bookmarkDao.bookmarkedIds(BookmarkKind.AYAH).toSet()
            entities.map { entity ->
                entity.toDomain(translationMap[entity.ayah.id]).copy(
                    isBookmarked = entity.ayah.id in bookmarkedIds
                )
            }
        }
    }

    override fun getSajdaAyahs(): Flow<List<Ayah>> {
        return quranDao.getSajdaAyahsWithText().mapItems { it.toDomain() }
    }

    override suspend fun getPageAyahRanges(script: MushafScript): List<PageAyahRange> =
        if (script.isLineAccurate) {
            layoutRanges(script)
        } else {
            quranDao.getPageAyahRanges().map { it.toDomain() }
        }

    /**
     * A line-accurate edition's page→ayah mapping, memoised per edition — each is several
     * hundred immutable rows that both the Page tab and every page fetch consult.
     */
    private suspend fun layoutRanges(script: MushafScript): List<PageAyahRange> {
        cachedLayoutRanges[script]?.let { return it }
        return layoutRangesMutex.withLock {
            cachedLayoutRanges[script]?.let { return@withLock it }
            quranDao.getLayoutPageAyahRanges(script.name)
                .map { it.toDomain() }
                .also { cachedLayoutRanges[script] = it }
        }
    }

    private val cachedLayoutRanges = ConcurrentHashMap<MushafScript, List<PageAyahRange>>()
    private val layoutRangesMutex = Mutex()

    override suspend fun getMushafPageLayout(page: Int, script: MushafScript): MushafPageLayout {
        val textSource = script.textSource ?: return MushafPageLayout(page, emptyList())
        return MushafLayoutMapper.toPageLayout(
            page,
            quranDao.getMushafLayoutByPage(script.name, textSource, page)
        )
    }

    override fun getSurahWithAyahs(surahNumber: Int, translatorId: String?): Flow<SurahWithAyahs?> {
        val surahFlow = quranDao.getAllSurahs().map { surahs ->
            surahs.find { it.number == surahNumber }?.toDomain()
        }

        return surahFlow.map { surah ->
            if (surah != null) {
                val surahEntity = quranDao.getSurahByNumber(surahNumber)
                val ayahEntities = surahEntity?.let {
                    quranDao.getAyahsWithTextBySurah(it.id).first()
                } ?: emptyList()

                // Fetch translations if translatorId is provided
                val translation = translationId(translatorId)
                val translationMap = if (translation != null && ayahEntities.isNotEmpty()) {
                    val ayahIds = ayahEntities.map { it.ayah.id }
                    quranDao.getTranslationsForAyahs(ayahIds, translation)
                        .first()
                        .associate { it.ayahId to it.text }
                } else {
                    emptyMap()
                }

                // Fetch bookmark IDs to set isBookmarked correctly
                val bookmarkedIds = bookmarkDao.bookmarkedIds(BookmarkKind.AYAH).toSet()

                // Map ayahs with translations and bookmark status
                val ayahs = ayahEntities.map { ayah ->
                    ayah.toDomain(translationMap[ayah.ayah.id]).copy(
                        isBookmarked = ayah.ayah.id in bookmarkedIds
                    )
                }

                SurahWithAyahs(surah = surah, ayahs = ayahs)
            } else {
                null
            }
        }
    }

    /**
     * Driven by the [QuranTranslation] catalogue rather than by which translations happen to
     * be in the DB: translations are seeded lazily, so a DISTINCT over `translations` would
     * only ever list the ones already opened — and it had no display name or language to
     * report either.
     */
    override suspend fun getAvailableTranslators(): List<Translator> =
        QuranTranslation.entries.map { translation ->
            Translator(
                id = translation.id,
                name = translation.translator,
                languageCode = translation.language.code
            )
        }

    override fun getTranslationsForAyahs(
        ayahIds: List<Int>,
        translatorId: String
    ): Flow<Map<Int, String>> = flow {
        val translation = translationId(translatorId) ?: return@flow
        emitAll(
            quranDao.getTranslationsForAyahs(ayahIds, translation).map { translations ->
                translations.associate { it.ayahId to it.text }
            }
        )
    }

    override fun searchQuran(query: String, translatorId: String?): Flow<List<QuranSearchResult>> {
        return flow {
            // Get all surahs for name lookup
            val surahs = quranDao.getAllSurahs().first()
            val surahMap = surahs.associate { it.id to it.nameEnglish }

            if (searchIndex.isAvailable()) {
                emit(searchQuranByIndex(query, translatorId, surahMap))
                return@flow
            }

            // Search Arabic text
            val arabicResults = quranDao.searchAyahsWithText(query).first().map { ayah ->
                QuranSearchResult(
                    ayah = ayah.toDomain(),
                    surahName = surahMap[ayah.ayah.surahId] ?: "Surah ${ayah.ayah.surahId}",
                    matchedText = ayah.textUthmani.orEmpty(),
                    searchType = SearchType.ARABIC
                )
            }

            // If translatorId provided, also search translations
            val translatorKey = translationId(translatorId)
            val translationResults = if (translatorKey != null) {
                quranDao.searchTranslations(query, translatorKey).first().mapNotNull { translation ->
                    quranDao.getAyahWithTextById(translation.ayahId)?.let { ayah ->
                        QuranSearchResult(
                            ayah = ayah.toDomain(),
                            surahName = surahMap[ayah.ayah.surahId] ?: "Surah ${ayah.ayah.surahId}",
                            matchedText = translation.text,
                            searchType = SearchType.TRANSLATION
                        )
                    }
                }
            } else emptyList()

            emit((arabicResults + translationResults).distinctBy { it.ayah.id })
        }
    }

    /**
     * The same search, through the index the artifact ships (#330).
     *
     * The `LIKE` path above it is not dead code and not a fallback in the apologetic
     * sense: `createFromAsset` copies the artifact once, on first install, so a phone
     * that installed before the index shipped does not have one and never will without
     * a reinstall. For those installs this is the search they already had. For everyone
     * else, Arabic works for the first time — and both paths return the same shape, so
     * nothing above the repository knows which one ran.
     */
    private suspend fun searchQuranByIndex(
        query: String,
        translatorId: String?,
        surahMap: Map<Int, String>,
    ): List<QuranSearchResult> {
        val arabicIds = searchIndex.refs(query, SearchKind.QURAN).mapNotNull(String::toIntOrNull)
        val arabicResults = quranDao.getAyahsWithTextByIds(arabicIds).map { ayah ->
            QuranSearchResult(
                ayah = ayah.toDomain(),
                surahName = surahMap[ayah.ayah.surahId] ?: "Surah ${ayah.ayah.surahId}",
                matchedText = ayah.textUthmani.orEmpty(),
                searchType = SearchType.ARABIC
            )
        }

        val translatorKey = translationId(translatorId)
        val translationResults = if (translatorKey == null) emptyList() else {
            // Narrowed by `source`, so a hit in the Bengali translation cannot surface
            // for a reader who has Sahih International selected. All fifteen are indexed.
            val ids = searchIndex
                .refs(query, SearchKind.TRANSLATION, source = translatorKey)
                .mapNotNull(String::toIntOrNull)
            val ayahs = quranDao.getAyahsWithTextByIds(ids).associateBy { it.ayah.id }
            quranDao.getTranslationsByAyahIds(ids, translatorKey).mapNotNull { translation ->
                ayahs[translation.ayahId]?.let { ayah ->
                    QuranSearchResult(
                        ayah = ayah.toDomain(),
                        surahName = surahMap[ayah.ayah.surahId] ?: "Surah ${ayah.ayah.surahId}",
                        matchedText = translation.text,
                        searchType = SearchType.TRANSLATION
                    )
                }
            }
        }

        return (arabicResults + translationResults).distinctBy { it.ayah.id }
    }

    // Bookmarks, favourites and the reading position are the user's, and come from the user's
    // database. A verse can be bookmarked *and* favourited: that was two tables and is now two
    // flags on one row, which is why toggling one must not disturb the other.

    override fun getAllBookmarks(): Flow<List<QuranBookmark>> {
        return bookmarkDao.bookmarks(BookmarkKind.AYAH).mapItems { it.toQuranBookmark() }
    }

    override suspend fun getBookmarkByAyahId(ayahId: Int): QuranBookmark? {
        return bookmarkDao.find(BookmarkKind.AYAH, ayahId)
            ?.takeIf { it.bookmarked }
            ?.toQuranBookmark()
    }

    override fun isAyahBookmarked(ayahId: Int): Flow<Boolean> {
        return bookmarkDao.observeIsBookmarked(BookmarkKind.AYAH, ayahId)
    }

    override suspend fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        val existing = bookmarkDao.find(BookmarkKind.AYAH, ayahId)
        val now = System.currentTimeMillis()
        when {
            existing == null -> bookmarkDao.upsert(
                mark(ayahId, surahNumber, ayahNumber, bookmarked = true, favourite = false, now = now)
            )
            // Clearing the flag rather than the row, so a favourite on the same verse survives.
            existing.bookmarked && existing.favourite ->
                bookmarkDao.clearBookmark(BookmarkKind.AYAH, ayahId, now)
            existing.bookmarked -> bookmarkDao.delete(BookmarkKind.AYAH, ayahId)
            else -> bookmarkDao.upsert(existing.copy(bookmarked = true, updatedAt = now))
        }
    }

    override suspend fun addBookmark(
        ayahId: Int,
        surahNumber: Int,
        ayahNumber: Int,
        note: String?,
        color: String?
    ) {
        val existing = bookmarkDao.find(BookmarkKind.AYAH, ayahId)
        val now = System.currentTimeMillis()
        bookmarkDao.upsert(
            mark(
                ayahId, surahNumber, ayahNumber,
                bookmarked = true,
                favourite = existing?.favourite ?: false,
                note = note,
                colour = color,
                createdAt = existing?.createdAt ?: now,
                now = now,
            )
        )
    }

    override suspend fun updateBookmark(bookmark: QuranBookmark) {
        val existing = bookmarkDao.find(BookmarkKind.AYAH, bookmark.ayahId)
        bookmarkDao.upsert(
            mark(
                bookmark.ayahId, bookmark.surahNumber, bookmark.ayahNumber,
                bookmarked = true,
                favourite = existing?.favourite ?: false,
                note = bookmark.note,
                colour = bookmark.color,
                createdAt = bookmark.createdAt,
                now = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun deleteBookmark(ayahId: Int) {
        val existing = bookmarkDao.find(BookmarkKind.AYAH, ayahId) ?: return
        if (existing.favourite) {
            bookmarkDao.clearBookmark(BookmarkKind.AYAH, ayahId, System.currentTimeMillis())
        } else {
            bookmarkDao.delete(BookmarkKind.AYAH, ayahId)
        }
    }

    override fun getAllFavorites(): Flow<List<QuranFavorite>> {
        return bookmarkDao.favourites(BookmarkKind.AYAH).mapItems { it.toQuranFavorite() }
    }

    override fun getFavoriteAyahIds(): Flow<List<Int>> {
        return bookmarkDao.favourites(BookmarkKind.AYAH).map { rows -> rows.map { it.targetId } }
    }

    override suspend fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        val existing = bookmarkDao.find(BookmarkKind.AYAH, ayahId)
        val now = System.currentTimeMillis()
        when {
            existing == null -> bookmarkDao.upsert(
                mark(ayahId, surahNumber, ayahNumber, bookmarked = false, favourite = true, now = now)
            )
            existing.favourite && existing.bookmarked ->
                bookmarkDao.clearFavourite(BookmarkKind.AYAH, ayahId, now)
            existing.favourite -> bookmarkDao.delete(BookmarkKind.AYAH, ayahId)
            else -> bookmarkDao.upsert(existing.copy(favourite = true, updatedAt = now))
        }
    }

    override fun getReadingProgress(): Flow<ReadingProgress?> {
        return readingProgressDao.observe().map { entity -> entity?.toDomain() }
    }

    override suspend fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int) {
        val existing = readingProgressDao.get()
        readingProgressDao.upsert(
            ReadingProgressEntity(
                id = 1,
                lastReadSurah = surah,
                lastReadAyah = ayah,
                lastReadPage = page,
                lastReadJuz = juz,
                totalAyahsRead = existing?.totalAyahsRead ?: 0,
                currentKhatmaCount = existing?.currentKhatmaCount ?: 0,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun incrementAyahsRead(count: Int) {
        val existing = readingProgressDao.get() ?: ReadingProgressEntity(
            id = 1,
            lastReadSurah = 1,
            lastReadAyah = 1,
            lastReadPage = 1,
            lastReadJuz = 1,
            totalAyahsRead = 0,
            currentKhatmaCount = 0,
        )
        readingProgressDao.upsert(
            existing.copy(
                totalAyahsRead = existing.totalAyahsRead + count,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun getSurahInfo(surahNumber: Int): SurahInfo? {
        return quranDao.getSurahInfo(surahNumber)?.let { entity ->
            SurahInfo(
                description = entity.description,
                themes = entity.themes.split(",").map { it.trim() }
            )
        }
    }

    override suspend fun initializeQuranData() {
        // Data is pre-populated in the database
    }

    override suspend fun isDataInitialized(): Boolean {
        return quranDao.getAllSurahs().first().isNotEmpty()
    }

    // Extension functions for mapping
    private fun SurahEntity.toDomain(): Surah {
        return Surah(
            number = number,
            nameArabic = nameArabic,
            nameEnglish = nameEnglish,
            nameTransliteration = nameTransliteration,
            revelationType = RevelationType.fromString(revelationType),
            ayahCount = versesCount,
            juzStart = 1,
            orderInMushaf = orderRevealed,
            startPage = startPage
        )
    }

    /**
     * A verse row plus the text and structure resolved beside it (schemaVersion 22).
     *
     * `textArabic` is the Uthmani rendering — the one that carries the mushaf pause marks — and
     * `textSimple` is now a genuinely different string rather than the same bytes under a second
     * name. Before this, `ayahs.text_arabic` and `.text_uthmani` were byte-identical in all 6,236
     * rows, so a reader who chose the plain script got the Uthmani one and could not tell.
     *
     * `rubNumber` was hard-coded to `0` with the comment "Not available in database". It is the
     * quarter from `hizb_quarters` now.
     */
    private fun AyahWithText.toDomain(translation: String? = null): Ayah {
        return Ayah(
            id = ayah.id,
            surahNumber = ayah.surahId,
            ayahNumber = ayah.numberInSurah,
            textArabic = textUthmani.orEmpty(),
            textSimple = textSimple ?: textUthmani.orEmpty(),
            juzNumber = ayah.juz,
            hizbNumber = ayah.hizb,
            rubNumber = rubNumber ?: 0,
            pageNumber = ayah.page,
            sajdaType = SajdaType.fromString(sajdaKind),
            sajdaNumber = sajdaSequence,
            translation = translation,
            transliteration = ayah.transliteration,
            textTajweed = ayah.textTajweed
        )
    }

    private fun QuranBookmarkEntity.toDomain(): QuranBookmark {
        return QuranBookmark(
            id = id,
            ayahId = ayahId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            note = note,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun QuranBookmark.toEntity(): QuranBookmarkEntity {
        return QuranBookmarkEntity(
            id = id,
            ayahId = ayahId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            note = note,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun QuranFavoriteEntity.toDomain(): QuranFavorite {
        return QuranFavorite(
            ayahId = ayahId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            createdAt = createdAt
        )
    }

    private fun ReadingProgressEntity.toDomain(): ReadingProgress {
        return ReadingProgress(
            lastReadSurah = lastReadSurah,
            lastReadAyah = lastReadAyah,
            lastReadPage = lastReadPage,
            lastReadJuz = lastReadJuz,
            totalAyahsRead = totalAyahsRead,
            currentKhatmaCount = currentKhatmaCount,
            updatedAt = updatedAt
        )
    }

    private fun PageAyahRangeRow.toDomain(): PageAyahRange {
        return PageAyahRange(
            page = page,
            minAyahId = minAyahId,
            maxAyahId = maxAyahId,
            ayahCount = ayahCount
        )
    }

    /** One consolidated row, built for a verse. */
    private fun mark(
        ayahId: Int,
        surahNumber: Int,
        ayahNumber: Int,
        bookmarked: Boolean,
        favourite: Boolean,
        note: String? = null,
        colour: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        now: Long,
    ) = BookmarkEntity(
        kind = BookmarkKind.AYAH,
        targetId = ayahId,
        bookmarked = bookmarked,
        favourite = favourite,
        note = note,
        colour = colour,
        contextId = surahNumber,
        ordinal = ayahNumber,
        createdAt = createdAt,
        updatedAt = now,
    )

    private fun BookmarkEntity.toQuranBookmark() = QuranBookmark(
        id = 0,
        ayahId = targetId,
        surahNumber = contextId ?: 0,
        ayahNumber = ordinal ?: 0,
        note = note,
        color = colour,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BookmarkEntity.toQuranFavorite() = QuranFavorite(
        ayahId = targetId,
        surahNumber = contextId ?: 0,
        ayahNumber = ordinal ?: 0,
        createdAt = createdAt,
    )
}
