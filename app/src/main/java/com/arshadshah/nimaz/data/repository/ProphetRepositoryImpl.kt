package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class ProphetRepositoryImpl @Inject constructor(
    private val dao: ProphetDao,
    private val bookmarkDao: BookmarkDao
) : ProphetRepository {

    override fun getAllProphets(): Flow<List<Prophet>> {
        return combine(dao.getAllProphets(), bookmarkDao.favourites(BookmarkKind.PROPHET)) { prophets, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.targetId }.toSet()
            prophets.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override suspend fun getProphetById(id: Int): Prophet? {
        val entity = dao.getProphetById(id) ?: return null
        val isFav = isFavouriteOf(id)
        return entity.toDomain(isFavorite = isFav)
    }

    override fun getFavoriteProphets(): Flow<List<Prophet>> {
        return bookmarkDao.favourites(BookmarkKind.PROPHET)
            .flatMapLatest { marks ->
                if (marks.isEmpty()) flowOf(emptyList())
                else dao.getByIds(marks.map { it.targetId })
            }
            .mapItems { it.toDomain(isFavorite = true) }
    }

    override suspend fun toggleFavorite(prophetId: Int) {
        toggleFavouriteMark(prophetId)
    }

    override suspend fun isFavorite(prophetId: Int): Boolean {
        return isFavouriteOf(prophetId)
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            emptyList()
        }
    }

    private fun ProphetEntity.toDomain(isFavorite: Boolean = false): Prophet {
        return Prophet(
            id = id, number = number, nameArabic = nameArabic,
            nameEnglish = nameEnglish, nameTransliteration = nameTransliteration,
            titleArabic = titleArabic, titleEnglish = titleEnglish,
            storySummary = storySummary, keyLessons = parseJsonArray(keyLessons),
            quranMentions = parseJsonArray(quranMentions), era = era,
            lineage = lineage, yearsLived = yearsLived,
            placeOfPreaching = placeOfPreaching, miracles = parseJsonArray(miracles),
            displayOrder = displayOrder, isFavorite = isFavorite
        )
    }

    /** Is this one favourited? One row in the user's database answers it. */
    private suspend fun isFavouriteOf(id: Int): Boolean =
        bookmarkDao.find(BookmarkKind.PROPHET, id)?.favourite == true

    /**
     * Favourite on, or off.
     *
     * The old table had no other state, so a toggle was insert-or-delete. The consolidated row
     * can also be bookmarked, so turning a favourite off clears the flag and leaves the row when
     * something else is still set — and removes it when nothing is.
     */
    private suspend fun toggleFavouriteMark(id: Int) {
        val now = System.currentTimeMillis()
        val existing = bookmarkDao.find(BookmarkKind.PROPHET, id)
        when {
            existing == null -> bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.PROPHET,
                    targetId = id,
                    bookmarked = false,
                    favourite = true,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            existing.favourite && existing.bookmarked ->
                bookmarkDao.clearFavourite(BookmarkKind.PROPHET, id, now)
            existing.favourite -> bookmarkDao.delete(BookmarkKind.PROPHET, id)
            else -> bookmarkDao.upsert(existing.copy(favourite = true, updatedAt = now))
        }
    }
}
