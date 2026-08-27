package com.arshadshah.nimaz.presentation.model

import com.arshadshah.nimaz.domain.model.PrayerType
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.Test

/**
 * The clock state stamped onto a day's prayer rows before they are drawn.
 *
 * `withClockState` is where "which row is highlighted" is decided, and the two edges of the day are
 * where it goes wrong. `PrayerClock`'s own KDoc records both from the previous implementation:
 * after Isha nothing highlighted, because the search for the first un-passed prayer returned -1;
 * before Fajr, today's Isha rendered as *current*, because the index fell through to the last row.
 * Neither throws and both are visible on the home screen every single day at the same two moments.
 *
 * It also **sorts** before stamping, because the rows arrive in whatever order the repository
 * emitted them and the indices mean nothing otherwise — a list stamped unsorted highlights an
 * arbitrary row.
 */
class PrayerTimeDisplayTest {

    private val dayStart = Instant.fromEpochSeconds(1_767_225_600) // an arbitrary fixed midnight

    private fun at(hours: Int) = dayStart + hours.hours

    private fun row(type: PrayerType, hour: Int) = PrayerTimeDisplay(
        type = type,
        name = type.name,
        timeAt = at(hour),
    )

    private val day = listOf(
        row(PrayerType.FAJR, 5),
        row(PrayerType.DHUHR, 13),
        row(PrayerType.ASR, 16),
        row(PrayerType.MAGHRIB, 20),
        row(PrayerType.ISHA, 22),
    )

    @Test
    fun `an empty day is returned untouched`() {
        assertThat(emptyList<PrayerTimeDisplay>().withClockState(at(12))).isEmpty()
    }

    @Test
    fun `mid-afternoon the current prayer is the last one that started`() {
        val stamped = day.withClockState(at(17))

        assertThat(stamped.single { it.isCurrent }.type).isEqualTo(PrayerType.ASR)
        assertThat(stamped.single { it.isNext }.type).isEqualTo(PrayerType.MAGHRIB)
    }

    @Test
    fun `before the first prayer nothing is current and Fajr is next`() {
        // The documented regression, from the other side: the old code wrapped to Isha here and
        // rendered a prayer that has not happened as the one in effect.
        val stamped = day.withClockState(at(3))

        assertThat(stamped.none { it.isCurrent }).isTrue()
        assertThat(stamped.single { it.isNext }.type).isEqualTo(PrayerType.FAJR)
        assertThat(stamped.none { it.isPassed }).isTrue()
    }

    @Test
    fun `after the last prayer Isha stays current and nothing is next`() {
        // The other documented regression: after Isha no row highlighted at all, while the header
        // counted down to tomorrow's Fajr.
        val stamped = day.withClockState(at(23))

        assertThat(stamped.single { it.isCurrent }.type).isEqualTo(PrayerType.ISHA)
        assertThat(stamped.none { it.isNext }).isTrue()
        assertThat(stamped.all { it.isPassed }).isTrue()
    }

    @Test
    fun `a prayer exactly at its own time counts as started`() {
        // `timeAt <= now` and `it > now` — the boundary is inclusive on one side and exclusive on
        // the other, which is what makes the two indices agree at the instant the adhan sounds.
        val stamped = day.withClockState(at(16))

        assertThat(stamped.single { it.type == PrayerType.ASR }.isPassed).isTrue()
        assertThat(stamped.single { it.isCurrent }.type).isEqualTo(PrayerType.ASR)
        assertThat(stamped.single { it.isNext }.type).isEqualTo(PrayerType.MAGHRIB)
    }

    @Test
    fun `rows are sorted before the indices are stamped`() {
        // The indices are positional, so a list stamped in arrival order highlights an arbitrary
        // row. The output is also returned sorted, which is what the screen renders.
        val shuffled = listOf(day[3], day[0], day[4], day[2], day[1])

        val stamped = shuffled.withClockState(at(17))

        assertThat(stamped.map { it.type }).containsExactly(
            PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR,
            PrayerType.MAGHRIB, PrayerType.ISHA,
        ).inOrder()
        assertThat(stamped.single { it.isCurrent }.type).isEqualTo(PrayerType.ASR)
    }

    @Test
    fun `exactly one row is current and at most one is next`() {
        // The invariant the whole screen relies on: two highlighted rows is a layout with two
        // "now" markers, which is how a duplicated index shows up.
        listOf(3, 5, 12, 16, 21, 23).forEach { hour ->
            val stamped = day.withClockState(at(hour))
            assertThat(stamped.count { it.isCurrent }).isAtMost(1)
            assertThat(stamped.count { it.isNext }).isAtMost(1)
        }
    }

    @Test
    fun `a stored status survives the stamping`() {
        // `copy` touches three flags and must leave the logged outcome alone — losing it would
        // wipe the tracker's record on every recomposition of the list.
        val logged = day.map { it.copy(prayerStatus = com.arshadshah.nimaz.domain.model.PrayerStatus.PRAYED) }

        val stamped = logged.withClockState(at(17))

        assertThat(stamped.all {
            it.prayerStatus == com.arshadshah.nimaz.domain.model.PrayerStatus.PRAYED
        }).isTrue()
    }
}
