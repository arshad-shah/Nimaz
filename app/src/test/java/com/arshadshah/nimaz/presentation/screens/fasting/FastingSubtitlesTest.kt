package com.arshadshah.nimaz.presentation.screens.fasting

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.screens.SubtitleArg
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Fasting's "Go deeper" rows, and what each one claims.
 *
 * Same shape as `MoreSubtitlesTest`, and for the same reason: these are assertions about the app's
 * state rather than labels, so a wrong one is worse than the static text it replaced. Null is
 * again the interesting case — a month with no fasts logged is entirely ordinary, and a row that
 * announces "0 fasted this month" reads as a reproach.
 */
class FastingSubtitlesTest {

    // ── Calendar ─────────────────────────────────────────────────────────

    @Test
    fun `the calendar reports how much of the month is fasted`() {
        val spec = FastingSubtitles.calendar(fastedThisMonth = 18)
        assertThat(spec?.res).isEqualTo(R.plurals.fasting_row_calendar_fasted)
        assertThat(spec?.quantity).isEqualTo(18)
        assertThat(spec?.args).containsExactly(SubtitleArg.Count(18))
    }

    @Test
    fun `an empty month says nothing rather than zero`() {
        assertThat(FastingSubtitles.calendar(fastedThisMonth = 0)).isNull()
        assertThat(FastingSubtitles.calendar(fastedThisMonth = null)).isNull()
    }

    // ── Recommended ──────────────────────────────────────────────────────

    @Test
    fun `today and tomorrow get their own words`() {
        // "In 0 days" is not something anyone says, and "in 1 day" is a clumsier tomorrow.
        assertThat(FastingSubtitles.recommended(0)?.res)
            .isEqualTo(R.string.fasting_row_recommended_today)
        assertThat(FastingSubtitles.recommended(1)?.res)
            .isEqualTo(R.string.fasting_row_recommended_tomorrow)
        assertThat(FastingSubtitles.recommended(0)?.quantity).isNull()
        assertThat(FastingSubtitles.recommended(1)?.quantity).isNull()
    }

    @Test
    fun `further out counts the days as a plural`() {
        val spec = FastingSubtitles.recommended(4)
        assertThat(spec?.res).isEqualTo(R.plurals.fasting_row_recommended_in_days)
        assertThat(spec?.quantity).isEqualTo(4)
    }

    @Test
    fun `a negative distance is not rendered as a countdown`() {
        // Guards against a stale computation phrasing "in -2 days".
        assertThat(FastingSubtitles.recommended(-2)).isNull()
        assertThat(FastingSubtitles.recommended(null)).isNull()
    }

    // ── Makeup ───────────────────────────────────────────────────────────

    @Test
    fun `outstanding makeup fasts are counted`() {
        val spec = FastingSubtitles.makeup(pending = 3, fidyaPaid = null)
        assertThat(spec?.res).isEqualTo(R.plurals.fasting_row_makeup_pending)
        assertThat(spec?.quantity).isEqualTo(3)
    }

    @Test
    fun `nothing outstanding is worth saying, unlike an empty month`() {
        // The asymmetry is deliberate: "you are square" is the answer someone opens this row to
        // get, where "no voluntary fasts this month" is not a question anyone asked.
        assertThat(FastingSubtitles.makeup(pending = 0, fidyaPaid = null)?.res)
            .isEqualTo(R.string.fasting_row_makeup_none)
    }

    @Test
    fun `fidya paid is reported once there is nothing left to make up`() {
        val spec = FastingSubtitles.makeup(pending = 0, fidyaPaid = "£24.00")
        assertThat(spec?.res).isEqualTo(R.string.fasting_row_makeup_fidya)
        assertThat(spec?.args).containsExactly(SubtitleArg.Text("£24.00"))
    }

    @Test
    fun `outstanding fasts outrank a fidya figure`() {
        // Both can be true at once, and what someone owes matters more than what they have paid.
        val spec = FastingSubtitles.makeup(pending = 2, fidyaPaid = "£24.00")
        assertThat(spec?.res).isEqualTo(R.plurals.fasting_row_makeup_pending)
    }

    @Test
    fun `an unloaded count says nothing at all`() {
        assertThat(FastingSubtitles.makeup(pending = null, fidyaPaid = null)).isNull()
        assertThat(FastingSubtitles.makeup(pending = null, fidyaPaid = "£24.00")).isNull()
    }

    @Test
    fun `a blank fidya string is treated as absent`() {
        assertThat(FastingSubtitles.makeup(pending = 0, fidyaPaid = "  ")?.res)
            .isEqualTo(R.string.fasting_row_makeup_none)
    }

    // ── Ramadan ──────────────────────────────────────────────────────────

    @Test
    fun `during Ramadan the useful fact is which day it is`() {
        val spec = FastingSubtitles.ramadan(isRamadan = true, currentDay = 12, daysUntil = null)
        assertThat(spec?.res).isEqualTo(R.string.fasting_row_ramadan_day)
        assertThat(spec?.args).containsExactly(SubtitleArg.Count(12))
    }

    @Test
    fun `outside Ramadan it counts down`() {
        val spec = FastingSubtitles.ramadan(isRamadan = false, currentDay = null, daysUntil = 17)
        assertThat(spec?.res).isEqualTo(R.plurals.fasting_row_ramadan_in_days)
        assertThat(spec?.quantity).isEqualTo(17)

        assertThat(
            FastingSubtitles.ramadan(isRamadan = false, currentDay = null, daysUntil = 0)?.res
        ).isEqualTo(R.string.fasting_row_ramadan_today)
    }

    @Test
    fun `in Ramadan with no day number yet, say nothing rather than day zero`() {
        assertThat(FastingSubtitles.ramadan(isRamadan = true, currentDay = 0, daysUntil = 5))
            .isNull()
        assertThat(FastingSubtitles.ramadan(isRamadan = true, currentDay = null, daysUntil = 5))
            .isNull()
    }

    @Test
    fun `a plural spec always carries its quantity, and a singular one never does`() {
        val plurals = listOf(
            FastingSubtitles.calendar(18),
            FastingSubtitles.recommended(4),
            FastingSubtitles.makeup(pending = 3, fidyaPaid = null),
            FastingSubtitles.ramadan(isRamadan = false, currentDay = null, daysUntil = 17),
        )
        assertThat(plurals.all { it?.quantity != null }).isTrue()

        val singulars = listOf(
            FastingSubtitles.recommended(0),
            FastingSubtitles.recommended(1),
            FastingSubtitles.makeup(pending = 0, fidyaPaid = null),
            FastingSubtitles.makeup(pending = 0, fidyaPaid = "£1"),
            FastingSubtitles.ramadan(isRamadan = true, currentDay = 12, daysUntil = null),
            FastingSubtitles.ramadan(isRamadan = false, currentDay = null, daysUntil = 0),
        )
        assertThat(singulars.all { it?.quantity == null }).isTrue()
    }
}
