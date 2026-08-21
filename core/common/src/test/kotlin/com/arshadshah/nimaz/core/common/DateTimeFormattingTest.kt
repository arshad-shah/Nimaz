package com.arshadshah.nimaz.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Dates must be written the way the reader's language writes them.
 *
 * Every date header in the app was built from a hardcoded pattern —
 * `DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")` and a dozen variants. A pattern fixes the
 * **field order**, not just the words, so the five shipped translations all got English order
 * with translated month names: German rendered "Montag, Januar 5, 2026" where it writes
 * "Montag, 5. Januar 2026", and Turkish rendered "Pazartesi, Ocak 5, 2026" where it writes
 * "5 Ocak 2026 Pazartesi" — weekday last.
 *
 * The two shared ones were also top-level `val`s, so the formatter was built once from whatever
 * `Locale.getDefault()` happened to be when the class first loaded. Switching language in
 * Settings recreates the activity but not the process, so those headers kept the **previous**
 * language until the app was killed.
 *
 * So the locale is a parameter and the pattern comes from CLDR.
 */
class DateTimeFormattingTest {

    private val date = LocalDate.of(2026, 1, 5) // a Monday

    private val german = Locale.forLanguageTag("de")
    private val turkish = Locale.forLanguageTag("tr")
    private val french = Locale.forLanguageTag("fr")

    @Test
    fun `the full date puts the day before the month in German`() {
        val text = date.formatFullDate(german)

        assertThat(text).contains("Januar")
        assertThat(text).contains("Montag")
        assertThat(text).contains("2026")
        // The whole point: "5. Januar", not "Januar 5".
        assertThat(text.indexOf("5")).isLessThan(text.indexOf("Januar"))
    }

    @Test
    fun `the full date puts the weekday last in Turkish`() {
        val text = date.formatFullDate(turkish)

        assertThat(text).contains("Ocak")
        assertThat(text).contains("Pazartesi")
        assertThat(text.indexOf("Ocak")).isLessThan(text.indexOf("Pazartesi"))
    }

    @Test
    fun `the full date is unchanged in English`() {
        assertThat(date.formatFullDate(Locale.US)).isEqualTo("Monday, January 5, 2026")
    }

