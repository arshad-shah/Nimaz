package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PageAyahRange
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
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

class GetAyahByIdUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int): Ayah? = repository.getAyahById(ayahId)
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
    operator fun invoke(
        pageNumber: Int,
        translatorId: String? = null,
        script: MushafScript = MushafScript.DEFAULT
    ): Flow<List<Ayah>> = repository.getAyahsByPage(pageNumber, translatorId, script)
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

class InsertQuranBookmarkUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(bookmark: QuranBookmark) {
        repository.addBookmark(
            ayahId = bookmark.ayahId,
            surahNumber = bookmark.surahNumber,
            ayahNumber = bookmark.ayahNumber,
            note = bookmark.note,
            color = bookmark.color
        )
    }
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

/**
 * The thematic layer (schemaVersion 24). Every one of these returns nothing rather than
 * failing on an install whose artifact predates it — see [HasThematicContentUseCase], which
 * is what a screen asks before it offers an entry point into any of them.
 */
class GetSurahOverviewUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahNumber: Int): SurahOverview? =
        repository.getSurahOverview(surahNumber)
}

class GetSurahThemesUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahNumber: Int): List<AyahTheme> =
        repository.getThemesForSurah(surahNumber)
}

/** The passage a verse falls in — at most one, since the ranges never overlap. */
class GetThemeForAyahUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): AyahTheme? =
        repository.getThemeForAyah(surahNumber, ayahNumber)
}

class GetTopicTreeRootsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(tree: TopicTree): List<QuranTopic> =
        repository.getTopicTreeRoots(tree)
}

class GetTopicDetailUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(topicId: Int, tree: TopicTree): TopicDetail? =
        repository.getTopicDetail(topicId, tree)
}

/** Which subjects cite this verse — busiest first. */
class GetTopicsForAyahUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int): List<QuranTopic> =
        repository.getTopicsForAyah(ayahId)
}

class SearchTopicsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 60): List<QuranTopic> =
        repository.searchTopics(query, limit)
}

/**
 * Whether this install's artifact carries the thematic layer at all.
 *
 * `MIGRATION_23_24` creates the tables on every device; the rows only arrive with a
 * schemaVersion 24 artifact. Screens ask this before showing an entry point, so an install
 * in between shows one fewer affordance rather than an empty screen behind a promising tab.
 */
class HasThematicContentUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(): Boolean = repository.hasThematicContent()
}

class GetPageAyahRangesUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(
        script: MushafScript = MushafScript.DEFAULT
    ): List<PageAyahRange> = repository.getPageAyahRanges(script)
}

/**
 * The active edition's page↔ayah mapping (#325) — the single source of truth for page
 * counts, juz page spans and page→ayah lookups on the Page tab, the reader and khatam page
 * progress. Falls back to [MushafPagination.fallback] when the edition has no page data yet.
 */
class GetMushafPaginationUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(script: MushafScript): MushafPagination =
        MushafPagination.from(script, repository.getPageAyahRanges(script))
}

/**
 * Returns a line-accurate edition's layout of a page (4/7) for the renderer (5/7): its
 * ordered printed lines with typed segments (ayah / surah-header / basmalah). Empty for an
 * ayah-flow edition, which has no stored layout.
 */
class GetMushafPageLayoutUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(
        page: Int,
        script: MushafScript = MushafScript.DEFAULT
    ): MushafPageLayout = repository.getMushafPageLayout(page, script)
}

/**
 * One ayah's text in one translation, or null when that ayah has no translation.
 *
 * Reading it also *seeds* that translation if it is not on the device yet (translations
 * ship as assets and are populated on first use), so this doubles as the "make this
 * translation available" call — which is what lets the Quran settings preview show a live
 * sample of whichever translation the user just picked.
 */
class GetAyahTranslationUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int, translatorId: String): String? =
        repository.getTranslationsForAyahs(listOf(ayahId), translatorId).first()[ayahId]
}

/**
 * Returns a deterministic "verse of the day" for a given day. The same day always
 * resolves to the same ayah for every user (no randomness/persistence required),
 * and the selected ayah carries its translation for the requested translator.
 */
class GetVerseOfTheDayUseCase @Inject constructor(
    private val repository: QuranRepository,
    private val getAyahTranslation: GetAyahTranslationUseCase
) {
    suspend operator fun invoke(epochDay: Long, translatorId: String? = null): Ayah? {
        // Map the day onto a stable ayah id in 1..TOTAL_AYAHS (handles negative epochDay).
        val ayahId = (((epochDay % TOTAL_AYAHS) + TOTAL_AYAHS) % TOTAL_AYAHS).toInt() + 1
        val ayah = repository.getAyahById(ayahId) ?: return null
        val translation = translatorId?.let { getAyahTranslation(ayahId, it) }
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
    val getAyahById: GetAyahByIdUseCase,
    val getSurahWithAyahs: GetSurahWithAyahsUseCase,
    val getAyahsByJuz: GetAyahsByJuzUseCase,
    val getAyahsByPage: GetAyahsByPageUseCase,
    val getSajdaAyahs: GetSajdaAyahsUseCase,
    val searchQuran: SearchQuranUseCase,
    val getAvailableTranslators: GetAvailableTranslatorsUseCase,
    val toggleBookmark: ToggleQuranBookmarkUseCase,
    val getBookmarks: GetQuranBookmarksUseCase,
    val isAyahBookmarked: IsAyahBookmarkedUseCase,
    val insertBookmark: InsertQuranBookmarkUseCase,
    val updateBookmark: UpdateQuranBookmarkUseCase,
    val deleteBookmark: DeleteQuranBookmarkUseCase,
    val toggleFavorite: ToggleQuranFavoriteUseCase,
    val getFavorites: GetQuranFavoritesUseCase,
    val getFavoriteAyahIds: GetQuranFavoriteAyahIdsUseCase,
    val getReadingProgress: GetReadingProgressUseCase,
    val updateReadingPosition: UpdateReadingPositionUseCase,
    val incrementAyahsRead: IncrementAyahsReadUseCase,
    val getSurahInfo: GetSurahInfoUseCase,
    val getSurahOverview: GetSurahOverviewUseCase,
    val getSurahThemes: GetSurahThemesUseCase,
    val getThemeForAyah: GetThemeForAyahUseCase,
    val getTopicTreeRoots: GetTopicTreeRootsUseCase,
    val getTopicDetail: GetTopicDetailUseCase,
    val getTopicsForAyah: GetTopicsForAyahUseCase,
    val searchTopics: SearchTopicsUseCase,
    val hasThematicContent: HasThematicContentUseCase,
    val getPageAyahRanges: GetPageAyahRangesUseCase,
    val getMushafPagination: GetMushafPaginationUseCase,
    val getMushafPageLayout: GetMushafPageLayoutUseCase,
    val getVerseOfTheDay: GetVerseOfTheDayUseCase,
    val getAyahTranslation: GetAyahTranslationUseCase
)
