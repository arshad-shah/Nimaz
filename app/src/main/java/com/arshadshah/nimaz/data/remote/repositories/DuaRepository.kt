package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.remote.models.Category
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.api.ApiResponse
import java.io.IOException

object DuaRepository {

    suspend fun getCategories(): ApiResponse<ArrayList<Category>> {
        return try {
            val dataStore = LocalDataStore.getDataStore()
            ApiResponse.Success(dataStore.getAllCategories() as ArrayList<Category>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun getChaptersByCategory(id: Int): ApiResponse<ArrayList<Chapter>> {
        return try {
            val dataStore = LocalDataStore.getDataStore()
            //result
            ApiResponse.Success(dataStore.getChaptersByCategory(id) as ArrayList<Chapter>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    //get duas of a chapter by chapter id
    suspend fun getDuasOfChapter(chapterId: Int): ApiResponse<ArrayList<Dua>> {
        return try {
            val dataStore = LocalDataStore.getDataStore()
            //result
            return ApiResponse.Success(dataStore.getDuasOfChapter(chapterId) as ArrayList<Dua>)
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }

    }
}