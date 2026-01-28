package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.audio.AudioState
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReadingMode { SURAH, JUZ, PAGE }

data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val filteredSurahs: List<Surah> = emptyList(),
    val searchQuery: String = "",
    val topTab: Int = 0, // 0 = Home, 1 = Browse
    val selectedTab: Int = 0, // Browse sub-tab: Surah/Juz/Page/Favorites
    val readingProgress: ReadingProgress? = null,
    val favorites: List<QuranFavorite> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class QuranReaderUiState(
    val readingMode: ReadingMode = ReadingMode.SURAH,
    val surahWithAyahs: SurahWithAyahs? = null,
    val ayahs: List<Ayah> = emptyList(),
    val title: String = "",
    val subtitle: String = "",
    val currentAyahIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val selectedTranslatorId: String = "en.sahih",
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 28f,
    val keepScreenOn: Boolean = true,
    val continuousReading: Boolean = true,
    val favoriteAyahIds: Set<Int> = emptySet()
)

data class QuranSearchUiState(
    val query: String = "",
    val results: List<QuranSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

data class QuranBookmarksUiState(
    val bookmarks: List<QuranBookmark> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface QuranEvent {
    data class LoadSurah(val surahNumber: Int) : QuranEvent
    data class LoadJuz(val juzNumber: Int) : QuranEvent
    data class LoadPage(val pageNumber: Int) : QuranEvent
    data class Search(val query: String) : QuranEvent
    data class SetTopTab(val index: Int) : QuranEvent
    data class SetTab(val index: Int) : QuranEvent
    data class ToggleBookmark(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) : QuranEvent
    data class ToggleFavorite(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) : QuranEvent
    data class UpdateReadingPosition(val surah: Int, val ayah: Int, val page: Int, val juz: Int) : QuranEvent
    data object ToggleTranslation : QuranEvent
    data object ClearSearch : QuranEvent
    // Audio events
    data class PlaySurahAudio(val surahNumber: Int, val surahName: String) : QuranEvent
    data class PlayAyahAudio(val ayahGlobalId: Int, val surahNumber: Int, val ayahNumber: Int) : QuranEvent
    data object PauseAudio : QuranEvent
    data object StopAudio : QuranEvent
    data class PlaySurahFromInfo(val surahNumber: Int) : QuranEvent
    data class LoadSurahInfo(val surahNumber: Int) : QuranEvent
}

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    val audioManager: QuranAudioManager,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _homeState = MutableStateFlow(QuranHomeUiState())
    val homeState: StateFlow<QuranHomeUiState> = _homeState.asStateFlow()

    private val _readerState = MutableStateFlow(QuranReaderUiState())
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(QuranSearchUiState())
    val searchState: StateFlow<QuranSearchUiState> = _searchState.asStateFlow()

    private val _bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    val bookmarksState: StateFlow<QuranBookmarksUiState> = _bookmarksState.asStateFlow()

    private val _surahInfo = MutableStateFlow<SurahInfo?>(null)
    val surahInfo: StateFlow<SurahInfo?> = _surahInfo.asStateFlow()

    val audioState: StateFlow<AudioState> = audioManager.audioState

    init {
        loadSurahs()
        loadReadingProgress()
        loadBookmarks()
        loadFavorites()
        loadFavoriteAyahIds()
        loadQuranSettings()
    }

    fun onEvent(event: QuranEvent) {
        when (event) {
            is QuranEvent.LoadSurah -> loadSurah(event.surahNumber)
            is QuranEvent.LoadJuz -> loadJuz(event.juzNumber)
            is QuranEvent.LoadPage -> loadPage(event.pageNumber)
            is QuranEvent.Search -> search(event.query)
            is QuranEvent.SetTopTab -> _homeState.update { it.copy(topTab = event.index) }
            is QuranEvent.SetTab -> _homeState.update { it.copy(selectedTab = event.index) }
            is QuranEvent.ToggleBookmark -> toggleBookmark(event.ayahId, event.surahNumber, event.ayahNumber)
            is QuranEvent.ToggleFavorite -> toggleFavorite(event.ayahId, event.surahNumber, event.ayahNumber)
            is QuranEvent.UpdateReadingPosition -> updateReadingPosition(event.surah, event.ayah, event.page, event.juz)
            QuranEvent.ToggleTranslation -> {
                val newValue = !_readerState.value.showTranslation
                _readerState.update { it.copy(showTranslation = newValue) }
                viewModelScope.launch { preferencesDataStore.setShowTranslation(newValue) }
            }
            QuranEvent.ClearSearch -> {
                _searchState.update { QuranSearchUiState() }
                _homeState.update { it.copy(searchQuery = "", filteredSurahs = it.surahs) }
            }
            is QuranEvent.PlaySurahAudio -> playSurahAudio(event.surahNumber, event.surahName)
            is QuranEvent.PlayAyahAudio -> playAyahAudio(event.ayahGlobalId, event.surahNumber, event.ayahNumber)
            QuranEvent.PauseAudio -> audioManager.togglePlayPause()
            QuranEvent.StopAudio -> audioManager.stop()
            is QuranEvent.PlaySurahFromInfo -> playSurahFromInfo(event.surahNumber)
            is QuranEvent.LoadSurahInfo -> loadSurahInfo(event.surahNumber)
        }
    }

    private fun loadQuranSettings() {
        viewModelScope.launch {
            val translatorId = preferencesDataStore.quranTranslatorId.first()
            val showTranslation = preferencesDataStore.showTranslation.first()
            val showTransliteration = preferencesDataStore.showTransliteration.first()
            val arabicFontSize = preferencesDataStore.quranArabicFontSize.first()
            val translationFontSize = preferencesDataStore.quranTranslationFontSize.first()
            val continuousReading = preferencesDataStore.continuousReading.first()
            val keepScreenOn = preferencesDataStore.keepScreenOn.first()

            val reciterId = preferencesDataStore.selectedReciterId.first()
            audioManager.setReciter(reciterId)

            _readerState.update {
                it.copy(
                    selectedTranslatorId = translatorId,
                    showTranslation = showTranslation,
                    showTransliteration = showTransliteration,
                    arabicFontSize = arabicFontSize,
                    fontSize = translationFontSize,
                    continuousReading = continuousReading,
                    keepScreenOn = keepScreenOn
                )
            }
        }
    }

    fun refreshSettings() {
        loadQuranSettings()
        _readerState.value.surahWithAyahs?.let { current ->
            if (_readerState.value.readingMode == ReadingMode.SURAH) {
                loadSurah(current.surah.number)
            }
        }
    }

    private fun loadSurahInfo(surahNumber: Int) {
        viewModelScope.launch {
            _surahInfo.value = quranUseCases.getSurahInfo(surahNumber)
        }
    }

    private fun playSurahFromInfo(surahNumber: Int) {
        viewModelScope.launch {
            quranUseCases.getSurahWithAyahs(surahNumber, _readerState.value.selectedTranslatorId)
                .first()?.let { surahWithAyahs ->
                    val audioItems = surahWithAyahs.ayahs.map { ayah ->
                        QuranAudioManager.AyahAudioItem(
                            ayahGlobalId = ayah.id,
                            surahNumber = ayah.surahNumber,
                            ayahNumber = ayah.ayahNumber
                        )
                    }
                    audioManager.playSurah(surahNumber, surahWithAyahs.surah.nameEnglish, audioItems)
                }
        }
    }

    private fun playSurahAudio(surahNumber: Int, surahName: String) {
        val ayahs = _readerState.value.ayahs.ifEmpty {
            _readerState.value.surahWithAyahs?.ayahs ?: emptyList()
        }
        if (ayahs.isEmpty()) return
        val audioItems = ayahs.map { ayah ->
            QuranAudioManager.AyahAudioItem(
                ayahGlobalId = ayah.id,
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber
            )
        }
        audioManager.playSurah(surahNumber, surahName, audioItems)
    }

    private fun playAyahAudio(ayahGlobalId: Int, surahNumber: Int, ayahNumber: Int) {
        val ayahs = _readerState.value.ayahs.ifEmpty {
            _readerState.value.surahWithAyahs?.ayahs ?: emptyList()
        }
        val audioItems = ayahs.map { ayah ->
            QuranAudioManager.AyahAudioItem(
                ayahGlobalId = ayah.id,
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber
            )
        }
        val title = _readerState.value.title.ifEmpty { "Surah $surahNumber" }
        audioManager.playFromAyah(ayahGlobalId, audioItems, title)
    }

    private fun loadSurahs() {
        viewModelScope.launch {
            quranUseCases.getSurahList()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { surahs ->
                    _homeState.update { state ->
                        state.copy(
                            surahs = surahs,
                            filteredSurahs = filterSurahs(surahs, state.searchQuery),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadReadingProgress() {
        viewModelScope.launch {
            quranUseCases.getReadingProgress()
                .collect { progress ->
                    _homeState.update { it.copy(readingProgress = progress) }
                }
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            quranUseCases.getBookmarks()
                .collect { bookmarks ->
                    _bookmarksState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
                }
        }
    }

    private fun loadSurah(surahNumber: Int) {
        _readerState.update {
            it.copy(
                isLoading = true,
                error = null,
                readingMode = ReadingMode.SURAH,
                surahWithAyahs = null,
                ayahs = emptyList(),
                title = "",
                subtitle = ""
            )
        }
        viewModelScope.launch {
            quranUseCases.getSurahWithAyahs(surahNumber, _readerState.value.selectedTranslatorId)
                .collect { surahWithAyahs ->
                    _readerState.update {
                        it.copy(
                            surahWithAyahs = surahWithAyahs,
                            ayahs = surahWithAyahs?.ayahs ?: emptyList(),
                            title = surahWithAyahs?.surah?.nameEnglish ?: "",
                            subtitle = surahWithAyahs?.let { s ->
                                "Surah ${s.surah.number} \u2022 ${s.surah.numberOfAyahs} Ayahs"
                            } ?: "",
                            isLoading = false,
                            readingMode = ReadingMode.SURAH
                        )
                    }
                }
        }
    }

    private fun loadJuz(juzNumber: Int) {
        _readerState.update {
            it.copy(
                isLoading = true,
                error = null,
                readingMode = ReadingMode.JUZ,
                surahWithAyahs = null,
                ayahs = emptyList(),
                title = "",
                subtitle = ""
            )
        }
        viewModelScope.launch {
            quranUseCases.getAyahsByJuz(juzNumber)
                .collect { ayahs ->
                    _readerState.update {
                        it.copy(
                            ayahs = ayahs,
                            subtitle = "${ayahs.size} Ayahs",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadPage(pageNumber: Int) {
        _readerState.update {
            it.copy(
                isLoading = true,
                error = null,
                readingMode = ReadingMode.PAGE,
                surahWithAyahs = null,
                ayahs = emptyList(),
                title = "",
                subtitle = ""
            )
        }
        viewModelScope.launch {
            quranUseCases.getAyahsByPage(pageNumber)
                .collect { ayahs ->
                    _readerState.update {
                        it.copy(
                            ayahs = ayahs,
                            subtitle = "${ayahs.size} Ayahs",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun search(query: String) {
        _homeState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            _homeState.update { it.copy(filteredSurahs = filterSurahs(it.surahs, "")) }
            _searchState.update { QuranSearchUiState() }
            return
        }

        _homeState.update { state ->
            state.copy(filteredSurahs = filterSurahs(state.surahs, query))
        }

        _searchState.update { it.copy(query = query, isSearching = true) }
        viewModelScope.launch {
            quranUseCases.searchQuran(query, _readerState.value.selectedTranslatorId)
                .collect { results ->
                    _searchState.update { it.copy(results = results, isSearching = false) }
                }
        }
    }

    private fun filterSurahs(surahs: List<Surah>, query: String): List<Surah> {
        return surahs.filter { surah ->
            query.isBlank() ||
            surah.nameEnglish.contains(query, ignoreCase = true) ||
            surah.nameTransliteration.contains(query, ignoreCase = true) ||
            surah.nameArabic.contains(query)
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            quranUseCases.getFavorites()
                .collect { favorites ->
                    _homeState.update { it.copy(favorites = favorites) }
                }
        }
    }

    private fun loadFavoriteAyahIds() {
        viewModelScope.launch {
            quranUseCases.getFavoriteAyahIds()
                .collect { ids ->
                    _readerState.update { it.copy(favoriteAyahIds = ids.toSet()) }
                }
        }
    }

    private fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranUseCases.toggleFavorite(ayahId, surahNumber, ayahNumber)
        }
    }

    private fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        // Optimistic local update so the icon changes immediately
        _readerState.update { state ->
            val updatedSurah = state.surahWithAyahs?.let { swa ->
                swa.copy(
                    ayahs = swa.ayahs.map { a ->
                        if (a.id == ayahId) a.copy(isBookmarked = !a.isBookmarked) else a
                    }
                )
            }
            val updatedAyahs = state.ayahs.map { a ->
                if (a.id == ayahId) a.copy(isBookmarked = !a.isBookmarked) else a
            }
            state.copy(surahWithAyahs = updatedSurah, ayahs = updatedAyahs)
        }
        viewModelScope.launch {
            quranUseCases.toggleBookmark(ayahId, surahNumber, ayahNumber)
        }
    }

    private fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int) {
        viewModelScope.launch {
            quranUseCases.updateReadingPosition(surah, ayah, page, juz)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
