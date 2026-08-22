package com.arshadshah.nimaz

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Every module that declares a `@HiltWorker` must also apply the `androidx.hilt` KSP processor.
 *
 * **This one fails at runtime, not at build time**, which is why it needs a test rather than a
 * convention. `nimaz.android.hilt` supplies Dagger's compiler but deliberately leaves
 * `hilt-work-compiler` to the module that needs it. A module with `implementation(libs.hilt.work)`
 * and no `ksp(libs.hilt.work.compiler)`:
 *
 * - compiles, because `@HiltWorker` is just an annotation;
 * - passes its own `check`, because nothing generated is missing from anything it references;
 * - and then, in the app, generates no `_AssistedFactory`, so `HiltWorkerFactory` has no entry for
 *   the class, returns `null`, and WorkManager falls back to reflecting a
 *   `(Context, WorkerParameters)` constructor — which an `@AssistedInject` worker does not have.
 *
 * The result is `NoSuchMethodException` the first time the Worker actually runs. When
 * `:feature:widget` took the six widget refresh Workers out of `:app` in PR 13 of #551, every
 * local gate was green and six `WidgetWorkersTest` cases died on the emulator.
 *
 * PRs 15–21 move more Workers (`data/audio` and the prayer notification files among them), so the
 * next module to get this wrong would get it wrong the same way.
 */
class HiltWorkerProcessorTest {

    @Test
    fun `every module declaring a HiltWorker applies the androidx hilt processor`() {
        val root = File(REPO_ROOT)
        assertThat(root.isDirectory).isTrue()

        val modules = root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.isFile && it.name == "build.gradle.kts" && it.parentFile != root }
            .map { it.parentFile }
            .toList()

        assertThat(modules.size).isAtLeast(MINIMUM_MODULES)

        val offenders = mutableListOf<String>()
        var withWorkers = 0

        modules.forEach { module ->
            val sources = File(module, "src/main")
            if (!sources.isDirectory) return@forEach

            val declaresWorker = sources.walkTopDown()
                .any { it.isFile && it.extension == "kt" && HILT_WORKER in it.readText() }
            if (!declaresWorker) return@forEach
            withWorkers++

            val buildFile = File(module, "build.gradle.kts").readText()
            if (PROCESSOR !in buildFile) {
                offenders += "${module.name} declares @HiltWorker but no ksp(libs.hilt.work.compiler)"
            }
        }

        // Both modules with Workers today are `:app` and `:feature:widget`. Without this the test
        // passes having found no Workers at all — the ninth instance in this epic of a scan that is
        // green because it looked in the wrong place.
        assertThat(withWorkers).isAtLeast(MINIMUM_MODULES_WITH_WORKERS)
        assertThat(offenders).isEmpty()
    }

    private companion object {
        /** `:app` unit tests run with the module directory as cwd. */
        const val REPO_ROOT = ".."

        const val HILT_WORKER = "@HiltWorker"

        /** Matches `ksp(libs.hilt.work.compiler)` regardless of surrounding formatting. */
        const val PROCESSOR = "libs.hilt.work.compiler"

        /** Ten today (`:app`, six `:core:*`, `:feature:widget`, `:baselineprofile`, `build-logic`). */
        const val MINIMUM_MODULES = 8

        /** `:app` (`AdhanDownloadWorker`) and `:feature:widget` (six refresh Workers). */
        const val MINIMUM_MODULES_WITH_WORKERS = 2
    }
}
