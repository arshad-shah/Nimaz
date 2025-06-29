package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranViewModel @Inject constructor(
    sharedPreferences: PrivateSharedPreferences,
    private val dataStore: DataStore
) : ViewModel() {

    //general state for error and loading
    private val _errorState = MutableStateFlow("")
    val errorState = _errorState.asStateFlow()
    private val _loadingState = MutableStateFlow(false)
    val loadingState = _loadingState.asStateFlow()

    //surah list state
    private var _surahListState = MutableStateFlow(ArrayList<LocalSurah>(114))
    val surahListState = _surahListState.asStateFlow()

    //juz list state
    private var _juzListState = MutableStateFlow(ArrayList<LocalJuz>(30))
    val juzListState = _juzListState.asStateFlow()

    private val _translation = MutableStateFlow("English")
    val translation = _translation.asStateFlow()

    private val _surahState = MutableStateFlow(
        LocalSurah(0, 0, 0, "", "", "", "", 0, 0)
    )

    // NEW: Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LocalAya>>(emptyList())
    val searchResults: StateFlow<List<LocalAya>> = _searchResults.asStateFlow()

    private val _searchLanguage = MutableStateFlow("All") // All, Arabic, English, Urdu
    val searchLanguage: StateFlow<String> = _searchLanguage.asStateFlow()

    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    // Filtered search results based on query
    val filteredSearchResults: StateFlow<List<LocalAya>> = combine(
        _searchResults,
        _searchQuery
    ) { results, query ->
        if (query.isBlank()) emptyList() else results
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    //state for bookmarking, favoriting, adding a note
    private val _bookmarks = MutableStateFlow(listOf<LocalAya>())
    val bookmarks = _bookmarks.asStateFlow()

    private val _favorites = MutableStateFlow(listOf<LocalAya>())
    val favorites = _favorites.asStateFlow()

    private val _notes = MutableStateFlow(listOf<LocalAya>())
    val notes = _notes.asStateFlow()

    data class SearchFilters(
        val surahNumber: Int? = null,
        val juzNumber: Int? = null,
        val isFavorite: Boolean? = null,
        val isBookmarked: Boolean? = null,
        val hasNote: Boolean? = null
    )

    init {
        _translation.value =
            sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE, "English")
        getSurahList()
        getJuzList()
        getAllBookmarks()
        getAllFavorites()
        getAllNotes()
    }

    fun getSurahList() {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val surahList = dataStore.getAllSurah().toMutableList() as ArrayList<LocalSurah>
                _surahListState.value = surahList
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _surahListState.value = ArrayList()
                _loadingState.value = false
                _errorState.value = e.message!!
            }
        }
    }

    fun getJuzList() {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val juzList = dataStore.getAllJuz().toMutableList() as ArrayList<LocalJuz>
                _juzListState.value = juzList
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _juzListState.value = ArrayList()
                _loadingState.value = false
                _errorState.value = e.message!!
            }
        }
    }

    // NEW: Search functions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchLanguage(language: String) {
        _searchLanguage.value = language
    }

    fun updateSearchFilters(filters: SearchFilters) {
        _searchFilters.value = filters
    }

    fun searchAyas(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val results = when (_searchLanguage.value) {
                    "Arabic" -> dataStore.searchAyasInArabic(query)
                    "English" -> dataStore.searchAyasInEnglish(query)
                    "Urdu" -> dataStore.searchAyasInUrdu(query)
                    else -> dataStore.searchAyas(query) // All languages
                }
                _searchResults.value = results
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _loadingState.value = false
                _errorState.value = e.message ?: "Search failed"
                Log.e("QuranViewModel", "Error searching ayas", e)
            }
        }
    }

    fun searchAyasAdvanced() {
        val query = _searchQuery.value
        val filters = _searchFilters.value

        if (query.isBlank()) {
            clearSearch()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val results = dataStore.searchAyasAdvanced(
                    query = query,
                    surahNumber = filters.surahNumber,
                    juzNumber = filters.juzNumber,
                    isFavorite = filters.isFavorite?.let { if (it) 1 else 0 },
                    isBookmarked = filters.isBookmarked?.let { if (it) 1 else 0 },
                    hasNote = filters.hasNote?.let { if (it) 1 else 0 }
                )
                _searchResults.value = results
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _loadingState.value = false
                _errorState.value = e.message ?: "Advanced search failed"
                Log.e("QuranViewModel", "Error in advanced search", e)
            }
        }
    }

    fun searchInFavorites(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val results = dataStore.searchFavoriteAyas(query)
                _searchResults.value = results
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _loadingState.value = false
                _errorState.value = e.message ?: "Favorites search failed"
                Log.e("QuranViewModel", "Error searching favorites", e)
            }
        }
    }

    fun searchInBookmarks(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val results = dataStore.searchBookmarkedAyas(query)
                _searchResults.value = results
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _loadingState.value = false
                _errorState.value = e.message ?: "Bookmarks search failed"
                Log.e("QuranViewModel", "Error searching bookmarks", e)
            }
        }
    }

    fun searchInNotes(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val results = dataStore.searchAyasWithNotes(query)
                _searchResults.value = results
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _loadingState.value = false
                _errorState.value = e.message ?: "Notes search failed"
                Log.e("QuranViewModel", "Error searching notes", e)
            }
        }
    }

    fun getRandomSearchResult(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val randomAya = dataStore.getRandomSearchAya(query)
                randomAya?.let { aya ->
                    _searchResults.value = listOf(aya)
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error getting random search result", e)
            }
        }
    }

    fun getSearchResultsCount(query: String, callback: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = dataStore.countSearchResults(query)
                callback(count)
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error counting search results", e)
                callback(0)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchFilters.value = SearchFilters()
    }

    //events to bookmark an aya, favorite an aya, add a note to an aya
    sealed class AyaEvent {

        data class BookmarkAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val bookmark: Boolean,
        ) : AyaEvent()

        data class FavoriteAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val favorite: Boolean,
        ) : AyaEvent()

        data class AddNoteToAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val note: String,
        ) : AyaEvent()

        data class getNoteForAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        //get all bookmarks
        object getBookmarks : AyaEvent()

        //get all favorites
        object getFavorites : AyaEvent()

        //get all notes
        object getNotes : AyaEvent()

        //addAudioToAya
        class addAudioToAya(
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val audio: String,
        ) : AyaEvent()

        //delete a note from an aya
        class deleteNoteFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        //delete a bookmark from an aya
        class deleteBookmarkFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        //delete a favorite from an aya
        class deleteFavoriteFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        class getSurahById(val id: Int) : AyaEvent()

        // NEW: Search events
        data class SearchAyas(val query: String) : AyaEvent()
        data class SearchInLanguage(val query: String, val language: String) : AyaEvent()
        data class SearchWithFilters(val query: String, val filters: SearchFilters) : AyaEvent()
        object ClearSearch : AyaEvent()
    }

    //events handler
    fun handleAyaEvent(ayaEvent: AyaEvent) {
        when (ayaEvent) {
            is AyaEvent.BookmarkAya -> {
                bookmarkAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.bookmark
                )
            }

            is AyaEvent.FavoriteAya -> {
                favoriteAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.favorite
                )
            }

            is AyaEvent.AddNoteToAya -> {
                addNoteToAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.note
                )
            }

            is AyaEvent.getNoteForAya -> {
                getNoteForAya(ayaEvent.ayaNumber, ayaEvent.surahNumber, ayaEvent.ayaNumberInSurah)
            }

            is AyaEvent.getBookmarks -> {
                getAllBookmarks()
            }

            is AyaEvent.getFavorites -> {
                getAllFavorites()
            }

            is AyaEvent.getNotes -> {
                getAllNotes()
            }

            is AyaEvent.addAudioToAya -> {
                addAudioToAya(
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.audio
                )
            }

            is AyaEvent.deleteNoteFromAya -> {
                deleteNoteFromAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah
                )
            }

            is AyaEvent.deleteBookmarkFromAya -> {
                deleteBookmarkFromAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah
                )
            }

            is AyaEvent.deleteFavoriteFromAya -> {
                deleteFavoriteFromAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah
                )
            }

            is AyaEvent.getSurahById -> {
                getSurahById(ayaEvent.id)
            }

            // NEW: Handle search events
            is AyaEvent.SearchAyas -> {
                setSearchQuery(ayaEvent.query)
                searchAyas(ayaEvent.query)
            }

            is AyaEvent.SearchInLanguage -> {
                setSearchLanguage(ayaEvent.language)
                setSearchQuery(ayaEvent.query)
                searchAyas(ayaEvent.query)
            }

            is AyaEvent.SearchWithFilters -> {
                setSearchQuery(ayaEvent.query)
                updateSearchFilters(ayaEvent.filters)
                searchAyasAdvanced()
            }

            is AyaEvent.ClearSearch -> {
                clearSearch()
            }
        }
    }

    //add audio to aya
    private fun addAudioToAya(
        surahNumber: Int,
        ayaNumberInSurah: Int,
        audio: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.addAudioToAya(surahNumber, ayaNumberInSurah, audio)
            } catch (e: Exception) {
                Log.d("addAudioToAya", e.message ?: "Unknown error")
            }
        }
    }

    //bookmark an aya
    private fun bookmarkAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        bookmark: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.bookmarkAya(ayaNumber, surahNumber, ayaNumberInSurah, bookmark)
                // Refresh bookmarks list
                getAllBookmarks()
                // Update the current search results to reflect the change
                if (_searchResults.value.isNotEmpty()) {
                    val updatedResults = _searchResults.value.map { aya ->
                        if (aya.ayaNumberInQuran == ayaNumber &&
                            aya.suraNumber == surahNumber &&
                            aya.ayaNumberInSurah == ayaNumberInSurah) {
                            // Create a copy of the aya with updated bookmark status
                            aya.copy(bookmark = bookmark)
                        } else {
                            aya
                        }
                    }
                    _searchResults.value = updatedResults
                }
                // Update search results if currently showing bookmarks
                if (_searchLanguage.value == "Bookmarks" && _searchQuery.value.isNotBlank()) {
                    searchInBookmarks(_searchQuery.value)
                }
            } catch (e: Exception) {
                Log.d("bookmarkAya", e.message ?: "Unknown error")
            }
        }
    }

    //favorite an aya
    private fun favoriteAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        favorite: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.favoriteAya(ayaNumber, surahNumber, ayaNumberInSurah, favorite)
                // Refresh favorites list
                getAllFavorites()
                // Update the current search results to reflect the change
                if (_searchResults.value.isNotEmpty()) {
                    val updatedResults = _searchResults.value.map { aya ->
                        if (aya.ayaNumberInQuran == ayaNumber &&
                            aya.suraNumber == surahNumber &&
                            aya.ayaNumberInSurah == ayaNumberInSurah) {
                            // Create a copy of the aya with updated bookmark status
                            aya.copy(favorite = favorite)
                        } else {
                            aya
                        }
                    }
                    _searchResults.value = updatedResults
                }
                // Update search results if currently showing favorites
                if (_searchLanguage.value == "Favorites" && _searchQuery.value.isNotBlank()) {
                    searchInFavorites(_searchQuery.value)
                }
            } catch (e: Exception) {
                Log.d("favoriteAya", e.message ?: "Unknown error")
            }
        }
    }

    //add a note to an aya
    private fun addNoteToAya(id: Int, surahNumber: Int, ayaNumberInSurah: Int, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.addNoteToAya(id, surahNumber, ayaNumberInSurah, note)
                // Refresh notes list
                getAllNotes()
                // Update search results if currently showing notes
                if (_searchLanguage.value == "Notes" && _searchQuery.value.isNotBlank()) {
                    searchInNotes(_searchQuery.value)
                }
            } catch (e: Exception) {
                Log.d("addNoteToAya", e.message ?: "Unknown error")
            }
        }
    }

    //get a note for an aya
    private fun getNoteForAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                dataStore.getNoteOfAya(ayaNumber, surahNumber, ayaNumberInSurah)
            } catch (e: Exception) {
                Log.d("getNoteForAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun deleteNoteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.deleteNoteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val notes = dataStore.getAyasWithNotes()
                _notes.value = notes
                // Update search results if currently showing notes
                if (_searchLanguage.value == "Notes" && _searchQuery.value.isNotBlank()) {
                    searchInNotes(_searchQuery.value)
                    searchAyas(_searchQuery.value)
                }
            } catch (e: Exception) {
                Log.d("deleteNoteFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val bookmarks = dataStore.getBookmarkedAyas()
                _bookmarks.value = bookmarks
                // Update search results if currently showing bookmarks
                if (_searchLanguage.value == "Bookmarks" && _searchQuery.value.isNotBlank()) {
                    searchInBookmarks(_searchQuery.value)
                    searchAyas(_searchQuery.value)
                }
            } catch (e: Exception) {
                Log.d("deleteBookmarkFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun deleteFavoriteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.deleteFavoriteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val favorites = dataStore.getFavoritedAyas()
                _favorites.value = favorites
                // Update search results if currently showing favorites
                if (_searchLanguage.value == "Favorites" && _searchQuery.value.isNotBlank()) {
                    searchInFavorites(_searchQuery.value)
                    searchAyas(_searchQuery.value)
                }
            } catch (e: Exception) {
                Log.d("deleteFavoriteFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun getAllNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val notes = dataStore.getAyasWithNotes()
                _notes.value = notes
            } catch (e: Exception) {
                Log.d("getAllNotes", e.message ?: "Unknown error")
            }
        }
    }

    private fun getAllFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favorites = dataStore.getFavoritedAyas()
                _favorites.value = favorites
            } catch (e: Exception) {
                Log.d("getAllFavorites", e.message ?: "Unknown error")
            }
        }
    }

    private fun getAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bookmarks = dataStore.getBookmarkedAyas()
                _bookmarks.value = bookmarks
            } catch (e: Exception) {
                Log.d("getAllBookmarks", e.message ?: "Unknown error")
            }
        }
    }

    private fun getSurahById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val surah = dataStore.getSurahById(id)
                _surahState.value = surah
            } catch (e: Exception) {
                Log.d("getSurahById", e.message ?: "Unknown error")
            }
        }
    }
}