package com.arshadshah.nimaz.presentation.screens.more

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.screens.SubtitleArg
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * More stopped describing itself and started reporting. Each row's subtitle is therefore a
 * claim about the app's state, and a wrong one is worse than the static text it replaced —
 * "4 of 5 logged today" that is actually yesterday's count is a lie the old copy could not tell.
 *
 * Null is the interesting case throughout: where there is nothing true to say, the row says
 * nothing. It does not fall back to a dash, a spinner, or a restatement of its own title. A dash
 * reads as a value.
 */
class MoreSubtitlesTest {

    // ── Prayer tracker ───────────────────────────────────────────────────

    @Test
    fun `the tracker reports how much of today is logged`() {
        val spec = MoreSubtitles.prayerTracker(logged = 4, total = 5)
        assertThat(spec?.res).isEqualTo(R.string.more_tracker_logged)
        assertThat(spec?.args).containsExactly(
            SubtitleArg.Count(4),
            SubtitleArg.Count(5),
        ).inOrder()
    }

    @Test
    fun `a full day and an empty one each get their own sentence`() {
        // "5 of 5 logged today" is true but graceless, and "0 of 5" reads as a scolding
        // fraction. Both are common enough to be worth their own copy.
        assertThat(MoreSubtitles.prayerTracker(logged = 5, total = 5)?.res)
            .isEqualTo(R.string.more_tracker_all)
        assertThat(MoreSubtitles.prayerTracker(logged = 0, total = 5)?.res)
            .isEqualTo(R.string.more_tracker_none)
    }

    @Test
    fun `a row with nothing true to say has no subtitle`() {
        // Not yet loaded — absent, never "—".
        assertThat(MoreSubtitles.prayerTracker(logged = null, total = 5)).isNull()
        assertThat(MoreSubtitles.khatam(juz = null, daysAgainstPace = null)).isNull()
        assertThat(MoreSubtitles.qaida(currentLesson = null, totalLessons = 21)).isNull()
        assertThat(MoreSubtitles.islamicCalendar(hijriToday = null)).isNull()
        assertThat(MoreSubtitles.nightWorship(nameRes = null, minutesUntil = 312)).isNull()
    }

    @Test
    fun `a total of zero is a bad question, not a subtitle`() {
        // Guards the divide-by-nothing shape: no prayers to log means the row has no fraction
        // to report, and "0 of 0" is not a report.
        assertThat(MoreSubtitles.prayerTracker(logged = 0, total = 0)).isNull()
    }

    // ── Fasting ──────────────────────────────────────────────────────────

    @Test
    fun `no makeup fasts pending means no subtitle rather than zero pending`() {
        assertThat(MoreSubtitles.fasting(pendingMakeup = 0)).isNull()
        assertThat(MoreSubtitles.fasting(pendingMakeup = null)).isNull()
    }

    @Test
    fun `makeup fasts are counted as a plural, with the quantity carried`() {
        val spec = MoreSubtitles.fasting(pendingMakeup = 3)
        assertThat(spec?.res).isEqualTo(R.plurals.more_fasting_makeup_pending)
        // The quantity has to travel separately: Turkish and Malay do not pluralise like
        // English, so the screen needs pluralStringResource, not getString.
        assertThat(spec?.quantity).isEqualTo(3)
        assertThat(spec?.args).containsExactly(SubtitleArg.Count(3))
    }

    // ── Night worship ────────────────────────────────────────────────────

    @Test
    fun `night worship names the reminder and how long is left`() {
        val spec = MoreSubtitles.nightWorship(
            nameRes = R.string.worship_tahajjud_name,
            minutesUntil = 312,
        )
        assertThat(spec?.res).isEqualTo(R.string.more_worship_next_hm)
        assertThat(spec?.args).containsExactly(
            SubtitleArg.Resource(R.string.worship_tahajjud_name),
            SubtitleArg.Count(5),
            SubtitleArg.Count(12),
        ).inOrder()
    }

    @Test
    fun `under an hour drops the hours part instead of saying zero hours`() {
        val spec = MoreSubtitles.nightWorship(
            nameRes = R.string.worship_witr_name,
            minutesUntil = 42,
        )
        assertThat(spec?.res).isEqualTo(R.string.more_worship_next_m)
        assertThat(spec?.args).containsExactly(
            SubtitleArg.Resource(R.string.worship_witr_name),
            SubtitleArg.Count(42),
        ).inOrder()
    }

    @Test
    fun `a window that has already opened says so rather than counting backwards`() {
        // The resolver keeps surfacing an occurrence while its window is still active, so a
        // non-positive remainder is a real state — and "in -3m" is not a thing to render.
        val spec = MoreSubtitles.nightWorship(
            nameRes = R.string.worship_tahajjud_name,
            minutesUntil = 0,
        )
        assertThat(spec?.res).isEqualTo(R.string.more_worship_now)
        assertThat(MoreSubtitles.nightWorship(R.string.worship_tahajjud_name, -3)?.res)
            .isEqualTo(R.string.more_worship_now)
    }

    // ── Khatam ───────────────────────────────────────────────────────────

