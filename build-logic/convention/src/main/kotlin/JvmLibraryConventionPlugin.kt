import com.arshadshah.nimaz.buildlogic.ANDROID_COMPONENT_GROUPS
import com.arshadshah.nimaz.buildlogic.NimazBuild
import com.arshadshah.nimaz.buildlogic.configureRobolectricCoverage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * `nimaz.jvm.library` — a pure JVM Kotlin module, for `:core:domain`.
 *
 * No Android plugin, so the Android SDK is not on the classpath and a stray `import android.*`
 * in the domain layer becomes a compile error rather than a review comment. That is the point of
 * the module, not a side effect.
 *
 * The compiler alone does not keep it that way. Nothing stops someone adding
 * `implementation(libs.androidx.core.ktx)` to the module a month from now — the Android classes
 * would resolve, everything would compile, and no gate would object. So the plugin also registers
 * **`androidFreeClasspath`** and wires it into `check`: it walks every resolvable `*Classpath`
 * configuration and fails on any component from an Android group. A screenshot in a PR
 * description proves the rule held on the day of the PR; this proves it on every build.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureRobolectricCoverage()

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = NimazBuild.JAVA_VERSION
            targetCompatibility = NimazBuild.JAVA_VERSION
        }
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(NimazBuild.JVM_TOOLCHAIN)
            compilerOptions.freeCompilerArgs.add(NimazBuild.ANNOTATION_DEFAULT_TARGET_ARG)
        }

        val guard = tasks.register("androidFreeClasspath") {
            group = "verification"
            description = "Fails if any Android artifact reaches this pure JVM module's classpath."

            // Every resolvable classpath, not just `compileClasspath`: Robolectric or
            // `androidx.test` arriving on the *test* classpath would mean the domain tests had
            // quietly stopped being plain JVM tests, which is half of what this module buys.
            //
            // Held as Providers rather than resolved here. Resolving a configuration at
            // configuration time is both a configuration-cache problem — and this build runs with
            // `problems=fail` — and a needless cost on every build that does not run the check.
            val classpaths = configurations
                .matching { it.name.endsWith("Classpath") && it.isCanBeResolved }
                .associate { it.name to it.incoming.artifacts.resolvedArtifacts }
            val projectPath = project.path

            doLast {
                val offenders = classpaths.entries
                    .flatMap { (name, artifacts) ->
                        artifacts.get()
                            .mapNotNull { it.id.componentIdentifier as? ModuleComponentIdentifier }
                            .filter { id -> ANDROID_COMPONENT_GROUPS.any { id.group == it || id.group.startsWith("$it.") } }
                            .map { "$name -> ${it.group}:${it.module}:${it.version}" }
                    }
                    .distinct()
                    .sorted()

                check(offenders.isEmpty()) {
                    buildString {
                        appendLine("$projectPath is a pure JVM module and must stay Android-free.")
                        appendLine("These Android components are on its classpath:")
                        offenders.forEach { appendLine("  - $it") }
                        append("Invert the dependency behind a domain port instead — ")
                        append("see docs/ARCHITECTURE.md §2 and the WidgetRefresher / PrayerAlarmScheduler seams.")
                    }
                }
            }
        }
        tasks.named("check") { dependsOn(guard) }
    }
}
