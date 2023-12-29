package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HadithViewModel : ViewModel() {
    private var _allHadithBooks = MutableStateFlow(listOf<HadithMetadata>())
    val allHadithBooks = _allHadithBooks.asStateFlow()

    private var _chaptersForABook = MutableStateFlow(listOf<HadithChapter>())
    val chaptersForABook = _chaptersForABook.asStateFlow()

    private var _hadithForAChapter = MutableStateFlow(listOf<HadithEntity>())
    val hadithForAChapter = _hadithForAChapter.asStateFlow()

    private var _allFavourites = MutableStateFlow(listOf<HadithFavourite>())
    val allFavourites = _allFavourites.asStateFlow()

    private var _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private var _error = MutableStateFlow("")
    val error = _error.asStateFlow()

    init {
        getAllHadithBooks()
    }


    private fun getAllHadithBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loading.value = true
                _error.value = ""
                val dataStore = LocalDataStore.getDataStore()
                val allHadithBooks = dataStore.getAllMetadata()
                Log.d("All Hadith Books", allHadithBooks.toString())
                _loading.value = false
                _allHadithBooks.value = allHadithBooks
            } catch (e: Exception) {
                Log.d("getAllHadithBooks", e.message ?: "Unknown error")
                _loading.value = false
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun getAllChaptersForABook(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loading.value = true
                _error.value = ""
                val dataStore = LocalDataStore.getDataStore()
                val chaptersForABook = dataStore.getAllHadithChaptersForABook(bookId)
                Log.d("All Chapters For A Book", chaptersForABook.toString())
                _loading.value = false
                _chaptersForABook.value = chaptersForABook
            } catch (e: Exception) {
                Log.d("getAllChaptersForABook", e.message ?: "Unknown error")
                _loading.value = false
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun getAllHadithForChapter(bookId: Int, chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loading.value = true
                _error.value = ""
                val dataStore = LocalDataStore.getDataStore()
                val chaptersForABook = dataStore.getAllHadithsForABook(bookId, chapterId)
                Log.d("All Hadith For A Chapter", chaptersForABook.toString())
                _loading.value = false
                _hadithForAChapter.value = chaptersForABook
            } catch (e: Exception) {
                Log.d("getAllHadithForChapter", e.message ?: "Unknown error")
                _loading.value = false
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun updateFavouriteStatus(bookId: Int, chapterId: Int, id: Int, favouriteStatus: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.updateFavouriteStatus(id, favouriteStatus)

                val hadithForAChapter = dataStore.getAllHadithsForABook(bookId, chapterId)
                _hadithForAChapter.value = hadithForAChapter

                getAllFavourites()
            } catch (e: Exception) {
                Log.d("updateFavouriteStatus", e.message ?: "Unknown error")
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun getAllFavourites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loading.value = true
                _error.value = ""
                val dataStore = LocalDataStore.getDataStore()
                val allFavourites = dataStore.getAllFavourites()
                Log.d("All Favourites", allFavourites.toString())
                _allFavourites.value = allFavourites
                _loading.value = false
            } catch (e: Exception) {
                Log.d("getAllFavourites", e.message ?: "Unknown error")
                _loading.value = false
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

}
