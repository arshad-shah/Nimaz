package com.arshadshah.nimaz.presentation.viewmodel.more

import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.model.ZakatDefaults

/**
 * What More knows about the app right now.
 *
 * **Every reported field is nullable and defaults to null**, and that is the loading contract:
 * a subtitle that has not resolved renders as *absent*, never as a spinner or a dash. The screen
 * is twenty navigable rows from its first frame — there is nothing to wait for — so a row simply
 * gains its subtitle when the figure arrives.
 *
 * **There is deliberately no `UiError` field.** #454's contract is that a ViewModel setting an
 * error must have a screen that reads it, and the corollary holds here: if the makeup-fast query
 * fails, the honest outcome is *the fasting row has no subtitle*, not a full-screen error state
 * covering nineteen rows that work perfectly. A failed decoration must not block a working menu.
 * Failures go to `Telemetry.failure` and leave the field null. See spec §2.4.
 */
data class MoreUiState(
    /** The pin row. Starts at the defaults rather than empty, so it never flashes blank. */
    val pinnedShortcuts: List<PinnedShortcut> = PinnedShortcut.DEFAULTS,

    /** Prayers logged today, out of how many are trackable. */
    val prayersLogged: Int? = null,
    val prayersTrackable: Int? = null,

    /** Makeup fasts still owed. Zero is a real answer and means the row says nothing. */
    val pendingMakeupFasts: Int? = null,

    /** The nearest upcoming worship reminder, and minutes until its event. */
    val nextWorship: WorshipReminderType? = null,
    val minutesUntilNextWorship: Long? = null,

    /** The juz being read on the active khatam, and days for/against its daily target. */
    val khatamJuz: Int? = null,
    val khatamDaysAgainstPace: Int? = null,

    /** Qaida position, out of the course length. */
    val qaidaLesson: Int? = null,
    val qaidaTotalLessons: Int? = null,

    /**
     * Whether the zakat history query has returned.
     *
     * Zakat is the one row needing three states: not loaded, loaded-with-nothing-this-year, and
     * loaded-with-a-figure. "Not calculated this year" is worth saying, but saying it before the
     * query returns accuses someone of not doing a thing they may well have done.
     */
    val zakatHistoryLoaded: Boolean = false,

    /** This lunar year's saved zakat, unformatted — the screen renders it in [zakatCurrency]. */
    val zakatDueThisYear: Double? = null,
    val zakatCurrency: String = ZakatDefaults.CURRENCY,

    /** Today's Hijri date, pre-formatted by the ViewModel from the user's sighting offset. */
    val hijriToday: String? = null,
) {
    /** Whether another shortcut can be pinned, or the cap has been reached. */
    val canPinMore: Boolean get() = pinnedShortcuts.size < PinnedShortcut.MAX_PINS
}
