package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.database.dao.PageAyahRangeRow
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.quran.QuranLayoutSeeder
import com.arshadshah.nimaz.data.local.quran.QuranTranslationSeeder
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition
import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.arshadshah.nimaz.domain.model.PageAyahRange
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
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
    private val layoutSeeder: QuranLayoutSeeder,
    private val translationSeeder: QuranTranslationSeeder
) : QuranRepository {

    /**
     * Fetches one translation's texts for [ayahIds], seeding that edition on first use.
     *
     * Every read of translated ayahs goes through here, so the seeding trigger lives in one
     * place rather than at each of the three call sites. Seeding is idempotent, version-gated
     * and short-circuited per edition after the first confirmation, so the ~6.2k-row write
     * runs once and later reads cost nothing extra. A translation with no bundled asset
     * (`sahih_international`, which ships in the prepopulated DB) is a no-op.
     */
    private suspend fun translationsFor(
        ayahIds: List<Int>,
        translatorId: String
    ): Map<Int, String> {
        translationSeeder.seedIfNeeded(QuranEditions.translation(translatorId))
        return quranDao.getTranslationsForAyahs(ayahIds, translatorId)
            .first()
            .associate { it.ayahId to it.text }
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
        return quranDao.searchSurahs(query).mapItems { it.toDomain() }
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
                quranDao.getAyahsBySurah(surahId).first().map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getAyahById(ayahId: Int): Ayah? {
        return quranDao.getAyahById(ayahId)?.toDomain()
    }

    override fun getAyahsByJuz(juzNumber: Int, translatorId: String?): Flow<List<Ayah>> {
        return quranDao.getAyahsByJuz(juzNumber).map { entities ->
            // Fetch translations if translatorId is provided
            val translationMap = if (translatorId != null && entities.isNotEmpty()) {
                translationsFor(entities.map { it.id }, translatorId)
            } else {
                emptyMap()
            }
            // Fetch bookmark IDs to set isBookmarked correctly
            val bookmarkedIds = quranDao.getAllBookmarkIds().toSet()
            entities.map { entity ->
                entity.toDomain(translationMap[entity.id]).copy(
                    isBookmarked = entity.id in bookmarkedIds
                )
            }
        }
    }

    override fun getAyahsByPage(
        pageNumber: Int,
        translatorId: String?,
        script: MushafLayoutEdition
    ): Flow<List<Ayah>> {
        val entityFlow = if (!script.hasLineLayout) {
            quranDao.getAyahsByPage(pageNumber)
        } else {
            // A line-accurate edition does not paginate by `ayahs.page`, so resolve the
            // page's span through its own layout rows and fetch that id range (#325). Emits
            // an empty page rather than the unrelated Madani page when the span is unknown.
            flow {
                val range = layoutRanges(script).firstOrNull { it.page == pageNumber }
                if (range == null) {
                    emit(emptyList())
                } else {
                    emitAll(quranDao.getAyahsByIdRange(range.minAyahId, range.maxAyahId))
                }
            }
        }
        return entityFlow.map { entities ->
            // Fetch translations if translatorId is provided
            val translationMap = if (translatorId != null && entities.isNotEmpty()) {
                translationsFor(entities.map { it.id }, translatorId)
            } else {
                emptyMap()
            }
            // Fetch bookmark IDs to set isBookmarked correctly
            val bookmarkedIds = quranDao.getAllBookmarkIds().toSet()
            entities.map { entity ->
                entity.toDomain(translationMap[entity.id]).copy(
                    isBookmarked = entity.id in bookmarkedIds
                )
            }
        }
    }

    override fun getSajdaAyahs(): Flow<List<Ayah>> {
        return quranDao.getSajdaAyahs().mapItems { it.toDomain() }
    }

    override suspend fun getPageAyahRanges(script: MushafLayoutEdition): List<PageAyahRange> =
        if (script.hasLineLayout) {
            layoutRanges(script)
        } else {
            quranDao.getPageAyahRanges().map { it.toDomain() }
        }

    /**
     * A line-accurate edition's page→ayah mapping, seeded on demand (an edition's layout rows
     * are only written once that edition is actually read) and memoised per edition — they
     * are a few hundred immutable rows that both the Page tab and every page fetch consult.
     */
    private suspend fun layoutRanges(edition: MushafLayoutEdition): List<PageAyahRange> {
        cachedLayoutRanges[edition.id]?.let { return it }
        return layoutRangesMutex.withLock {
            cachedLayoutRanges[edition.id]?.let { return@withLock it }
            layoutSeeder.seedIfNeeded(edition)
            quranDao.getLayoutPageAyahRanges(edition.id)
                .map { it.toDomain() }
                .also { cachedLayoutRanges[edition.id] = it }
        }
    }

    // Keyed by edition id so switching layouts does not serve the previous edition's pages.
    private val cachedLayoutRanges = ConcurrentHashMap<String, List<PageAyahRange>>()
    private val layoutRangesMutex = Mutex()

    override suspend fun getMushafPageLayout(
        page: Int,
        layout: MushafLayoutEdition
    ): MushafPageLayout {
        // First use of a line-accurate edition seeds its layout (and, where it needs one,
        // its ayah text) — idempotent and version-gated, so the ~20k-row seed never runs on
        // a normal Quran open. A flowed edition has no layout rows and returns empty.
        if (!layout.hasLineLayout) return MushafPageLayout(page = page, lines = emptyList())
        layoutSeeder.seedIfNeeded(layout)
        return MushafLayoutMapper.toPageLayout(
            page = page,
            rows = quranDao.getMushafLayoutByPage(layout.id, page),
            // The stored word positions index into exactly this column.
            textSource = layout.textSource
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
                    quranDao.getAyahsBySurah(it.id).first()
                } ?: emptyList()

                // Fetch translations if translatorId is provided
                val translationMap = if (translatorId != null && ayahEntities.isNotEmpty()) {
                    translationsFor(ayahEntities.map { it.id }, translatorId)
                } else {
                    emptyMap()
                }

                // Fetch bookmark IDs to set isBookmarked correctly
                val bookmarkedIds = quranDao.getAllBookmarkIds().toSet()

                // Map ayahs with translations and bookmark status
                val ayahs = ayahEntities.map { ayah ->
                    ayah.toDomain(translationMap[ayah.id]).copy(
                        isBookmarked = ayah.id in bookmarkedIds
                    )
                }

                SurahWithAyahs(surah = surah, ayahs = ayahs)
            } else {
                null
            }
        }
    }

    /**
     * The translations actually present in the database, named from the content registry.
     *
     * The DB only knows translator *ids*, so this used to return `name = translatorId` and a
     * language code guessed by splitting that id on `.` — which produced "sahih_international"
     * as a display name and "sahih_international" as a language. The catalogue carries the
     * real metadata; an id with no catalogue entry (seeded by an older build, since removed)
     * still surfaces under its own id rather than disappearing.
     */
    override suspend fun getAvailableTranslators(): List<Translator> {
        return quranDao.getAvailableTranslatorIds().map { translatorId ->
            val edition = QuranEditions.translations.firstOrNull { it.id == translatorId }
            Translator(
                id = translatorId,
                name = edition?.displayName ?: translatorId,
                languageCode = edition?.languageTag ?: translatorId.substringBefore(".")
            )
        }
    }

    override fun getTranslationsForAyahs(
        ayahIds: List<Int>,
        translatorId: String
    ): Flow<Map<Int, String>> = flow {
        // Seed before collecting: an edition whose rows have not been written yet would
        // otherwise emit an empty map, which reads as "this ayah has no translation".
        translationSeeder.seedIfNeeded(QuranEditions.translation(translatorId))
        emitAll(
            quranDao.getTranslationsForAyahs(ayahIds, translatorId).map { translations ->
                translations.associate { it.ayahId to it.text }
            }
        )
    }

    override fun searchQuran(query: String, translatorId: String?): Flow<List<QuranSearchResult>> {
        return flow {
            // Get all surahs for name lookup
            val surahs = quranDao.getAllSurahs().first()
            val surahMap = surahs.associate { it.id to it.nameEnglish }

            // Search Arabic text
            val arabicResults = quranDao.searchAyahs(query).first().map { ayah ->
                QuranSearchResult(
                    ayah = ayah.toDomain(),
                    surahName = surahMap[ayah.surahId] ?: "Surah ${ayah.surahId}",
                    matchedText = ayah.textArabic,
                    searchType = SearchType.ARABIC
                )
            }

            // If translatorId provided, also search translations. Seed first — searching an
            // edition that has not been written yet would quietly return zero hits rather
            // than an error, which looks like "the Quran does not contain that phrase".
            val translationResults = if (translatorId != null) {
                translationSeeder.seedIfNeeded(QuranEditions.translation(translatorId))
                quranDao.searchTranslations(query, translatorId).first().mapNotNull { translation ->
                    quranDao.getAyahById(translation.ayahId)?.let { ayah ->
                        QuranSearchResult(
                            ayah = ayah.toDomain(),
                            surahName = surahMap[ayah.surahId] ?: "Surah ${ayah.surahId}",
                            matchedText = translation.text,
                            searchType = SearchType.TRANSLATION
                        )
                    }
                }
            } else emptyList()

            emit((arabicResults + translationResults).distinctBy { it.ayah.id })
        }
    }

    override fun getAllBookmarks(): Flow<List<QuranBookmark>> {
        return quranDao.getAllBookmarks().mapItems { it.toDomain() }
    }

    override suspend fun getBookmarkByAyahId(ayahId: Int): QuranBookmark? {
        return quranDao.getBookmarkByAyahId(ayahId)?.toDomain()
    }

    override fun isAyahBookmarked(ayahId: Int): Flow<Boolean> {
        return quranDao.isAyahBookmarked(ayahId)
    }

    override suspend fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        quranDao.toggleBookmark(ayahId, surahNumber, ayahNumber)
    }

    override suspend fun addBookmark(
        ayahId: Int,
        surahNumber: Int,
        ayahNumber: Int,
        note: String?,
        color: String?
    ) {
        quranDao.insertBookmark(
            QuranBookmarkEntity(
                ayahId = ayahId,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                note = note,
                color = color
            )
        )
    }

    override suspend fun updateBookmark(bookmark: QuranBookmark) {
        quranDao.updateBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(ayahId: Int) {
        quranDao.deleteBookmarkByAyahId(ayahId)
    }

    override fun getAllFavorites(): Flow<List<QuranFavorite>> {
        return quranDao.getAllFavorites().mapItems { it.toDomain() }
    }

    override fun getFavoriteAyahIds(): Flow<List<Int>> {
        return quranDao.getFavoriteAyahIds()
    }

    override suspend fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        quranDao.toggleFavorite(ayahId, surahNumber, ayahNumber)
    }

    override fun getReadingProgress(): Flow<ReadingProgress?> {
        return quranDao.getReadingProgress().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int) {
        val progress = quranDao.getReadingProgress().firstOrNull()
        if (progress == null) {
            quranDao.insertReadingProgress(
                ReadingProgressEntity(
                    lastReadSurah = surah,
                    lastReadAyah = ayah,
                    lastReadPage = page,
                    lastReadJuz = juz,
                    totalAyahsRead = 0,
                    currentKhatmaCount = 0
                )
            )
        } else {
            quranDao.updateReadingPosition(surah, ayah, page, juz)
        }
    }

    override suspend fun incrementAyahsRead(count: Int) {
        quranDao.incrementAyahsRead(count)
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

    private fun AyahEntity.toDomain(translation: String? = null): Ayah {
        return Ayah(
            id = id,
            surahNumber = surahId,
            ayahNumber = numberInSurah,
            textArabic = textArabic,
            textSimple = textUthmani,
            juzNumber = juz,
            hizbNumber = hizb,
            rubNumber = 0, // Not available in database
            pageNumber = page,
            sajdaType = SajdaType.fromString(sajdaType),
            sajdaNumber = if (sajda > 0) sajda else null,
            translation = translation,
            transliteration = transliteration,
            textTajweed = textTajweed
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
}
