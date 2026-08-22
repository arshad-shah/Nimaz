package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TafseerChaptersViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val tafseerUseCases: TafseerUseCases,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(TafseerChaptersUiState())
    val state: StateFlow<TafseerChaptersUiState> = _state.asStateFlow()

    init {
        // A read-only ViewModel has no `onEvent` to hang this on, so construction *is* the
        // action: this screen is reached by an explicit tap and nothing else builds it. Every
        // sibling reader logs an open; this one logged nothing at all.
        telemetry.featureUsed(AppAnalytics.Feature.TAFSEER_CHAPTERS, "open")
        combine(
            quranUseCases.getSurahList(),
            tafseerUseCases.getTafseerNotes()
        ) { surahs, notes ->
            TafseerChaptersUiState(surahs = surahs, notes = notes, isLoading = false)
        }
            .onEach { loaded -> _state.update { loaded } }
            .catchAndReport(telemetry, DOMAIN, "load") { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.tafseer_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private companion object {
        const val DOMAIN = "tafseer_chapters"
    }
}
