package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsmaUlHusnaRepositoryImpl @Inject constructor(
    private val dao: AsmaUlHusnaDao
) : AsmaUlHusnaRepository {

    override fun getAllNames(): Flow<List<AsmaUlHusna>> {
        return combine(dao.getAllNames(), dao.getAllBookmarks()) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.nameId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override suspend fun getNameById(id: Int): AsmaUlHusna? {
        val entity = dao.getNameById(id) ?: return null
        val isFav = dao.isBookmarked(id)
        return entity.toDomain(isFavorite = isFav)
    }

    override fun searchNames(query: String): Flow<List<AsmaUlHusna>> {
        return combine(dao.searchNames(query), dao.getAllBookmarks()) { names, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.nameId }.toSet()
            names.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override fun getFavoriteNames(): Flow<List<AsmaUlHusna>> {
        return dao.getFavoriteNames().mapItems { it.toDomain(isFavorite = true) }
    }

    override suspend fun toggleFavorite(nameId: Int) {
        dao.toggleFavorite(nameId)
    }

    override suspend fun isFavorite(nameId: Int): Boolean {
        return dao.isBookmarked(nameId)
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
}
