package com.arshadshah.nimaz.viewModel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.KhatamProgress
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.ReadingProgress
import com.arshadshah.nimaz.repositories.SpacesFileRepository
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.QuranUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

// ============ STATE CLASSES ============

data class AyatState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val ayatList: List<LocalAya> = emptyList(),
    val currentSurah: LocalSurah? = null,
    val currentJuz: LocalJuz? = null,
    val scrollToAya: Int? = null,
    val displaySettings: DisplaySettings = DisplaySettings(),
    val audioState: AudioState = AudioState(),
    val selectedAya: LocalAya? = null,

    // Navigation-related states
    val showNavigationPanel: Boolean = false,
    val readingProgress: ReadingProgress? = null,
    val currentAyaIndex: Int = 0,
    val bookmarkedAyasInSurah: List<Int> = emptyList(),
    val favoriteAyasInSurah: List<Int> = emptyList(),
    val notedAyasInSurah: List<Int> = emptyList(),
    val totalAyasInSurah: Int = 0,

    val activeKhatam: KhatamSession? = null,
    val isKhatamMode: Boolean = false,
    val isUpdatingKhatam: Boolean = false,
    val khatamTodayAya: Int? = null,
    val khatamTodaySurah: Int? = null,

    // Page navigation
    val isPaginationMode: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val ayasPerPage: Int = 15  // Average ayas per Mushaf page
)

data class AudioState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentAudioPath: String = "",
    val currentPlayingAya: LocalAya? = null
)

// ============ VIEWMODEL ============

