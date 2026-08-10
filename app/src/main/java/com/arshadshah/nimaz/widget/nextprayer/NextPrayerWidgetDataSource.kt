package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.resolveLocation
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
 */
class NextPrayerWidgetDataSource @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
    private val clock: java.time.Clock,
) {

    suspend fun load(): NextPrayerData {
        val resolved = resolveLocation(
            settingsRepository.latitude.first(),
            settingsRepository.longitude.first(),
        )
        val use24Hour = settingsRepository.use24HourFormat.first()
        val timeZone = TimeZone.currentSystemDefault()
        val now = Instant.fromEpochMilliseconds(clock.millis())

        val prayerTimes = prayerTimeCalculator.getPrayerTimes(resolved.latitude, resolved.longitude)
        val localNow = now.toLocalDateTime(timeZone)

        val nextPrayer = prayerTimes.firstOrNull {
            it.time.toLocalDateTime(timeZone).time > localNow.time
        }

        if (nextPrayer != null) {
            val prayerLocalTime = nextPrayer.time.toLocalDateTime(timeZone)
            return NextPrayerData(
                prayerName = nextPrayer.type.displayName,
                prayerTime = formatWidgetTime(
                    prayerLocalTime.hour,
                    prayerLocalTime.minute,
                    includeAmPm = true,
                    use24Hour = use24Hour,
                ),
                countdown = formatWidgetCountdown((nextPrayer.time - now).inWholeSeconds),
                isValid = true,
                nextPrayerEpochMillis = nextPrayer.time.toEpochMilliseconds(),
            )
        }

        // Every prayer for today has passed, so the widget shows tomorrow's Fajr. This is the
        // state it sits in all evening, every evening.
        val tomorrowFajr = prayerTimeCalculator
            .getPrayerTimes(resolved.latitude, resolved.longitude, todayProvider.today().plusDays(1))
            .firstOrNull()
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
            prayerName = tomorrowFajr.type.displayName,
            // Deliberately blank: the widget renders "Tomorrow" beside the countdown, and a
            // time without a date beside it reads as today's.
            prayerTime = "",
            isTomorrow = true,
            countdown = formatWidgetCountdown((tomorrowFajr.time - now).inWholeSeconds),
            isValid = true,
            nextPrayerEpochMillis = tomorrowFajr.time.toEpochMilliseconds(),
        )
    }
}
