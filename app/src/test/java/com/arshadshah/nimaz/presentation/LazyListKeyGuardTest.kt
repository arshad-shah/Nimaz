package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression guard: every `items(<collection>)` in a lazy list declares a stable `key`.
 *
 * Without one, Compose identifies a row by its **position**. Two consequences:
 * - Any state remembered inside a row (expanded, swipe offset, a text field) belongs to the
 *   slot rather than to the item, so inserting or deleting anything above it hands that state
 *   to a different row.
 * - Every row after a change is treated as new, so the whole tail recomposes instead of the
 *   one row that actually moved. On the search and location lists — which rebuild on every
 *   keystroke — that is the difference between recomposing one row and recomposing all of them.
 *
 * The `items(count)` and `items(list.size)` overloads take an index and have no key parameter,
 * so they are excluded.
 *
 * `itemsIndexed` is checked too. It was not, and the gap was invisible: an audit counting
 * `items(` on a single line reported 22 keyless sites here, all of which turned out to be
 * multi-line calls whose `key =` sat on the next line — while the one genuinely keyless call in
 * the presentation layer was an `itemsIndexed` the guard never looked at.
 *
 * **It scans every module's presentation sources, not `:app`'s.** It used to name
 * `app/src/main/java/...presentation` directly, so from PR 10 of #551 onwards it was checking a
 * shrinking fraction of the app without saying so - and when `:feature:home` took the last of
 * that directory it failed outright, which is the only reason anybody noticed. Reading
 * [PresentationSourceRoots] is what stops the next module move quietly narrowing it again.
 */
class LazyListKeyGuardTest {

    @Test
    fun `lazy list items and itemsIndexed declare a stable key`() {
        PresentationSourceRoots.assertAllExist()
        val files = PresentationSourceRoots.sources()
        // A floor, not a filter: a mis-rooted scan finds nothing and would otherwise pass.
        assertThat(files.size).isAtLeast(MINIMUM_FILES)

        val callStart = Regex("""(?<![A-Za-z0-9_])items(?:Indexed)?\s*\(""")
        val hasKey = Regex("""\bkey\s*=""")
        val countOverload = Regex("""^\d+$|^[\w.]+\.size$""")

        val offenders = mutableListOf<String>()
        files.forEach { file ->
            val text = file.readText()
            callStart.findAll(text).forEach { match ->
                val args = argumentsOf(text, match.range.last) ?: return@forEach
                if (hasKey.containsMatchIn(args)) return@forEach
                if (countOverload.matches(args.trim())) return@forEach
                val line = text.substring(0, match.range.first).count { it == '\n' } + 1
                offenders += "${file.name}:$line  ${match.value}${args.trim().take(60)})"
            }
        }

        assert(offenders.isEmpty()) {
            "Lazy list items() without a stable key (${offenders.size} site(s)):\n" +
                offenders.joinToString("\n")
        }
    }

    /** The text between the `(` at [openIndex] and its matching `)`, or null if unbalanced. */
    private fun argumentsOf(text: String, openIndex: Int): String? {
        var depth = 0
        for (i in openIndex until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return text.substring(openIndex + 1, i)
                }
            }
        }
        return null
    }

    private companion object {
        /** Presentation sources across every module - several hundred. See the class KDoc. */
        const val MINIMUM_FILES = 100
    }
}
