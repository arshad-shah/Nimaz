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
        "HelpTopicDetailScreen.kt",
        "HomeScreen.kt",
        "LocationScreen.kt",
        "QuranHomeScreen.kt",
        "QuranReaderScreen.kt",
        "QuranTopicDetailScreen.kt",
        "QuranTopicsScreen.kt",
        "SearchScreen.kt",
        "SurahBackgroundScreen.kt",
        "SurahInfoScreen.kt",
        "SurahPassagesScreen.kt",
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
        "HelpUiState.kt",
        "HomeUiState.kt",
        "SurahThematicUiState.kt",
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
            .filterNot { file -> featureReadsItsError(file.name.removeSuffix("UiState.kt")) }
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
     * True when some screen in [feature]'s own directory reads an error off a state.
     *
     * Deliberately a hand-written directory map rather than a clever regex over state type
     * names. A feature's screens do not always live in a directory named after its
     * `UiState` — `SurahThematicUiState` is read by three screens in `quran/` — and a
     * heuristic that guesses wrong here fails the build for the wrong reason. An
     * unmapped feature counts as unread, so a new one has to be declared rather than
     * silently passing.
     */
    private fun featureReadsItsError(feature: String): Boolean =
        screensDir.walkTopDown()
            .filter { it.extension == "kt" && it.parentFile?.name in featureDirs(feature) }
            .any { it.readText().contains(Regex("""(state|uiState|phase|\w+State)\.error""")) }

    /** Directory names a feature's screens live in. */
    private fun featureDirs(feature: String): Set<String> = when (feature) {
        "Hadith" -> setOf("hadith")
        "Help" -> setOf("help")
        "Dua" -> setOf("dua")
        "Quran", "SurahThematic", "TafseerChapters" -> setOf("quran")
        "Bookmarks" -> setOf("bookmarks")
        // Khatam's errorRes is form validation read as a TextField supportingText, which
        // is the right tool for a field error and out of this epic's scope.
        "Khatam" -> setOf("khatam")
        "Licenses" -> setOf("about")
        "Home" -> setOf("home")
        "Zakat" -> setOf("zakat")
        "Search" -> setOf("search")
        "Sync" -> setOf("settings")
        "Location" -> setOf("settings")
        "Calendar" -> setOf("calendar")
        "Qibla" -> setOf("qibla")
        "NightWorship" -> setOf("worship")
        "PrayerTracker" -> setOf("prayer")
        "Tasbih" -> setOf("tasbih")
        "Fasting" -> setOf("fasting")
        "Onboarding" -> setOf("onboarding")
        else -> emptySet()
    }
}
