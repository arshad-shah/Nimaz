package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.DuaDao
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DuaSystem @Inject constructor(
    private val duaDao: DuaDao
) {
    // Chapter related operations
    suspend fun getChaptersByCategory(categoryId: Int) = duaDao.getChaptersByCategory(categoryId)
    suspend fun getChapterById(chapterId: Int) = duaDao.getChapterById(chapterId)
    suspend fun saveChapters(chapters: List<LocalChapter>) = duaDao.saveChapters(chapters)
    suspend fun replaceChapters(chapters: List<LocalChapter>) = duaDao.replaceChapters(chapters)
    suspend fun deleteAllChapters() = duaDao.deleteAllChapters()
    suspend fun countChapters() = duaDao.countChapters()

    // Dua retrieval operations
    suspend fun getDuasOfChapter(chapterId: Int) = duaDao.getDuasOfChapter(chapterId)
    suspend fun getDuaById(duaId: Int) = duaDao.getDuaById(duaId)
    suspend fun getRandomDua() = duaDao.getRandomDua()
    suspend fun getLastAccessedDua() = duaDao.getLastAccessedDua()

    // Favorite related operations
    suspend fun getFavoriteDuas() = duaDao.getFavoriteDuas()
    fun getFavoriteDuasFlow(): Flow<List<LocalDua>> = duaDao.getFavoriteDuasFlow()
    suspend fun getRandomFavoriteDua() = duaDao.getRandomFavoriteDua()
    suspend fun countFavoriteDuas() = duaDao.countFavoriteDuas()

    // Search operations
    suspend fun searchDuas(query: String) = duaDao.searchDuas(query)
    suspend fun searchDuasAdvanced(
        query: String = "",
        chapterId: Int? = null,
        isFavorite: Int? = null
    ) = duaDao.searchDuasAdvanced(query, chapterId, isFavorite)

    // Related content
    suspend fun getRelatedDuas(chapterId: Int, currentDuaId: Int) =
        duaDao.getRelatedDuas(chapterId, currentDuaId)

    // Dua management operations
    suspend fun updateDua(dua: LocalDua) = duaDao.updateDua(dua)
    suspend fun saveDuas(duas: List<LocalDua>) = duaDao.saveDuas(duas)
    suspend fun replaceDuas(duas: List<LocalDua>) = duaDao.replaceDuas(duas)
    suspend fun deleteAllDuas() = duaDao.deleteAllDuas()
    suspend fun countDuas() = duaDao.countDuas()
    suspend fun countDuasInChapter(chapterId: Int) = duaDao.countDuasInChapter(chapterId)
}