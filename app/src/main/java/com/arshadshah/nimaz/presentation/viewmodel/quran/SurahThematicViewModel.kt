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

    /**
     * The same surah's thematic layer as one object, for the surah **info** screen — which
     * summarises what the other two screens then show in full.
     *
     * `QuranViewModel` used to load this itself, from `loadSurahInfo`, into its own
     * `_surahThematic`. That was the same three reads against the same use cases producing the
     * same state class, in the ViewModel this one exists precisely to keep the prose screens
     * out of: its KDoc above says so, and the split was half-done because the info screen still
     * read the copy. One load path now, so the two cannot drift.
     */
    private val _thematic = MutableStateFlow(SurahThematicUiState())
    val thematic: StateFlow<SurahThematicUiState> = _thematic.asStateFlow()

    /** One job per surah, so a slower load for a surah since left cannot repaint this one. */
    private var loadJob: Job? = null

    /** The surah whose load **completed**. Assigned on success, never on entry. */
    private var loadedSurah: Int? = null

    /**
     * The surah a load is currently running for.
     *
     * Split from [loadedSurah] because the old single marker was assigned at entry and so
     * recorded a surah that might never arrive. Cancel the job — a pane swap, a fast back —
     * and `loadedSurah == surahNumber` with `loadJob.isActive == false` made the guard
     * short-circuit every retry, stranding the screen on `isLoading = true` **forever**. It
     * also let a second `Load` for the surah already in flight restart the whole three-call
     * load, despite the KDoc calling the event idempotent.
     */
    private var loadingSurah: Int? = null

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

            SurahThematicEvent.ClearFilter -> {
                telemetry.featureUsed(DOMAIN, "clear_filter")
                _passagesState.update { it.copy(query = "") }
            }
        }
    }

    private fun load(surahNumber: Int) {
        // Already showing it, or already fetching it: both are "nothing to do". A cancelled
        // job satisfies neither, so a retry after one gets through.
        if (loadedSurah == surahNumber) return
        if (loadingSurah == surahNumber && loadJob?.isActive == true) return
        loadingSurah = surahNumber
        loadJob?.cancel()
        loadJob = launchSafely(
            telemetry,
            domain = DOMAIN,
            type = "load",
            onFailure = { throwable ->
                // The surah was never actually loaded, so clear the marker: otherwise the
                // guard above would short-circuit a later retry and strand the screen.
                loadingSurah = null
                _backgroundState.update { it.copy(isLoading = false, error = throwable.message) }
                _passagesState.update { it.copy(isLoading = false, error = throwable.message) }
                _thematic.update { it.copy(isLoading = false) }
            },
        ) {
            _backgroundState.update { it.copy(isLoading = true, error = null) }
            _passagesState.update { it.copy(isLoading = true, error = null) }
            _thematic.value = SurahThematicUiState(isLoading = true)

            val surah = quranUseCases.getSurahByNumber(surahNumber)
            val overview = quranUseCases.getSurahOverview(surahNumber)
            val passages = quranUseCases.getSurahThemes(surahNumber)
            // A count, not the list: the info screen labels one row with it.
            val subjectCount = quranUseCases.getTopicsForSurah.count(surahNumber)

            _backgroundState.update {
                it.copy(surah = surah, overview = overview, isLoading = false)
            }
            _passagesState.update {
                it.copy(surah = surah, passages = passages, isLoading = false)
            }
            _thematic.value = SurahThematicUiState(
                overview = overview,
                passages = passages,
                subjectCount = subjectCount,
                isLoading = false,
            )
            // Recorded only now that all three reads have landed.
            loadedSurah = surahNumber
            loadingSurah = null
            telemetry.featureUsed(DOMAIN, "open")
        }
    }
}

private const val DOMAIN = "surah_thematic"
