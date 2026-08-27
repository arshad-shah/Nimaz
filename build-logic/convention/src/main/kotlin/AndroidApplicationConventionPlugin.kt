import com.android.build.api.dsl.ApplicationExtension
import com.arshadshah.nimaz.buildlogic.NimazBuild
import com.arshadshah.nimaz.buildlogic.configureAndroidCommon
import com.arshadshah.nimaz.buildlogic.configureModuleCoverage
import com.arshadshah.nimaz.buildlogic.configureRobolectricCoverage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * `nimaz.android.application` — the application-side twin of [AndroidLibraryConventionPlugin].
 *
 * `:app` is `com.android.application` and cannot take the library plugin, so the shared config
 * has to exist on both sides. Like the library plugin it does **not** apply
 * `org.jetbrains.kotlin.android`: AGP 9 compiles Kotlin through its built-in support.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> { configureAndroidCommon(this) }
        configureRobolectricCoverage()
        configureModuleCoverage()
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.freeCompilerArgs.add(NimazBuild.ANNOTATION_DEFAULT_TARGET_ARG)
        }
    }
}
