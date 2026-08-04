package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SurahThematicViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    settingsRepository: SettingsRepository,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _backgroundState = MutableStateFlow(SurahBackgroundState())
    val backgroundState: StateFlow<SurahBackgroundState> = _backgroundState.asStateFlow()

    private val _passagesState = MutableStateFlow(SurahPassagesState())
    val passagesState: StateFlow<SurahPassagesState> = _passagesState.asStateFlow()

    /** One job per surah, so a slower load for a surah since left cannot repaint this one. */
    private var loadJob: Job? = null
    private var loadedSurah: Int? = null

    init {
        settingsRepository.quranTranslationFontSize
            .onEach { size -> _backgroundState.update { it.copy(proseFontSize = size) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SurahThematicEvent) {
        when (event) {
            is SurahThematicEvent.Load -> load(event.surahNumber)

            is SurahThematicEvent.Filter -> {
                _passagesState.update { it.copy(query = event.query) }
                // Logged post-filter with the length only, never the query itself.
                if (event.query.isNotBlank()) telemetry.search(DOMAIN, event.query.trim().length)
            }

            SurahThematicEvent.ClearFilter ->
                _passagesState.update { it.copy(query = "") }
        }
    }

    private fun load(surahNumber: Int) {
        if (loadedSurah == surahNumber && loadJob?.isActive != true) return
        loadedSurah = surahNumber
        loadJob?.cancel()
        loadJob = launchSafely(
            telemetry,
            domain = DOMAIN,
            type = "load",
            onFailure = { throwable ->
                // The surah was never actually loaded, so clear the marker: otherwise the
                // guard above would short-circuit a later retry and strand the screen.
                loadedSurah = null
                _backgroundState.update { it.copy(isLoading = false, error = throwable.message) }
                _passagesState.update { it.copy(isLoading = false, error = throwable.message) }
            },
        ) {
            _backgroundState.update { it.copy(isLoading = true, error = null) }
            _passagesState.update { it.copy(isLoading = true, error = null) }

            val surah = quranUseCases.getSurahByNumber(surahNumber)
            val overview = quranUseCases.getSurahOverview(surahNumber)
            val passages = quranUseCases.getSurahThemes(surahNumber)

            _backgroundState.update {
                it.copy(surah = surah, overview = overview, isLoading = false)
            }
            _passagesState.update {
                it.copy(surah = surah, passages = passages, isLoading = false)
            }
            telemetry.featureUsed(DOMAIN, "open")
        }
    }
}

private const val DOMAIN = "surah_thematic"
