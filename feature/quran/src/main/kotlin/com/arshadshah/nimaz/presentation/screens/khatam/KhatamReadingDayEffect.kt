package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamViewModel

/** Recompute calendar-day data after returning from the reader or an overnight background. */
@Composable
internal fun KhatamReadingDayEffect(viewModel: KhatamViewModel) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(KhatamEvent.RefreshReadingDay)
    }
}
