package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class AsmaUnNabiRepositoryImpl @Inject constructor(
    private val dao: AsmaUnNabiDao,
    private val bookmarkDao: BookmarkDao
) : AsmaUnNabiRepository {

    override fun getAllNames(): Flow<List<AsmaUnNabi>> {
        return combine(dao.getAllNames(), bookmarkDao.favourites(BookmarkKind.ASMA_UN_NABI)) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.targetId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override suspend fun getNameById(id: Int): AsmaUnNabi? {
        val entity = dao.getNameById(id) ?: return null
        val isFav = isFavouriteOf(id)
        return entity.toDomain(isFavorite = isFav)
    }

    override fun getFavoriteNames(): Flow<List<AsmaUnNabi>> {
        return bookmarkDao.favourites(BookmarkKind.ASMA_UN_NABI)
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

    private fun AsmaUnNabiEntity.toDomain(isFavorite: Boolean = false): AsmaUnNabi {
        return AsmaUnNabi(
            id = id, number = number, nameArabic = nameArabic,
            nameTransliteration = nameTransliteration, nameEnglish = nameEnglish,
            meaning = meaning, explanation = explanation, source = source,
            displayOrder = displayOrder, isFavorite = isFavorite
        )
    }

    /** Is this one favourited? One row in the user's database answers it. */
    private suspend fun isFavouriteOf(id: Int): Boolean =
        bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, id)?.favourite == true

    /**
     * Favourite on, or off.
     *
     * The old table had no other state, so a toggle was insert-or-delete. The consolidated row
     * can also be bookmarked, so turning a favourite off clears the flag and leaves the row when
     * something else is still set — and removes it when nothing is.
     */
    private suspend fun toggleFavouriteMark(id: Int) {
        val now = System.currentTimeMillis()
        val existing = bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, id)
        when {
            existing == null -> bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.ASMA_UN_NABI,
                    targetId = id,
                    bookmarked = false,
                    favourite = true,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            existing.favourite && existing.bookmarked ->
                bookmarkDao.clearFavourite(BookmarkKind.ASMA_UN_NABI, id, now)
            existing.favourite -> bookmarkDao.delete(BookmarkKind.ASMA_UN_NABI, id)
            else -> bookmarkDao.upsert(existing.copy(favourite = true, updatedAt = now))
        }
    }
}
