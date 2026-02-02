package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProphetRepositoryImpl @Inject constructor(
    private val dao: ProphetDao
) : ProphetRepository {

    override fun getAllProphets(): Flow<List<Prophet>> {
        return combine(dao.getAllProphets(), dao.getAllBookmarks()) { prophets, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.prophetId }.toSet()
            prophets.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override suspend fun getProphetById(id: Int): Prophet? {
        val entity = dao.getProphetById(id) ?: return null
        val isFav = dao.isBookmarked(id)
        return entity.toDomain(isFavorite = isFav)
    }

    override fun searchProphets(query: String): Flow<List<Prophet>> {
        return combine(dao.searchProphets(query), dao.getAllBookmarks()) { prophets, bookmarks ->
            val bookmarkedIds = bookmarks.map { it.prophetId }.toSet()
            prophets.map { it.toDomain(isFavorite = it.id in bookmarkedIds) }
        }
    }

    override fun getFavoriteProphets(): Flow<List<Prophet>> {
        return dao.getFavoriteProphets().map { list -> list.map { it.toDomain(isFavorite = true) } }
    }

    override suspend fun toggleFavorite(prophetId: Int) {
        dao.toggleFavorite(prophetId)
    }

    override suspend fun isFavorite(prophetId: Int): Boolean {
        return dao.isBookmarked(prophetId)
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
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
}
