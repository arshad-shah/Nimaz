package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * A `NimazCard` must say how it will be seen.
 *
 * `NimazCard`'s defaults are `style = FILLED` and `tone = NEUTRAL`, and
 * `NimazCardDefaults.tone(NEUTRAL, BASE)` resolves to **`colorScheme.surface`** — which is what
 * most screens use as their own background. So the default card is invisible on the default
 * screen: it lays out, it takes its space, it ripples on tap, and it paints nothing you can see.
 *
 * That is not a hypothetical. It shipped four times before this guard existed — the Prayer Times
 * rows card, its About-this-day card, `NimazAccordion`'s CARD style (which made every carded
 * accordion in settings, tools and the tracker invisible at once), and the Zakat breakdown. Each
 * was found by looking at a render, because nothing else can see it: the code type-checks, the
 * semantics tree is identical, and every assertion passes.
 *
 * **What counts as saying so.** Any one of an explicit `tone`, an `ELEVATED` or `OUTLINED` style,
 * an `elevation`, explicit `colors`, a `gradient`, or `selected` — each gives the card a border,
 * a shadow or a fill that is not the page. A card with none of them is the invisible case.
 *
 * The rule is "be explicit", not "always be MUTED": `surface` is right for a card nested inside a
 * `surfaceContainer` parent. This asks for the choice to be made, not for one answer.
 */
class CardToneGuardTest {

    /** Any one of these makes the card visible against a `surface` background. */
    private val affordances = listOf(
        "tone =", "ELEVATED", "OUTLINED", "elevation =", "colors =", "gradient =", "selected =",
    )

    @Test
    fun `every NimazCard declares how it will be seen`() {
        PresentationSourceRoots.assertAllExist(PresentationSourceRoots.ALL)

        val offenders = mutableListOf<String>()
        var checked = 0

        PresentationSourceRoots.ALL
            .map(::File)
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" }
            // The component's own file declares the defaults under test.
            .filterNot { it.name == "NimazCard.kt" }
            .forEach { file ->
                val text = file.readText()
                var index = text.indexOf(CALL)
                while (index >= 0) {
                    val args = argumentList(text, index + CALL.length - 1)
                    // Prose mentions the component too — several of these files carry a KDoc
                    // explaining why they use `NimazCard(onClick = …)` rather than a wrapping
                    // `Modifier.clickable`. A scan that counts those reports sites that do not
                    // exist, and a patch driven by it edits documentation. Both happened.
                    if (args != null && !isInComment(text, index)) {
                        checked++
                        if (affordances.none { it in args }) {
                            val line = text.substring(0, index).count { it == '\n' } + 1
                            offenders += "${file.name}:$line"
                        }
                    }
                    index = text.indexOf(CALL, index + 1)
                }
            }

        // A guard that silently stops finding its subject is worse than no guard: if a rename
        // makes `NimazCard(` unmatchable, this fails rather than passing over nothing.
        assertThat(checked).isAtLeast(MINIMUM_CALLS)

        assertThat(offenders).isEmpty()
    }

    /** Whether the match at [index] sits on a `//` or KDoc/block-comment line. */
    private fun isInComment(text: String, index: Int): Boolean {
        val lineStart = text.lastIndexOf('\n', index - 1) + 1
        val prefix = text.substring(lineStart, index).trimStart()
        return prefix.startsWith("//") || prefix.startsWith("*") || prefix.startsWith("/*")
    }

    /**
     * The text between the `(` at [open] and its matching `)`, or null if unbalanced.
     *
     * Brace-counting rather than a regex: these calls span up to a dozen lines and contain nested
     * parentheses of their own (`Modifier.padding(14.dp)`), which no single-line pattern survives.
     */
    private fun argumentList(text: String, open: Int): String? {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return text.substring(open + 1, i)
                }
            }
        }
        return null
    }

    private companion object {
        const val CALL = "NimazCard("

        /** Roughly the count at the time of writing, less headroom for deletions. */
        const val MINIMUM_CALLS = 120
    }
}
