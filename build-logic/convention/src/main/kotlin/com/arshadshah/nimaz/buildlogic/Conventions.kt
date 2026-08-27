package com.arshadshah.nimaz.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

/**
 * The single definition of the values every Nimaz module shares. Before `build-logic` existed
 * these lived inline in `app/build.gradle.kts`; eighteen modules cannot each repeat them.
 */
object NimazBuild {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 29
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
    const val JVM_TOOLCHAIN = 21

    /**
     * Opt in to applying annotations (e.g. Hilt's `@ApplicationContext`) to both the value
     * parameter and the backing field/property. This is the future default and silences the
     * KT-73255 deprecation warning emitted for constructor-injected parameters.
     * See https://youtrack.jetbrains.com/issue/KT-73255
     */
    const val ANNOTATION_DEFAULT_TARGET_ARG = "-Xannotation-default-target=param-property"
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * compileSdk, minSdk and Java 21 — the half of the shared config that lives on the AGP DSL and is
 * identical for `com.android.application` and `com.android.library`.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.compileSdk = NimazBuild.COMPILE_SDK
    extension.defaultConfig.minSdk = NimazBuild.MIN_SDK
    extension.compileOptions.sourceCompatibility = NimazBuild.JAVA_VERSION
    extension.compileOptions.targetCompatibility = NimazBuild.JAVA_VERSION
}

/**
 * Maven groups that mean "this is Android code". Matched exactly or as a `group.` prefix, so
 * `androidx.room` is caught by `androidx` without also catching a hypothetical `androidxfoo`.
 *
 * Used by [JvmLibraryConventionPlugin]'s `androidFreeClasspath` guard. `com.google.android` rather
 * than `com.google`, because Truth, Guava and Gson are all `com.google.*` and all fine on a JVM
 * classpath.
 */
val ANDROID_COMPONENT_GROUPS = listOf(
    "com.android",
    "androidx",
    "com.google.android",
)

/**
 * Whether a module at Gradle path [from] is allowed to depend on the module at [to].
 *
 * SPEC §4 of the multi-module migration (#551): *":core:* never depends on :feature:*; no
 * :feature:* depends on another :feature:*; only :app depends on features."* Enforced by
 * `moduleBoundary`, registered by [AndroidLibraryConventionPlugin].
 *
 * Deliberately prefix-based rather than a per-module allowlist. A list has to be edited every
 * time a module is added, and the one time that is forgotten the rule silently stops applying to
 * the new module — which is the failure mode this whole epic keeps running into. Kept here rather
 * than inside the plugin so it can be unit-tested as the table of cases it is, without spawning
 * a Gradle build per case.
 *
 * **A module never counts as depending on itself.** Every Android module's own test source sets
 * show up as a `ProjectDependency` on the module itself — `debugUnitTestCompileClasspath ->
 * :feature:widget` — and without the first branch below, `:feature:widget` matches the
 * feature-to-feature rule against itself and cannot build at all. That lay dormant from PR 1 of
 * #551 until PR 13: until the first `:feature:*` module existed there was nothing to
 * misclassify, because a `:core:` path matches neither `:app` nor `:feature:`. It failed on that
 * module's very first `check`.
 */
fun isForbiddenModuleDependency(from: String, to: String): Boolean = when {
    from == to -> false
    from.startsWith(":core:") -> to == ":app" || to.startsWith(":feature:")
    from.startsWith(":feature:") -> to == ":app" || to.startsWith(":feature:")
    else -> false
}

/**
 * Makes Robolectric-executed code count toward coverage.
 *
 * Robolectric loads the classes under test through its own instrumenting classloader, and the
 * classes it hands back carry no source location. JaCoCo's agent skips those by default, so a
 * Robolectric test runs, passes, and contributes **nothing** to the report — silently, because
 * a class with no execution data is indistinguishable from a class no test touched.
 *
 * This was not theoretical here. Before this function existed, `:core:database`'s exec file held
 * **42** `com/arshadshah/nimaz` class records; `LegacyUserDataImport` (94 lines, twelve passing
 * Robolectric tests that build a legacy database and run the import) and `ContentArtifactInstaller`
 * (64 lines, thirteen tests) appeared in it **zero times** and reported 0% in the merged report.
 * With `isIncludeNoLocationClasses` the same run records **181** classes and both appear. The
 * repository has 936 `@Test` methods in 154 Robolectric files across nine modules, so this is not
 * a rounding correction to the headline number.
 *
 * `jdk.internal.*` has to be excluded alongside it: those classes also have no location, and
 * instrumenting them fails on a sealed JDK module with `IllegalAccessError` at class-load time.
 * The exclusion is the standard companion to the flag, not an optimisation.
 *
 * Applied through `pluginManager.withPlugin` because the `jacoco` plugin is applied by each
 * module's own `plugins {}` block, which runs *after* the convention plugin — configuring the
 * extension eagerly here would find nothing to configure.
 */
internal fun Project.configureRobolectricCoverage() {
    pluginManager.withPlugin("jacoco") {
        tasks.withType(Test::class.java).configureEach {
            extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}
