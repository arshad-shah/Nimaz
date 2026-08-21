import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `nimaz.android.feature` — library + compose + hilt, the shape every one of the eleven planned
 * `:feature:*` modules has. One id so a feature module's `plugins {}` block is a single line and
 * cannot drift from its siblings.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("nimaz.android.library")
        pluginManager.apply("nimaz.android.compose")
        pluginManager.apply("nimaz.android.hilt")
    }
}
