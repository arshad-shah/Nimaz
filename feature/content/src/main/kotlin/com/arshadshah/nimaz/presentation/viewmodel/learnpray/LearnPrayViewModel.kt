package com.arshadshah.nimaz.presentation.viewmodel.learnpray

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ephemeral lesson position survives configuration changes and saved-state restoration. */
@HiltViewModel
class LearnPrayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        LearnPrayUiState(
            (savedStateHandle.get<Int>(PAGE_KEY) ?: -1).coerceIn(-1, LearnPrayUiState.STEP_COUNT),
            PrayerFigure.entries.firstOrNull { it.name == savedStateHandle.get<String>(FIGURE_KEY) }
                ?: PrayerFigure.MAN,
        ),
    )
    val uiState = mutableState.asStateFlow()

    fun onEvent(event: LearnPrayEvent) {
        val current = mutableState.value
        val next = when (event) {
            LearnPrayEvent.Next -> current.copy(page = (current.page + 1).coerceAtMost(LearnPrayUiState.STEP_COUNT))
            LearnPrayEvent.Previous -> current.copy(page = (current.page - 1).coerceAtLeast(-1))
            LearnPrayEvent.Restart -> current.copy(page = -1)
            is LearnPrayEvent.SelectFigure -> current.copy(figure = event.figure)
        }
        savedStateHandle[PAGE_KEY] = next.page
        savedStateHandle[FIGURE_KEY] = next.figure.name
        mutableState.value = next
    }

    companion object {
        internal const val PAGE_KEY = "learn_pray_v1_page"
        internal const val FIGURE_KEY = "learn_pray_v1_figure"
    }
}
