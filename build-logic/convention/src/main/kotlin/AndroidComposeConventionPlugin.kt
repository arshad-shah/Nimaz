import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `nimaz.android.compose` — the Compose compiler plugin and `buildFeatures.compose`.
 *
 * Reacts to whichever AGP plugin the module happens to have rather than presupposing a library:
 * `:app` is an application and applies this plugin too, so it is exercised from day one.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val enableCompose: () -> Unit = {
            extensions.configure(CommonExtension::class.java) {
                buildFeatures.compose = true
            }
        }
        plugins.withType(AppPlugin::class.java) { enableCompose() }
        plugins.withType(LibraryPlugin::class.java) { enableCompose() }
        Unit
    }
}
