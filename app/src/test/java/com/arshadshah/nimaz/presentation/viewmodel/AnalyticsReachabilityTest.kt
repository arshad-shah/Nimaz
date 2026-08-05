package com.arshadshah.nimaz.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * An analytics call on a branch nothing can reach is worse than no analytics at all: the
 * dashboard reads zero, and zero is indistinguishable from "nobody does this".
 *
 * Three shipped that way — `zakat/calculate`, `quran/play_surah_audio`, and two of the calendar's
 * three instrumented events — and every one was invisible to the whole test suite, because a test
 * can only assert about code it runs. This asserts about code it *doesn't*: for each `onEvent`
 * branch that logs, is there a screen that dispatches that event?
 *
 * Source-scanning rather than reflection, deliberately. The question is "does a producer exist in
 * `presentation/screens/`", which is a fact about the source tree, and reflection cannot see it.
 */
class AnalyticsReachabilityTest {

    private val viewModelDir = File("src/main/java/com/arshadshah/nimaz/presentation/viewmodel")
    private val uiDirs = listOf(
        File("src/main/java/com/arshadshah/nimaz/presentation/screens"),
        File("src/main/java/com/arshadshah/nimaz/presentation/components"),
        File("src/main/java/com/arshadshah/nimaz/core/navigation"),
        File("src/main/java/com/arshadshah/nimaz/presentation/widget"),
    )

    /**
     * The unreachable-analytics backlog, which is now **empty** — and must stay that way.
     *
     * An entry here is a branch that logs and that no screen, component, widget or nav callback
     * dispatches, so its metric reads zero in production and always will. Scanning for #357
     * found **29** of them, against the three #359 had named. Twenty-one were deleted in the
     * layer that introduced this list; the remaining eight were the ones signed off as features
     * someone meant to ship, and their UI landed here — a lesson-navigation footer in the Qaida
     * reader, the zakat breakdown's disclosure, an all-prayers pre-reminder control, hadith
     * grade browsing, editing a custom tasbih, and browsing duas by occasion.
     *
     * The list only ever shrinks, and it has nowhere left to shrink to. A new unreachable
     * analytics branch fails this test on the PR that introduces it, which is the whole point —
     * all three of #359's findings survived because nothing could see them.
     */
    private val accepted = emptySet<String>()

    @Test
    fun `every analytics-bearing event branch has a producer`() {
        val ui = uiDirs.filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { f -> f.extension == "kt" } }
            .joinToString("\n") { it.readText() }

        val unreachable = mutableListOf<String>()

        viewModelDir.walkTopDown().filter { it.name.endsWith("ViewModel.kt") }.forEach { file ->
            val body = file.readText()
            val onEvent = body.substringAfter("fun onEvent(", "").takeIf { it.isNotEmpty() }
                ?: return@forEach

            // `is XxxEvent.Name ->` or `XxxEvent.Name ->`, and whether its branch logs.
            BRANCH.findAll(onEvent).forEach { match ->
                val event = match.groupValues[1]
                val branch = branchBody(onEvent, match.range.last)
                if (!LOGGING.containsMatchIn(branch)) return@forEach
                if (event in accepted) return@forEach
                if (!ui.contains(event)) unreachable += "$event  (${file.name})"
            }
        }

        assertThat(unreachable).isEmpty()
    }

    /**
     * One branch's body: from its `->` to the start of the next branch, or the window's end.
     *
     * This used to be a flat 220-character window cut only on `"\n            is "`, which
     * attributed a **neighbour's** logging to a branch that logged nothing whenever the next
     * branch was `Xxx.Name ->` rather than `is Xxx.Name ->`. That produced false positives the
     * moment an adjacent branch gained a `telemetry` call — `ZakatEvent.SetCurrency`, whose
     * body is a `persist { … }` and logs no usage at all, was reported as an unreachable
     * analytics branch because `ClearAll` two lines below it had just been instrumented.
     *
     * Ending at the next branch of **either** shape is strictly more precise: nothing that
     * genuinely logs stops being seen, and a branch is no longer judged by its neighbours.
     */
    private fun branchBody(onEvent: String, arrowEnd: Int): String {
        val window = onEvent.substring(
            arrowEnd,
            minOf(onEvent.length, arrowEnd + BRANCH_WINDOW),
        )
        val next = NEXT_BRANCH.find(window, startIndex = 1)?.range?.first ?: window.length
        return window.substring(0, next)
    }

    private companion object {
        val BRANCH = Regex("""(?:is\s+)?(\w+Event\.\w+)\s*->""")

        /** The start of the following branch, at `when` indentation, either shape. */
        val NEXT_BRANCH = Regex("""\n {12}(?:is\s+)?\w+Event\.\w+""")
        val LOGGING = Regex("""telemetry\.(featureUsed|search|settingChanged|prayerTracked|fastTracked|aiAnswered)|AppAnalytics\.log""")
        const val BRANCH_WINDOW = 220
    }
}
