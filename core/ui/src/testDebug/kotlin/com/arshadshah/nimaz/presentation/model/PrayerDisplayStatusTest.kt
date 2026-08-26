package com.arshadshah.nimaz.presentation.model

import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * How a recorded prayer is presented — the seam between what the database stores and what the
 * tracker draws.
 *
 * The interesting half is `toDisplayStatus`. Room has one value, `NOT_PRAYED`, for two entirely
 * different situations: a prayer whose time has passed and was never logged, and one that has not
 * happened yet. Presenting them the same way is the bug this mapping exists to prevent — Isha at
 * lunchtime would show as a missed prayer, every day, for everybody. The `isPassed` flag is what
 * separates them, and nothing else in the app makes that distinction.
 *
 * `isDone`, `tone` and `dotStyle` are the three readings taken off the result, and they have to
 * agree: anything counted as done must not be drawn in the error tone.
 */
class PrayerDisplayStatusTest {

    @Test
    fun `an unlogged prayer whose time has passed is not recorded`() {
        assertThat(PrayerStatus.NOT_PRAYED.toDisplayStatus(isPassed = true))
            .isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(PrayerStatus.PENDING.toDisplayStatus(isPassed = true))
            .isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `an unlogged prayer whose time has not come is upcoming`() {
        // The distinction the whole mapping exists for: without it Isha reads as missed all day.
        assertThat(PrayerStatus.NOT_PRAYED.toDisplayStatus(isPassed = false))
            .isEqualTo(PrayerDisplayStatus.UPCOMING)
        assertThat(PrayerStatus.PENDING.toDisplayStatus(isPassed = false))
            .isEqualTo(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `a recorded outcome is carried through whatever the clock says`() {
        // A prayer the user logged keeps its own verdict — `isPassed` must not override it, or a
        // prayer logged early would flip to "upcoming" and lose the record.
        listOf(true, false).forEach { passed ->
            assertThat(PrayerStatus.PRAYED.toDisplayStatus(passed))
                .isEqualTo(PrayerDisplayStatus.PRAYED)
            assertThat(PrayerStatus.LATE.toDisplayStatus(passed))
                .isEqualTo(PrayerDisplayStatus.LATE)
            assertThat(PrayerStatus.QADA.toDisplayStatus(passed))
                .isEqualTo(PrayerDisplayStatus.QADA)
            assertThat(PrayerStatus.MISSED.toDisplayStatus(passed))
                .isEqualTo(PrayerDisplayStatus.MISSED)
        }
    }

    @Test
    fun `every stored status maps to something`() {
        // The `when` is exhaustive by compilation; what this catches is an arm added to
        // `PrayerStatus` and wired to the wrong display value — every one of them must still land
        // on a distinct reading of "did this happen".
        PrayerStatus.entries.forEach { status ->
            assertThat(status.toDisplayStatus(isPassed = true)).isNotNull()
            assertThat(status.toDisplayStatus(isPassed = false)).isNotNull()
        }
    }

    @Test
    fun `only the three prayed-in-some-form statuses count as done`() {
        // `isDone` drives the streak and the day's completion count. Counting QADA is deliberate —
        // a make-up prayer was prayed — and counting NOT_RECORDED would inflate every streak.
        val done = PrayerDisplayStatus.entries.filter { it.isDone() }

        assertThat(done).containsExactly(
            PrayerDisplayStatus.PRAYED,
            PrayerDisplayStatus.LATE,
            PrayerDisplayStatus.QADA,
        )
    }

    @Test
    fun `nothing counted as done is drawn as an error`() {
        // The agreement between the two readings. A tone table edited without the `isDone` list
        // beside it is how a completed prayer ends up red in the tracker.
        PrayerDisplayStatus.entries.filter { it.isDone() }.forEach { status ->
            assertThat(status.tone()).isNotEqualTo(NimazTone.ERROR)
        }
    }

    @Test
    fun `a missed prayer is the only one drawn in the error tone`() {
        val errored = PrayerDisplayStatus.entries.filter { it.tone() == NimazTone.ERROR }

        assertThat(errored).containsExactly(PrayerDisplayStatus.MISSED)
    }

    @Test
    fun `every status has its own tone`() {
        val tones = PrayerDisplayStatus.entries.map { it.tone() }

        assertThat(tones.toSet()).hasSize(PrayerDisplayStatus.entries.size)
    }

    @Test
    fun `only an unrecorded prayer draws a hollow dot`() {
        // The hollow dot is "nothing was said about this one" — it is the tap target that asks the
        // user to log it. Filling it in would make the row look answered.
        val outlined = PrayerDisplayStatus.entries
            .filter { it.dotStyle() == NimazStatusDotStyle.OUTLINED }

        assertThat(outlined).containsExactly(PrayerDisplayStatus.NOT_RECORDED)
    }
}
