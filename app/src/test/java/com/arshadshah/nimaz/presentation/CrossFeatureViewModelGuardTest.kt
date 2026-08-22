package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

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
 * It has caught two real cases so far, and they failed differently:
 *
 * - `AdaptiveMoreScreen` observed `SettingsViewModel.shouldRestart` to call `onRestartApp()`. That
 *   effect **could not fire** — the flag is only set by settings actions this instance never
 *   performs — so it was dead code, deleted in PR 14 of #551.
 * - `DuaReaderScreen`'s "add to tasbih" button dispatched `TasbihEvent.CreateCustomPreset` on a
 *   `TasbihViewModel` of its own. That one **worked**, because the operation is a fire-and-forget
 *   write that reaches the repository whichever instance makes it — so nothing would ever have
 *   reported it. It became `DuaEvent.AddToTasbih` on the feature's own ViewModel in PR 17.
 *
 * The second is the reason this test exists rather than a code review note: a cross-feature
 * `hiltViewModel()` is only *sometimes* visibly broken, and always a compile error later.
 *
 * **A directory under `screens/` is not a feature.** `dua`, `hadith`, `qaida`, `names`, `asma`,
 * `asmaunnabi` and `prophets` are seven directories driving one `viewmodel/content`; `tasbih` and
 * `fasting` drive `viewmodel/tracker`; `khatam` drives `viewmodel/quran`. Screens still in `:app`
 * are unconstrained — inside one module a cross-package `hiltViewModel()` is a latent bug, not a
 * compile error, and flagging all ~90 of them would say nothing actionable.
 *
 * **This grows with the epic.** A file's own module is read from its path, so screens need no
 * registration; each PR that extracts a feature adds only its `viewmodel/` packages to
 * [MODULE_OF], and every screen in the module comes under the rule that day. The constraint
 * should arrive with the module, not after someone has spent a compile failure finding it.
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
            val module = moduleOf(file)
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

        // The floor that matters. Every skip above is a `return@forEach`, so a renamed root or a
        // mistyped path leaves this test examining nothing and passing — the failure shape this
        // epic has now hit a dozen times.
        assertThat(constrained).isAtLeast(MINIMUM_CONSTRAINED)
        assertThat(offenders).isEmpty()
    }

    /** `:app` for anything not yet extracted, `:feature:<name>` for anything that is. */
    private fun label(module: String) = if (module == APP) ":app" else ":feature:$module"

    /**
     * Which module a source file belongs to, read from its **path**.
     *
     * This was a directory-name lookup first — `screens/dua` meant `:feature:content` — and it
     * broke the moment one directory name existed in two modules. PR 17 of #551 left
     * `DuaSettingsScreen` and `HadithSettingsScreen` in `:app` (both drive `SettingsViewModel`, so
     * by the ViewModel axis they belong to the settings feature) while the rest of `screens/dua`
     * and `screens/hadith` moved, and the guard promptly reported two `:app` files as
     * `:feature:content` offenders.
     *
     * A path cannot be wrong in that way, and it needs no map to maintain.
     */
    private fun moduleOf(file: File): String =
        FEATURE_PATH.find(file.path.replace('\\', '/'))?.groupValues?.get(1) ?: APP

    private companion object {
        /** Not yet extracted — still one module, so unconstrained. */
        const val APP = "app"

        /**
         * `presentation/viewmodel/<package>` -> the feature module that owns it.
         *
         * Only ViewModel packages need mapping now that a file's own module comes from its path:
         * this answers "whose ViewModel is this?" for the *import* side. Only extracted modules
         * appear; add each module's ViewModel packages in the PR that extracts it.
         */
        val MODULE_OF = mapOf(
            "onboarding" to "onboarding",
            "about" to "about",
            "help" to "about",
            "more" to "about",
            "tools" to "tools",
            "calendar" to "calendar",
            "search" to "search",
            "ai" to "search",
            "content" to "content",
            "tracker" to "tracker",
        )

        /** `…/feature/<module>/src/…` — the module a source file lives in. */
        val FEATURE_PATH = Regex("/feature/(\\w+)/src/")

        /**
         * Presentation sources across every module — several hundred. Floored low enough that
         * deletions are never failures, high enough that a mis-rooted scan cannot pass.
         */
        const val MINIMUM_FILES = 100

        /**
         * Files in extracted modules that this rule actually reaches. Raise it as modules are
         * extracted — it is what proves the rule got to them, and `FeatureModuleRegistrationTest`
         * is what stops a module being extracted without its ViewModel packages being mapped.
         */
        const val MINIMUM_CONSTRAINED = 60

        /** `import …presentation.viewmodel.<pkg>.Something` — the root (no `<pkg>`) never matches. */
        val VIEWMODEL_IMPORT =
            Regex("""import\s+com\.arshadshah\.nimaz\.presentation\.viewmodel\.(\w+)\.\w+""")
    }
}
