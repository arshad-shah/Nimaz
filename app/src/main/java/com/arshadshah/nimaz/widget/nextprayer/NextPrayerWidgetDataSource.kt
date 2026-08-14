package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.widget.core.formatWidgetCountdown
import com.arshadshah.nimaz.widget.core.formatWidgetTime
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Computes what the next-prayer widget shows.
 *
 * Split out of [NextPrayerWorker] so it can be tested — `doWork()` returns early when no widget
 * is placed, which is always true on a test device, so none of this ran under
 * `WidgetWorkersTest` (#474). This is the widget with the most branching of the six: today's next
 * prayer, tomorrow's Fajr once the day's prayers have all passed, and a last-resort state when
 * even that cannot be computed.
 *
 * Takes [java.time.Clock] and [TodayProvider] rather than reading `Clock.System.now()` and
 * `LocalDate.now()` directly. Both were unmockable, which meant the rollover to tomorrow — the
 * one branch a user actually notices, because it is what the widget shows all evening — could
 * not be tested at all.
 *
 * **Prayer times come from [PrayerRepository], not from `PrayerTimeCalculator` directly.** This
 * used to call `getPrayerTimes(latitude, longitude)` and take all four calculation defaults —
 * Muslim World League, Shafi asr, no high-latitude rule, no per-prayer adjustments — so the
 * widget disagreed with the app it sits beside for everyone who had changed any of them. The
 * repository resolves the user's settings once and applies all of them, which is the same fix
 * `FastingViewModel` got when it had the same bug.
 *
 * It publishes the whole day's schedule, not just the prayer that is next right now, so the
 * widget can advance to the following prayer on its own between refreshes — see [NextPrayerData].
 */
class NextPrayerWidgetDataSource @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
    private val clock: java.time.Clock,
) {

    suspend fun load(): NextPrayerData {
        val use24Hour = settingsRepository.use24HourFormat.first()
        val timeZone = TimeZone.currentSystemDefault()
        val now = Instant.fromEpochMilliseconds(clock.millis())

        val settings = prayerRepository.observeCalculationSettings().first()
        val today = todayProvider.today()

        fun scheduleFor(date: java.time.LocalDate, isTomorrow: Boolean): List<NextPrayerEntry> =
            prayerRepository.getDaySchedule(date, settings).map { it.toEntry(timeZone, use24Hour, isTomorrow) }

        val todaysPrayers = scheduleFor(today, isTomorrow = false)
        // Tomorrow's first prayer closes the schedule, so the widget still has something ahead
        // of it after Isha — the state it sits in all evening, every evening.
        val tomorrowsFirst = scheduleFor(today.plusDays(1), isTomorrow = true).take(1)

        // The whole day in chronological order, with the past left in: the widget selects from
        // it by wall clock, so entries that have already passed simply never get picked.
        val schedule = todaysPrayers + tomorrowsFirst

        val next = schedule.firstOrNull { it.epochMillis > now.toEpochMilliseconds() }
            ?: return NextPrayerData(
                // Nothing computable — a bad location, say. Name the prayer anyway rather than
                // rendering an empty widget: "Fajr —" reads as "not known yet", an empty box
                // reads as broken.
                prayerName = PrayerType.FAJR.displayName,
                prayerTime = "",
                isTomorrow = true,
                countdown = "—",
                isValid = true,
            )

        return NextPrayerData(
            prayerName = next.prayerName,
            // Tomorrow's time is deliberately blank: the widget renders "Tomorrow" beside the
            // countdown, and a time without a date beside it reads as today's.
            prayerTime = if (next.isTomorrow) "" else next.prayerTime,
            isTomorrow = next.isTomorrow,
            countdown = formatWidgetCountdown(
                (Instant.fromEpochMilliseconds(next.epochMillis) - now).inWholeSeconds
            ),
            isValid = true,
            nextPrayerEpochMillis = next.epochMillis,
            schedule = schedule,
        )
    }

    private fun PrayerTime.toEntry(
        timeZone: TimeZone,
        use24Hour: Boolean,
        isTomorrow: Boolean,
    ): NextPrayerEntry {
        val local = time.toLocalDateTime(timeZone)
        return NextPrayerEntry(
            prayerName = type.displayName,
            prayerTime = formatWidgetTime(
                local.hour,
                local.minute,
                includeAmPm = true,
                use24Hour = use24Hour,
            ),
            isTomorrow = isTomorrow,
            epochMillis = time.toEpochMilliseconds(),
        )
    }
}
