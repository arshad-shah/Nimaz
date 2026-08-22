package com.arshadshah.nimaz

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Extracting a feature module means registering it in four places. This fails if you miss one.
 *
 * The four are not optional and none of them announces itself:
 *
 * | Register in | Missing it means |
 * |---|---|
 * | `PresentationSourceRoots` | four cross-module scans stop covering the module's sources |
 * | `inputs.dir` in `app/build.gradle.kts` | those scans stay `UP-TO-DATE` and do not run at all |
 * | `coverageModules` in `app/build.gradle.kts` | reported coverage *rises*, by measuring less |
 * | `CrossFeatureViewModelGuardTest.MODULE_OF` | the module's screens are exempt from the rule |
 *
 * **Every one of these has actually been missed.** PR 14 of #551 added two modules to
 * `PresentationSourceRoots` and not to `inputs.dir`; PR 15 added two more to
 * `PresentationSourceRoots` and `coverageModules` but not to `MODULE_OF`. Both were caught by
 * chance while doing the next module, and both were silent: nothing fails when a scan quietly
 * stops covering a module, which is the whole reason this epic keeps floors on its scans.
 *
 * A prose checklist in `EPIC.md` did not survive two milestones, so this is the checklist instead.
 * It reads `settings.gradle.kts` as the source of truth for which modules exist — that file cannot
 * be forgotten, because without it the module does not build.
 */
class FeatureModuleRegistrationTest {

    @Test
    fun `every feature module is registered everywhere it has to be`() {
        val modules = featureModules()
        assertThat(modules.size).isAtLeast(MINIMUM_MODULES)

        val appBuildFile = File(APP_BUILD_FILE)
        assertThat(appBuildFile.isFile).isTrue()
        val appBuild = appBuildFile.readText()

        val guardFile = File(GUARD_TEST)
        assertThat(guardFile.isFile).isTrue()
        val guard = guardFile.readText()

        val missing = mutableListOf<String>()

        modules.forEach { module ->
            // A module whose sources are not `presentation/` (only `:feature:widget` so far) is
            // exempt from the presentation-scan registrations, but never from coverage.
            val presentationRoot = File("../feature/$module/src/main/kotlin/com/arshadshah/nimaz/presentation")

            if (presentationRoot.isDirectory) {
                if (PresentationSourceRoots.ALL.none { it.contains("/feature/$module/") }) {
                    missing += "$module: not in PresentationSourceRoots"
                }
                if ("\"feature/$module/src/main/kotlin" !in appBuild) {
                    missing += "$module: no inputs.dir entry in app/build.gradle.kts"
                }
                // Its `screens/` packages must each be mapped, or the cross-feature rule skips them.
                screenPackages(module).forEach { pkg ->
                    if ("\"$pkg\" to " !in guard) {
                        missing += "$module: screens/$pkg not in CrossFeatureViewModelGuardTest.MODULE_OF"
                    }
                }
            }

            if ("gradlePath = \":feature:$module\"" !in appBuild) {
                missing += "$module: not in coverageModules"
            }
        }

        assertThat(missing).isEmpty()
    }

    /** `screens/<pkg>` directories the module ships. */
    private fun screenPackages(module: String): List<String> =
        File("../feature/$module/src/main/kotlin/com/arshadshah/nimaz/presentation/screens")
            .listFiles { f: File -> f.isDirectory }
            ?.map { it.name }
            ?.filterNot { it == "adaptive" }   // mapped by file name, not directory
            ?: emptyList()

    /** `include(":feature:x")` lines in settings.gradle.kts, which cannot be forgotten. */
    private fun featureModules(): List<String> {
        val settings = File(SETTINGS)
        assertThat(settings.isFile).isTrue()
        return INCLUDE.findAll(settings.readText()).map { it.groupValues[1] }.toList()
    }

    private companion object {
        const val SETTINGS = "../settings.gradle.kts"
        const val APP_BUILD_FILE = "build.gradle.kts"
        const val GUARD_TEST =
            "src/test/java/com/arshadshah/nimaz/presentation/CrossFeatureViewModelGuardTest.kt"

        val INCLUDE = Regex("""include\("\:feature\:(\w+)"\)""")

        /** Five today. A parse that finds none must fail rather than pass vacuously. */
        const val MINIMUM_MODULES = 5
    }
}