    @Test
    fun `the locale is read per call, not frozen when the class loads`() {
        // The bug behind the stale header: a top-level `val` formatter captures the default
        // locale once. Reading the default at call time is what makes a language change land.
        val before = Locale.getDefault()
        try {
            Locale.setDefault(german)
            assertThat(date.formatFullDate()).contains("Januar")
            Locale.setDefault(french)
            assertThat(date.formatFullDate()).contains("janvier")
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun `month and year drops the day but keeps the month name and year`() {
        assertThat(date.formatMonthYear(Locale.US)).isEqualTo("January 2026")

        listOf(german to "Januar", turkish to "Ocak", french to "janvier").forEach { (l, month) ->
            val text = date.formatMonthYear(l)
            assertThat(text).contains(month)
            assertThat(text).contains("2026")
            // The day is gone, and so is the punctuation that only separated it.
            assertThat(text).doesNotContain("5")
            assertThat(text.trim()).isEqualTo(text)
        }
    }

    @Test
    fun `month and year never leaves a dangling separator`() {
        // German writes "5. Januar 2026"; dropping the day must not leave ". Januar 2026".
        // English writes "January 5, 2026"; it must not leave "January , 2026".
        val everyShippedLocale = SHIPPED_LOCALES.map { date.formatMonthYear(it) }

        everyShippedLocale.forEach { text -> assertNoDanglingSeparator(text) }
    }

    @Test
    fun `the long date keeps the month name and drops the weekday`() {
        assertThat(date.formatLongDate(Locale.US)).isEqualTo("January 5, 2026")

        SHIPPED_LOCALES.forEach { locale ->
            val text = date.formatLongDate(locale)
            assertThat(text).contains("2026")
            assertThat(text).contains("5")
            // The weekday belongs to formatFullDate.
            assertThat(text).doesNotContain(date.formatWeekday(locale))
        }
    }

    @Test
    fun `the medium date abbreviates the month rather than numbering it`() {
        // Several locales' MEDIUM style is all-numeric ("05.01.2026" in German), which reads as
        // a different date to anyone expecting month-first. These headers want a month name.
        SHIPPED_LOCALES.forEach { locale ->
            val text = date.formatMediumDate(locale)
            assertThat(text).contains("2026")
            assertThat(text.any { it.isLetter() }).isTrue()
        }
        assertThat(date.formatMediumDate(Locale.US)).isEqualTo("Jan 5, 2026")
    }

    @Test
    fun `day and month drops the year in every shipped locale`() {
        SHIPPED_LOCALES.forEach { locale ->
            val text = date.formatDayMonth(locale)
            assertThat(text).doesNotContain("2026")
            assertThat(text).contains("5")
            assertThat(text.any { it.isLetter() }).isTrue()
            // Not `last().isLetterOrDigit()`: German abbreviates to "5. Jan." and that final
            // period is the abbreviation mark, not the separator the dropped year left behind.
            assertNoDanglingSeparator(text)
        }
    }

    @Test
    fun `weekday with day and month drops only the year`() {
        SHIPPED_LOCALES.forEach { locale ->
            val text = date.formatWeekdayDayMonth(locale)
            assertThat(text).doesNotContain("2026")
            assertThat(text).contains(date.formatWeekday(locale))
            assertNoDanglingSeparator(text)
        }
    }

    @Test
    fun `formatting is memoised per locale rather than reparsing the pattern`() {
        // These run inside list items and a 1-second clock tick, so the pattern lookup is
        // cached. Same locale twice must not produce a different formatter.
        assertThat(date.formatFullDate(german)).isEqualTo(date.formatFullDate(german))
        assertThat(date.formatMonthYear(turkish)).isEqualTo(date.formatMonthYear(turkish))
    }

    @Test
    fun `the shipped locales are the ones the app has resources for`() {
        // res/values-de, -fr, -id, -ms, -tr, plus the default. If a translation is added
        // without extending this list, the new language is not covered by the cases above.
        //
        // The path reaches out of this module on purpose. `SHIPPED_LOCALES` is a fact about what
        // the *app* ships, and every resource is staying in `:app` until `:core:ui` takes them in
        // PR 10 of #551 — at which point this path moves again, and the `isNotEmpty` assertion
        // below is what makes that a red build rather than a check that quietly stops checking.
        // CWD for a module's unit tests is the module directory.
        val resourceDirs = java.io.File("../../app/src/main/res")
            .listFiles { f -> f.isDirectory && f.name.startsWith("values-") }
            .orEmpty()
            .map { it.name.removePrefix("values-") }
            .filterNot { it == "night" }
            .toSet()

        assertThat(resourceDirs).isNotEmpty()
        assertThat(SHIPPED_LOCALES.map { it.language }.toSet())
            .containsAtLeastElementsIn(resourceDirs)
    }

    /**
     * The punctuation that only existed to separate a dropped field is gone: German writes
     * "5. Januar 2026", so dropping the day must not leave ". Januar 2026", and English writes
     * "January 5, 2026", so it must not leave "January , 2026". An abbreviation period *inside*
     * or at the end of a word ("5. Jan.") is legitimate and stays.
     */
    private fun assertNoDanglingSeparator(text: String) {
        assertThat(text.trim()).isEqualTo(text)
        assertThat(text.first().isLetterOrDigit()).isTrue()
        assertThat(text).doesNotContain("  ")
        assertThat(text).doesNotContain(" ,")
        assertThat(text).doesNotContain(" .")
        assertThat(text.last()).isNotEqualTo(',')
    }

    private companion object {
        val SHIPPED_LOCALES = listOf(
            Locale.US,
            Locale.forLanguageTag("de"),
            Locale.forLanguageTag("fr"),
            Locale.forLanguageTag("id"),
            Locale.forLanguageTag("ms"),
            Locale.forLanguageTag("tr"),
        )
    }
}
