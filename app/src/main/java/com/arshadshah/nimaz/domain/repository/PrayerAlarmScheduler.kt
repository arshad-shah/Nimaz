package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType

/**
 * Arms today's prayer alarms.
 *
 * A seam of the same kind as [WidgetRefresher] and [CompassSensors]: the domain says *what*
 * should be scheduled, in domain terms, and the Android layer owns *how* — `AlarmManager`,
 * `PendingIntent`, notification channels and the rest.
 *
 * It exists because `RescheduleNotificationsUseCase` constructor-injected the concrete
 * `core.util.PrayerNotificationScheduler`, which pulls in `AlarmManager`, `Context`,
 * `NotificationCompat`, `R` and `@ApplicationContext`. That made the domain layer depend on
 * Android *transitively* — invisible to an import census of `domain/`, but fatal to compiling
 * the domain layer as a plain JVM module. Only the direction of the arrow changes here; the
 * implementation stays exactly where it was.
 *
 * The default argument values live on this declaration rather than on the implementation:
 * Kotlin forbids an override from restating them, and callers holding the concrete type inherit
 * them from here unchanged.
 */
interface PrayerAlarmScheduler {

    /**
     * Schedule (or cancel) every prayer alarm for today from the values given.
     *
     * @param enabledPrayers null means "every prayer", which is what the pre-per-prayer-toggle
     *   callers meant; an empty set means none.
     * @param preReminders lead time in minutes per prayer. A prayer absent from the map gets no
     *   pre-reminder — that is how "off" is expressed, rather than a zero offset.
     */
    fun scheduleTodaysPrayerNotifications(
        latitude: Double,
        longitude: Double,
        notificationsEnabled: Boolean,
        enabledPrayers: Set<PrayerType>? = null,
        preReminders: Map<PrayerType, Int> = emptyMap(),
        calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation: AsrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule: HighLatitudeRule? = null,
        adjustments: Map<PrayerType, Int> = emptyMap(),
        fridayReminderEnabled: Boolean = false,
        fridayReminderMinutes: Int = 60,
    )
}
