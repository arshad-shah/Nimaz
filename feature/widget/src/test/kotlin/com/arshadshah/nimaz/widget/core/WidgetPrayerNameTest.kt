package com.arshadshah.nimaz.widget.core

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.foundation.tokens.prayerShortNameRes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The home-screen widgets must name prayers in the reader's language.
 *
 * `widget_prayer_short_fajr` and its siblings are already translated into all five shipped
 * locales — Turkish carries "Sabah / Öğle / İkin. / Akşam / Yatsı" — and `WidgetsScreen`'s
 * in-app *preview* of the widgets resolves them properly. The widgets themselves did not:
 * `PrayerTimesWidget` and `PrayerTrackerWidget` built their rows from `"Fajr"`, `"Dhuhr"`,
 * `"Asr"`, `"Maghrib"`, `"Isha"` as Kotlin literals, and `HijriCalendarWidget` labelled its
 * columns `"Su"`…`"Sa"`. So a Turkish reader saw their own language in the settings preview and
 * English on the actual widget.
 *
 * The translation was never the missing part — the lookup was.
 *
 * `prayerShortNameRes` itself is tested in `:core:ui`'s `PrayerShortNamesTest` as of PR 21 of
 * #551, where the function moved. What is still tested here is what still lives here: the icon
 * lookup, the weekday initials, and the sweep over the widget sources for English literals. The
 * one case below reads the name lookup only to check it agrees with the icon lookup.
 */
class WidgetPrayerNameTest {

    @Test
    fun `the name and icon lookups agree on which prayers exist`() {
        // They are read side by side in the same row, so a name resolving where the icon does
        // not — or the reverse — puts a labelled row next to a default sun.
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Zuhr").forEach { prayer ->
            assertThat(prayerShortNameRes(prayer)).isNotNull()
            assertThat(prayerIconRes(prayer)).isNotEqualTo(0)
        }
    }

    @Test
    fun `no widget names a prayer or a weekday with an English literal`() {
        // Reads the sources directly, in the shape of WidgetGlyphGuardTest; runs from the
        // module dir. The widgets render outside the app's own composition, so a hardcoded
        // string here is invisible to every other localization check.
        val english = Regex(
            """"(Fajr|Sunrise|Dhuhr|Zuhr|Asr|Maghrib|Isha|Location|Tomorrow""" +
                    """|Su|Mo|Tu|We|Th|Fr|Sa)""""
        )

        val offenders = File("src/main/java/com/arshadshah/nimaz/widget").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    // Comments are exempt: the KDoc on `prayerShortNameRes` quotes the very
                    // literals it exists to replace, and that explanation should stay.
                    val trimmed = line.trim()
                    val isComment = trimmed.startsWith("//") || trimmed.startsWith("*") ||
                            trimmed.startsWith("/*")
                    val code = line.substringBefore("//")
                    if (!isComment && english.containsMatchIn(code)) {
                        "${file.name}:${index + 1}: $trimmed"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertThat(offenders).isEmpty()
    }

    @Test
    fun `weekday initials come from the locale rather than a hardcoded list`() {
        val german = java.util.Locale.forLanguageTag("de")
        val labels = weekdayInitials(german)

        assertThat(labels).hasSize(7)
        // German abbreviates Monday "Mo" and Sunday "So" — the hardcoded list said "Su".
        assertThat(labels).contains("So")
        assertThat(labels).doesNotContain("Su")
    }

    @Test
    fun `weekday initials start on the day the widget grid starts on`() {
        // The Hijri calendar grid lays out Sunday-first, so the labels must too or every
        // column is captioned with the wrong day.
        assertThat(weekdayInitials(java.util.Locale.US).first()).isEqualTo("Su")
        assertThat(weekdayInitials(java.util.Locale.US))
            .containsExactly("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").inOrder()
    }
}
