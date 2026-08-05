package com.arshadshah.nimaz.presentation.screens.more

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.arshadshah.nimaz.R

/**
 * One argument inside a [SubtitleSpec].
 *
 * A plain `List<Any>` would not do: some subtitles interpolate a **string resource** (the worship
 * reminder's own translated name), and an `Int` in an untyped arg list is indistinguishable from a
 * count. That distinction only shows up as a bug in a non-English locale, where the row renders a
 * resource id where a name should be.
 */
sealed interface SubtitleArg {
    /** A number to interpolate — and, for a plural, also the quantity. */
    data class Count(val value: Int) : SubtitleArg

    /** Text already resolved by the layer that had it (a formatted amount, a Hijri date). */
    data class Text(val value: String) : SubtitleArg

    /** A string resource to resolve at render time, so it lands in the reader's language. */
    data class Resource(@StringRes val res: Int) : SubtitleArg
}

/**
 * What a More row should say, before anything has resolved it into text.
 *
 * [quantity] is non-null exactly when [res] is a `plurals` resource. The screen switches on it to
 * pick `pluralStringResource` over `stringResource`; getting that wrong throws at render time, in
 * a locale the author may well not read, which is why `MoreSubtitlesTest` asserts the invariant
 * rather than trusting each call site.
 */
data class SubtitleSpec(
    /** A `@StringRes` when [quantity] is null, a `@PluralsRes` when it is not. */
    @StringRes @PluralsRes val res: Int,
    val args: List<SubtitleArg> = emptyList(),
    val quantity: Int? = null,
)

/**
 * Which string each More row reports, given the state behind it.
 *
 * More used to restate itself — every subtitle was a static `stringResource` that said in other
 * words what its own title already said ("Fasting" / "Fasting tracker"). These functions replace
 * that with a claim about the app right now, which means they can be **wrong**, which is why the
 * choosing is kept apart from the rendering: no `Context`, no `stringResource`, no ViewModel. The
 * screen resolves a [SubtitleSpec]; this decides what one to hand it.
 *
 * **Null means the row renders no subtitle**, and it is the most important return value here. It
 * covers two situations that must look identical on screen: the value has not arrived yet, and
 * there is nothing true to say. Neither gets a dash, a spinner, or a zero — a dash reads as a
 * value, a spinner makes a menu look busy, and "0 makeup fasts pending" is noise dressed as
 * information. A row that briefly says nothing is honest.
 *
 * Follows `NotificationHubSubtitles` from #351.
 */
object MoreSubtitles {

    /**
     * How much of today's prayer is logged.
     *
     * A complete day and an untouched one get their own sentences: "5 of 5 logged today" is true
     * but graceless, and "0 of 5 logged today" reads as a scolding fraction rather than an
     * invitation. Both are the common cases.
     */
    fun prayerTracker(logged: Int?, total: Int): SubtitleSpec? {
        if (logged == null || total <= 0) return null
        return when {
            logged <= 0 -> SubtitleSpec(R.string.more_tracker_none)
            logged >= total -> SubtitleSpec(R.string.more_tracker_all)
            else -> SubtitleSpec(
                res = R.string.more_tracker_logged,
                args = listOf(SubtitleArg.Count(logged), SubtitleArg.Count(total)),
            )
        }
    }

    /**
     * Makeup fasts still owed.
     *
     * Zero is silence, not "0 pending": having nothing to make up is the normal state, and a row
     * that announces it every day is a row people stop reading.
     */
    fun fasting(pendingMakeup: Int?): SubtitleSpec? {
        if (pendingMakeup == null || pendingMakeup <= 0) return null
        return SubtitleSpec(
            res = R.plurals.more_fasting_makeup_pending,
            args = listOf(SubtitleArg.Count(pendingMakeup)),
            quantity = pendingMakeup,
        )
    }

