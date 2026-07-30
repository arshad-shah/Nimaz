package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class AsmaUlHusnaRepositoryImpl @Inject constructor(
    private val dao: AsmaUlHusnaDao,
    private val bookmarkDao: BookmarkDao
) : AsmaUlHusnaRepository {

    override fun getAllNames(): Flow<List<AsmaUlHusna>> {
        return combine(dao.getAllNames(), bookmarkDao.favourites(BookmarkKind.ASMA_UL_HUSNA)) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.targetId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override suspend fun getNameById(id: Int): AsmaUlHusna? {
        val entity = dao.getNameById(id) ?: return null
        val isFav = isFavouriteOf(id)
        return entity.toDomain(isFavorite = isFav)
    }

    override fun searchNames(query: String): Flow<List<AsmaUlHusna>> {
        return combine(dao.searchNames(query), bookmarkDao.favourites(BookmarkKind.ASMA_UL_HUSNA)) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.targetId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override fun getFavoriteNames(): Flow<List<AsmaUlHusna>> {
        // Ids from the user's database, then the names from the content one.
        return bookmarkDao.favourites(BookmarkKind.ASMA_UL_HUSNA)
            .flatMapLatest { marks ->
                if (marks.isEmpty()) flowOf(emptyList())
                else dao.getByIds(marks.map { it.targetId })
            }
            .mapItems { it.toDomain(isFavorite = true) }
    }

    override suspend fun toggleFavorite(nameId: Int) {
        toggleFavouriteMark(nameId)
    }

    override suspend fun isFavorite(nameId: Int): Boolean {
        return isFavouriteOf(nameId)
    }

    private fun AsmaUlHusnaEntity.toDomain(isFavorite: Boolean = false): AsmaUlHusna {
        val refs = try {
            val jsonArray = JSONArray(quranReferences)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            emptyList()
        }
        return AsmaUlHusna(
            id = id, number = number, nameArabic = nameArabic,
            nameTransliteration = nameTransliteration, nameEnglish = nameEnglish,
            meaning = meaning, explanation = explanation, benefits = benefits,
            quranReferences = refs, usageInDua = usageInDua,
            displayOrder = displayOrder, isFavorite = isFavorite
        )
    }

    /** Is this one favourited? One row in the user's database answers it. */
    private suspend fun isFavouriteOf(id: Int): Boolean =
        bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, id)?.favourite == true

    /**
     * Favourite on, or off.
     *
     * The old table had no other state, so a toggle was insert-or-delete. The consolidated row
     * can also be bookmarked, so turning a favourite off clears the flag and leaves the row when
     * something else is still set — and removes it when nothing is.
     */
    private suspend fun toggleFavouriteMark(id: Int) {
        val now = System.currentTimeMillis()
        val existing = bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, id)
        when {
            existing == null -> bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.ASMA_UL_HUSNA,
                    targetId = id,
                    bookmarked = false,
                    favourite = true,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            existing.favourite && existing.bookmarked ->
                bookmarkDao.clearFavourite(BookmarkKind.ASMA_UL_HUSNA, id, now)
            existing.favourite -> bookmarkDao.delete(BookmarkKind.ASMA_UL_HUSNA, id)
            else -> bookmarkDao.upsert(existing.copy(favourite = true, updatedAt = now))
        }
    }
}
