package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.domain.model.SearchType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.model.Translator
import com.arshadshah.nimaz.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepositoryImpl @Inject constructor(
    private val quranDao: QuranDao
) : QuranRepository {

    override fun getAllSurahs(): Flow<List<Surah>> {
        return quranDao.getAllSurahs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSurahByNumber(surahNumber: Int): Surah? {
        return quranDao.getSurahByNumber(surahNumber)?.toDomain()
    }

    override fun getSurahsByRevelationType(type: RevelationType): Flow<List<Surah>> {
        val typeString = when (type) {
            RevelationType.MECCAN -> "meccan"
            RevelationType.MEDINAN -> "medinan"
        }
        return quranDao.getSurahsByRevelationType(typeString).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchSurahs(query: String): Flow<List<Surah>> {
        return quranDao.searchSurahs(query).map { entities ->
            entities.map { it.toDomain() }
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
                quranDao.getAyahsBySurah(surahId).first().map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getAyahById(ayahId: Int): Ayah? {
        return quranDao.getAyahById(ayahId)?.toDomain()
    }

    override fun getAyahsByJuz(juzNumber: Int): Flow<List<Ayah>> {
        return quranDao.getAyahsByJuz(juzNumber).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAyahsByPage(pageNumber: Int): Flow<List<Ayah>> {
        return quranDao.getAyahsByPage(pageNumber).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSajdaAyahs(): Flow<List<Ayah>> {
        return quranDao.getSajdaAyahs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSurahWithAyahs(surahNumber: Int, translatorId: String?): Flow<SurahWithAyahs?> {
        val surahFlow = quranDao.getAllSurahs().map { surahs ->
            surahs.find { it.number == surahNumber }?.toDomain()
        }

        return surahFlow.map { surah ->
            if (surah != null) {
                val surahEntity = quranDao.getSurahByNumber(surahNumber)
                val ayahs = surahEntity?.let {
                    quranDao.getAyahsBySurah(it.id).first().map { ayah -> ayah.toDomain() }
                } ?: emptyList()
                SurahWithAyahs(surah = surah, ayahs = ayahs)
            } else {
                null
            }
        }
    }

    override suspend fun getAvailableTranslators(): List<Translator> {
        return quranDao.getAvailableTranslatorIds().map { translatorId ->
            Translator(
                id = translatorId,
                name = translatorId, // Use id as name since we don't have name info
                languageCode = translatorId.substringBefore(".")
            )
        }
    }

    override fun getTranslationsForAyahs(ayahIds: List<Int>, translatorId: String): Flow<Map<Int, String>> {
        return quranDao.getTranslationsForAyahs(ayahIds, translatorId).map { translations ->
            translations.associate { it.ayahId to it.text }
        }
    }

    override fun searchQuran(query: String, translatorId: String?): Flow<List<QuranSearchResult>> {
        val arabicSearchFlow = quranDao.searchAyahs(query).map { ayahs ->
            ayahs.map { ayah ->
                QuranSearchResult(
                    ayah = ayah.toDomain(),
                    surahName = "", // Will be populated later
                    matchedText = ayah.textArabic,
                    searchType = SearchType.ARABIC
                )
            }
        }

        return if (translatorId != null) {
            val translationSearchFlow = quranDao.searchTranslations(query, translatorId).map { translations ->
                translations.mapNotNull { translation ->
                    quranDao.getAyahById(translation.ayahId)?.let { ayah ->
                        QuranSearchResult(
                            ayah = ayah.toDomain(),
                            surahName = "",
                            matchedText = translation.text,
                            searchType = SearchType.TRANSLATION
                        )
                    }
                }
            }

            combine(arabicSearchFlow, translationSearchFlow) { arabicResults, translationResults ->
                (arabicResults + translationResults).distinctBy { it.ayah.id }
            }
        } else {
            arabicSearchFlow
        }
    }

    override fun getAllBookmarks(): Flow<List<QuranBookmark>> {
        return quranDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }
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

    override suspend fun addBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int, note: String?, color: String?) {
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
            juzStart = 1, // Not available in database, default to 1
            orderInMushaf = orderRevealed
        )
    }

    private fun AyahEntity.toDomain(): Ayah {
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
            sajdaNumber = if (sajda > 0) sajda else null
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
}
