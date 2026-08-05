package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressEntity
import com.arshadshah.nimaz.data.local.user.ProgressKind
import kotlinx.coroutines.flow.map
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuaRepositoryImpl @Inject constructor(
    private val duaDao: DuaDao,
    private val bookmarkDao: BookmarkDao,
    private val progressDao: ProgressDao,
    private val searchIndex: ContentSearchIndex
) : DuaRepository {

    /**
     * Builds the DB-backed flow at collection time rather than at call time.
     *
     * This used to seed the bundled dua content first; all 43 categories and 311 duas arrive
     * in the artifact since `DuaContentSeeder` was retired at versionCode 385
     * (`docs/retirement.yaml`). The deferral stays because callers hold these flows without
     * collecting them.
     */
    private fun <T> deferredFlow(block: () -> Flow<T>): Flow<T> =
        flow { emitAll(block()) }

    override fun getAllCategories(): Flow<List<DuaCategory>> = deferredFlow {
        duaDao.getAllCategories().mapItems { it.toDomain() }
    }

    override suspend fun getCategoryById(categoryId: String): DuaCategory? {
        return duaDao.getCategoryById(categoryId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getDuasByCategory(categoryId: String): Flow<List<Dua>> = deferredFlow {
        duaDao.getDuasByCategory(categoryId.toIntOrNull() ?: 0).mapItems { it.toDomain() }
    }

    override suspend fun getDuasByCategoryOnce(categoryId: String): List<Dua> {
        return duaDao.getDuasByCategoryOnce(categoryId.toIntOrNull() ?: return emptyList())
            .map { it.toDomain() }
    }

    override suspend fun getDuasByIds(duaIds: List<String>): List<Dua> {
        val numeric = duaIds.mapNotNull { it.toIntOrNull() }
        if (numeric.isEmpty()) return emptyList()
        return duaDao.getDuasByIds(numeric).map { it.toDomain() }
    }

    override suspend fun getDuaById(duaId: String): Dua? {
        return duaDao.getDuaById(duaId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getDuasByOccasion(occasion: DuaOccasion): Flow<List<Dua>> = deferredFlow {
        // Since there's no occasion column in the database, return empty list
        duaDao.searchDuas(occasion.name.lowercase()).mapItems { it.toDomain() }
    }

    /**
     * The index first, the scan only where there is no index (#330).
     *
     * A dua carries `text_arabic` and a transliteration, and neither was reachable by
     * `LIKE`: the Arabic is vocalised and the transliteration is full of macrons and
     * dots. Both are folded into the shipped index.
     */
    override fun searchDuas(query: String): Flow<List<DuaSearchResult>> = deferredFlow {
        combine(
            duaDao.getAllCategories(),
            flow {
                if (searchIndex.isAvailable()) {
                    val ids = searchIndex.refs(query, SearchKind.DUA)
                        .mapNotNull(String::toIntOrNull)
                    emit(duaDao.getDuasByIds(ids))
                } else {
                    emitAll(duaDao.searchDuas(query))
                }
            }
        ) { categories, entities ->
            // Create a map from category id to category name for lookup
            val categoryMap = categories.associate { it.id to it.nameEnglish }

            entities.map { dua ->
                DuaSearchResult(
                    dua = dua.toDomain(),
                    categoryName = categoryMap[dua.categoryId] ?: "",
                    matchedText = dua.translation
                )
            }
        }
    }

    // Marks and counts are the user's: `dua_bookmarks` and `dua_progress` are rows in the
    // consolidated `bookmarks` and `progress` tables now, keyed by kind.

    override fun getAllBookmarks(): Flow<List<DuaBookmark>> {
        return bookmarkDao.bookmarks(BookmarkKind.DUA).mapItems { it.toDuaBookmark() }
    }

    override fun getFavoriteDuas(): Flow<List<DuaBookmark>> {
        return bookmarkDao.favourites(BookmarkKind.DUA).mapItems { it.toDuaBookmark() }
    }

    override suspend fun getBookmarkByDuaId(duaId: String): DuaBookmark? {
        return bookmarkDao.find(BookmarkKind.DUA, duaId.toIntOrNull() ?: return null)
            ?.toDuaBookmark()
    }

    override fun isDuaBookmarked(duaId: String): Flow<Boolean> {
        return bookmarkDao.observeIsBookmarked(BookmarkKind.DUA, duaId.toIntOrNull() ?: 0)
    }

    override fun isDuaFavorite(duaId: String): Flow<Boolean> {
        return bookmarkDao.observeIsFavourite(BookmarkKind.DUA, duaId.toIntOrNull() ?: 0)
    }

    override suspend fun toggleFavorite(duaId: String, categoryId: String) {
        val target = duaId.toIntOrNull() ?: return
        val existing = bookmarkDao.find(BookmarkKind.DUA, target)
        val now = System.currentTimeMillis()
        when {
            existing == null -> bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.DUA,
                    targetId = target,
                    bookmarked = true,
                    favourite = true,
                    contextId = categoryId.toIntOrNull(),
                    createdAt = now,
                    updatedAt = now,
                )
            )
            // A favourite that is also a plain bookmark keeps the bookmark.
            existing.favourite && existing.bookmarked ->
                bookmarkDao.clearFavourite(BookmarkKind.DUA, target, now)
            existing.favourite -> bookmarkDao.delete(BookmarkKind.DUA, target)
            else -> bookmarkDao.upsert(existing.copy(favourite = true, updatedAt = now))
        }
    }

    override suspend fun insertBookmark(bookmark: DuaBookmark) {
        bookmarkDao.upsert(bookmark.toBookmarkEntity())
    }

    override suspend fun updateBookmark(bookmark: DuaBookmark) {
        bookmarkDao.upsert(bookmark.toBookmarkEntity())
    }

    override suspend fun deleteBookmark(duaId: String) {
        bookmarkDao.delete(BookmarkKind.DUA, duaId.toIntOrNull() ?: return)
    }

    override suspend fun getProgressForDuaOnDate(duaId: String, date: Long): DuaProgress? {
        return progressDao.find(ProgressKind.DUA, duaId.toIntOrNull() ?: return null, date)
            ?.toDuaProgress()
    }

    override fun getProgressForDate(date: Long): Flow<List<DuaProgress>> {
        return progressDao.onDate(ProgressKind.DUA, date).mapItems { it.toDuaProgress() }
    }

    override fun getProgressHistoryForDua(duaId: String): Flow<List<DuaProgress>> {
        val target = duaId.toIntOrNull() ?: 0
        return progressDao.ofKind(ProgressKind.DUA)
            .map { rows -> rows.filter { it.targetId == target }.map { it.toDuaProgress() } }
    }

    override suspend fun incrementDuaProgress(duaId: String, date: Long, targetCount: Int) {
        progressDao.increment(ProgressKind.DUA, duaId.toIntOrNull() ?: return, date, targetCount)
    }

    override suspend fun decrementDuaProgress(duaId: String, date: Long) {
        progressDao.decrement(ProgressKind.DUA, duaId.toIntOrNull() ?: return, date)
    }

    override suspend fun isDataInitialized(): Boolean {
        return duaDao.getAllCategories().first().isNotEmpty()
    }

    // Mapping functions
    private fun DuaCategoryEntity.toDomain(): DuaCategory {
        return DuaCategory(
            id = id.toString(),
            nameArabic = nameArabic,
            nameEnglish = nameEnglish,
            description = null,
            iconName = icon,
            displayOrder = displayOrder,
            duaCount = duaCount
        )
    }

    private fun DuaEntity.toDomain(): Dua {
        return Dua(
            id = id.toString(),
            categoryId = categoryId.toString(),
            titleArabic = titleArabic,
            titleEnglish = titleEnglish,
            textArabic = textArabic,
            textTransliteration = transliteration,
            textEnglish = translation,
            reference = source,
            occasion = null,
            benefits = virtue,
            repeatCount = repeatCount,
            audioUrl = audioFile,
            displayOrder = displayOrder
        )
    }

    private fun BookmarkEntity.toDuaBookmark(): DuaBookmark {
        return DuaBookmark(
            id = 0,
            duaId = targetId.toString(),
            categoryId = (contextId ?: 0).toString(),
            note = note,
            isFavorite = favourite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun DuaBookmark.toBookmarkEntity(): BookmarkEntity {
        return BookmarkEntity(
            kind = BookmarkKind.DUA,
            targetId = duaId.toIntOrNull() ?: 0,
            bookmarked = true,
            favourite = isFavorite,
            note = note,
            contextId = categoryId.toIntOrNull(),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ProgressEntity.toDuaProgress(): DuaProgress {
        return DuaProgress(
            id = 0,
            duaId = targetId.toString(),
            date = date,
            completedCount = completed,
            targetCount = total ?: 0,
            isCompleted = isCompleted,
            createdAt = createdAt
        )
    }
}
