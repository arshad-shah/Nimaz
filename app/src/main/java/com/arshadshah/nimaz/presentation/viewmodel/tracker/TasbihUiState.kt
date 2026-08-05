package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.model.TasbihStats

data class TasbihPresetsUiState(
    val defaultPresets: List<TasbihPreset> = emptyList(),
    val customPresets: List<TasbihPreset> = emptyList(),
    val selectedCategory: TasbihCategory? = null,
    val favorites: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
) {
    /**
     * The presets the list should show — **derived**, never stored.
     *
     * This used to be a stored field recomputed by hand wherever an input changed, and the
     * two Room collectors in `loadPresets` rebuilt it as `defaults + customs` without
     * consulting [selectedCategory]. So saving or deleting a custom dhikr re-emitted the
     * presets flow and silently dropped the active category filter, while the category chip
     * carried on looking selected. Deriving it here means there is no site left to forget.
     */
    val filteredPresets: List<TasbihPreset>
        get() {
            val all = defaultPresets + customPresets
            return if (selectedCategory == null) all else all.filter { it.category == selectedCategory }
        }
}

data class TasbihCounterUiState(
    val selectedPreset: TasbihPreset? = null,
    val currentSession: TasbihSession? = null,
    val count: Int = 0,
    val laps: Int = 0,
    val targetCount: Int = 33,
    val isActive: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val autoLap: Boolean = true,
    val counterStyle: TasbihCounterStyle = TasbihCounterStyle.CLASSIC,
    val beadDesignKey: String = "wood",
    val leftHanded: Boolean = false
)

data class TasbihHistoryUiState(
    val todaySessions: List<TasbihSession> = emptyList(),
    val weekSessions: List<TasbihSession> = emptyList(),
    val isLoading: Boolean = true
)

data class TasbihStatsUiState(
    val stats: TasbihStats? = null,
    val totalToday: Int = 0,
    val baseTotalToday: Int = 0, // Total excluding current session, for real-time display
    val totalThisWeek: Int = 0,
    val completedSessions: Int = 0,
    val isLoading: Boolean = true
)