    /**
     * The nearest upcoming worship reminder, and how long is left.
     *
     * A non-positive [minutesUntil] is a real state rather than a bug — `NextWorshipResolver`
     * keeps surfacing an occurrence while its window is still open — so it reports "now" instead
     * of counting backwards into "in -3m".
     */
    fun nightWorship(@StringRes nameRes: Int?, minutesUntil: Long?): SubtitleSpec? {
        if (nameRes == null || minutesUntil == null) return null
        val name = SubtitleArg.Resource(nameRes)
        if (minutesUntil <= 0) {
            return SubtitleSpec(res = R.string.more_worship_now, args = listOf(name))
        }
        val hours = (minutesUntil / 60).toInt()
        val minutes = (minutesUntil % 60).toInt()
        // Zero hours is not worth a character of a menu row's subtitle.
        return if (hours == 0) {
            SubtitleSpec(
                res = R.string.more_worship_next_m,
                args = listOf(name, SubtitleArg.Count(minutes)),
            )
        } else {
            SubtitleSpec(
                res = R.string.more_worship_next_hm,
                args = listOf(name, SubtitleArg.Count(hours), SubtitleArg.Count(minutes)),
            )
        }
    }

    /**
     * Where the active khatam has reached, and whether it is keeping up.
     *
     * [daysAgainstPace] is signed — positive is ahead, negative is behind — but the plural
     * quantity is its magnitude, because a plural rule cannot take a negative count and the
     * direction is already in the string.
     *
     * A null pace still reports the juz. On day one there is no meaningful pace yet, and the juz
     * is true regardless; dropping the whole subtitle for a missing half of it would hide the part
     * that is known.
     */
    fun khatam(juz: Int?, daysAgainstPace: Int?): SubtitleSpec? {
        if (juz == null || juz <= 0) return null
        val juzArg = SubtitleArg.Count(juz)
        return when {
            daysAgainstPace == null -> SubtitleSpec(
                res = R.string.more_khatam_juz,
                args = listOf(juzArg),
            )

            daysAgainstPace == 0 -> SubtitleSpec(
                res = R.string.more_khatam_juz_on_pace,
                args = listOf(juzArg),
            )

            daysAgainstPace > 0 -> SubtitleSpec(
                res = R.plurals.more_khatam_juz_ahead,
                args = listOf(juzArg, SubtitleArg.Count(daysAgainstPace)),
                quantity = daysAgainstPace,
            )

            else -> SubtitleSpec(
                res = R.plurals.more_khatam_juz_behind,
                args = listOf(juzArg, SubtitleArg.Count(-daysAgainstPace)),
                quantity = -daysAgainstPace,
            )
        }
    }

    /** The Qaida lesson someone is on. Lesson 0 is not a place anyone is, so it says nothing. */
    fun qaida(currentLesson: Int?, totalLessons: Int): SubtitleSpec? {
        if (currentLesson == null || currentLesson <= 0 || totalLessons <= 0) return null
        return SubtitleSpec(
            res = R.string.more_qaida_lesson,
            args = listOf(SubtitleArg.Count(currentLesson), SubtitleArg.Count(totalLessons)),
        )
    }

    /**
     * This lunar year's zakat: the figure if it has been worked out, or that it has not.
     *
     * The only row needing three states rather than two. "Not calculated this year" is a genuine
     * and useful finding, but saying it before the history query returns would accuse someone of
     * not having done a thing they may well have done, then quietly correct itself — so [loaded]
     * gates it, and an unloaded row says nothing at all.
     *
     * [dueThisYear] arrives **already formatted**: the amount's currency is a Zakat setting, and
     * re-deriving it here would put a second opinion about someone's currency in the app.
     */
    fun zakat(loaded: Boolean, dueThisYear: String?): SubtitleSpec? {
        if (!loaded) return null
        val amount = dueThisYear?.takeIf { it.isNotBlank() }
            ?: return SubtitleSpec(R.string.more_zakat_not_calculated)
        return SubtitleSpec(
            res = R.string.more_zakat_due,
            args = listOf(SubtitleArg.Text(amount)),
        )
    }

    /** Today's Hijri date. Blank is absent — an empty subtitle still costs a row its spacing. */
    fun islamicCalendar(hijriToday: String?): SubtitleSpec? {
        val date = hijriToday?.takeIf { it.isNotBlank() } ?: return null
        return SubtitleSpec(res = R.string.more_calendar_hijri, args = listOf(SubtitleArg.Text(date)))
    }
}
