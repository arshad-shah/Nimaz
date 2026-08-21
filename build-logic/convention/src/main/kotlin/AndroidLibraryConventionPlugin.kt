import com.android.build.api.dsl.LibraryExtension
import com.arshadshah.nimaz.buildlogic.NimazBuild
import com.arshadshah.nimaz.buildlogic.configureAndroidCommon
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * `nimaz.android.library` — compileSdk 37, minSdk 29, Java 21 and the `param-property` compiler
 * arg, for every `:core:*` and `:feature:*` Android module.
 *
 * **It must never apply `org.jetbrains.kotlin.android`.** AGP 9 provides built-in Kotlin support
 * and applying the standalone plugin alongside it fails the build — the same constraint recorded
 * at `app/build.gradle.kts` and in the root `build.gradle.kts`. `ConventionPluginTest` asserts
 * the negative, because it is the one regression a well-meaning future edit reintroduces silently.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> { configureAndroidCommon(this) }
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.freeCompilerArgs.add(NimazBuild.ANNOTATION_DEFAULT_TARGET_ARG)
        }
    }
}
