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

/**
 * One surah's background and its passage outline — the two screens that carry the long stuff.
 *
 * Deliberately *not* [QuranViewModel]. That one loads the surah list, the bookmarks, the
 * favourites, the active khatam, the verse of the day and the audio session on construction,
 * which is the right shape for the Qur'an home and the reader and a lot to spin up to draw a
 * page of prose. These two screens want one surah, one query, and the size the reader sets
 * its translation in.
 */
data class SurahBackgroundState(
    val surah: Surah? = null,
    val overview: SurahOverview? = null,

    /**
     * The size the reader draws its translation at.
     *
     * Reused rather than given a dial of its own: this is Qur'an-adjacent long-form prose, the
     * same thing the reader's translation is, and two settings for one act of reading is one
     * too many. The reader's Qur'an settings remain the single place it is changed.
     */
    val proseFontSize: Float = DEFAULT_PROSE_FONT_SIZE,
    val isLoading: Boolean = true,
    /** Set when the background fails to load, so the screen can say so rather than look empty. */
    val error: String? = null,
)

data class SurahPassagesState(
    val surah: Surah? = null,
    val passages: List<AyahTheme> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    /** Set when the passages fail to load. */
    val error: String? = null,
) {
    /**
     * The outline, narrowed by the filter.
     *
     * A query matches the passage's subject, and — because this is a table of contents and the
     * thing a reader most often has in hand is a verse number — a number that falls inside the
     * passage's range. "153" finds "Patience and prayer" without the reader knowing that is
     * where it starts.
     */
    val visiblePassages: List<AyahTheme>
        get() {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return passages
            val ayah = trimmed.toIntOrNull()
            return passages.filter { passage ->
                passage.theme.contains(trimmed, ignoreCase = true) ||
                    (ayah != null && passage.contains(ayah))
            }
        }

    val isFiltered: Boolean get() = query.isNotBlank()
}

sealed interface SurahThematicEvent {
    /** Load a surah's background and outline. Idempotent — safe to re-send. */
    data class Load(val surahNumber: Int) : SurahThematicEvent

    data class Filter(val query: String) : SurahThematicEvent
    data object ClearFilter : SurahThematicEvent
}

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

private const val DEFAULT_PROSE_FONT_SIZE = 16f
private const val DOMAIN = "surah_thematic"
