package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.KhatamProgress
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.ReadingProgress
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
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
    // Remove the old simple StateFlow
    // val searchResults: StateFlow<List<LocalAya>> = _searchResults.asStateFlow()

    private val _searchLanguage = MutableStateFlow("All") // All, Arabic, English, Urdu
    val searchLanguage: StateFlow<String> = _searchLanguage.asStateFlow()

    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    // Debounced search logic inside ViewModel
    init {
        // Observe search query changes and trigger search with debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce
                .collect { query ->
                    if (query.isNotBlank()) {
                        searchAyasInternal(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
        
        // ... existing init code
        _translation.value =
            sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE, "English")
        getSurahList()
        getJuzList()
        getAllBookmarks()
        getAllFavorites()
        getAllNotes()
        getAllReadingProgress()
        loadActiveKhatam()
        loadKhatamHistory()
        loadAllKhatams()
    }
    
    // Expose searchResults using stateIn for UI consumption
    val filteredSearchResults: StateFlow<List<LocalAya>> = _searchResults.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )


    data class KhatamState(
        val activeKhatam: KhatamSession? = null,
        val khatamHistory: List<KhatamSession> = emptyList(),
        val allKhatams: List<KhatamSession> = emptyList(),
        val isLoadingKhatam: Boolean = false,
        val khatamError: String? = null,
        val showKhatamDialog: Boolean = false,
        val showEditDialog: Boolean = false,
        val showDeleteDialog: Boolean = false,
        val editingKhatam: KhatamSession? = null,
        val todayProgress: Int = 0,
        val totalQuranAyas: Int = 6236
    )

    //state for bookmarking, favoriting, adding a note
    private val _bookmarks = MutableStateFlow(listOf<LocalAya>())
    val bookmarks = _bookmarks.asStateFlow()

    private val _favorites = MutableStateFlow(listOf<LocalAya>())
    val favorites = _favorites.asStateFlow()

    private val _notes = MutableStateFlow(listOf<LocalAya>())
    val notes = _notes.asStateFlow()

    private val _readingProgress = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val readingProgress = _readingProgress.asStateFlow()

    private val _khatamState = MutableStateFlow(KhatamState())
    val khatamState = _khatamState.asStateFlow()

    data class SearchFilters(
        val surahNumber: Int? = null,
        val juzNumber: Int? = null,
        val isFavorite: Boolean? = null,
        val isBookmarked: Boolean? = null,
        val hasNote: Boolean? = null
    )

    // ... [EXISTING FUNCTIONS UNCHANGED: getAllReadingProgress, deleteReadingProgress, clearAllReadingProgress, getSurahList, getJuzList]

    fun getAllReadingProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val progressList = dataStore.getAllProgressOrderedByCompletion()
                _readingProgress.value = progressList
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error loading reading progress", e)
            }
        }
    }

    fun deleteReadingProgress(progress: ReadingProgress) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.deleteReadingProgress(progress)
                getAllReadingProgress() // Refresh
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error deleting reading progress", e)
            }
        }
    }

    fun clearAllReadingProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.clearAllReadingProgress()
                getAllReadingProgress() // Refresh
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error clearing reading progress", e)
            }
        }
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
        // Immediate clear if empty
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        }
    }

    fun setSearchLanguage(language: String) {
        _searchLanguage.value = language
        // Trigger search again with new language if query exists
        if (_searchQuery.value.isNotBlank()) {
             searchAyasInternal(_searchQuery.value)
        }
    }

    fun updateSearchFilters(filters: SearchFilters) {
        _searchFilters.value = filters
    }
    
    // Internal function to perform the actual search logic
    private fun searchAyasInternal(query: String) {
        // If advanced filters are active, use that path
        val filters = _searchFilters.value
        if (filters.surahNumber != null || filters.juzNumber != null || 
            filters.isFavorite == true || filters.isBookmarked == true || filters.hasNote == true) {
            searchAyasAdvancedInternal(query, filters)
            return
        }

        // Standard search
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val results = when (_searchLanguage.value) {
                    "Arabic" -> dataStore.searchAyasInArabic(query)
                    "English" -> dataStore.searchAyasInEnglish(query)
                    "Urdu" -> dataStore.searchAyasInUrdu(query)
                    "Favorites" -> dataStore.searchFavoriteAyas(query)
                    "Bookmarks" -> dataStore.searchBookmarkedAyas(query)
                    "Notes" -> dataStore.searchAyasWithNotes(query)
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
    
    private fun searchAyasAdvancedInternal(query: String, filters: SearchFilters) {
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

    fun searchAyas(query: String) {
        setSearchQuery(query)
        // Manual search trigger (like pressing enter) overrides debounce
        searchAyasInternal(query)
    }

    fun searchAyasAdvanced() {
        val query = _searchQuery.value
        val filters = _searchFilters.value

        if (query.isBlank()) {
            clearSearch()
            return
        }
        
        searchAyasAdvancedInternal(query, filters)
    }

    fun searchInFavorites(query: String) {
        setSearchQuery(query)
        setSearchLanguage("Favorites")
        // Trigger logic handled by setSearchLanguage -> searchAyasInternal
    }

    fun searchInBookmarks(query: String) {
        setSearchQuery(query)
        setSearchLanguage("Bookmarks")
        // Trigger logic handled by setSearchLanguage -> searchAyasInternal
    }

    fun searchInNotes(query: String) {
        setSearchQuery(query)
        setSearchLanguage("Notes")
        // Trigger logic handled by setSearchLanguage -> searchAyasInternal
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
        _loadingState.value = false
        _errorState.value = ""
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
        
        //get all reading progress
        object GetReadingProgress : AyaEvent()

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

        data class StartNewKhatam(
            val name: String,
            val targetDate: String? = null,
            val dailyTarget: Int? = null
        ) : AyaEvent()

        data class UpdateKhatamProgress(
            val khatamId: Long,
            val surahNumber: Int,
            val ayaNumber: Int
        ) : AyaEvent()

        data class CompleteKhatam(val khatamId: Long) : AyaEvent()
        data class PauseKhatam(val khatamId: Long) : AyaEvent()
        data class ResumeKhatam(val khatamId: Long) : AyaEvent()
        data class DeleteKhatam(val khatamId: Long) : AyaEvent()
        object LoadActiveKhatam : AyaEvent()
        object LoadKhatamHistory : AyaEvent()
        object LoadAllKhatams : AyaEvent()
        object RefreshKhatamData : AyaEvent()  // NEW: Refresh all khatam data
        data class ShowKhatamDialog(val show: Boolean) : AyaEvent()
        data class ShowEditKhatamDialog(val show: Boolean, val khatam: KhatamSession? = null) : AyaEvent()
        data class ShowDeleteKhatamDialog(val show: Boolean, val khatam: KhatamSession? = null) : AyaEvent()
        data class UpdateKhatam(val khatam: KhatamSession) : AyaEvent()
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
            
            is AyaEvent.GetReadingProgress -> {
                getAllReadingProgress()
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
                searchAyas(ayaEvent.query)
            }

            is AyaEvent.SearchInLanguage -> {
                setSearchLanguage(ayaEvent.language)
                searchAyas(ayaEvent.query)
            }

            is AyaEvent.SearchWithFilters -> {
                updateSearchFilters(ayaEvent.filters)
                searchAyasAdvanced()
            }

            is AyaEvent.ClearSearch -> {
                clearSearch()
            }
            is AyaEvent.StartNewKhatam -> startNewKhatam(ayaEvent.name, ayaEvent.targetDate, ayaEvent.dailyTarget)
            is AyaEvent.UpdateKhatamProgress -> updateKhatamProgress(ayaEvent.khatamId, ayaEvent.surahNumber, ayaEvent.ayaNumber)
            is AyaEvent.CompleteKhatam -> completeKhatam(ayaEvent.khatamId)
            is AyaEvent.PauseKhatam -> pauseKhatam(ayaEvent.khatamId)
            is AyaEvent.ResumeKhatam -> resumeKhatam(ayaEvent.khatamId)
            is AyaEvent.DeleteKhatam -> deleteKhatam(ayaEvent.khatamId)
            is AyaEvent.LoadActiveKhatam -> loadActiveKhatam()
            is AyaEvent.LoadKhatamHistory -> loadKhatamHistory()
            is AyaEvent.LoadAllKhatams -> loadAllKhatams()
            is AyaEvent.RefreshKhatamData -> refreshKhatamData()  // NEW
            is AyaEvent.ShowKhatamDialog -> showKhatamDialog(ayaEvent.show)
            is AyaEvent.ShowEditKhatamDialog -> showEditKhatamDialog(ayaEvent.show, ayaEvent.khatam)
            is AyaEvent.ShowDeleteKhatamDialog -> showDeleteKhatamDialog(ayaEvent.show, ayaEvent.khatam)
            is AyaEvent.UpdateKhatam -> updateKhatam(ayaEvent.khatam)
        }
    }

    private fun startNewKhatam(name: String, targetDate: String?, dailyTarget: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _khatamState.update { it.copy(isLoadingKhatam = true) }

                val newKhatam = KhatamSession(
                    name = name,
                    startDate = java.time.LocalDate.now().toString(),
                    targetCompletionDate = targetDate,
                    dailyTarget = dailyTarget
                )

                dataStore.insertKhatam(newKhatam)
                loadActiveKhatam()

                _khatamState.update {
                    it.copy(
                        isLoadingKhatam = false,
                        showKhatamDialog = false
                    )
                }
            } catch (e: Exception) {
                _khatamState.update {
                    it.copy(
                        isLoadingKhatam = false,
                        khatamError = e.message
                    )
                }
                Log.e("QuranViewModel", "Error starting new khatam", e)
            }
        }
    }

    private fun updateKhatamProgress(khatamId: Long, surahNumber: Int, ayaNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = java.time.LocalDate.now().toString()

                // Insert progress entry
                val progress = KhatamProgress(
                    khatamId = khatamId,
                    surahNumber = surahNumber,
                    ayaNumber = ayaNumber,
                    dateRead = today
                )
                dataStore.insertKhatamProgress(progress)

                // Calculate total ayas read (simplified calculation)
                val totalRead = calculateTotalAyasRead(surahNumber, ayaNumber)

                // Update khatam session
                dataStore.updateKhatamProgress(khatamId, surahNumber, ayaNumber, totalRead)

                // Check if khatam is complete
                if (totalRead >= 6236) {
                    completeKhatam(khatamId)
                } else {
                    loadActiveKhatam()
                }

                loadTodayProgress(khatamId)

            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error updating khatam progress", e)
            }
        }
    }

    private fun completeKhatam(khatamId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val completionDate = java.time.LocalDate.now().toString()
                dataStore.completeKhatam(khatamId, completionDate)
                loadActiveKhatam()
                loadKhatamHistory()
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error completing khatam", e)
            }
        }
    }

    private fun pauseKhatam(khatamId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.pauseKhatam(khatamId)
                loadActiveKhatam()
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error pausing khatam", e)
            }
        }
    }

    private fun resumeKhatam(khatamId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.resumeKhatam(khatamId)
                loadActiveKhatam()
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error resuming khatam", e)
            }
        }
    }

    private fun deleteKhatam(khatamId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val khatam = dataStore.getKhatamById(khatamId)
                khatam?.let {
                    dataStore.deleteKhatam(it)
                    loadActiveKhatam()
                    loadKhatamHistory()
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error deleting khatam", e)
            }
        }
    }

    private fun loadActiveKhatam() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeKhatam = dataStore.getActiveKhatam()
                _khatamState.update { it.copy(activeKhatam = activeKhatam) }

                activeKhatam?.let { khatam ->
                    loadTodayProgress(khatam.id)
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error loading active khatam", e)
            }
        }
    }

    private fun loadKhatamHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = dataStore.getCompletedKhatams()
                _khatamState.update { it.copy(khatamHistory = history) }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error loading khatam history", e)
            }
        }
    }

    private fun loadTodayProgress(khatamId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = java.time.LocalDate.now().toString()
                val todayProgress = dataStore.getAyasReadToday(khatamId, today)
                _khatamState.update { it.copy(todayProgress = todayProgress) }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error loading today's progress", e)
            }
        }
    }

    fun showKhatamDialog(show: Boolean) {
        _khatamState.update { it.copy(showKhatamDialog = show) }
    }

    private fun showEditKhatamDialog(show: Boolean, khatam: KhatamSession?) {
        _khatamState.update { it.copy(showEditDialog = show, editingKhatam = khatam) }
    }

    private fun showDeleteKhatamDialog(show: Boolean, khatam: KhatamSession?) {
        _khatamState.update { it.copy(showDeleteDialog = show, editingKhatam = khatam) }
    }

    private fun updateKhatam(khatam: KhatamSession) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.updateKhatam(khatam)
                loadActiveKhatam()
                loadKhatamHistory()
                loadAllKhatams()
                _khatamState.update { it.copy(showEditDialog = false, editingKhatam = null) }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error updating khatam", e)
            }
        }
    }

    private fun loadAllKhatams() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allKhatams = dataStore.getAllKhatamSessions()
                _khatamState.update { it.copy(allKhatams = allKhatams) }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error loading all khatams", e)
            }
        }
    }

    // Helper method to calculate total ayas read based on current position using database data
    private fun calculateTotalAyasRead(surahNumber: Int, ayaNumber: Int): Int {
        if (surahNumber < 1 || surahNumber > 114 || ayaNumber < 1) return 0

        // Try to find the aya in available data
        // This calculation should match the one in AyatViewModel
        // Using: startAya (first aya in Quran) - 1 + ayaNumber

        // Look up the surah from our state if available
        val surah = _surahListState.value.find { it.number == surahNumber }
        if (surah != null) {
            // startAya - 1 gives position in Quran, then add the aya within surah
            return (surah.startAya - 1) + ayaNumber
        }

        // Fallback to simple calculation if no surah data available
        return 0
    }

    fun getSurahAndAyaFromTotal(totalAyas: Int): Pair<Int, Int> {
        if (totalAyas <= 0) return Pair(1, 1)

        var remaining = totalAyas
        for (surah in _surahListState.value) {
            if (remaining <= surah.numberOfAyahs) {
                return Pair(surah.number, remaining)
            }
            remaining -= surah.numberOfAyahs
        }

        // Fallback to end of Quran
        return Pair(114, 6)
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
                
                // CRITICAL FIX: Refresh the bookmarks list immediately
                val updatedBookmarks = dataStore.getBookmarkedAyas()
                _bookmarks.value = updatedBookmarks

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
                
                // CRITICAL FIX: Refresh the favorites list immediately
                val updatedFavorites = dataStore.getFavoritedAyas()
                _favorites.value = updatedFavorites
                
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

    // NEW: Refresh all khatam-related data
    private fun refreshKhatamData() {
        loadActiveKhatam()
        loadKhatamHistory()
        loadAllKhatams()
    }
}