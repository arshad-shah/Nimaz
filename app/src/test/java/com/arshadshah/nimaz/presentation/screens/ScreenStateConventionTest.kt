package com.arshadshah.nimaz.presentation.screens

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The three screen states are only consistent if nothing quietly re-rolls them.
 *
 * Source-scanning rather than reflection, for the same reason as
 * [com.arshadshah.nimaz.presentation.viewmodel.AnalyticsReachabilityTest]: the questions
 * here are facts about the source tree — does a screen spin its own spinner, does
 * anything read this error field, does this failure path record something a user could
 * see — and a test that merely *runs* the code cannot answer any of them.
 *
 * Each backlog below was seeded with exactly the violations that existed when the
 * screen-state epic began, and shrank as its layers landed. **All three are empty**, so
 * all three are pure ratchets: the next regression fails the PR that introduces it.
 *
 * Entries only ever come out. If one of these sets ever gains a member, something has
 * gone in the wrong direction and the commit adding it should say why.
 *
 * See `docs/superpowers/specs/2026-08-05-screen-state-migration-design.md`.
 */
class ScreenStateConventionTest {

    private val screensDir = File("src/main/java/com/arshadshah/nimaz/presentation/screens")
    private val viewModelDir = File("src/main/java/com/arshadshah/nimaz/presentation/viewmodel")

    /**
     * Screens that still centre their own `CircularProgressIndicator` instead of using
     * `NimazLoadingState`.
     *
     * **Empty**, as of layer 5 — 25 call sites across 19 screens, gone. This one is a pure
     * ratchet now: the next hand-rolled spinner fails the PR that introduces it.
     */
    private val acceptedSpinners = emptySet<String>()

    /**
     * `UiState` files declaring an `error` that no screen reads — a failure the user is
     * never told about.
     *
     * **Empty**, as of layer 6. Eleven states started here: seven were given a screen that
     * renders them, and four were deleted, because no ViewModel ever assigned them and an
     * error field connected at neither end is not a state — it is a field.
     */
    private val acceptedUnreadErrors = emptySet<String>()

    /**
     * ViewModels with a `launchSafely` that turns a spinner on and cannot turn it off with
     * a reason.
     *
     * **Empty**, as of layer 6 — but the number this reached along the way is the more
     * useful fact. Seeded at 9. After #441 converted every bare `viewModelScope.launch` in
     * the app to `launchSafely` — a strict improvement, since those failures used to reach
     * the uncaught handler — the same question returned **211** call sites, because it was
     * now being asked of every coroutine in every ViewModel rather than of the loads a
     * screen waits on.
     *
     * Answering it 211 times would have meant rubber-stamping, so the question was narrowed
     * instead, to the one this epic can actually assert: **if a launch sets
     * `isLoading = true`, it must be able to set it false with a reason.** That is the
     * defect class — a spinner that never stops — and it left exactly two real sites, both
     * fixed here. A `launchSafely` that only performs a repository write is fire-and-forget
     * by construction: nothing is showing a spinner for it, so there is no stuck state for
     * a failure to strand.
     *
     * What is *not* covered, and is worth its own pass one day: whether those
     * fire-and-forget writes should tell the user. `launchBestEffort` marks the ones where
     * the answer has been considered and is no.
     */
    private val acceptedSilentFailures = emptySet<String>()

    @Test
    fun `no screen rolls its own loading spinner`() {
        val offenders = screensDir.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file -> indeterminateSpinners(file.readText()).isNotEmpty() }
            .map { it.name }
            .toSortedSet()