    @Test
    fun `khatam leads with the juz and adds the pace when there is one`() {
        val ahead = MoreSubtitles.khatam(juz = 7, daysAgainstPace = 4)
        assertThat(ahead?.res).isEqualTo(R.plurals.more_khatam_juz_ahead)
        assertThat(ahead?.quantity).isEqualTo(4)
        assertThat(ahead?.args)
            .containsExactly(SubtitleArg.Count(7), SubtitleArg.Count(4)).inOrder()

        val behind = MoreSubtitles.khatam(juz = 7, daysAgainstPace = -4)
        assertThat(behind?.res).isEqualTo(R.plurals.more_khatam_juz_behind)
        // The quantity is the magnitude — a plural rule cannot take a negative count, and
        // "behind" is already in the string.
        assertThat(behind?.quantity).isEqualTo(4)
        assertThat(behind?.args)
            .containsExactly(SubtitleArg.Count(7), SubtitleArg.Count(4)).inOrder()
    }

    @Test
    fun `on pace and unknown pace are different subtitles`() {
        assertThat(MoreSubtitles.khatam(juz = 7, daysAgainstPace = 0)?.res)
            .isEqualTo(R.string.more_khatam_juz_on_pace)
        // Pace not computable yet (day one, no logs) — still report the juz, which is true.
        assertThat(MoreSubtitles.khatam(juz = 7, daysAgainstPace = null)?.res)
            .isEqualTo(R.string.more_khatam_juz)
    }

    // ── Qaida ────────────────────────────────────────────────────────────

    @Test
    fun `qaida reports the lesson someone is on`() {
        val spec = MoreSubtitles.qaida(currentLesson = 4, totalLessons = 21)
        assertThat(spec?.res).isEqualTo(R.string.more_qaida_lesson)
        assertThat(spec?.args)
            .containsExactly(SubtitleArg.Count(4), SubtitleArg.Count(21)).inOrder()
    }

    @Test
    fun `a qaida never opened has no lesson to report`() {
        // "Lesson 0 of 21" is not a place anyone is.
        assertThat(MoreSubtitles.qaida(currentLesson = 0, totalLessons = 21)).isNull()
        assertThat(MoreSubtitles.qaida(currentLesson = 4, totalLessons = 0)).isNull()
    }

    // ── Zakat ────────────────────────────────────────────────────────────

    @Test
    fun `zakat distinguishes not-yet-calculated from a saved figure`() {
        assertThat(MoreSubtitles.zakat(loaded = true, dueThisYear = null)?.res)
            .isEqualTo(R.string.more_zakat_not_calculated)
        val due = MoreSubtitles.zakat(loaded = true, dueThisYear = "€1,284.50")
        assertThat(due?.res).isEqualTo(R.string.more_zakat_due)
        assertThat(due?.args).containsExactly(SubtitleArg.Text("€1,284.50"))
    }

    @Test
    fun `zakat says nothing at all until its history has loaded`() {
        // The three-state distinction the other rows do not need: "no calculation this year" is
        // a real finding and worth saying, but it must not be said before the query returns —
        // otherwise the row accuses someone of not having done it, then corrects itself.
        assertThat(MoreSubtitles.zakat(loaded = false, dueThisYear = null)).isNull()
    }

    // ── Islamic calendar ─────────────────────────────────────────────────

    @Test
    fun `the calendar row reports today's Hijri date`() {
        val spec = MoreSubtitles.islamicCalendar(hijriToday = "13 Sha'ban 1447")
        assertThat(spec?.res).isEqualTo(R.string.more_calendar_hijri)
        assertThat(spec?.args).containsExactly(SubtitleArg.Text("13 Sha'ban 1447"))
    }

    @Test
    fun `a blank date is treated as absent, not rendered as an empty subtitle`() {
        assertThat(MoreSubtitles.islamicCalendar(hijriToday = "")).isNull()
        assertThat(MoreSubtitles.islamicCalendar(hijriToday = "   ")).isNull()
    }

    // ── The contract itself ──────────────────────────────────────────────

    @Test
    fun `a plural spec always carries its quantity, and a singular one never does`() {
        // The screen picks pluralStringResource vs stringResource off this field, so a plural
        // resource without a quantity would throw at render time on a device, in a locale the
        // author may not read.
        val plurals = listOf(
            MoreSubtitles.fasting(pendingMakeup = 3),
            MoreSubtitles.khatam(juz = 7, daysAgainstPace = 4),
            MoreSubtitles.khatam(juz = 7, daysAgainstPace = -1),
        )
        assertThat(plurals.all { it?.quantity != null }).isTrue()

        val singulars = listOf(
            MoreSubtitles.prayerTracker(logged = 4, total = 5),
            MoreSubtitles.qaida(currentLesson = 4, totalLessons = 21),
            MoreSubtitles.zakat(loaded = true, dueThisYear = "€1"),
            MoreSubtitles.islamicCalendar(hijriToday = "1 Muharram 1448"),
            MoreSubtitles.nightWorship(R.string.worship_witr_name, 42),
        )
        assertThat(singulars.all { it?.quantity == null }).isTrue()
    }
}
