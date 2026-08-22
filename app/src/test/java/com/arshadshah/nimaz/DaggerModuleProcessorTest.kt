package com.arshadshah.nimaz

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Every module that declares a Dagger `@Module` must run a Hilt processor.
 *
 * **The third forgotten-plugin trap this epic has hit**, after `@HiltWorker` without
 * `hilt-work-compiler` (PR 13) and `@Serializable` without `kotlin-serialization` (PR 19). This
 * one surfaces one step later than a Kotlin compile and in a different project entirely:
 *
 * - the declaring module compiles, because `@Module` and `@InstallIn` are just annotations;
 * - it passes its own `check`, because nothing it references is missing;
 * - `:app:compileDebugKotlin` passes too;
 * - and then `:app:hiltJavaCompileDebug` fails with a `[Dagger/MissingBinding]` for **every type
 *   the forgotten module provided**, naming none of them as the cause.
 *
 * That is exactly what happened when PR 22 of #551 moved `UseCaseModule` into `:core:domain`:
 * sixteen missing bindings, reported against `NimazApp_HiltComponents.java`, for a mistake in a
 * `build.gradle.kts` two modules away. Hilt aggregates `@InstallIn` metadata that its own
 * processor generates, so a project that does not run one contributes nothing, silently.
 *
 * `nimaz.android.hilt` supplies the processor for Android modules. `:core:domain` is a plain JVM
 * module and applies KSP with `hilt-compiler` directly — which is the case this test exists to
 * keep true.
 */
class DaggerModuleProcessorTest {

    @Test
    fun `every module declaring a Dagger module runs a Hilt processor`() {
        val root = File(REPO_ROOT)
        assertThat(root.isDirectory).isTrue()

        val modules = root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.name == "build.gradle.kts" && it.parentFile != root }
            .map { it.parentFile }
            .toList()

        assertThat(modules.size).isAtLeast(MINIMUM_MODULES)

        val offenders = mutableListOf<String>()
        var withModules = 0

        modules.forEach { module ->
            val sources = File(module, "src/main")
            if (!sources.isDirectory) return@forEach

            val declares = sources.walkTopDown().any { file ->
                file.isFile && file.extension == "kt" && INSTALL_IN in file.readText()
            }
            if (!declares) return@forEach
            withModules++

            val buildFile = File(module, "build.gradle.kts").readText()
            val hasProcessor = PROCESSOR_MARKERS.any { it in buildFile }
            if (!hasProcessor) {
                offenders += "${module.name} declares @InstallIn but applies no Hilt processor " +
                    "(neither `nimaz.android.hilt` nor `ksp(libs.hilt.compiler)`)"
            }
        }

        // Six modules declare `@InstallIn` today: `:app`, `:core:domain`, `:core:data`,
        // `:core:database`, `:core:datastore` and `:feature:widget`. Without this the test passes
        // having found none at all, which is the shape this epic keeps catching.
        assertThat(withModules).isAtLeast(MINIMUM_MODULES_WITH_MODULES)
        assertThat(offenders).isEmpty()
    }

    private companion object {
        /** `:app` unit tests run with the module directory as cwd. */
        const val REPO_ROOT = ".."

        val SKIPPED_DIRS = setOf("build", ".git", ".gradle", "node_modules")

        /**
         * `@InstallIn` rather than `@Module`: a Dagger `@Module` that is not installed into a Hilt
         * component is not aggregated at all, so `@InstallIn` is the annotation whose presence
         * actually implies "this project must run a processor".
         */
        const val INSTALL_IN = "@InstallIn"

        /**
         * Either route to a Hilt processor. `nimaz.android.hilt` adds `ksp(libs.hilt.compiler)`
         * for Android modules; a JVM module declares it itself.
         */
        val PROCESSOR_MARKERS = listOf("nimaz.android.hilt", "libs.hilt.compiler")

        /** Twenty-one today; floored well below so a deletion is never a failure. */
        const val MINIMUM_MODULES = 12

        /** `:app`, four `:core:*` and `:feature:widget`. */
        const val MINIMUM_MODULES_WITH_MODULES = 5
    }
}
