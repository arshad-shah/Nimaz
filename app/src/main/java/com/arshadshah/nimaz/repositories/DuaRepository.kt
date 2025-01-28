package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.utils.ApiResponse
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuaRepository @Inject constructor(
    private val dataStore: DataStore
) {
    // Existing functions
    suspend fun getCategories(): ApiResponse<ArrayList<LocalCategory>> {
        return try {
            ApiResponse.Success(dataStore.getAllCategories() as ArrayList<LocalCategory>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getChaptersByCategory(id: Int): ApiResponse<ArrayList<LocalChapter>> {
        return try {
            ApiResponse.Success(dataStore.getChaptersByCategory(id) as ArrayList<LocalChapter>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getDuasOfChapter(chapterId: Int): ApiResponse<ArrayList<LocalDua>> {
        return try {
            ApiResponse.Success(dataStore.getDuasOfChapter(chapterId) as ArrayList<LocalDua>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    // New functions
    suspend fun getFavoriteDuas(): ApiResponse<ArrayList<LocalDua>> {
        return try {
            ApiResponse.Success(dataStore.getFavoriteDuas() as ArrayList<LocalDua>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    fun getFavoriteDuasFlow(): Flow<List<LocalDua>> = dataStore.getFavoriteDuasFlow()

    suspend fun updateDua(dua: LocalDua): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.updateDua(dua))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun searchDuas(query: String): ApiResponse<ArrayList<LocalDua>> {
        return try {
            ApiResponse.Success(dataStore.searchDuas(query) as ArrayList<LocalDua>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun searchDuasAdvanced(
        query: String = "",
        chapterId: Int? = null,
        isFavorite: Int? = null
    ): ApiResponse<ArrayList<LocalDua>> {
        return try {
            ApiResponse.Success(
                dataStore.searchDuasAdvanced(query, chapterId, isFavorite) as ArrayList<LocalDua>
            )
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getDuaById(duaId: Int): ApiResponse<LocalDua?> {
        return try {
            ApiResponse.Success(dataStore.getDuaById(duaId))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getChapterById(chapterId: Int): ApiResponse<LocalChapter?> {
        return try {
            ApiResponse.Success(dataStore.getChapterById(chapterId))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getRelatedDuas(
        chapterId: Int,
        currentDuaId: Int
    ): ApiResponse<ArrayList<LocalDua>> {
        return try {
            ApiResponse.Success(
                dataStore.getRelatedDuas(
                    chapterId,
                    currentDuaId
                ) as ArrayList<LocalDua>
            )
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun countFavoriteDuas(): ApiResponse<Int> {
        return try {
            ApiResponse.Success(dataStore.countFavoriteDuas())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun countDuasInChapter(chapterId: Int): ApiResponse<Int> {
        return try {
            ApiResponse.Success(dataStore.countDuasInChapter(chapterId))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getLastAccessedDua(): ApiResponse<LocalDua?> {
        return try {
            ApiResponse.Success(dataStore.getLastAccessedDua())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getRandomDua(): ApiResponse<LocalDua?> {
        return try {
            ApiResponse.Success(dataStore.getRandomDua())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getRandomFavoriteDua(): ApiResponse<LocalDua?> {
        return try {
            ApiResponse.Success(dataStore.getRandomFavoriteDua())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    // Bulk operations
    suspend fun saveDuas(duas: List<LocalDua>): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.saveDuas(duas))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun saveChapters(chapters: List<LocalChapter>): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.saveChapters(chapters))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun deleteAllDuas(): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.deleteAllDuas())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun deleteAllChapters(): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.deleteAllChapters())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun replaceDuas(duas: List<LocalDua>): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.replaceDuas(duas))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun replaceChapters(chapters: List<LocalChapter>): ApiResponse<Unit> {
        return try {
            ApiResponse.Success(dataStore.replaceChapters(chapters))
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    // Count operations
    suspend fun countChapters(): ApiResponse<Int> {
        return try {
            ApiResponse.Success(dataStore.countChapters())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun countDuas(): ApiResponse<Int> {
        return try {
            ApiResponse.Success(dataStore.countDuas())
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }
}