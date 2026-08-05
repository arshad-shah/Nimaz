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
 * Each backlog below is seeded with exactly the violations that existed when the
 * screen-state epic began, and shrinks as its layers land. Entries are only ever
 * removed. When a set empties it becomes a pure ratchet: the next regression fails the
 * PR that introduces it.
 *
 * See `docs/superpowers/specs/2026-08-05-screen-state-migration-design.md`.
 */
class ScreenStateConventionTest {

    private val screensDir = File("src/main/java/com/arshadshah/nimaz/presentation/screens")
    private val viewModelDir = File("src/main/java/com/arshadshah/nimaz/presentation/viewmodel")

    /**
     * Screens that still centre their own `CircularProgressIndicator` instead of using
     * `NimazLoadingState`. Emptied by layer 5.
     */
    private val acceptedSpinners = setOf(
        "AsmaUnNabiDetailScreen.kt",
        "BookmarksScreen.kt",
        "DuaReaderScreen.kt",
        "HomeScreen.kt",
        "LocationScreen.kt",
        "QuranHomeScreen.kt",
        "QuranReaderScreen.kt",
        "QuranTopicDetailScreen.kt",
        "QuranTopicsScreen.kt",
        "SearchScreen.kt",
        "SurahInfoScreen.kt",
        // Renders QuranTopicsViewModel's state, not the thematic one — its own group.
        "SurahSubjectsScreen.kt",
        "SyncScreen.kt",
        "TafseerChaptersScreen.kt",
        "TafseerScreen.kt",
    )

    /**
     * `UiState` files declaring an `error` that no screen reads — a failure the user is
     * never told about. Emptied by layers 2-4 and 6.
     */
    private val acceptedUnreadErrors = setOf(
        "BookmarksUiState.kt",
        // Vestigial: no ViewModel ever assigns these. Layer 6 gives them a producer or
        // deletes them — an error field connected at neither end is not a state.
        "FastingUiState.kt",
        "PrayerTrackerUiState.kt",
        "QuranUiState.kt",
        "TasbihUiState.kt",
        "HomeUiState.kt",
        "TafseerChaptersUiState.kt",
        "ZakatUiState.kt",
    )

    /**
     * ViewModels with a `launchSafely` that passes no `onFailure`, so the failure reaches
     * telemetry and the abandoned state still says `isLoading = true`. Emptied by layers 3-4.
     */
    private val acceptedSilentFailures = setOf(
        "AskViewModel.kt",
        "BookmarksViewModel.kt",
        "CalendarViewModel.kt",
        "CatalogViewModel.kt",
        "DuaViewModel.kt",
        "PrayerTrackerViewModel.kt",
        "SearchSettingsViewModel.kt",
        "TafseerViewModel.kt",
        "ZakatViewModel.kt",
    )

    @Test
    fun `no screen rolls its own loading spinner`() {
        val offenders = screensDir.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file ->
                file.readText()
                    .lineSequence()
                    .filterNot { it.trimStart().startsWith("//") }
                    .any { "CircularProgressIndicator(" in it }
            }
            .map { it.name }
            .toSortedSet()

        // A determinate `LinearProgressIndicator` is deliberately not checked: a bar
        // reporting how far along a known-length operation is (widget pin, sync, the
        // fasting day) is not a loading state, and NimazLoadingState cannot express it.
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
            .filter { file ->
                val text = file.readText()
                val launches = Regex("""launchSafely\(""").findAll(text).count()
                val handled = Regex("""onFailure\s*=""").findAll(text).count()
                launches > handled
            }
            .map { it.name }
            .toSortedSet()

        assertThat(offenders - acceptedSilentFailures).isEmpty()
        assertThat(acceptedSilentFailures - offenders).isEmpty()
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
        "ZakatUiState.kt" to setOf("ZakatCalculatorScreen.kt"),
    )
}
