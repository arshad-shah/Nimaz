package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsmaUnNabiRepositoryImpl @Inject constructor(
    private val dao: AsmaUnNabiDao
) : AsmaUnNabiRepository {

    override fun getAllNames(): Flow<List<AsmaUnNabi>> {
        return combine(dao.getAllNames(), dao.getAllBookmarks()) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.nameId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override suspend fun getNameById(id: Int): AsmaUnNabi? {
        val entity = dao.getNameById(id) ?: return null
        val isFav = dao.isBookmarked(id)
        return entity.toDomain(isFavorite = isFav)
    }

    override fun searchNames(query: String): Flow<List<AsmaUnNabi>> {
        return combine(dao.searchNames(query), dao.getAllBookmarks()) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.nameId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override fun getFavoriteNames(): Flow<List<AsmaUnNabi>> {
        return dao.getFavoriteNames().map { list -> list.map { it.toDomain(isFavorite = true) } }
    }

    override suspend fun toggleFavorite(nameId: Int) {
        dao.toggleFavorite(nameId)
    }

    override suspend fun isFavorite(nameId: Int): Boolean {
        return dao.isBookmarked(nameId)
    }

    private fun AsmaUnNabiEntity.toDomain(isFavorite: Boolean = false): AsmaUnNabi {
        return AsmaUnNabi(
            id = id, number = number, nameArabic = nameArabic,
            nameTransliteration = nameTransliteration, nameEnglish = nameEnglish,
            meaning = meaning, explanation = explanation, source = source,
            displayOrder = displayOrder, isFavorite = isFavorite
        )
    }
}
