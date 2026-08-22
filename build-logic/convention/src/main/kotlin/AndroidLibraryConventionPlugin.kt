import com.android.build.api.dsl.LibraryExtension
import com.arshadshah.nimaz.buildlogic.NimazBuild
import com.arshadshah.nimaz.buildlogic.configureAndroidCommon
import com.arshadshah.nimaz.buildlogic.isForbiddenModuleDependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * `nimaz.android.library` — compileSdk 37, minSdk 29, Java 21 and the `param-property` compiler
 * arg, for every `:core:*` and `:feature:*` Android module.
 *
 * **It must never apply `org.jetbrains.kotlin.android`.** AGP 9 provides built-in Kotlin support
 * and applying the standalone plugin alongside it fails the build — the same constraint recorded
 * at `app/build.gradle.kts` and in the root `build.gradle.kts`. `ConventionPluginTest` asserts
 * the negative, because it is the one regression a well-meaning future edit reintroduces silently.
 *
 * It also registers **`moduleBoundary`** and wires it into `check`. `:core:domain` gets its
 * purity from `nimaz.jvm.library`'s `androidFreeClasspath`, but an Android library has Android on
 * its classpath by definition, so the rule worth enforcing here is a different one — SPEC §4's
 * *"`:core:*` never depends on `:feature:*`; no `:feature:*` depends on another `:feature:*`;
 * only `:app` depends on features."* Nothing in the compiler objects to a `:core:` module
 * reaching sideways or upward, and #551's whole premise is that a rule enforced by review alone
 * is not enforced.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> { configureAndroidCommon(this) }
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.freeCompilerArgs.add(NimazBuild.ANNOTATION_DEFAULT_TARGET_ARG)
        }

        val guard = tasks.register("moduleBoundary") {
            group = "verification"
            description = "Fails if this module depends on a module it is not allowed to see."

            val modulePath = project.path
            // Read here and captured as plain strings: holding a Configuration or a Project in a
            // task action is a configuration-cache failure, and this build runs with
            // `problems=fail`.
            //
            // *Every* configuration, not a name filter. The obvious filter —
            // `name.endsWith("Implementation") || name.endsWith("Api")` — silently misses the
            // two that matter most, because the base configurations are called `implementation`
            // and `api` with a lower-case first letter. That shipped in the first draft of this
            // task and was caught only by deliberately adding a forbidden dependency and seeing
            // the build stay green.
            val dependedOn = configurations
                .flatMap { configuration ->
                    configuration.dependencies
                        .filterIsInstance<ProjectDependency>()
                        .map { configuration.name to it.path }
                }
                .distinct()

            doLast {
                val offenders = dependedOn.filter { (_, dependency) ->
                    isForbiddenModuleDependency(from = modulePath, to = dependency)
                }
                check(offenders.isEmpty()) {
                    buildString {
                        appendLine("$modulePath depends on a module it must not see:")
                        offenders.forEach { (configuration, dependency) ->
                            appendLine("  - $configuration -> $dependency")
                        }
                        append("SPEC §4: :core:* never depends on :feature:* or :app, and no ")
                        append(":feature:* depends on another :feature:*. Move the shared code ")
                        append("down into a :core module, or invert it behind an interface the ")
                        append("lower module owns.")
                    }
                }
            }
        }
        tasks.named("check") { dependsOn(guard) }
    }
}
