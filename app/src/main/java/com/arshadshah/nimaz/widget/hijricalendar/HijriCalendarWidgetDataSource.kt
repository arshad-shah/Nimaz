package com.arshadshah.nimaz.widget.hijricalendar

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Computes what the Hijri-calendar widget shows.
 *
 * Split out of [HijriCalendarWorker] so it can be tested — `doWork()` returns early when no
 * widget is placed, which is always true on a test device (#474).
 *
 * **Behaviour preserved exactly, including the same bug as the Hijri-date widget**:
 * `hijriDayOffset` is a moon-sighting correction to the *Hijri* date, and it is also added to the
 * Gregorian date rendered beside it, so an offset of +1 shows tomorrow's Gregorian day. Carried
 * over unchanged rather than fixed, because a refactor that silently changes what a widget
 * displays is not reviewable. Filed as #509, which covers both widgets.
 */
class HijriCalendarWidgetDataSource @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
) {

    suspend fun load(): HijriCalendarData {
        val offset = settingsRepository.hijriDayOffset.first()
        val hijriDate = HijriDateCalculator.today(offset)

        // See the class KDoc: the offset is applied to the Gregorian half too. Preserved. (#509)
        val today = todayProvider.today().plusDays(offset.toLong())

        val firstOfMonth = HijriDateCalculator.toGregorian(1, hijriDate.month, hijriDate.year)

        return HijriCalendarData(
            hijriMonth = hijriDate.month,
            hijriMonthName = hijriDate.monthName,
            hijriYear = hijriDate.year,
            gregorianDate = "${today.dayOfMonth} ${
                today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }",
            daysInMonth = HijriDateCalculator.getDaysInHijriMonth(hijriDate.year, hijriDate.month),
            firstDayOfWeekOffset = sundayBasedOffset(firstOfMonth.dayOfWeek.value),
            todayHijriDay = hijriDate.day,
            events = HijriDateCalculator.getIslamicEvents(hijriDate.year)
                .filter { it.day == hijriDate.day && it.month == hijriDate.month }
                .map {
                    HijriCalendarEventData(
                        name = it.name,
                        nameArabic = it.nameArabic,
                        type = it.type.name,
                    )
                },
        )
    }

    /**
     * How many blank cells precede the 1st in a Sunday-first grid.
     *
     * `java.time` numbers weekdays Monday=1 … Sunday=7; the widget's grid starts on Sunday, so
     * Sunday has to wrap to 0 and everything else keeps its number. Getting this wrong shifts
     * every date in the month by a column, which looks like a calendar and is not one.
     */
    private fun sundayBasedOffset(javaDayOfWeek: Int): Int =
        if (javaDayOfWeek == SUNDAY_ISO) 0 else javaDayOfWeek

    private companion object {
        const val SUNDAY_ISO = 7
    }
}
