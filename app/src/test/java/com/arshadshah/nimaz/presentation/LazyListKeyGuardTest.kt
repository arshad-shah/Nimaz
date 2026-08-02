package com.arshadshah.nimaz.presentation

import org.junit.Test
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
 */
class LazyListKeyGuardTest {

    @Test
    fun `lazy list items declare a stable key`() {
        val dir = File("src/main/java/com/arshadshah/nimaz/presentation")
        assert(dir.isDirectory) { "Presentation source dir not found at ${dir.absolutePath}" }

        val callStart = Regex("""(?<![A-Za-z0-9_])items\s*\(""")
        val hasKey = Regex("""\bkey\s*=""")
        val countOverload = Regex("""^\d+$|^[\w.]+\.size$""")

        val offenders = mutableListOf<String>()
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            callStart.findAll(text).forEach { match ->
                val args = argumentsOf(text, match.range.last) ?: return@forEach
                if (hasKey.containsMatchIn(args)) return@forEach
                if (countOverload.matches(args.trim())) return@forEach
                val line = text.substring(0, match.range.first).count { it == '\n' } + 1
                offenders += "${file.name}:$line  items(${args.trim().take(60)})"
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
