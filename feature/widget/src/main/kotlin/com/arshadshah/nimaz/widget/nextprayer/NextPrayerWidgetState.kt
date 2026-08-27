package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.widget.core.nextPrayerIndex
import kotlinx.serialization.Serializable

@Serializable
sealed interface NextPrayerWidgetState {

    /**
     * Whether this state is a reading worth keeping on screen when a refresh fails — see
     * `refreshWidget`.
     *
     * The default state is `Success` with an empty payload — widgets draw em-dash skeletons
     * rather than a spinner before the first worker run — so "is it Success" is not the question.
     * Having an instant to count down to is.
     */
    val hasData: Boolean get() = false

    @Serializable
    data object Loading : NextPrayerWidgetState

    @Serializable
    data class Success(val data: NextPrayerData) : NextPrayerWidgetState {
        override val hasData: Boolean
            get() = data.schedule.isNotEmpty() || data.nextPrayerEpochMillis > 0L
    }

    @Serializable
    data class Error(val message: String?) : NextPrayerWidgetState
}

/**
 * One prayer the widget could show: the canonical name, the already-formatted clock time, and
 * the absolute instant it falls at.
 *
 * The instant is what makes the widget self-correcting. Without it the widget could only show
 * whatever the worker decided was next when it last ran, which goes stale the moment that prayer
 * starts — see [NextPrayerData.schedule].
 */
@Serializable
data class NextPrayerEntry(
    val prayerName: String = "",
    val prayerTime: String = "",
    val isTomorrow: Boolean = false,
    val epochMillis: Long = 0L,
)

@Serializable
data class NextPrayerData(
    /**
     * The canonical English prayer name, **not** display text: `prayerIconRes` and
     * `prayerShortName` both key off it, and the widget localizes it at render time so a
     * language change lands without waiting for the next worker run.
     */
    val prayerName: String = "",
    val prayerTime: String = "",
    /** True when [prayerTime] is not a clock time but "the first prayer of tomorrow". */
    val isTomorrow: Boolean = false,
    val countdown: String = "",
    val isValid: Boolean = true,
    val nextPrayerEpochMillis: Long = 0L,
    /**
     * The whole day's prayers in chronological order, closed by tomorrow's first.
     *
     * The widget picks the next one from this by wall clock on every redraw — once a minute, via
     * the `WidgetUpdateScheduler` tick — instead of showing whichever prayer the worker decided
     * was next up to 15 minutes ago. Without it the widget kept naming a prayer that had already
     * started, with an em dash where its countdown should be, until the next worker run; under
     * Doze that is much longer than 15 minutes. `PrayerTimesWidget` already worked this way; this
     * is the same fix, and the fields above are what it falls back to for state written by a
     * version that did not persist a schedule.
     */
    val schedule: List<NextPrayerEntry> = emptyList(),
)

/**
 * The prayer to show at [nowMillis] — the first one in [NextPrayerData.schedule] still ahead of
 * the clock.
 *
 * Falls back to the flat fields when there is no schedule to choose from: state persisted by a
 * version before [NextPrayerData.schedule] existed, or a load that could not compute any times
 * and named Fajr rather than render an empty widget.
 */
fun NextPrayerData.nextEntry(nowMillis: Long): NextPrayerEntry {
    val index = nextPrayerIndex(schedule.map { it.epochMillis }, nowMillis)
    return schedule.getOrNull(index) ?: NextPrayerEntry(
        prayerName = prayerName,
        prayerTime = prayerTime,
        isTomorrow = isTomorrow,
        epochMillis = nextPrayerEpochMillis,
    )
}

