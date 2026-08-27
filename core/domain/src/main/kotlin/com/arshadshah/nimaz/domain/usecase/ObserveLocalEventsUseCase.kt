package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.domain.model.IslamicEvents
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Emits occasion cards for today's Hijri date (offset-adjusted) from the static
 * [IslamicEvents] calendar.
 *
 * Design note: `IslamicEvents.events` is a static list and `nowDate()` is
 * wall-clock, so this use case only re-emits when [SettingsRepository.hijriDayOffset]
 * changes (and the ViewModel re-subscribes on resume). That is sufficient for a
 * day-granularity card; a midnight ticker to auto-advance the date is out of scope.
 */
class ObserveLocalEventsUseCase(
    private val settingsRepository: SettingsRepository,
    private val nowDate: () -> LocalDate = { LocalDate.now() },
) {
    operator fun invoke(): Flow<List<HomeEventCard>> =
        settingsRepository.hijriDayOffset.map { offset ->
            val today = HijriDateCalculator.toHijri(nowDate().plusDays(offset.toLong()))
            IslamicEvents.events
                .filter { it.hijriMonth == today.month && it.hijriDay == today.day }
                .sortedByDescending { it.priority }
                .map { ev ->
                    HomeEventCard(
                        event = CelebrationEvent.fromKey(normaliseId(ev.id)),
                        eyebrow = ev.nameEnglish,
                        headline = ev.nameEnglish,
                        body = ev.description.orEmpty(),
                        arabic = ev.nameArabic.ifBlank { null },
                        priority = ev.priority,
                    )
                }
        }

    /** Collapse the five dated Laylat al-Qadr ids onto the single CelebrationEvent key. */
    private fun normaliseId(id: String): String =
        if (id.startsWith("laylat_al_qadr")) "laylat_al_qadr" else id
}
