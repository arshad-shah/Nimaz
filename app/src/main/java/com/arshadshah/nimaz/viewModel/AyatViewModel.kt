package com.arshadshah.nimaz.viewModel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalSurah
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

// AyatState.kt
data class AyatState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val ayatList: List<LocalAya> = emptyList(),
    val currentSurah: LocalSurah? = null,
    val currentJuz: LocalJuz? = null,
    val scrollToAya: Int? = null,
    val displaySettings: DisplaySettings = DisplaySettings(),
    val audioState: AudioState = AudioState(),
    val selectedAya: LocalAya? = null
)

data class AudioState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentAudioPath: String = "",
    val currentPlayingAya: LocalAya? = null
)

// AyatViewModel.kt
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
    }

    private fun loadInitialPreferences() {
        viewModelScope.launch {
            val settings = preferences.getDisplaySettings()
            _state.update {
                it.copy(displaySettings = settings)
            }
        }
    }

    sealed class AyatEvent {
        data class LoadAyat(
            val number: Int,
            val isSurah: Boolean,
            val language: String
        ) : AyatEvent()

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
    }

    fun handleEvent(event: AyatEvent) {
        when (event) {
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
        }
    }

    private fun getSurahById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val surah = dataStore.getSurahById(id)
                _state.update {
                    it.copy(
                        currentSurah = surah
                    )
                }
            } catch (e: Exception) {
                Log.d("getSurahById", e.message ?: "Unknown error")
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

                val surahDeferred = async {
                    dataStore.getSurahById(number)
                }

                val juzDeferred = async {
                    dataStore.getJuzById(number)
                }

                val ayatList = ayatDeferred.await()
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

                // Update the state with the modified aya
                updateAyaInList(aya.copy(bookmark = isBookmarked))
            }
        }
    }

    private fun addBismillahToFirstAya(
        surahAyatList: ArrayList<LocalAya>,
        languageConverted: String,
        surahNumber: Int,
    ): ArrayList<LocalAya> {
        ViewModelLogger.d("addBismillahToFirstAya: $surahNumber", "Language: $languageConverted")
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
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
        //first check if an object like this is already in the list
        //check all the attributes of the object bisimillah with the attributes of the object in the list at index 0
        if (surahAyatList[0].ayaArabic != ayaArabicOfBismillah && surahAyatList[0].suraNumber != 1) {
            if (surahNumber != 9) {
                surahAyatList.add(0, aya)
            }
        }

        return QuranUtils.processAyaEnd(surahAyatList)
    }

    //function to add biismillah to the start of every surah
    private fun addBismillahInJuz(
        juzNumber: Int,
        languageConverted: String,
        listOfJuzAyat: ArrayList<LocalAya>,
    ): ArrayList<LocalAya> {
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

        //create a map of the aya of bismillah
        var aya: LocalAya
        //find all the objects in arraylist ayaForJuz where ayaForJuz[i]!!.ayaNumber = 1
        //add object bismillah before it for every occurance of ayaForJuz[i]!!.ayaNumber = 1
        var index = 0
        while (index < listOfJuzAyat.size) {
            if (listOfJuzAyat[index].ayaNumberInSurah == 1 && listOfJuzAyat[index].suraNumber != 1) {
                //add bismillah before ayaForJuz[i]
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

                        //add the map of bismillah to ayaList at the current index
                        listOfJuzAyat.add(index, aya)
                        //skip the next iteration
                        index++
                    }
                }
            }
            index++
        }

        return QuranUtils.processAyaEnd(listOfJuzAyat)
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
                                //write the path to the database
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
        viewModelScope.cancel()
    }
}