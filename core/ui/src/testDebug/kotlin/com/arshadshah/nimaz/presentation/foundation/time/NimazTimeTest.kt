package com.arshadshah.nimaz.presentation.foundation.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The clock-face arithmetic behind every reminder time the app stores.
 *
 * `NimazTime` is what a persisted `"06:00"` becomes, and what the wheel picker edits. Its 12-hour
 * conversions are the classic place to be off by twelve: midnight is 12 AM and not 0 AM, noon is
 * 12 PM and not 0 PM, and `withHour12` has to take the modulo *before* adding the twelve or 12 PM
 * lands at 24:00. A reminder an hour or twelve out is silent until it fires at the wrong time.
 *
 * `parse` is the other half: it reads a value that has been on disk across upgrades, so every
 * malformed shape has to fall back rather than throw on launch.
 */
class NimazTimeTest {

    @Test
    fun `a time renders as the zero-padded value it is stored as`() {
        assertThat(NimazTime(6, 0).toStorageString()).isEqualTo("06:00")
        assertThat(NimazTime(23, 45).toStorageString()).isEqualTo("23:45")
    }

    @Test
    fun `midnight reads as twelve AM, not zero`() {
        // `0 -> 12` in the `when`. Dropping that arm prints "0:00 AM" on the picker.
        assertThat(NimazTime(0, 30).hour12).isEqualTo(12)
        assertThat(NimazTime(0, 30).isPm).isFalse()
    }

    @Test
    fun `noon reads as twelve PM`() {
        // `hour >= 12` — the boundary itself. A `>` here makes noon read as AM.
        assertThat(NimazTime(12, 0).hour12).isEqualTo(12)
        assertThat(NimazTime(12, 0).isPm).isTrue()
    }

    @Test
    fun `afternoon hours convert down`() {
        assertThat(NimazTime(13, 5).hour12).isEqualTo(1)
        assertThat(NimazTime(23, 59).hour12).isEqualTo(11)
        assertThat(NimazTime(11, 59).isPm).isFalse()
    }

    @Test
    fun `setting twelve PM gives noon, not midnight of the next day`() {
        // `hour12 % 12` before adding — without it 12 PM becomes hour 24.
        assertThat(NimazTime(6, 0).withHour12(12, pm = true)).isEqualTo(NimazTime(12, 0))
    }

    @Test
    fun `setting twelve AM gives midnight`() {
        assertThat(NimazTime(6, 0).withHour12(12, pm = false)).isEqualTo(NimazTime(0, 0))
    }

    @Test
    fun `setting an afternoon hour adds the twelve`() {
        assertThat(NimazTime(6, 30).withHour12(3, pm = true)).isEqualTo(NimazTime(15, 30))
        assertThat(NimazTime(6, 30).withHour12(3, pm = false)).isEqualTo(NimazTime(3, 30))
    }

    @Test
    fun `the twelve-hour conversion round-trips for every hour of the day`() {
        // The property that makes the picker safe: reading an hour as 12-hour and writing it back
        // unchanged must not move it. One off-by-twelve anywhere breaks this for twelve hours.
        (0..23).forEach { hour ->
            val time = NimazTime(hour, 30)
            assertThat(time.withHour12(time.hour12, time.isPm)).isEqualTo(time)
        }
    }

    @Test
    fun `a stored value parses back to the time that wrote it`() {
        (0..23).forEach { hour ->
            val time = NimazTime(hour, 15)
            assertThat(NimazTime.parse(time.toStorageString())).isEqualTo(time)
        }
    }

    @Test
    fun `a missing value falls back rather than throwing`() {
        // Every one of these has been on disk at some point across upgrades. A throw here is a
        // crash on launch, not a wrong reminder.
        assertThat(NimazTime.parse(null)).isEqualTo(NimazTime(6, 0))
        assertThat(NimazTime.parse("")).isEqualTo(NimazTime(6, 0))
        assertThat(NimazTime.parse("6")).isEqualTo(NimazTime(6, 0))
        assertThat(NimazTime.parse("aa:bb")).isEqualTo(NimazTime(6, 0))
    }

    @Test
    fun `an out-of-range value falls back`() {
        // `h !in 0..23 || m !in 0..59`. 24:00 is a legal-looking string and not a legal time.
        assertThat(NimazTime.parse("24:00")).isEqualTo(NimazTime(6, 0))
        assertThat(NimazTime.parse("12:60")).isEqualTo(NimazTime(6, 0))
        assertThat(NimazTime.parse("-1:00")).isEqualTo(NimazTime(6, 0))
    }

    @Test
    fun `a caller's own fallback is used`() {
        assertThat(NimazTime.parse(null, fallback = NimazTime(21, 30)))
            .isEqualTo(NimazTime(21, 30))
        assertThat(NimazTime.parse("nonsense", fallback = NimazTime(21, 30)))
            .isEqualTo(NimazTime(21, 30))
    }

    @Test
    fun `extra segments after the minute are ignored rather than rejected`() {
        // `getOrNull(0)` / `getOrNull(1)` — a seconds-bearing value from an older write still
        // parses to the right minute instead of resetting the user's reminder to 06:00.
        assertThat(NimazTime.parse("07:45:00")).isEqualTo(NimazTime(7, 45))
    }
}
