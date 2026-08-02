package com.arshadshah.nimaz.core.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Number of milliseconds in a calendar day. */
const val MILLIS_PER_DAY: Long = 86_400_000L

/**
 * Epoch milliseconds at UTC midnight of this date.
 *
 * This is the canonical key used for date-bucketed database rows (prayer
 * records, fasts, tasbih sessions, …). The codebase previously computed it two
 * equivalent-but-different-looking ways — `toEpochDay() * 86400000L` and
 * `atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000` — which are unified here.
 */
fun LocalDate.toUtcMidnightMillis(): Long = toEpochDay() * MILLIS_PER_DAY

// ── Localized date text ─────────────────────────────────────────────────────────────────────
//
// A `DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")` fixes the **field order**, not only the
// words, so every translation got English order with translated month names — German showed
// "Montag, Januar 5, 2026" where it writes "Montag, 5. Januar 2026", and Turkish put the weekday
// first where it writes it last. The order belongs to the locale, so the pattern comes from CLDR
// via `getLocalizedDateTimePattern` rather than from a literal.
//
// These are functions, not top-level `val`s, for the same reason: a `val` formatter captures
// `Locale.getDefault()` once, when the class first loads. Changing language in Settings recreates
// the activity but not the process, so a cached formatter kept rendering the previous language
// until the app was killed. The locale is read per call and the *formatter* is memoised per
// locale, which is the part worth caching.

/** Full weekday and date, e.g. "Monday, January 5, 2026" / "Montag, 5. Januar 2026". */
fun LocalDate.formatFullDate(locale: Locale = Locale.getDefault()): String =
    format(formatter(FormatStyle.FULL, locale))

/** Date without the weekday, e.g. "January 5, 2026" / "5. Januar 2026". */
fun LocalDate.formatLongDate(locale: Locale = Locale.getDefault()): String =
    format(formatter(FormatStyle.LONG, locale))

/**
 * Date with an abbreviated month, e.g. "Jan 5, 2026" / "5. Jan. 2026".
 *
 * Derived from the LONG pattern with the month shortened rather than from MEDIUM, because
 * several locales' MEDIUM style is all-numeric ("05.01.2026") — which reads as a *different*
 * date to anyone expecting month-first, and is not what these compact headers want.
 */
fun LocalDate.formatMediumDate(locale: Locale = Locale.getDefault()): String =
    format(cached(CacheKey("medium", locale)) { abbreviateMonth(pattern(FormatStyle.LONG, it)) })

/** Month and year, e.g. "January 2026" / "Januar 2026". */
fun LocalDate.formatMonthYear(locale: Locale = Locale.getDefault()): String =
    format(cached(CacheKey("monthYear", locale)) { without(pattern(FormatStyle.LONG, it), 'd') })

/** Month and year, e.g. "January 2026" / "Januar 2026". */
fun YearMonth.formatMonthYear(locale: Locale = Locale.getDefault()): String =
    atDay(1).formatMonthYear(locale)

/** Day and abbreviated month without the year, e.g. "Jan 5" / "5. Jan.". */
fun LocalDate.formatDayMonth(locale: Locale = Locale.getDefault()): String =
    format(cached(CacheKey("dayMonth", locale)) {
        abbreviateMonth(without(pattern(FormatStyle.LONG, it), 'y'))
    })

/** Weekday, day and month without the year, e.g. "Monday, January 5" / "5. Januar Montag". */
fun LocalDate.formatWeekdayDayMonth(locale: Locale = Locale.getDefault()): String =
    format(cached(CacheKey("weekdayDayMonth", locale)) { without(pattern(FormatStyle.FULL, it), 'y') })

/** The weekday on its own, e.g. "Monday" / "Montag". */
fun LocalDate.formatWeekday(locale: Locale = Locale.getDefault()): String =
    dayOfWeek.getDisplayName(TextStyle.FULL, locale)

// ── Pattern derivation ──────────────────────────────────────────────────────────────────────

private data class CacheKey(val shape: String, val locale: Locale)

private val formatters = ConcurrentHashMap<CacheKey, DateTimeFormatter>()

private fun formatter(style: FormatStyle, locale: Locale): DateTimeFormatter =
    cached(CacheKey(style.name, locale)) { pattern(style, it) }

private fun cached(key: CacheKey, buildPattern: (Locale) -> String): DateTimeFormatter =
    formatters.getOrPut(key) { DateTimeFormatter.ofPattern(buildPattern(key.locale), key.locale) }

private fun pattern(style: FormatStyle, locale: Locale): String =
    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        style, null, IsoChronology.INSTANCE, locale
    )

/**
 * [pattern] with every run of [field] removed, and the punctuation that existed only to
 * separate it removed too.
 *
 * Dropping the day from the German "d. MMMM y" naively leaves ". MMMM y", and from the English
 * "MMMM d, y" leaves "MMMM , y" — so separators are dropped alongside: a run of adjacent
 * separators collapses to its first, and separators at either end go entirely.
 */
private fun without(pattern: String, field: Char): String {
    val kept = tokenize(pattern).filterNot { it is Token.Field && it.letter == field }

    val collapsed = kept.filterIndexed { index, token ->
        token !is Token.Separator || kept.getOrNull(index - 1) !is Token.Separator
    }

    return collapsed
        .dropWhile { it is Token.Separator }
        .dropLastWhile { it is Token.Separator }
        .joinToString("") { it.text }
}

/** `MMMM` → `MMM`, so a compact header names the month instead of numbering it. */
private fun abbreviateMonth(pattern: String): String =
    tokenize(pattern).joinToString("") { token ->
        if (token is Token.Field && token.letter == 'M') "MMM" else token.text
    }

private sealed interface Token {
    val text: String

    /** A run of one pattern letter, e.g. `MMMM`. */
    data class Field(override val text: String) : Token {
        val letter: Char get() = text[0]
    }

    /** Punctuation and spacing, or a quoted literal — never a field. */
    data class Separator(override val text: String) : Token
}

/**
 * Splits a pattern into field runs and everything else.
 *
 * Quoted sections are literals, not fields — Spanish writes "d 'de' MMMM 'de' y", and treating
 * that `d` as a day field would mangle it.
 */
private fun tokenize(pattern: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var index = 0

    while (index < pattern.length) {
        val char = pattern[index]
        when {
            char == '\'' -> {
                val close = pattern.indexOf('\'', index + 1)
                val end = if (close == -1) pattern.length else close + 1
                tokens += Token.Separator(pattern.substring(index, end))
                index = end
            }

            char.isLetter() -> {
                var end = index
                while (end < pattern.length && pattern[end] == char) end++
                tokens += Token.Field(pattern.substring(index, end))
                index = end
            }

            else -> {
                var end = index
                while (end < pattern.length && !pattern[end].isLetter() && pattern[end] != '\'') end++
                tokens += Token.Separator(pattern.substring(index, end))
                index = end
            }
        }
    }
    return tokens
}
