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
     * The **existing** unreachable-analytics backlog, captured so this test can be a ratchet.
     *
     * Every entry is a branch that logs and that no screen, component, widget or nav callback
     * dispatches — so its metric reads zero in production and always will. They are listed
     * rather than fixed here because each is a wire-or-delete **product** decision, which is
     * exactly what #357 tracks: whether "start a tasbih session" or "filter hadith by grade" is
     * a feature someone meant to ship or a leftover.
     *
     * #359 names three of these. Scanning found **29**. Twenty-one were deleted in the layer
     * that introduced this list; the eight below are the ones signed off to be **wired** as
     * features, and they leave when their UI lands.
     *
     * The list only ever shrinks. A new unreachable analytics branch fails this test on the PR
     * that introduces it, which is the whole point — all three of #359's findings survived
     * because nothing could see them.
     */
    private val accepted = setOf(
        "DuaEvent.LoadDuasByOccasion",
        "HadithEvent.FilterByGrade",
        "QaidaReaderEvent.PreviousLesson",
        "QaidaReaderEvent.Resume",
        "SettingsEvent.SetReminderMinutes",
        "SettingsEvent.SetShowReminderBefore",
        "TasbihEvent.UpdateCustomPreset",
        "ZakatEvent.ToggleBreakdown",
    )

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
                val branch = onEvent.substring(
                    match.range.last,
                    minOf(onEvent.length, match.range.last + BRANCH_WINDOW),
                )
                val logs = LOGGING.containsMatchIn(branch.substringBefore("\n            is ")
                    .substringBefore("\n            Xxx"))
                if (!logs) return@forEach
                if (event in accepted) return@forEach
                if (!ui.contains(event)) unreachable += "$event  (${file.name})"
            }
        }

        assertThat(unreachable).isEmpty()
    }

    private companion object {
        val BRANCH = Regex("""(?:is\s+)?(\w+Event\.\w+)\s*->""")
        val LOGGING = Regex("""telemetry\.(featureUsed|search|settingChanged|prayerTracked|fastTracked)|AppAnalytics\.log""")
        const val BRANCH_WINDOW = 220
    }
}
