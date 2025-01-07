package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.utils.ApiResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuaRepository @Inject constructor(
    private val dataStore: DataStore
) {
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
}