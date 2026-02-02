package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.audio.AudioState
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReadingMode { SURAH, JUZ, PAGE }

data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val filteredSurahs: List<Surah> = emptyList(),
    val searchQuery: String = "",
    val topTab: Int = 0, // 0 = Home, 1 = Browse, 2 = Favorites, 3 = Bookmarks
    val selectedTab: Int = 0, // Browse sub-tab: 0=Surah, 1=Juz, 2=Page
    val readingProgress: ReadingProgress? = null,
    val favorites: List<QuranFavorite> = emptyList(),
    val activeKhatam: Khatam? = null,
    val khatamReadAyahIds: Set<Int> = emptySet(),
    val completedKhatamCount: Int = 0,
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
    val selectedTranslatorId: String = "sahih_international",
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 28f,
    val keepScreenOn: Boolean = true,
    val continuousReading: Boolean = true,
    val favoriteAyahIds: Set<Int> = emptySet(),
    val pageCache: Map<Int, List<Ayah>> = emptyMap(),
    val showTajweed: Boolean = false,
    val activeKhatamId: Long? = null,
    val khatamReadAyahIds: Set<Int> = emptySet()
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
    data object ResumeAudio : QuranEvent
    data object StopAudio : QuranEvent
    data class PlaySurahFromInfo(val surahNumber: Int) : QuranEvent
    data class LoadSurahInfo(val surahNumber: Int) : QuranEvent
    data class MarkAyahsReadForKhatam(val ayahIds: List<Int>) : QuranEvent
    data class UnmarkAyahReadForKhatam(val ayahId: Int) : QuranEvent
    data class ToggleKhatamAyah(val ayahId: Int) : QuranEvent
    data class MarkSurahAsReadForKhatam(val surahNumber: Int) : QuranEvent
}

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    val audioManager: QuranAudioManager,
    private val preferencesDataStore: PreferencesDataStore,
    private val khatamUseCases: KhatamUseCases
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

    // Debounced search support
    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        loadSurahs()
        loadReadingProgress()
        loadBookmarks()
        loadFavorites()
        loadFavoriteAyahIds()
        observeQuranSettings()
        setupDebouncedSearch()
        observeActiveKhatam()
        observeActiveKhatamForHome()
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedSearch() {
        searchQueryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
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
            QuranEvent.ResumeAudio -> audioManager.togglePlayPause()
            QuranEvent.StopAudio -> audioManager.stop()
            is QuranEvent.PlaySurahFromInfo -> playSurahFromInfo(event.surahNumber)
            is QuranEvent.LoadSurahInfo -> loadSurahInfo(event.surahNumber)
            is QuranEvent.MarkAyahsReadForKhatam -> markAyahsReadForKhatam(event.ayahIds)
            is QuranEvent.UnmarkAyahReadForKhatam -> unmarkAyahReadForKhatam(event.ayahId)
            is QuranEvent.ToggleKhatamAyah -> toggleKhatamAyah(event.ayahId)
            is QuranEvent.MarkSurahAsReadForKhatam -> markSurahAsReadForKhatam(event.surahNumber)
        }
    }

    private fun observeQuranSettings() {
        viewModelScope.launch {
            // Split into two groups of 4 to use typed combine overloads
            val displayFlow = combine(
                preferencesDataStore.quranTranslatorId,
                preferencesDataStore.showTranslation,
                preferencesDataStore.showTransliteration,
                preferencesDataStore.quranArabicFontSize
            ) { translatorId: String, showTrans: Boolean, showTranslit: Boolean, arabicSize: Float ->
                QuranDisplaySettings(translatorId, showTrans, showTranslit, arabicSize)
            }

            val behaviorFlow = combine(
                preferencesDataStore.quranTranslationFontSize,
                preferencesDataStore.continuousReading,
                preferencesDataStore.keepScreenOn,
                preferencesDataStore.selectedReciterId
            ) { transSize: Float, continuous: Boolean, keepOn: Boolean, reciter: String? ->
                QuranBehaviorSettings(transSize, continuous, keepOn, reciter)
            }

            combine(displayFlow, behaviorFlow, preferencesDataStore.showTajweed) { display, behavior, showTajweed ->
                Triple(display, behavior, showTajweed)
            }.collect { (display, behavior, showTajweed) ->
                audioManager.setReciter(behavior.reciterId)
                _readerState.update {
                    it.copy(
                        selectedTranslatorId = display.translatorId,
                        showTranslation = display.showTranslation,
                        showTransliteration = display.showTransliteration,
                        arabicFontSize = display.arabicFontSize,
                        fontSize = behavior.translationFontSize,
                        continuousReading = behavior.continuousReading,
                        keepScreenOn = behavior.keepScreenOn,
                        showTajweed = showTajweed
                    )
                }
            }
        }
    }

    private data class QuranDisplaySettings(
        val translatorId: String,
        val showTranslation: Boolean,
        val showTransliteration: Boolean,
        val arabicFontSize: Float
    )

    private data class QuranBehaviorSettings(
        val translationFontSize: Float,
        val continuousReading: Boolean,
        val keepScreenOn: Boolean,
        val reciterId: String?
    )

    fun refreshSettings() {
        // Settings are now observed reactively; just reload current surah if needed
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
                    // SurahInfoScreen always plays full surah continuously
                    audioManager.setContinuousPlayback(true)
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
        // Respect continuousReading setting from QuranReaderScreen
        audioManager.setContinuousPlayback(_readerState.value.continuousReading)
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
        // Respect continuousReading setting from QuranReaderScreen
        audioManager.setContinuousPlayback(_readerState.value.continuousReading)
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
                title = "Juz $juzNumber",
                subtitle = ""
            )
        }
        viewModelScope.launch {
            quranUseCases.getAyahsByJuz(juzNumber, _readerState.value.selectedTranslatorId)
                .collect { ayahs ->
                    _readerState.update {
                        it.copy(
                            ayahs = ayahs,
                            title = "Juz $juzNumber",
                            subtitle = "${ayahs.size} Ayahs",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadPage(pageNumber: Int) {
        // Check cache first - no loading needed if already cached
        val cached = _readerState.value.pageCache[pageNumber]
        if (cached != null) {
            _readerState.update {
                it.copy(
                    ayahs = cached,
                    readingMode = ReadingMode.PAGE,
                    title = "Page $pageNumber",
                    subtitle = "${cached.size} Ayahs",
                    isLoading = false
                )
            }
            return
        }

        // Load from database - DON'T clear ayahs to prevent flicker
        _readerState.update {
            it.copy(
                error = null,
                readingMode = ReadingMode.PAGE,
                surahWithAyahs = null,
                // Keep existing ayahs to prevent flicker during page transition
                title = "Page $pageNumber",
                subtitle = ""
            )
        }
        viewModelScope.launch {
            quranUseCases.getAyahsByPage(pageNumber, _readerState.value.selectedTranslatorId)
                .collect { ayahs ->
                    _readerState.update {
                        it.copy(
                            ayahs = ayahs,
                            pageCache = it.pageCache + (pageNumber to ayahs),
                            subtitle = "${ayahs.size} Ayahs",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun search(query: String) {
        _homeState.update { it.copy(searchQuery = query) }

        // Always update filtered surahs immediately (no debounce needed for filtering)
        _homeState.update { state ->
            state.copy(filteredSurahs = filterSurahs(state.surahs, query))
        }

        if (query.isBlank()) {
            _searchState.update { QuranSearchUiState() }
            searchQueryFlow.value = ""
            return
        }

        // Mark as searching and trigger debounced search
        _searchState.update { it.copy(query = query, isSearching = true) }
        searchQueryFlow.value = query
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchState.update { QuranSearchUiState() }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            quranUseCases.searchQuran(query, _readerState.value.selectedTranslatorId)
                .collect { results ->
                    // Populate surah names and limit results to 50 for performance
                    val surahs = _homeState.value.surahs
                    val enrichedResults = results.take(50).map { result ->
                        if (result.surahName.isEmpty()) {
                            val surahName = surahs.find { it.number == result.ayah.surahNumber }?.nameEnglish
                                ?: "Surah ${result.ayah.surahNumber}"
                            result.copy(surahName = surahName)
                        } else {
                            result
                        }
                    }
                    _searchState.update { it.copy(results = enrichedResults, isSearching = false) }
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

    private fun observeActiveKhatam() {
        viewModelScope.launch {
            khatamUseCases.observeActiveKhatam().collect { khatam ->
                val khatamId = khatam?.id
                _readerState.update { it.copy(activeKhatamId = khatamId) }
                if (khatamId != null) {
                    // Start observing read ayah IDs for this khatam
                    khatamUseCases.observeReadAyahIds(khatamId).collect { ids ->
                        _readerState.update { it.copy(khatamReadAyahIds = ids) }
                        // Reactive completion check
                        if (ids.size >= Khatam.TOTAL_QURAN_AYAHS) {
                            khatamUseCases.completeKhatam(khatamId)
                        }
                    }
                } else {
                    _readerState.update { it.copy(khatamReadAyahIds = emptySet()) }
                }
            }
        }
    }

    private fun observeActiveKhatamForHome() {
        viewModelScope.launch {
            khatamUseCases.observeActiveKhatam().collect { khatam ->
                _homeState.update { it.copy(activeKhatam = khatam) }
                if (khatam != null) {
                    khatamUseCases.observeReadAyahIds(khatam.id).collect { ids ->
                        _homeState.update { it.copy(khatamReadAyahIds = ids) }
                    }
                } else {
                    _homeState.update { it.copy(khatamReadAyahIds = emptySet()) }
                }
            }
        }
        viewModelScope.launch {
            khatamUseCases.observeCompletedKhatams().collect { completed ->
                _homeState.update { it.copy(completedKhatamCount = completed.size) }
            }
        }
    }

    private fun toggleKhatamAyah(ayahId: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val isRead = ayahId in _readerState.value.khatamReadAyahIds
        viewModelScope.launch {
            if (isRead) {
                khatamUseCases.unmarkAyahRead(khatamId, ayahId)
            } else {
                khatamUseCases.markAyahsRead(khatamId, listOf(ayahId))
            }
        }
    }

    private fun markSurahAsReadForKhatam(surahNumber: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        viewModelScope.launch {
            khatamUseCases.markSurahAsRead(khatamId, surahNumber)
        }
    }

    private fun unmarkAyahReadForKhatam(ayahId: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        viewModelScope.launch {
            khatamUseCases.unmarkAyahRead(khatamId, ayahId)
        }
    }

    private fun markAyahsReadForKhatam(ayahIds: List<Int>) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val readIds = _readerState.value.khatamReadAyahIds
        val newIds = ayahIds.filter { it !in readIds }
        if (newIds.isEmpty()) return

        viewModelScope.launch {
            khatamUseCases.markAyahsRead(khatamId, newIds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
