package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.utils.ApiResponse
import com.arshadshah.nimaz.utils.LocalDataStore
import java.io.IOException

object DuaRepository {

    suspend fun getCategories(): ApiResponse<ArrayList<LocalCategory>> {
        return try {
            val dataStore = LocalDataStore.getDataStore()
            ApiResponse.Success(dataStore.getAllCategories() as ArrayList<LocalCategory>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getChaptersByCategory(id: Int): ApiResponse<ArrayList<LocalChapter>> {
        return try {
            val dataStore = LocalDataStore.getDataStore()
            //result
            ApiResponse.Success(dataStore.getChaptersByCategory(id) as ArrayList<LocalChapter>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    //get duas of a chapter by chapter id
    suspend fun getDuasOfChapter(chapterId: Int): ApiResponse<ArrayList<LocalDua>> {
        return try {
            val dataStore = LocalDataStore.getDataStore()
            //result
            return ApiResponse.Success(dataStore.getDuasOfChapter(chapterId) as ArrayList<LocalDua>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }

    }
}