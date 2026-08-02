package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.Khatam
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The create/edit khatam form's arithmetic: what a pace preset means in ayahs, and how long the
 * remainder will take at the chosen rate.
 *
 * Both are derived from [Khatam.TOTAL_QURAN_AYAHS] rather than hardcoded, which is the right
 * call but leaves the *rounding* unpinned: 6,236 does not divide evenly by 30, 60 or 120, and a
 * preset that rounds down would leave the reader short of finishing on the day it promises.
 */
class KhatamFormStateTest {

    @Test
    fun `every preset finishes the whole Quran within its promised span`() {
        // Rounding must be up. At 207 ayahs/day (6236/30 rounded down) a "juz a day" khatam is
        // still 26 ayahs short after a month.
        val spans = mapOf(
            KhatamPacePreset.JUZ_DAILY to Khatam.TOTAL_JUZ,
            KhatamPacePreset.HALF_JUZ_DAILY to Khatam.TOTAL_JUZ * 2,
            KhatamPacePreset.QUARTER_JUZ_DAILY to Khatam.TOTAL_JUZ * 4,
        )

        spans.forEach { (preset, days) ->
            val target = checkNotNull(preset.targetAyahs()) { "$preset has no target" }
            assertThat(target * days).isAtLeast(Khatam.TOTAL_QURAN_AYAHS)
            // …but not so generous that it finishes a whole day early.
            assertThat(target * (days - 1)).isLessThan(Khatam.TOTAL_QURAN_AYAHS)
        }
    }

    @Test
    fun `a hand-dialled pace has no target of its own`() {
        assertThat(KhatamPacePreset.CUSTOM.targetAyahs()).isNull()
    }

    @Test
    fun `a target that matches a preset selects it, and any other is custom`() {
        KhatamPacePreset.entries.filter { it != KhatamPacePreset.CUSTOM }.forEach { preset ->
            assertThat(KhatamPacePreset.forTarget(preset.targetAyahs()!!)).isEqualTo(preset)
        }
        assertThat(KhatamPacePreset.forTarget(37)).isEqualTo(KhatamPacePreset.CUSTOM)
        // CUSTOM's null target must not swallow an arbitrary lookup.
        assertThat(KhatamPacePreset.forTarget(0)).isEqualTo(KhatamPacePreset.CUSTOM)
    }

    @Test
    fun `creating a khatam leaves the whole Quran to read`() {
        assertThat(KhatamFormUiState().remainingAyahs).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
    }

    @Test
    fun `the remainder never goes negative`() {
        // A khatam whose recorded reads exceed the total — reachable by syncing two devices
        // that both marked the same ayahs — must show 0 left, not a negative countdown.
        val state = KhatamFormUiState(totalAyahsRead = Khatam.TOTAL_QURAN_AYAHS + 500)

        assertThat(state.remainingAyahs).isEqualTo(0)
        assertThat(state.projectedDays).isNull()
    }

    @Test
    fun `projected days rounds up so the last partial day still counts`() {
        val state = KhatamFormUiState(
            totalAyahsRead = Khatam.TOTAL_QURAN_AYAHS - 25,
            dailyTarget = 10
        )

        assertThat(state.remainingAyahs).isEqualTo(25)
        assertThat(state.projectedDays).isEqualTo(3)
    }

    @Test
    fun `a zero or negative daily target projects nothing rather than forever`() {
        // Without the guard this divides by zero: `ceil(6236.0 / 0)` is Infinity, and
        // `Infinity.toInt()` is Int.MAX_VALUE — a form reading "2147483647 days".
        assertThat(KhatamFormUiState(dailyTarget = 0).projectedDays).isNull()
        assertThat(KhatamFormUiState(dailyTarget = -5).projectedDays).isNull()
    }

    @Test
    fun `a juz-a-day khatam projects about a month`() {
        val target = KhatamPacePreset.JUZ_DAILY.targetAyahs()!!
        val days = KhatamFormUiState(dailyTarget = target).projectedDays

        assertThat(days).isEqualTo(Khatam.TOTAL_JUZ)
    }

    @Test
    fun `the form knows whether it is creating or editing`() {
        assertThat(KhatamFormUiState().isEdit).isFalse()
        assertThat(KhatamFormUiState(mode = KhatamFormMode.Edit(khatamId = 7)).isEdit).isTrue()
    }
}
