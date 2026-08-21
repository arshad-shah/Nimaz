import com.arshadshah.nimaz.buildlogic.NimazBuild
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * `nimaz.jvm.library` — a pure JVM Kotlin module, for `:core:domain`.
 *
 * No Android plugin, so the Android SDK is not on the classpath and a stray `import android.*`
 * in the domain layer becomes a compile error rather than a review comment. That is the point of
 * the module, not a side effect.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = NimazBuild.JAVA_VERSION
            targetCompatibility = NimazBuild.JAVA_VERSION
        }
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(NimazBuild.JVM_TOOLCHAIN)
            compilerOptions.freeCompilerArgs.add(NimazBuild.ANNOTATION_DEFAULT_TARGET_ARG)
        }
    }
}
