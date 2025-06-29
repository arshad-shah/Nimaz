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
import com.arshadshah.nimaz.data.local.models.QuickJump
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
    val quickJumps: List<QuickJump> = emptyList(),
    val currentAyaIndex: Int = 0,
    val bookmarkedAyasInSurah: List<Int> = emptyList(),
    val favoriteAyasInSurah: List<Int> = emptyList(),
    val notedAyasInSurah: List<Int> = emptyList(),
    val totalAyasInSurah: Int = 0,

    val activeKhatam: KhatamSession? = null,
    val isKhatamMode: Boolean = false
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
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val viewModelScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

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
        data class LoadAyat(val number: Int, val isSurah: Boolean, val language: String) : AyatEvent()
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
        data class AddQuickJump(val name: String) : AyatEvent()
        data class DeleteQuickJump(val quickJump: QuickJump) : AyatEvent()
        object LoadNavigationData : AyatEvent()
        data class UpdateReadingProgress(val ayaNumber: Int) : AyatEvent()
        object NavigatePrevious : AyatEvent()
        object NavigateNext : AyatEvent()
        data class UpdateCurrentAyaIndex(val index: Int) : AyatEvent()

        data class UpdateKhatamProgress(val surahNumber: Int, val ayaNumber: Int) : AyatEvent()
        object LoadActiveKhatam : AyatEvent()
    }

    // ============ EVENT HANDLER ============

    fun handleEvent(event: AyatEvent) {
        when (event) {
            // Existing events
            is AyatEvent.LoadAyat -> loadAyat(event.number, event.isSurah, event.language)
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
            is AyatEvent.AddQuickJump -> addQuickJump(event.name)
            is AyatEvent.DeleteQuickJump -> deleteQuickJump(event.quickJump)
            is AyatEvent.LoadNavigationData -> loadNavigationData()
            is AyatEvent.UpdateReadingProgress -> updateReadingProgress(event.ayaNumber)
            is AyatEvent.NavigatePrevious -> navigatePrevious()
            is AyatEvent.NavigateNext -> navigateNext()
            is AyatEvent.UpdateCurrentAyaIndex -> updateCurrentAyaIndex(event.index)
            is AyatEvent.UpdateKhatamProgress -> updateKhatamProgress(event.surahNumber, event.ayaNumber)
            is AyatEvent.LoadActiveKhatam -> loadActiveKhatam()
        }
    }

    private fun loadActiveKhatam() {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val activeKhatam = dataStore.getActiveKhatam()
                _state.update {
                    it.copy(
                        activeKhatam = activeKhatam,
                        isKhatamMode = activeKhatam != null
                    )
                }
            }
        }
    }

    private fun updateKhatamProgress(surahNumber: Int, ayaNumber: Int) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val activeKhatam = _state.value.activeKhatam
                activeKhatam?.let { khatam ->
                    val today = java.time.LocalDate.now().toString()
                    val progress = KhatamProgress(
                        khatamId = khatam.id,
                        surahNumber = surahNumber,
                        ayaNumber = ayaNumber,
                        dateRead = today
                    )
                    dataStore.insertKhatamProgress(progress)

                    // Update khatam position
                    val totalRead = calculateTotalAyasRead(surahNumber, ayaNumber)
                    dataStore.updateKhatamProgress(khatam.id, surahNumber, ayaNumber, totalRead)

                    // Reload active khatam to get updated data
                    loadActiveKhatam()
                }
            }
        }
    }

    private fun calculateTotalAyasRead(surahNumber: Int, ayaNumber: Int): Int {
        // Implement proper calculation based on your data
        // This is a placeholder
        return (surahNumber - 1) * 100 + ayaNumber
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
                    it.copy(currentAyaIndex = targetIndex)
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

    private fun addQuickJump(name: String) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val currentState = _state.value
                val currentAya = currentState.ayatList.getOrNull(currentState.currentAyaIndex)
                currentAya?.let { aya ->
                    val quickJump = QuickJump(
                        name = name,
                        surahNumber = aya.suraNumber,
                        ayaNumberInSurah = aya.ayaNumberInSurah
                    )
                    dataStore.insertQuickJump(quickJump)
                    loadNavigationData() // Refresh
                }
            }
        }
    }

    private fun deleteQuickJump(quickJump: QuickJump) {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                dataStore.deleteQuickJump(quickJump)
                loadNavigationData() // Refresh
            }
        }
    }

    private fun loadNavigationData() {
        viewModelScope.launch(ioDispatcher) {
            safeCall {
                val currentSurah = _state.value.currentSurah
                currentSurah?.let { surah ->
                    val quickJumps = dataStore.getQuickJumpsForSurah(surah.number)
                    val progress = dataStore.getReadingProgress(surah.number)
                    val bookmarked = dataStore.getBookmarkedAyaNumbers(surah.number)
                    val favorites = dataStore.getFavoriteAyaNumbers(surah.number)
                    val noted = dataStore.getNotedAyaNumbers(surah.number)

                    // Use surah's numberOfAyahs for correct total
                    val totalAyas = surah.numberOfAyahs

                    _state.update {
                        it.copy(
                            quickJumps = quickJumps,
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
                currentSurah?.let { surah ->
                    val progress = ReadingProgress(
                        surahNumber = surah.number,
                        lastReadAyaNumber = ayaNumber,
                        completionPercentage = (ayaNumber.toFloat() / surah.numberOfAyahs) * 100,
                        lastReadDate = java.time.LocalDate.now().toString()
                    )
                    dataStore.updateReadingProgress(progress)
                    _state.update { it.copy(readingProgress = progress) }
                }
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

    private fun loadAyat(number: Int, isSurah: Boolean, language: String) {
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
                    addBismillahToFirstAya(ayatList as ArrayList, language, number)
                } else {
                    addBismillahInJuz(number, language, ayatList as ArrayList)
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
        languageConverted: String,
        surahNumber: Int,
    ): ArrayList<LocalAya> {
        ViewModelLogger.d("addBismillahToFirstAya: $surahNumber", "Language: $languageConverted")
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
        languageConverted: String,
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