@HiltViewModel
class AyatViewModel @Inject constructor(
    private val dataStore: DataStore,
    private val spaceFilesRepository: SpacesFileRepository,
    private val preferences: PrivateSharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(AyatState())
    val state = _state.asStateFlow()

    private val mediaPlayer = MediaPlayer().apply {
        setOnCompletionListener {
            viewModelScope.launch {
                updateAudioState { it.copy(isPlaying = false, isPaused = false) }
            }
        }
    }

    init {
        loadInitialPreferences()
        loadNavigationData()
        loadActiveKhatam()
    }

    // ============ EVENTS ============

    sealed class AyatEvent {
        // Existing events
        data class LoadAyat(val number: Int, val isSurah: Boolean) : AyatEvent()
        data class UpdateDisplaySettings(val settings: DisplaySettings) : AyatEvent()
        data class ToggleBookmark(val aya: LocalAya) : AyatEvent()
        data class ToggleFavorite(val aya: LocalAya) : AyatEvent()
        data class UpdateNote(val aya: LocalAya, val note: String) : AyatEvent()
        data class SelectAya(val aya: LocalAya) : AyatEvent()
        data class PlayAudio(val aya: LocalAya) : AyatEvent()
        object PauseAudio : AyatEvent()
        object StopAudio : AyatEvent()
        data class DownloadAudio(val aya: LocalAya) : AyatEvent()
        object ClearError : AyatEvent()
        data class GetSurahById(val id: Int) : AyatEvent()

        // Navigation events
        object ToggleNavigationPanel : AyatEvent()
        data class JumpToAya(val ayaNumber: Int) : AyatEvent()
        object LoadNavigationData : AyatEvent()
        data class UpdateReadingProgress(val ayaNumber: Int) : AyatEvent()
        object NavigatePrevious : AyatEvent()
        object NavigateNext : AyatEvent()
        data class UpdateCurrentAyaIndex(val index: Int) : AyatEvent()
        object NavigateToNextSurah : AyatEvent()
        object NavigateToPreviousSurah : AyatEvent()

        // Page navigation events
        object TogglePaginationMode : AyatEvent()
        object NextPage : AyatEvent()
        object PreviousPage : AyatEvent()
        data class JumpToPage(val page: Int) : AyatEvent()

        data class UpdateKhatamProgress(val surahNumber: Int, val ayaNumber: Int) : AyatEvent()
        object LoadActiveKhatam : AyatEvent()
    }

    // ============ EVENT HANDLER ============

    fun handleEvent(event: AyatEvent) {
        when (event) {
            // Existing events
            is AyatEvent.LoadAyat -> loadAyat(event.number, event.isSurah)
            is AyatEvent.UpdateDisplaySettings -> updateDisplaySettings(event.settings)
            is AyatEvent.ToggleBookmark -> toggleBookmark(event.aya)
            is AyatEvent.ToggleFavorite -> toggleFavorite(event.aya)
            is AyatEvent.UpdateNote -> updateNote(event.aya, event.note)
            is AyatEvent.SelectAya -> selectAya(event.aya)
            is AyatEvent.PlayAudio -> playAudio(event.aya)
            is AyatEvent.PauseAudio -> pauseAudio()
            is AyatEvent.StopAudio -> stopAudio()
            is AyatEvent.DownloadAudio -> downloadAudio(event.aya)
            is AyatEvent.ClearError -> clearError()
            is AyatEvent.GetSurahById -> getSurahById(event.id)

            // Navigation events
            is AyatEvent.ToggleNavigationPanel -> toggleNavigationPanel()
            is AyatEvent.JumpToAya -> jumpToAya(event.ayaNumber)
            is AyatEvent.LoadNavigationData -> loadNavigationData()
            is AyatEvent.UpdateReadingProgress -> updateReadingProgress(event.ayaNumber)
            is AyatEvent.NavigatePrevious -> navigatePrevious()
            is AyatEvent.NavigateNext -> navigateNext()
            is AyatEvent.UpdateCurrentAyaIndex -> updateCurrentAyaIndex(event.index)
            is AyatEvent.NavigateToNextSurah -> navigateToNextSurah()
            is AyatEvent.NavigateToPreviousSurah -> navigateToPreviousSurah()

            // Page navigation events
            is AyatEvent.TogglePaginationMode -> togglePaginationMode()
            is AyatEvent.NextPage -> nextPage()
            is AyatEvent.PreviousPage -> previousPage()
            is AyatEvent.JumpToPage -> jumpToPage(event.page)

            is AyatEvent.UpdateKhatamProgress -> updateKhatamProgress(event.surahNumber, event.ayaNumber)
            is AyatEvent.LoadActiveKhatam -> loadActiveKhatam()
        }
    }

    // ============ KHATAM FUNCTIONS ============

    private fun loadActiveKhatam() {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val activeKhatam = dataStore.getActiveKhatam()

                Log.d("AyatViewModel", "Loading active khatam: ${activeKhatam?.name ?: "None"}")
                Log.d("AyatViewModel", "Khatam details: isActive=${activeKhatam?.isActive}, isCompleted=${activeKhatam?.isCompleted}")

                _state.update {
                    it.copy(
                        activeKhatam = activeKhatam,
                        isKhatamMode = activeKhatam != null
                    )
                }

                activeKhatam?.let { loadKhatamDailyProgress(it.id) }
            }
        }
    }

    private fun loadKhatamDailyProgress(khatamId: Long) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val today = LocalDate.now().toString()
                val todayProgress = dataStore.getProgressForDate(khatamId, today)

                // Get the latest progress entry for today based on total ayas read
                val latest = todayProgress.maxByOrNull {
                    calculateTotalAyasRead(it.surahNumber, it.ayaNumber)
                }

                // Store both surah and aya for proper comparison
                _state.update {
                    it.copy(
                        khatamTodayAya = latest?.ayaNumber,
                        khatamTodaySurah = latest?.surahNumber
                    )
                }

                Log.d("AyatViewModel", "Today's khatam progress: Surah ${latest?.surahNumber}, Aya ${latest?.ayaNumber}")
            }
        }
    }

    private fun updateKhatamProgress(surahNumber: Int, ayaNumber: Int) {
        viewModelScope.launch(ioDispatcher) {
            if (_state.value.isUpdatingKhatam) return@launch

            _state.update { it.copy(isUpdatingKhatam = true) }
            try {
                safeCall {
                    val activeKhatam = _state.value.activeKhatam ?: return@safeCall
                    val today = LocalDate.now().toString()
                    val todayEntries = dataStore.getProgressForDate(activeKhatam.id, today)

                    val newTotal = calculateTotalAyasRead(surahNumber, ayaNumber)
                    val existingTotal = todayEntries.maxOfOrNull {
                        calculateTotalAyasRead(it.surahNumber, it.ayaNumber)
                    } ?: -1

                    // Check if this is actually new progress
                    if (existingTotal >= newTotal) {
                        Log.d("AyatViewModel", "Progress already marked today: $existingTotal >= $newTotal")
                        val latest = todayEntries.maxByOrNull {
                            calculateTotalAyasRead(it.surahNumber, it.ayaNumber)
                        }
                        _state.update {
                            it.copy(
                                khatamTodayAya = latest?.ayaNumber,
                                khatamTodaySurah = latest?.surahNumber
                            )
                        }
                        return@safeCall
                    }

                    val progress = KhatamProgress(
                        khatamId = activeKhatam.id,
                        surahNumber = surahNumber,
                        ayaNumber = ayaNumber,
                        dateRead = today,
                        timestamp = LocalDateTime.now().toString()
                    )

                    dataStore.insertKhatamProgress(progress)
                    dataStore.updateKhatamProgress(activeKhatam.id, surahNumber, ayaNumber, newTotal)

                    _state.update {
                        it.copy(
                            khatamTodayAya = ayaNumber,
                            khatamTodaySurah = surahNumber
                        )
                    }

                    Log.d("AyatViewModel", "Khatam progress updated: Surah $surahNumber, Aya $ayaNumber (Total: $newTotal)")

                    loadActiveKhatam()
                }
            } finally {
                _state.update { it.copy(isUpdatingKhatam = false) }
            }
        }
    }

    // Surah aya counts for accurate progress calculation (Surah 1-114)
    private val surahAyaCounts = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,   // 1-10
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,    // 11-20
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,       // 21-30
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,         // 31-40
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,          // 41-50
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,          // 51-60
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,          // 61-70
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,          // 71-80
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,          // 81-90
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,               // 91-100
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,                   // 101-110
        5, 4, 5, 6                                        // 111-114
    )

    private fun calculateTotalAyasRead(surahNumber: Int, ayaNumber: Int): Int {
        if (surahNumber < 1 || surahNumber > 114) return 0

        // Sum all ayas from surahs before current surah
        var total = 0
        for (i in 0 until (surahNumber - 1)) {
            total += surahAyaCounts[i]
        }
        // Add the ayas read in current surah
        total += ayaNumber
        return total
    }

    // ============ NAVIGATION METHODS ============

    private fun toggleNavigationPanel() {
        _state.update {
            it.copy(showNavigationPanel = !it.showNavigationPanel)
        }
    }

    private fun jumpToAya(ayaNumber: Int) {
        viewModelScope.launch {
            Log.d("JumpToAya", "Jumping to Aya: $ayaNumber")

            val currentList = _state.value.ayatList
            Log.d("JumpToAya", "Total ayas in list: ${currentList.size}")

            // Find the correct index for the target aya number
            val targetIndex = currentList.indexOfFirst { aya ->
                aya.ayaNumberInSurah == ayaNumber
            }

            Log.d("JumpToAya", "Target Index: $targetIndex for aya number: $ayaNumber")

            if (targetIndex != -1 && targetIndex < currentList.size) {
                _state.update {
                    it.copy(currentAyaIndex = targetIndex
                    )
                }
                updateReadingProgress(ayaNumber)
                Log.d("JumpToAya", "Successfully set currentAyaIndex to: $targetIndex")
            } else {
                Log.e("JumpToAya", "Could not find aya $ayaNumber in the list")
                // DEBUG: Show available aya numbers
                val availableAyaNumbers = currentList.map { it.ayaNumberInSurah }.distinct().sorted()
                Log.d("JumpToAya", "Available aya numbers: $availableAyaNumbers")
            }
        }
    }

    private fun loadNavigationData() {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val currentSurah = _state.value.currentSurah
                currentSurah?.let { surah ->
                    val progress = dataStore.getReadingProgress(surah.number)
                    val bookmarked = dataStore.getBookmarkedAyaNumbers(surah.number)
                    val favorites = dataStore.getFavoriteAyaNumbers(surah.number)
                    val noted = dataStore.getNotedAyaNumbers(surah.number)

                    // Use surah's numberOfAyahs for correct total
                    val totalAyas = surah.numberOfAyahs

                    _state.update {
                        it.copy(
                            readingProgress = progress,
                            bookmarkedAyasInSurah = bookmarked,
                            favoriteAyasInSurah = favorites,
                            notedAyasInSurah = noted,
                            totalAyasInSurah = totalAyas
                        )
                    }
                }
            }
        }
    }

    private fun updateReadingProgress(ayaNumber: Int) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val currentSurah = _state.value.currentSurah
                if (currentSurah == null || ayaNumber <= 0) return@safeCall

                val cappedAya = ayaNumber.coerceAtMost(currentSurah.numberOfAyahs)

                // Only update if this is a forward progress (higher aya number than current)
                val currentProgress = _state.value.readingProgress
                if (currentProgress != null &&
                    currentProgress.surahNumber == currentSurah.number &&
                    cappedAya <= currentProgress.lastReadAyaNumber) {
                    // Don't update if scrolling backwards
                    return@safeCall
                }

                val progress = ReadingProgress(
                    surahNumber = currentSurah.number,
                    lastReadAyaNumber = cappedAya,
                    completionPercentage = (cappedAya.toFloat() / currentSurah.numberOfAyahs) * 100,
                    lastReadDate = LocalDate.now().toString()
                )
                dataStore.updateReadingProgress(progress)
                _state.update { it.copy(readingProgress = progress) }

                Log.d("AyatViewModel", "Updated reading progress: Surah ${currentSurah.number}, Aya $cappedAya")
            }
        }
    }

    private fun navigatePrevious() {
        val currentIndex = _state.value.currentAyaIndex
        if (currentIndex > 0) {
            val newIndex = currentIndex - 1
            _state.update { it.copy(currentAyaIndex = newIndex) }
            val currentAya = _state.value.ayatList[newIndex]
            if (currentAya.ayaNumberInSurah > 0) { // Don't update progress for Bismillah
                updateReadingProgress(currentAya.ayaNumberInSurah)
            }
        }
    }

    private fun navigateNext() {
        val currentIndex = _state.value.currentAyaIndex
        val maxIndex = _state.value.ayatList.size - 1
        if (currentIndex < maxIndex) {
            val newIndex = currentIndex + 1
            _state.update { it.copy(currentAyaIndex = newIndex) }
            val currentAya = _state.value.ayatList[newIndex]
            if (currentAya.ayaNumberInSurah > 0) { // Don't update progress for Bismillah
                updateReadingProgress(currentAya.ayaNumberInSurah)
            }
        }
    }

    private fun updateCurrentAyaIndex(index: Int) {
        if (index >= 0 && index < _state.value.ayatList.size) {
            _state.update { it.copy(currentAyaIndex = index) }
        }
    }

    private fun navigateToNextSurah() {
        val currentSurah = _state.value.currentSurah
        if (currentSurah != null && currentSurah.number < 114) {
            val nextSurahNumber = currentSurah.number + 1
            Log.d("AyatViewModel", "Navigating to next surah: $nextSurahNumber")
            loadAyat(nextSurahNumber, true)
        }
    }

    private fun navigateToPreviousSurah() {
        val currentSurah = _state.value.currentSurah
        if (currentSurah != null && currentSurah.number > 1) {
            val previousSurahNumber = currentSurah.number - 1
            Log.d("AyatViewModel", "Navigating to previous surah: $previousSurahNumber")
            loadAyat(previousSurahNumber, true)
        }
    }

    // ============ EXISTING METHODS (UNCHANGED) ============

    private fun loadInitialPreferences() {
        viewModelScope.launch {
            val settings = preferences.getDisplaySettings()
            _state.update { it.copy(displaySettings = settings) }
        }
    }

    private fun getSurahById(id: Int) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val surah = dataStore.getSurahById(id)
                _state.update { it.copy(currentSurah = surah) }
            }
        }
    }

    private fun loadAyat(number: Int, isSurah: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                setLoading(true)

                // Parallel loading of ayat and surah data
                val ayatDeferred = async {
                    if (isSurah) {
                        dataStore.getAyasOfSurah(number)
                    } else {
                        dataStore.getAyasOfJuz(number)
                    }
                }

                val ayatList = ayatDeferred.await()

                val surahDeferred = async {
                    dataStore.getSurahById(ayatList[0].suraNumber)
                }

                val juzDeferred = async {
                    dataStore.getJuzById(ayatList[0].juzNumber)
                }

                val surah = surahDeferred.await()
                val juz = juzDeferred.await()

                val ayatListWithBismillah = if (isSurah) {
                    addBismillahToFirstAya(ayatList as ArrayList, number)
                } else {
                    addBismillahInJuz(number, ayatList as ArrayList)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        ayatList = ayatListWithBismillah,
                        currentSurah = surah,
                        currentJuz = juz,
                    )
                }

                // Load navigation data after ayat are loaded
                loadNavigationData()
                loadActiveKhatam()
            }
        }
    }

    private fun updateDisplaySettings(settings: DisplaySettings) {
        viewModelScope.launch {
            _state.update { it.copy(displaySettings = settings) }
            preferences.saveDisplaySettings(settings)
        }
    }

    private fun toggleBookmark(aya: LocalAya) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val isBookmarked = !aya.bookmark
                dataStore.bookmarkAya(
                    aya.ayaNumberInQuran,
                    aya.suraNumber,
                    aya.ayaNumberInSurah,
                    isBookmarked
                )
                updateAyaInList(aya.copy(bookmark = isBookmarked))
            }
        }
    }

    private fun toggleFavorite(aya: LocalAya) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val isFavorited = !aya.favorite
                dataStore.favoriteAya(
                    aya.ayaNumberInQuran,
                    aya.suraNumber,
                    aya.ayaNumberInSurah,
                    isFavorited
                )
                updateAyaInList(aya.copy(favorite = isFavorited))
            }
        }
    }

    private fun updateNote(aya: LocalAya, note: String) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                dataStore.addNoteToAya(
                    aya.ayaNumberInQuran,
                    aya.suraNumber,
                    aya.ayaNumberInSurah,
                    note
                )
                updateAyaInList(aya.copy(note = note))
            }
        }
    }

    private fun downloadAudio(aya: LocalAya) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                updateAudioState { it.copy(isDownloading = true) }

                spaceFilesRepository.downloadAyaFile(
                    aya.suraNumber,
                    aya.ayaNumberInSurah
                ) { file, exception, progress, completed ->
                    viewModelScope.launch {
                        when {
                            exception != null -> {
                                setError(exception.message ?: "Download failed")
                                updateAudioState { it.copy(isDownloading = false) }
                            }

                            completed -> {
                                val audioPath = file?.absolutePath ?: ""
                                updateAudioState {
                                    it.copy(
                                        isDownloading = false,
                                        currentAudioPath = audioPath
                                    )
                                }
                                updateAyaInList(aya.copy(audioFileLocation = audioPath))
                                dataStore.addAudioToAya(
                                    aya.suraNumber,
                                    aya.ayaNumberInSurah,
                                    audioPath
                                )
                            }

                            else -> {
                                updateAudioState {
                                    it.copy(downloadProgress = progress.toFloat())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun playAudio(aya: LocalAya) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                if (aya.audioFileLocation.isEmpty()) {
                    setError("No audio file available")
                    return@safeCall
                }

                mediaPlayer.apply {
                    reset()
                    setDataSource(aya.audioFileLocation)
                    prepare()
                    start()
                }

                updateAudioState {
                    it.copy(
                        isPlaying = true,
                        isPaused = false,
                        currentPlayingAya = aya,
                        currentAudioPath = aya.audioFileLocation
                    )
                }
            }
        }
    }

    private fun pauseAudio() {
        viewModelScope.launch {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                updateAudioState { it.copy(isPlaying = false, isPaused = true) }
            }
        }
    }

    private fun stopAudio() {
        viewModelScope.launch {
            mediaPlayer.stop()
            updateAudioState {
                it.copy(
                    isPlaying = false,
                    isPaused = false,
                    currentPlayingAya = null
                )
            }
        }
    }

    private fun selectAya(aya: LocalAya) {
        viewModelScope.launch {
            _state.update { it.copy(selectedAya = aya) }
        }
    }

    // ============ BISMILLAH METHODS (UNCHANGED) ============

    private fun addBismillahToFirstAya(
        surahAyatList: ArrayList<LocalAya>,
        surahNumber: Int,
    ): ArrayList<LocalAya> {
        ViewModelLogger.d("addBismillahToFirstAya: $surahNumber", "Language unused")
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        val aya = LocalAya(
            0,
            ayaArabicOfBismillah,
            "In the name of Allah, the Entirely Merciful, the Especially Merciful",
            "اللہ کے نام سے جو رحمان و رحیم ہے",
            surahNumber,
            0,
            false,
            false,
            "",
            "",
            false,
            "",
            0,
            0,
        )

        if (surahAyatList[0].ayaArabic != ayaArabicOfBismillah && surahAyatList[0].suraNumber != 1) {
            if (surahNumber != 9) {
                surahAyatList.add(0, aya)
            }
        }

        return QuranUtils.processAyaEnd(surahAyatList)
    }

    private fun addBismillahInJuz(
        juzNumber: Int,
        listOfJuzAyat: ArrayList<LocalAya>,
    ): ArrayList<LocalAya> {
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

        var aya: LocalAya
        var index = 0
        while (index < listOfJuzAyat.size) {
            if (listOfJuzAyat[index].ayaNumberInSurah == 1 && listOfJuzAyat[index].suraNumber != 1) {
                if (listOfJuzAyat[index].ayaNumberInSurah == 1) {
                    if (juzNumber + 1 != 10 && index != 36) {
                        aya = LocalAya(
                            0,
                            ayaArabicOfBismillah,
                            "In the name of Allah, the Entirely Merciful, the Especially Merciful",
                            "اللہ کے نام سے جو رحمان و رحیم ہے",
                            listOfJuzAyat[index].suraNumber,
                            0,
                            false,
                            false,
                            "",
                            "",
                            false,
                            "",
                            0,
                            0,
                        )
                        listOfJuzAyat.add(index, aya)
                        index++
                    }
                }
            }
            index++
        }

        return QuranUtils.processAyaEnd(listOfJuzAyat)
    }

    // ============ UTILITY METHODS ============

    private fun updateAyaInList(updatedAya: LocalAya) {
        _state.update { currentState ->
            val updatedList = currentState.ayatList.map { aya ->
                if (aya.ayaNumberInQuran == updatedAya.ayaNumberInQuran) {
                    updatedAya
                } else {
                    aya
                }
            }
            currentState.copy(ayatList = updatedList)
        }
    }

    // ============ PAGE NAVIGATION FUNCTIONS ============

    private fun togglePaginationMode() {
        val newMode = !_state.value.isPaginationMode
        _state.update {
            it.copy(
                isPaginationMode = newMode,
                currentPage = if (newMode) calculateCurrentPage() else 0,
                totalPages = if (newMode) calculateTotalPages() else 0
            )
        }
        Log.d("AyatViewModel", "Pagination mode: $newMode, Current page: ${_state.value.currentPage}, Total pages: ${_state.value.totalPages}")
    }

    private fun calculateCurrentPage(): Int {
        val currentIndex = _state.value.currentAyaIndex
        val ayasPerPage = _state.value.ayasPerPage
        return (currentIndex / ayasPerPage) + 1
    }

    private fun calculateTotalPages(): Int {
        val totalAyas = _state.value.ayatList.size
        val ayasPerPage = _state.value.ayasPerPage
        return ((totalAyas + ayasPerPage - 1) / ayasPerPage).coerceAtLeast(1)
    }

    private fun nextPage() {
        val currentPage = _state.value.currentPage
        val totalPages = _state.value.totalPages

        if (currentPage < totalPages) {
            val newPage = currentPage + 1
            jumpToPage(newPage)
        } else {
            // At last page, navigate to next surah
            navigateToNextSurah()
        }
    }

    private fun previousPage() {
        val currentPage = _state.value.currentPage

        if (currentPage > 1) {
            val newPage = currentPage - 1
            jumpToPage(newPage)
        } else {
            // At first page, navigate to previous surah
            navigateToPreviousSurah()
        }
    }

    private fun jumpToPage(page: Int) {
        val totalPages = _state.value.totalPages
        val validPage = page.coerceIn(1, totalPages)

        val ayasPerPage = _state.value.ayasPerPage
        val targetIndex = (validPage - 1) * ayasPerPage

        _state.update {
            it.copy(
                currentPage = validPage,
                currentAyaIndex = targetIndex
            )
        }

        Log.d("AyatViewModel", "Jumped to page $validPage, index $targetIndex")
    }

    private fun updateAudioState(update: (AudioState) -> AudioState) {
        _state.update { it.copy(audioState = update(it.audioState)) }
    }

    private fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }

    private fun setError(error: String?) {
        _state.update { it.copy(error = error) }
    }

    private fun clearError() {
        viewModelScope.launch {
            setError(null)
        }
    }

    private suspend fun safeCall(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            setError(e.message ?: "An unknown error occurred")
            Log.e("AyatViewModel", "Error in safeCall", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
        viewModelScope.cancel()
    }
}