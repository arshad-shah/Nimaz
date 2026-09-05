package com.arshadshah.nimaz.presentation

import org.junit.Test
import com.arshadshah.nimaz.testing.PresentationSourceRoots
import java.io.File

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
 */
class LazyListKeyGuardTest {

    @Test
    fun `lazy list items and itemsIndexed declare a stable key`() {
        // Scans every presentation root, not just `:app`'s — which no longer has one. The code
        // this guards moved into the feature modules with #551 and the `:feature:home`
        // extraction; a guard still pointing at `:app` would pass by scanning nothing.
        PresentationSourceRoots.assertAllExist(PresentationSourceRoots.ALL)
        val roots = PresentationSourceRoots.ALL.map { File(it) }

        val callStart = Regex("""(?<![A-Za-z0-9_])items(?:Indexed)?\s*\(""")
        val hasKey = Regex("""\bkey\s*=""")
        val countOverload = Regex("""^\d+$|^[\w.]+\.size$""")

        val offenders = mutableListOf<String>()
        roots.asSequence().flatMap { it.walkTopDown() }.filter { it.isFile && it.extension == "kt" }.forEach { file ->
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
}
