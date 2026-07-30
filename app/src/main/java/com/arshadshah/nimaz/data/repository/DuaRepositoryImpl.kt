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
import com.arshadshah.nimaz.data.local.dua.DuaContentSeeder
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.DuaRepository
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
    private val seeder: DuaContentSeeder
) : DuaRepository {

    /** Seed (if the bundled content is new/missing) once, then emit DB-backed flows. */
    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> =
        flow { seeder.seedIfNeeded(); emitAll(block()) }

    override fun getAllCategories(): Flow<List<DuaCategory>> = seededFlow {
        duaDao.getAllCategories().mapItems { it.toDomain() }
    }

    override suspend fun getCategoryById(categoryId: String): DuaCategory? {
        seeder.seedIfNeeded()
        return duaDao.getCategoryById(categoryId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getDuasByCategory(categoryId: String): Flow<List<Dua>> = seededFlow {
        duaDao.getDuasByCategory(categoryId.toIntOrNull() ?: 0).mapItems { it.toDomain() }
    }

    override suspend fun getDuasByCategoryOnce(categoryId: String): List<Dua> {
        seeder.seedIfNeeded()
        return duaDao.getDuasByCategoryOnce(categoryId.toIntOrNull() ?: return emptyList())
            .map { it.toDomain() }
    }

    override suspend fun getDuaById(duaId: String): Dua? {
        seeder.seedIfNeeded()
        return duaDao.getDuaById(duaId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getDuasByOccasion(occasion: DuaOccasion): Flow<List<Dua>> = seededFlow {
        // Since there's no occasion column in the database, return empty list
        duaDao.searchDuas(occasion.name.lowercase()).mapItems { it.toDomain() }
    }

    override fun searchDuas(query: String): Flow<List<DuaSearchResult>> = seededFlow {
        combine(
            duaDao.getAllCategories(),
            duaDao.searchDuas(query)
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

    override suspend fun initializeDuaData() {
        // Content ships in the prepopulated DB for fresh installs and is
        // (re)seeded from the bundled assets/duas/duas.json so existing users
        // also receive newly added duas on update.
        seeder.seedIfNeeded()
    }

    override suspend fun isDataInitialized(): Boolean {
        seeder.seedIfNeeded()
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
