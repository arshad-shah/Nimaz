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

    // ── Recommended ──────────────────────────────────────────────────────

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

    @Test
    fun `a plural spec always carries its quantity, and a singular one never does`() {
        assertThat(FastingSubtitles.makeup(pending = 3, fidyaPaid = null)?.quantity).isNotNull()

        val singulars = listOf(
            FastingSubtitles.makeup(pending = 0, fidyaPaid = null),
            FastingSubtitles.makeup(pending = 0, fidyaPaid = "£1"),
        )
        assertThat(singulars.all { it?.quantity == null }).isTrue()
    }
}
