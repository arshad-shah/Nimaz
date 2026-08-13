package com.arshadshah.nimaz.widget.hijridate

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Computes what the Hijri-date widget shows.
 *
 * Split out of [HijriDateWorker] so it can be tested — `doWork()` returns early when no widget is
 * placed, which is always true on a test device, so none of this ran under `WidgetWorkersTest`
 * (#474).
 *
 * **Behaviour is preserved exactly, including one thing that looks wrong.** `hijriDayOffset` is a
 * moon-sighting correction to the *Hijri* date, but it is also added to the Gregorian date
 * rendered beside it, so a user who sets +1 sees tomorrow's Gregorian day and weekday on the
 * widget — a date their phone knows exactly. That is carried over unchanged here rather than
 * fixed, because a refactor that silently changes what a widget displays is not reviewable. It is
 * filed as #509.
 */
class HijriDateWidgetDataSource @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
) {

    suspend fun load(): HijriDateData {
        val offset = settingsRepository.hijriDayOffset.first()
        val hijriDate = HijriDateCalculator.today(offset)

        // See the class KDoc: the offset is applied to the Gregorian half too. Preserved.
        val today = todayProvider.today().plusDays(offset.toLong())

        return HijriDateData(
            hijriDay = hijriDate.day,
            hijriMonth = hijriDate.monthName,
            hijriYear = hijriDate.year,
            gregorianDayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            gregorianDate = "${today.dayOfMonth} ${
                today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }",
        )
    }
}
