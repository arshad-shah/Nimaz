package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.usecase.khatam.KhatamPortion
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamStats

data class KhatamListUiState(
    val activeKhatam: Khatam? = null,
    val activeInsights: KhatamInsights? = null,
    val inProgressKhatams: List<Khatam> = emptyList(),
    val completedKhatams: List<Khatam> = emptyList(),
    val abandonedKhatams: List<Khatam> = emptyList(),
    val stats: KhatamStats? = null,
    val nextUnreadSurah: Int? = null,
    val nextUnreadAyah: Int? = null,
    val nextUnreadSurahName: String? = null,
    val isLoading: Boolean = true
) {
    val hasAnyKhatam: Boolean
        get() = inProgressKhatams.isNotEmpty() ||
                completedKhatams.isNotEmpty() ||
                abandonedKhatams.isNotEmpty()
}

data class KhatamDetailUiState(
    val khatam: Khatam? = null,
    val juzProgress: List<JuzProgressInfo> = emptyList(),
    val dailyLogs: List<DailyLogEntry> = emptyList(),
    val insights: KhatamInsights = KhatamInsights(),
    val nextUnreadSurah: Int? = null,
    val nextUnreadAyah: Int? = null,
    val nextUnreadSurahName: String? = null,
    /**
     * What the plan asks for **today**, and how to say it.
     *
     * A khatam exists to assign a daily portion; where you happened to stop is the fallback.
     * The screen led with "resume", which answers a question about the past — the plan's actual
     * instruction was nowhere on it.
     */
    val todaysPortion: KhatamPortion? = null,
    val todaysPortionLabel: String? = null,
    val isLoading: Boolean = true,
    /** True once the khatam is known to be gone, so the screen can pop instead of spinning. */
    val notFound: Boolean = false
)

data class KhatamFormUiState(
    val mode: KhatamFormMode = KhatamFormMode.Create,
    val name: String = "",
    val dailyTarget: Int = DEFAULT_DAILY_TARGET,
    val preset: KhatamPacePreset = KhatamPacePreset.CUSTOM,
    val notes: String = "",
    val deadline: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    /** Ayahs already read — shown on edit so the reader can see progress is untouched. */
    val totalAyahsRead: Int = 0,
    /** Drives whether the overflow menu offers "set as active". */
    val isActiveKhatam: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    /**
     * Validation failure as a resource id, not a resolved string: a ViewModel has no
     * Compose context, and a string resolved here would not follow a locale change.
     */
    @StringRes val errorRes: Int? = null,
    /** Set once the write has actually committed, so the screen navigates on success only. */
    val saveComplete: Boolean = false
) {
    val isEdit: Boolean get() = mode is KhatamFormMode.Edit

    /** Ayahs still to read — the whole Quran when creating. */
    val remainingAyahs: Int
        get() = (Khatam.TOTAL_QURAN_AYAHS - totalAyahsRead).coerceAtLeast(0)

    /** Days to finish the remainder at the chosen target. */
    val projectedDays: Int?
        get() = if (dailyTarget > 0 && remainingAyahs > 0) {
            Math.ceil(remainingAyahs.toDouble() / dailyTarget).toInt()
        } else null

    companion object {
        const val DEFAULT_DAILY_TARGET = 20
    }
}
