package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ViewState<out T> {
    data object Loading : ViewState<Nothing>()
    data class Success<T>(val data: T) : ViewState<T>()
    data class Error(val message: String) : ViewState<Nothing>()
}

@HiltViewModel
class HadithViewModel @Inject constructor(
    private val dataStore: DataStore
) : ViewModel() {
    private var _booksState =
        MutableStateFlow<ViewState<List<HadithMetadata>>>(ViewState.Success(emptyList()))
    val booksState: StateFlow<ViewState<List<HadithMetadata>>> = _booksState.asStateFlow()

    private var _chaptersState =
        MutableStateFlow<ViewState<List<HadithChapter>>>(ViewState.Success(emptyList()))
    val chaptersState: StateFlow<ViewState<List<HadithChapter>>> = _chaptersState.asStateFlow()

    private var _hadithState =
        MutableStateFlow<ViewState<List<HadithEntity>>>(ViewState.Success(emptyList()))
    val hadithState: StateFlow<ViewState<List<HadithEntity>>> = _hadithState.asStateFlow()

    private var _favouritesState =
        MutableStateFlow<ViewState<List<HadithFavourite>>>(ViewState.Success(emptyList()))
    val favouritesState: StateFlow<ViewState<List<HadithFavourite>>> =
        _favouritesState.asStateFlow()

    init {
        getAllHadithBooks()
    }

    private fun getAllHadithBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _booksState.value = ViewState.Loading
                val books = dataStore.getAllMetadata()
                _booksState.value = ViewState.Success(books)
                Log.d("HadithViewModel", "Successfully loaded ${books.size} books")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error while loading books"
                Log.e("HadithViewModel", "Error loading books: $errorMsg", e)
                _booksState.value = ViewState.Error(errorMsg)
            }
        }
    }

    fun getAllChaptersForABook(bookId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _chaptersState.value = ViewState.Loading
                val chapters = dataStore.getAllHadithChaptersForABook(bookId)
                _chaptersState.value = ViewState.Success(chapters)
                Log.d(
                    "HadithViewModel",
                    "Successfully loaded ${chapters.size} chapters for book $bookId"
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error while loading chapters"
                Log.e("HadithViewModel", "Error loading chapters for book $bookId: $errorMsg", e)
                _chaptersState.value = ViewState.Error(errorMsg)
            }
        }
    }

    fun getAllHadithForChapter(bookId: Int, chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _hadithState.value = ViewState.Loading
                val hadiths = dataStore.getAllHadithsForABook(bookId, chapterId)
                val hadithChapter = dataStore.getAllHadithChaptersForABook(bookId)
                    .find { it.chapterId == chapterId }
                _hadithState.value = ViewState.Success(hadiths)
                _chaptersState.value = ViewState.Success(listOfNotNull(hadithChapter))
                Log.d(
                    "HadithViewModel",
                    "Successfully loaded ${hadiths.size} hadiths for book $bookId, chapter $chapterId"
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error while loading hadiths"
                Log.e(
                    "HadithViewModel",
                    "Error loading hadiths for book $bookId, chapter $chapterId: $errorMsg",
                    e
                )
                _hadithState.value = ViewState.Error(errorMsg)
            }
        }
    }

    fun updateFavouriteStatus(bookId: Int, chapterId: Int, id: Int, favouriteStatus: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update favourite status
                dataStore.updateFavouriteStatus(id, favouriteStatus)
                Log.d("HadithViewModel", "Successfully updated favourite status for hadith $id")

                // Refresh hadith list
                val hadiths = dataStore.getAllHadithsForABook(bookId, chapterId)
                _hadithState.value = ViewState.Success(hadiths)

                // Refresh favourites list
                getAllFavourites()
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error while updating favourite status"
                Log.e("HadithViewModel", "Error updating favourite status: $errorMsg", e)
                // Show error but keep existing data
                _hadithState.value = when (val currentState = _hadithState.value) {
                    is ViewState.Success -> currentState
                    else -> ViewState.Error(errorMsg)
                }
            }
        }
    }

    fun getAllFavourites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _favouritesState.value = ViewState.Loading
                val favourites = dataStore.getAllFavourites()
                _favouritesState.value = ViewState.Success(favourites)
                Log.d("HadithViewModel", "Successfully loaded ${favourites.size} favourites")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error while loading favourites"
                Log.e("HadithViewModel", "Error loading favourites: $errorMsg", e)
                _favouritesState.value = ViewState.Error(errorMsg)
            }
        }
    }

    fun getChapterName(bookId: Int = 1, chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hadithChapter = dataStore.getAllHadithChaptersForABook(bookId)
                    .find { it.chapterId == chapterId }
                _chaptersState.value = ViewState.Success(listOfNotNull(hadithChapter))
                Log.d("HadithViewModel", "Successfully loaded chapter $chapterId")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error while loading chapter"
                Log.e("HadithViewModel", "Error loading chapter $chapterId: $errorMsg", e)
                _chaptersState.value = ViewState.Error(errorMsg)
            }
        }
    }

    // Extension function to safely get current data
    private fun <T> ViewState<T>.getCurrentData(): T? = when (this) {
        is ViewState.Success -> data
        else -> null
    }
}