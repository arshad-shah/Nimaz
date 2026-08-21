package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.KhatamProgressCalculator
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import com.arshadshah.nimaz.domain.worship.NextWorshipResolver
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Everything the More screen reads, and nothing else.
 *
 * More is the first screen to want seven features' figures at once, and every one of them already
 * exists — so this bundle mostly *narrows*. The five use cases it defines here exist because the
 * raw sources are the wrong shape for a menu row: three of them ([GetNextWorshipUseCase],
 * [ObserveKhatamRowProgressUseCase], [GetHijriTodayUseCase]) wrap `core/util` classes that a
 * ViewModel must not hold directly — two are concrete classes and `HijriDateCalculator` is a
 * static `object`, so a ViewModel taking them cannot be constructed in a JVM test at all. Wrapping
 * them here is the difference between `MoreSubtitles` being testable and the thing feeding it
 * being testable too.
 */
data class MoreUseCases(
    /** Today's tracker entries. Resolves "today" at call time — re-invoke it at rollover. */
    val todayPrayerRecords: GetTodayPrayerRecordsUseCase,
    val pendingMakeupFasts: GetPendingMakeupFastsUseCase,
    val nextWorship: GetNextWorshipUseCase,
    val khatamRowProgress: ObserveKhatamRowProgressUseCase,
    val qaidaRowProgress: ObserveQaidaRowProgressUseCase,
    val zakatHistory: GetAllHistoryUseCase,
    val hijriToday: GetHijriTodayUseCase,
    val hijriDayOffset: ObserveHijriDayOffsetUseCase,
    val zakatCurrency: ObserveZakatCurrencyUseCase,
)

/**
 * The nearest upcoming worship reminder, or null when nothing is enabled, near, or locatable.
 *
 * A suspend call rather than a flow because that is what the resolver is: it reads a dozen
 * settings and computes each enabled reminder's next occurrence. The subtitle it feeds is
 * therefore a snapshot, refreshed when something else in More's state changes or the screen asks
 * — which is the right trade for a menu row, where a live-ticking countdown would mean keeping a
 * timer alive behind a list nobody looks at for long.
 */
class GetNextWorshipUseCase @Inject constructor(
    private val resolver: NextWorshipResolver,
) {
    suspend operator fun invoke(now: LocalDateTime = LocalDateTime.now()): WorshipReminderOccurrence? =
        resolver.nearest(now)
}

/** Where the active khatam has reached: the juz being read, and days for/against the target. */
data class KhatamRowProgress(
    /** 1-based juz currently being read; 30 once everything is complete. */
    val juz: Int,
    /** Positive is ahead of the daily target, negative behind, null when not yet measurable. */
    val daysAgainstPace: Int?,
)

/**
 * The two numbers More's khatam row reports, from the active khatam.
 *
 * Emits null when there is no active khatam — which is not an error and not a loading state, it is
 * simply a reader who has not started one, and the row says nothing.
 */
class ObserveKhatamRowProgressUseCase @Inject constructor(
    private val repository: KhatamRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<KhatamRowProgress?> =
        repository.observeActiveKhatam().flatMapLatest { khatam ->
            if (khatam == null) return@flatMapLatest flowOf(null)
            repository.observeJuzProgress(khatam.id).map { juzProgress ->
                KhatamRowProgress(
                    // The juz being read is the first incomplete one — not the count of complete
                    // ones, which reads a juz low the moment someone finishes one, and not the
                    // highest touched, which jumps ahead if they dip into a later juz.
                    juz = juzProgress.firstOrNull { !it.isComplete }?.juzNumber
                        ?: juzProgress.lastOrNull()?.juzNumber
                        ?: 1,
                    daysAgainstPace = KhatamProgressCalculator.daysAgainstPace(
                        totalAyahsRead = khatam.totalAyahsRead,
                        daysActive = KhatamProgressCalculator.daysActive(khatam.startedAt),
                        dailyTarget = khatam.dailyTarget,
                    ),
                )
            }
        }
}

/** Which Qaida lesson someone is on, out of how many there are. */
data class QaidaRowProgress(val currentLesson: Int, val totalLessons: Int)

/**
 * The lesson the learner has reached, and the course length.
 *
 * "Reached" is the furthest lesson they have actually touched, not the count of completed ones: a
 * learner halfway through lesson 4 is on lesson 4, and reporting 3 would tell them they are
 * somewhere they have already left.
 */
class ObserveQaidaRowProgressUseCase @Inject constructor(
    private val repository: QaidaRepository,
) {
    operator fun invoke(): Flow<QaidaRowProgress?> =
        combine(repository.getLessons(), repository.getAllProgress()) { lessons, progress ->
            val furthest = progress
                .filter { it.status == LessonStatus.IN_PROGRESS || it.status == LessonStatus.COMPLETED }
                .maxOfOrNull { it.lessonId }
            if (furthest == null || lessons.isEmpty()) null
            else QaidaRowProgress(currentLesson = furthest, totalLessons = lessons.size)
        }
}

/**
 * Today's Hijri date, given the day and the user's sighting offset.
 *
 * Takes the date rather than reading the clock, so the caller's `TodayProvider` stays the single
 * answer to "what day is it" — `HijriDateCalculator.today()` reads `LocalDate.now()` internally,
 * which is the frozen-today shape #363 removed from the ViewModel layer.
 */
class GetHijriTodayUseCase @Inject constructor() {
    operator fun invoke(today: LocalDate, offsetDays: Int): HijriDateCalculator.HijriDate =
        HijriDateCalculator.toHijri(today.plusDays(offsetDays.toLong()))
}

/**
 * The user's moon-sighting day offset.
 *
 * A use case rather than a seam member because it is a *read of someone else's* preference: the
 * offset belongs to appearance settings and More only consumes it. Reading it here keeps
 * `SettingsRepository` out of `MoreViewModel`'s constructor without inventing a `MoreSettings`
 * member for a preference More does not own.
 */
class ObserveHijriDayOffsetUseCase @Inject constructor(
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<Int> = settings.hijriDayOffset
}

/**
 * The currency the zakat figures are denominated in — the same setting the calculator uses.
 *
 * More reports a saved zakat amount, so it needs the code to render it with. Read through
 * `ZakatSettings`, not re-derived: two opinions about someone's currency is one too many.
 */
class ObserveZakatCurrencyUseCase @Inject constructor(
    private val settings: ZakatSettings,
) {
    operator fun invoke(): Flow<String> = settings.zakatCurrency
}
