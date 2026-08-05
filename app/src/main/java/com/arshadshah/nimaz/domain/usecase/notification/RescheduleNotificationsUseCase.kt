package com.arshadshah.nimaz.domain.usecase.notification

import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.core.util.enabledPrayerTypes
import com.arshadshah.nimaz.core.util.preReminderMinutesByPrayer
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Re-arms today's prayer alarms from **what is persisted**, never from a ViewModel's state.
 *
 * This lived in `SettingsViewModel.rescheduleNotifications()` and built its alarm set from
 * `_notificationState.value` and `_prayerState.value` — two one-shot snapshots taken by
 * `loadSettings()` at construction. `hiltViewModel()` scopes to the nav back-stack entry, so
 * Notification Settings, Prayer Settings and Appearance each run their **own** instance, and a
 * snapshot taken by one goes stale the moment another writes.
 *
 * That produced an alarm the user had switched off:
 *
 *  1. Open Prayer Settings — instance B snapshots `ishaNotification = true`.
 *  2. Go to Notification Settings — instance A. Turn Isha off. DataStore is now `false`.
 *  3. Back to Prayer Settings, change the Asr juristic method. That calls reschedule on **B**,
 *     whose snapshot still says `true` — and the Isha adhan is re-armed.
 *
 * There was a second way in with the same root cause. `loadSettings()` does ~40 sequential
 * `.first()` reads, and until it finishes the state holds its **defaults** — notifications on,
 * every prayer except sunrise on. A user with notifications globally off who tapped the
 * calculation-method row on the frame the screen appeared scheduled a full day of alarms.
 *
 * Reading DataStore here closes both, and being a use case means no ViewModel can reintroduce
 * either by passing its own state in — there is nothing to pass.
 */
class RescheduleNotificationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduler: PrayerNotificationScheduler,
) {

    suspend operator fun invoke() {
        val prefs = settingsRepository.userPreferences.first()

        val adjustments = mapOf(
            PrayerType.FAJR to settingsRepository.fajrAdjustment.first(),
            PrayerType.SUNRISE to settingsRepository.sunriseAdjustment.first(),
            PrayerType.DHUHR to settingsRepository.dhuhrAdjustment.first(),
            PrayerType.ASR to settingsRepository.asrAdjustment.first(),
            PrayerType.MAGHRIB to settingsRepository.maghribAdjustment.first(),
            PrayerType.ISHA to settingsRepository.ishaAdjustment.first(),
        )

        scheduler.scheduleTodaysPrayerNotifications(
            latitude = prefs.latitude,
            longitude = prefs.longitude,
            notificationsEnabled = settingsRepository.prayerNotificationsEnabled.first(),
            // `enabledPrayerTypes()` already existed beside `preReminderMinutesByPrayer()` and
            // reads the same six flags off the repository — BootReceiver has been using it all
            // along. The ViewModel built its own set from state instead.
            enabledPrayers = settingsRepository.enabledPrayerTypes(),
            preReminders = settingsRepository.preReminderMinutesByPrayer(),
            calculationMethod = CalculationMethod.fromString(prefs.calculationMethod),
            asrCalculation = AsrCalculation.fromString(prefs.asrCalculation),
            // The domain's own parser, which accepts both the "MIDDLE_OF_NIGHT" and
            // "MIDDLE_OF_THE_NIGHT" spellings. It replaces a hand-rolled string remap wrapped
            // in `catch (_: Exception) { null }` — so a persisted value the remap did not cover
            // silently dropped the high-latitude correction the user had chosen. At 60°N that
            // moves Fajr and Isha by hours, with no report and nothing on screen to explain it.
            highLatitudeRule = HighLatitudeRule.fromString(settingsRepository.highLatitudeRule.first()),
            adjustments = adjustments,
            fridayReminderEnabled = settingsRepository.fridayReminderEnabled.first(),
            fridayReminderMinutes = settingsRepository.fridayReminderMinutes.first(),
        )
    }
}