        // Determinate indicators are deliberately not checked, `Circular` as well as
        // `Linear`: a ring or bar reporting how far along a known-length operation is —
        // the sync transfer, the khatam ring, the widget pin, the fasting day — is not a
        // loading state, and NimazLoadingState cannot express it.
        assertThat(offenders - acceptedSpinners).isEmpty()
        assertThat(acceptedSpinners - offenders).isEmpty()
    }

    @Test
    fun `every UiState error field has a screen that reads it`() {
        val offenders = viewModelDir.walkTopDown()
            .filter { it.name.endsWith("UiState.kt") }
            .filter { file ->
                val text = file.readText()
                "val error" in text || "val errorRes" in text
            }
            .filterNot { file -> featureReadsItsError(file.name) }
            .map { it.name }
            .toSortedSet()

        assertThat(offenders - acceptedUnreadErrors).isEmpty()
        assertThat(acceptedUnreadErrors - offenders).isEmpty()
    }

    @Test
    fun `every launchSafely records the failure for the user`() {
        val offenders = viewModelDir.walkTopDown()
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { file -> silentLaunches(file.readText()).isNotEmpty() }
            .map { it.name }
            .toSortedSet()

        assertThat(offenders - acceptedSilentFailures).isEmpty()
        assertThat(acceptedSilentFailures - offenders).isEmpty()
    }

    /**
     * The hand-rolled **indeterminate** spinners in [source] — the ones that should be
     * `NimazLoadingState`.
     *
     * A `CircularProgressIndicator(progress = …)` is excluded for the same reason the
     * `Linear` ones always were: it reports a fraction of a known-length operation, which
     * is a different thing from "waiting", and the component has nothing to say about it.
     * Two survive on that basis — the sync transfer ring and the khatam progress ring.
     */
    private fun indeterminateSpinners(source: String): List<Int> {
        val found = mutableListOf<Int>()
        var from = 0
        while (true) {
            val at = source.indexOf("CircularProgressIndicator(", from)
            if (at < 0) return found
            from = at + 1
            val lineStart = source.lastIndexOf('\n', at) + 1
            if (source.substring(lineStart, at).trimStart().startsWith("//")) continue
            val call = source.substring(at, minOf(source.length, callEnd(source, at)))
            if (!Regex("""\bprogress\s*=""").containsMatchIn(call)) found += at
        }
    }

    /**
     * The `launchSafely` calls in [source] that record nothing a user could see.
     *
     * Reads each call's own text rather than counting call sites against `onFailure`
     * occurrences, which the first version of this check did. Counting cannot tell which
     * `onFailure` belongs to which call, and — the reason it had to change — it cannot see
     * a `launchSafely` whose inner flow already reports through a `catchAndReport`
     * fallback. Several call sites are that shape and were being reported as silent when
     * they are not.
     *
     * A call is satisfied when it passes `onFailure`, or its block guards a flow with a
     * `catchAndReport` that has a non-empty fallback. `launchBestEffort` is not examined
     * at all: choosing it is choosing to be exempt, out loud.
     */
    private fun silentLaunches(source: String): List<Int> {
        val silent = mutableListOf<Int>()
        var from = 0
        while (true) {
            val at = source.indexOf("launchSafely(", from)
            if (at < 0) return silent
            from = at + 1
            val call = source.substring(at, callEnd(source, at))
            // Only loads a screen waits on. A `launchSafely` that just calls a repository
            // write is fire-and-forget by construction: nothing is showing a spinner for
            // it, so there is no stuck state for a failure to leave behind.
            val context = source.substring(maxOf(0, at - LOOKBEHIND), at) + call
            if (!context.contains("isLoading = true")) continue
            val guarded = Regex("""catchAndReport\([^)]*\)\s*\{[^}]""").containsMatchIn(call)
            if ("onFailure" !in call && !guarded) silent += at
        }
    }

    /**
     * How far back to look for the `isLoading = true` that a `launchSafely` is clearing.
     *
     * Loads write the flag on the line or two above the launch far more often than inside
     * it, so the window has to reach behind the call — but only far enough to catch the
     * same statement group, not the previous function.
     */
    private val LOOKBEHIND = 400

    /** End index of the `launchSafely(...) { ... }` beginning at [start], braces balanced. */
    private fun callEnd(source: String, start: Int): Int {
        var depth = 0
        var seenBody = false
        for (i in start until source.length) {
            when (source[i]) {
                '{' -> { depth++; seenBody = true }
                '}' -> {
                    depth--
                    if (seenBody && depth == 0) return i + 1
                }
            }
        }
        return source.length
    }

    /**
     * True when one of the screens that actually renders [stateFile] reads an error off it.
     *
     * Named files, not a directory. This started as "any screen in the feature's package",
     * which is wrong wherever one package serves several ViewModels: `quran/` holds the
     * screens for `QuranViewModel`, `SurahThematicViewModel`, `TafseerChaptersViewModel`
     * and `QuranTopicsViewModel`, so the moment the thematic screens learned to render an
     * error, the other two states **passed without a line of their code changing**. A
     * false pass in a ratchet is worse than no ratchet: it retires the entry that was
     * supposed to keep the work honest.
     *
     * An unmapped state counts as unread, so a new one has to be declared here — which
     * also makes this map the answer to "which screen shows this state's failures?".
     */
    private fun featureReadsItsError(stateFile: String): Boolean {
        val screens = renderedBy[stateFile] ?: return false
        return screensDir.walkTopDown()
            .filter { it.extension == "kt" && it.name in screens }
            .any { it.readText().contains(Regex("""(state|uiState|phase|\w+State)\.error""")) }
    }

    /** Which screen files render which `UiState` file's failures. */
    private val renderedBy: Map<String, Set<String>> = mapOf(
        "BookmarksUiState.kt" to setOf("BookmarksScreen.kt"),
        "CalendarUiState.kt" to setOf("IslamicCalendarScreen.kt"),
        "DuaUiState.kt" to setOf(
            "DuaCategoryScreen.kt", "DuaOccasionScreen.kt",
            "DuasCollectionScreen.kt", "DuaReaderScreen.kt",
        ),
        "FastingUiState.kt" to setOf("FastTrackerScreen.kt"),
        "HadithUiState.kt" to setOf(
            "HadithCollectionScreen.kt", "HadithChaptersScreen.kt", "HadithReaderScreen.kt",
        ),
        "HelpUiState.kt" to setOf(
            "HelpScreen.kt", "HelpTopicDetailScreen.kt", "HelpGuideScreen.kt",
        ),
        "HomeUiState.kt" to setOf("HomeScreen.kt"),
        // Khatam's errorRes is form validation, read as a TextField supportingText — the
        // right tool for a field error, and out of this epic's scope.
        "KhatamUiState.kt" to setOf("KhatamFormScreen.kt"),
        "LicensesUiState.kt" to setOf("LicensesScreen.kt", "LicenseDetailScreen.kt"),
        "LocationUiState.kt" to setOf("LocationScreen.kt"),
        "NightWorshipUiState.kt" to setOf("NightWorshipScreen.kt"),
        "OnboardingUiState.kt" to setOf("OnboardingScreen.kt"),
        "PrayerTrackerUiState.kt" to setOf("PrayerTrackerScreen.kt"),
        "QiblaUiState.kt" to setOf("QiblaScreen.kt"),
        "QuranUiState.kt" to setOf("QuranHomeScreen.kt", "QuranReaderScreen.kt"),
        "SearchUiState.kt" to setOf("SearchScreen.kt"),
        "SurahThematicUiState.kt" to setOf(
            "SurahBackgroundScreen.kt", "SurahPassagesScreen.kt",
        ),
        "SyncUiState.kt" to setOf("SyncScreen.kt"),
        "TafseerChaptersUiState.kt" to setOf("TafseerChaptersScreen.kt"),
        "TasbihUiState.kt" to setOf("TasbihScreen.kt"),
        "ZakatUiState.kt" to setOf("ZakatCalculatorScreen.kt", "ZakatHistoryScreen.kt"),
    )
}
