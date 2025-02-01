package com.arshadshah.nimaz.viewModel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.Tafsir
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TafsirViewModel @Inject constructor(
    private val dataStore: DataStore<Any?>,
    private val sharedPreferences: PrivateSharedPreferences
) : ViewModel() {

    sealed class TafsirUiState {
        object Loading : TafsirUiState()
        data class Error(val message: String) : TafsirUiState()
        data class Success(
            val aya: LocalAya?,
            val surah: LocalSurah,
            val tafsir: Tafsir,
            val settings: TafsirSettings
        ) : TafsirUiState()
    }

    data class TafsirSettings(
        val arabicFontSize: Float = 28f,
        val translationFontSize: Float = 20f,
        val arabicFont: String = "Default",
        val translationLanguage: String = "English"
    )

    private val _uiState = MutableStateFlow<TafsirUiState>(TafsirUiState.Loading)
    val uiState: StateFlow<TafsirUiState> = _uiState.asStateFlow()

    // Settings StateFlows
    private val _arabicFontSize = MutableStateFlow(26.0f)
    private val _translationFontSize = MutableStateFlow(16.0f)
    private val _arabicFont = MutableStateFlow("Default")
    private val _translationLanguage =
        MutableStateFlow(sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE, "English"))

    init {
        initializeSettings()
    }

    private fun initializeSettings() {
        _arabicFont.value = sharedPreferences.getData(AppConstants.FONT_STYLE, "Default")
        _translationLanguage.value =
            sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE, "English")

        // Initialize font sizes with defaults if not set
        _arabicFontSize.value =
            if (sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE) == 0.0f) {
                sharedPreferences.saveDataFloat(AppConstants.ARABIC_FONT_SIZE, 26.0f)
                26.0f
            } else {
                sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE)
            }

        _translationFontSize.value =
            if (sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE) == 0.0f) {
                sharedPreferences.saveDataFloat(AppConstants.TRANSLATION_FONT_SIZE, 16.0f)
                16.0f
            } else {
                sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE)
            }
    }

    sealed class TafsirEvent {
        data class LoadTafsir(
            val surahNumber: Int,
            val ayaNumber: Int
        ) : TafsirEvent()

        data class UpdateSettings(val settings: TafsirSettings) : TafsirEvent()
        data class BookmarkAya(val aya: LocalAya) : TafsirEvent()
        data class FavoriteAya(val aya: LocalAya) : TafsirEvent()
        data class AddNote(val aya: LocalAya, val note: String) : TafsirEvent()
        object ResetSettings : TafsirEvent()
    }

    fun handleEvent(event: TafsirEvent) {
        when (event) {
            is TafsirEvent.LoadTafsir -> loadTafsirData(
                event.surahNumber,
                event.ayaNumber
            )

            is TafsirEvent.UpdateSettings -> updateSettings(event.settings)
            is TafsirEvent.BookmarkAya -> bookmarkAya(event.aya)
            is TafsirEvent.FavoriteAya -> favoriteAya(event.aya)
            is TafsirEvent.AddNote -> addNote(event.aya, event.note)
            is TafsirEvent.ResetSettings -> resetSettings()
        }
    }

    private fun resetSettings() {
        _arabicFont.value = "Default"
        sharedPreferences.saveData(AppConstants.FONT_STYLE, "Default")

        _translationLanguage.value = "English"
        sharedPreferences.saveData(AppConstants.TRANSLATION_LANGUAGE, "English")

        _arabicFontSize.value = 26.0f
        sharedPreferences.saveDataFloat(AppConstants.ARABIC_FONT_SIZE, 26.0f)

        _translationFontSize.value = 16.0f
        sharedPreferences.saveDataFloat(AppConstants.TRANSLATION_FONT_SIZE, 16.0f)

        // Update UI state with new settings
        updateCurrentStateWithSettings()
    }

    private fun updateCurrentStateWithSettings() {
        val currentState = _uiState.value
        if (currentState is TafsirUiState.Success) {
            _uiState.value = currentState.copy(
                settings = TafsirSettings(
                    arabicFontSize = _arabicFontSize.value,
                    translationFontSize = _translationFontSize.value,
                    arabicFont = _arabicFont.value,
                    translationLanguage = _translationLanguage.value
                )
            )
        }
    }

    private fun getCurrentSettings(): TafsirSettings {
        return TafsirSettings(
            arabicFontSize = _arabicFontSize.value,
            translationFontSize = _translationFontSize.value,
            arabicFont = _arabicFont.value,
            translationLanguage = _translationLanguage.value
        )
    }

    private fun updateSettings(newSettings: TafsirSettings) {
        // Update SharedPreferences
        sharedPreferences.saveDataFloat(AppConstants.ARABIC_FONT_SIZE, newSettings.arabicFontSize)
        sharedPreferences.saveDataFloat(
            AppConstants.TRANSLATION_FONT_SIZE,
            newSettings.translationFontSize
        )
        sharedPreferences.saveData(AppConstants.FONT_STYLE, newSettings.arabicFont)
        sharedPreferences.saveData(
            AppConstants.TRANSLATION_LANGUAGE,
            newSettings.translationLanguage
        )

        // Update StateFlows
        _arabicFontSize.value = newSettings.arabicFontSize
        _translationFontSize.value = newSettings.translationFontSize
        _arabicFont.value = newSettings.arabicFont
        _translationLanguage.value = newSettings.translationLanguage

        // Update UI State
        val currentState = _uiState.value
        if (currentState is TafsirUiState.Success) {
            _uiState.update { currentState.copy(settings = newSettings) }
        }
    }


    private fun loadTafsirData(surahNumber: Int, ayaNumber: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = TafsirUiState.Loading

                ViewModelLogger.d(
                    "Nimaz: TafsirViewModel",
                    "Loading Tafsir Data for Surah: $surahNumber, Aya: $ayaNumber"
                )

                // Load all required data on IO dispatcher
                val (surah, aya, tafsir) = withContext(Dispatchers.IO) {
                    // Get the current translation language from StateFlow
                    val currentLanguage =
                        _translationLanguage.value.replaceFirstChar { it.lowercase() }
                    ViewModelLogger.d(
                        "Nimaz: TafsirViewModel",
                        "Current Language: $currentLanguage"
                    )
                    // Load surah and aya data
                    val loadedSurah = dataStore.getSurahById(surahNumber)
                    val ayaList = dataStore.getAyasOfSurah(surahNumber)
                    val foundAya = ayaList.find { it.ayaNumberInQuran == ayaNumber }

                    // Load tafsir data
                    val edition = dataStore.getEditionsByLanguageAndAuthor(
                        currentLanguage,
                        "Hafiz Ibn Kathir"
                    )
                    ViewModelLogger.d("Nimaz: TafsirViewModel", "Edition: $edition")
                    val loadedTafsir = dataStore.getTafsirForAya(ayaNumber, edition.id)

                    Triple(loadedSurah, foundAya, loadedTafsir)
                }

                // Update UI state with loaded data
                if (aya != null) {
                    _uiState.value = TafsirUiState.Success(
                        aya = aya,
                        surah = surah,
                        tafsir = tafsir,
                        settings = getCurrentSettings()
                    )
                } else {
                    _uiState.value = TafsirUiState.Error("Aya not found")
                }
            } catch (e: Exception) {
                _uiState.value = TafsirUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun bookmarkAya(aya: LocalAya) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dataStore.bookmarkAya(
                        aya.ayaNumberInQuran,
                        aya.suraNumber,
                        aya.ayaNumberInSurah,
                        !aya.bookmark
                    )
                }
                refreshAyaState(aya.suraNumber, aya.ayaNumberInSurah)
            } catch (e: Exception) {
                _uiState.value = TafsirUiState.Error(e.message ?: "Failed to bookmark aya")
            }
        }
    }

    private fun favoriteAya(aya: LocalAya) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dataStore.favoriteAya(
                        aya.ayaNumberInQuran,
                        aya.suraNumber,
                        aya.ayaNumberInSurah,
                        !aya.favorite
                    )
                }
                refreshAyaState(aya.suraNumber, aya.ayaNumberInSurah)
            } catch (e: Exception) {
                _uiState.value = TafsirUiState.Error(e.message ?: "Failed to favorite aya")
            }
        }
    }

    private fun addNote(aya: LocalAya, note: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dataStore.addNoteToAya(
                        aya.ayaNumberInQuran,
                        aya.suraNumber,
                        aya.ayaNumberInSurah,
                        note
                    )
                }
                refreshAyaState(aya.suraNumber, aya.ayaNumberInSurah)
            } catch (e: Exception) {
                _uiState.value = TafsirUiState.Error(e.message ?: "Failed to add note")
            }
        }
    }

    private suspend fun refreshAyaState(surahNumber: Int, ayaNumber: Int) {
        when (val currentState = _uiState.value) {
            is TafsirUiState.Success -> {
                val updatedAya = withContext(Dispatchers.IO) {
                    val updatedAyaList = dataStore.getAyasOfSurah(surahNumber)
                    updatedAyaList.find { it.ayaNumberInSurah == ayaNumber }
                }
                _uiState.value = currentState.copy(aya = updatedAya)
            }

            else -> {} // Do nothing if not in success state
        }
    }
}