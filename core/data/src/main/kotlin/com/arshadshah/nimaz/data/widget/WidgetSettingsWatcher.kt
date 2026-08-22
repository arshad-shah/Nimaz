package com.arshadshah.nimaz.data.widget

import com.arshadshah.nimaz.core.common.IoDispatcher
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Redraws the widgets when a setting they are computed from changes.
 *
 * Every widget refreshed on its own schedule and nothing else — the prayer tracker was the only
 * one with a push, and only for prayer status. So changing location, calculation method, asr
 * madhab, high-latitude rule, a per-prayer adjustment, the clock format or the Hijri offset left
 * the home screen showing the previous answer for up to fifteen minutes, and up to six hours for
 * the two Hijri widgets. The setting had taken; the widget just had not been told.
 *
 * Watching the resolved settings rather than hooking each setter is what makes that complete:
 * these values are written from four different ViewModels — settings, location, onboarding and
 * home — and the next place that writes one gets this for free instead of having to remember it.
 *
 * The first emission is dropped: collection starts at app launch, and its value is the state the
 * widgets were already refreshed for.
 */
@Singleton
class WidgetSettingsWatcher @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val widgetRefresher: WidgetRefresher,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    fun start() {
        scope.launch {
            combine(
                prayerRepository.observeCalculationSettings(),
                settingsRepository.use24HourFormat,
                settingsRepository.hijriDayOffset,
            ) { calculation, use24Hour, hijriOffset ->
                WidgetInputs(calculation, use24Hour, hijriOffset)
            }
                .distinctUntilChanged()
                .drop(1)
                .catch { e ->
                    // A widget that stays stale is worth reporting and not worth crashing over.
                    CrashReporter.log("WidgetSettingsWatcher stopped")
                    CrashReporter.recordException(e)
                }
                .collect { widgetRefresher.refreshAll() }
        }
    }

    /** Everything the widgets read that a user can change. Compared by value. */
    private data class WidgetInputs(
        val calculation: PrayerCalculationSettings,
        val use24HourFormat: Boolean,
        val hijriDayOffset: Int,
    )
}
