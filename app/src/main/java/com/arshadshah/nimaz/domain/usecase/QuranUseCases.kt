package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.PageAyahRange
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
import com.arshadshah.nimaz.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetSurahListUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<List<Surah>> = repository.getAllSurahs()

    fun byRevelationType(type: RevelationType): Flow<List<Surah>> =
        repository.getSurahsByRevelationType(type)

    fun search(query: String): Flow<List<Surah>> = repository.searchSurahs(query)
}

class GetSurahWithAyahsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(surahNumber: Int, translatorId: String? = null): Flow<SurahWithAyahs?> =
        repository.getSurahWithAyahs(surahNumber, translatorId)
}

class GetAyahsByJuzUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(juzNumber: Int, translatorId: String? = null): Flow<List<Ayah>> =
        repository.getAyahsByJuz(juzNumber, translatorId)
}

class GetAyahsByPageUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(pageNumber: Int, translatorId: String? = null): Flow<List<Ayah>> =
        repository.getAyahsByPage(pageNumber, translatorId)
}

class GetSajdaAyahsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<List<Ayah>> = repository.getSajdaAyahs()
}

class SearchQuranUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(
        query: String,
        translatorId: String? = null
    ): Flow<List<QuranSearchResult>> =
        repository.searchQuran(query, translatorId)
}

class GetAvailableTranslatorsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(): List<Translator> = repository.getAvailableTranslators()
}

class ToggleQuranBookmarkUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        repository.toggleBookmark(ayahId, surahNumber, ayahNumber)
    }
}

class GetQuranBookmarksUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<List<QuranBookmark>> = repository.getAllBookmarks()
}

class IsAyahBookmarkedUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(ayahId: Int): Flow<Boolean> = repository.isAyahBookmarked(ayahId)
}

class UpdateQuranBookmarkUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(bookmark: QuranBookmark) {
        repository.updateBookmark(bookmark)
    }
}

class DeleteQuranBookmarkUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int) {
        repository.deleteBookmark(ayahId)
    }
}

class ToggleQuranFavoriteUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        repository.toggleFavorite(ayahId, surahNumber, ayahNumber)
    }
}

class GetQuranFavoritesUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<List<QuranFavorite>> = repository.getAllFavorites()
}

class GetQuranFavoriteAyahIdsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<List<Int>> = repository.getFavoriteAyahIds()
}

class GetReadingProgressUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<ReadingProgress?> = repository.getReadingProgress()
}

class UpdateReadingPositionUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surah: Int, ayah: Int, page: Int, juz: Int) {
        repository.updateReadingPosition(surah, ayah, page, juz)
    }
}

class IncrementAyahsReadUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(count: Int) {
        repository.incrementAyahsRead(count)
    }
}

class GetSurahInfoUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahNumber: Int): SurahInfo? = repository.getSurahInfo(surahNumber)
}

class GetPageAyahRangesUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(): List<PageAyahRange> = repository.getPageAyahRanges()
}

/**
 * Returns a deterministic "verse of the day" for a given day. The same day always
 * resolves to the same ayah for every user (no randomness/persistence required),
 * and the selected ayah carries its translation for the requested translator.
 */
class GetVerseOfTheDayUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(epochDay: Long, translatorId: String? = null): Ayah? {
        // Map the day onto a stable ayah id in 1..TOTAL_AYAHS (handles negative epochDay).
        val ayahId = (((epochDay % TOTAL_AYAHS) + TOTAL_AYAHS) % TOTAL_AYAHS).toInt() + 1
        val ayah = repository.getAyahById(ayahId) ?: return null
        val translation = if (translatorId != null) {
            repository.getTranslationsForAyahs(listOf(ayahId), translatorId).first()[ayahId]
        } else {
            null
        }
        return if (translation != null) ayah.copy(translation = translation) else ayah
    }

    private companion object {
        const val TOTAL_AYAHS = 6236
    }
}

class GetSurahByNumberUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahNumber: Int): Surah? =
        repository.getSurahByNumber(surahNumber)
}

class GetAyahsBySurahUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(surahNumber: Int): Flow<List<Ayah>> =
        repository.getAyahsBySurah(surahNumber)
}

// Wrapper class for all Quran use cases
data class QuranUseCases(
    val getSurahList: GetSurahListUseCase,
    val getSurahByNumber: GetSurahByNumberUseCase,
    val getAyahsBySurah: GetAyahsBySurahUseCase,
    val getSurahWithAyahs: GetSurahWithAyahsUseCase,
    val getAyahsByJuz: GetAyahsByJuzUseCase,
    val getAyahsByPage: GetAyahsByPageUseCase,
    val getSajdaAyahs: GetSajdaAyahsUseCase,
    val searchQuran: SearchQuranUseCase,
    val getAvailableTranslators: GetAvailableTranslatorsUseCase,
    val toggleBookmark: ToggleQuranBookmarkUseCase,
    val getBookmarks: GetQuranBookmarksUseCase,
    val isAyahBookmarked: IsAyahBookmarkedUseCase,
    val updateBookmark: UpdateQuranBookmarkUseCase,
    val deleteBookmark: DeleteQuranBookmarkUseCase,
    val toggleFavorite: ToggleQuranFavoriteUseCase,
    val getFavorites: GetQuranFavoritesUseCase,
    val getFavoriteAyahIds: GetQuranFavoriteAyahIdsUseCase,
    val getReadingProgress: GetReadingProgressUseCase,
    val updateReadingPosition: UpdateReadingPositionUseCase,
    val incrementAyahsRead: IncrementAyahsReadUseCase,
    val getSurahInfo: GetSurahInfoUseCase,
    val getPageAyahRanges: GetPageAyahRangesUseCase,
    val getVerseOfTheDay: GetVerseOfTheDayUseCase
)
