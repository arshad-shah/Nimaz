package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A screen in an **extracted feature module** may only `hiltViewModel()` a ViewModel from that
 * same module.
 *
 * `hiltViewModel()` resolves `LocalViewModelStoreOwner`, which inside a destination is that
 * destination's `NavBackStackEntry`. A screen reaching for another feature's ViewModel therefore
 * does not get the instance that feature is driving — it gets a **fresh one scoped to this
 * destination**, holding whatever its constructor put there and nothing the other feature has
 * since done. The code reads like shared state and behaves like a blank object.
 *
 * Not hypothetical. `AdaptiveMoreScreen` did exactly this:
 *
 * ```kotlin
 * val settingsViewModel: SettingsViewModel = hiltViewModel()
 * val shouldRestart by settingsViewModel.shouldRestart.collectAsStateWithLifecycle()
 * LaunchedEffect(shouldRestart) { if (shouldRestart) onRestartApp() }
 * ```
 *
 * `_shouldRestart` is a plain `MutableStateFlow(false)` that only settings actions set, and this
 * instance — scoped to `Route.More` — never performs one. The effect could not fire. The live path
 * was `SettingsScreen`, which makes the identical observation on the destination where the flag
 * does flip, and `AdaptiveMoreScreen` already took `onRestartApp` as a parameter, so nothing
 * depended on the dead copy. It was found by the module split (#551 PR 14) as the sole
 * `:feature:* -> :feature:*` edge in the adaptive layer, which `moduleBoundary` forbids.
 *
 * **A directory under `screens/` is not a feature.** `dua`, `hadith`, `qaida`, `names`, `asma`,
 * `asmaunnabi` and `prophets` are seven directories driving one `viewmodel/content`; `tasbih` and
 * `fasting` drive `viewmodel/tracker`; `khatam` drives `viewmodel/quran`. The unit that matters is
 * the **module**, so that is what [MODULE_OF] maps and what this test compares. Everything not yet
 * extracted is one blob called [APP], and a screen there is unconstrained — inside `:app` a
 * cross-package `hiltViewModel()` is a latent bug, not a compile error, and flagging all ~90 of
 * them would say nothing actionable.
 *
 * **This grows with the epic.** Each PR of #551 that extracts a feature adds its `screens/` and
 * `viewmodel/` packages to [MODULE_OF], and every screen in it comes under the rule that day. That
 * is the point: the constraint should arrive with the module, not after someone has spent a
 * compile failure finding it.
 */
class CrossFeatureViewModelGuardTest {

    @Test
    fun `no screen in an extracted module injects another module's ViewModel`() {
        PresentationSourceRoots.assertAllExist(PresentationSourceRoots.NAVIGABLE)

        val files = PresentationSourceRoots.sources(PresentationSourceRoots.NAVIGABLE)
        assertThat(files.size).isAtLeast(MINIMUM_FILES)

        val offenders = mutableListOf<String>()
        var constrained = 0

        files.forEach { file ->
            val screenPackage = ADAPTIVE_SCREEN_PACKAGE[file.name]
                ?: file.parentFile?.name
                ?: return@forEach
            val module = MODULE_OF[screenPackage] ?: APP
            if (module == APP) return@forEach
            constrained++

            VIEWMODEL_IMPORT.findAll(file.readText()).forEach { match ->
                val viewModelPackage = match.groupValues[1]
                val owner = MODULE_OF[viewModelPackage] ?: APP
                if (owner != module) {
                    offenders += "${file.name} (:feature:$module) injects " +
                        "viewmodel.$viewModelPackage (${label(owner)})"
                }
            }
        }

        // The floor that matters. Every skip above is a `return@forEach`, so a renamed `screens/`
        // or a mistyped package name leaves this test examining nothing and passing — the failure
        // shape this epic has now hit nine times.
        assertThat(constrained).isAtLeast(MINIMUM_CONSTRAINED)
        assertThat(offenders).isEmpty()
    }

    /** `:app` for anything not yet extracted, `:feature:<name>` for anything that is. */
    private fun label(module: String) = if (module == APP) ":app" else ":feature:$module"

    private companion object {
        /** Not yet extracted — still one module, so unconstrained. */
        const val APP = "app"

        /**
         * `screens/` and `viewmodel/` package name -> the feature module that owns it.
         *
         * Only extracted modules appear. Add each module's packages in the PR that extracts it.
         */
        val MODULE_OF = mapOf(
            "onboarding" to "onboarding",
            "about" to "about",
            "help" to "about",
            "more" to "about",
            "search" to "search",
            "ai" to "search",
        )

        /**
         * `adaptive/` two-pane wrappers, whose directory name is not the package they belong to.
         * Only the one owned by an extracted module needs an entry; the rest resolve to [APP].
         */
        val ADAPTIVE_SCREEN_PACKAGE = mapOf("AdaptiveMoreScreen.kt" to "more")

        /**
         * Presentation sources across every module — several hundred. Floored low enough that
         * deletions are never failures, high enough that a mis-rooted scan cannot pass.
         */
        const val MINIMUM_FILES = 100

        /**
         * The 12 files across `screens/{onboarding,about,help,more}` plus `AdaptiveMoreScreen.kt`.
         * Raise this as modules are extracted — it is what proves the rule reached them.
         */
        const val MINIMUM_CONSTRAINED = 13

        /** `import …presentation.viewmodel.<pkg>.Something` — the root (no `<pkg>`) never matches. */
        val VIEWMODEL_IMPORT =
            Regex("""import\s+com\.arshadshah\.nimaz\.presentation\.viewmodel\.(\w+)\.\w+""")
    }
}
