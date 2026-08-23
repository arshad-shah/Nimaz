package com.arshadshah.nimaz.buildlogic

import com.arshadshah.nimaz.buildlogic.ConventionFixture.reported
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AndroidLibraryConventionPluginTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun reportOfLibraryFixture(): Map<String, String> {
        val result = ConventionFixture.run(
            dir = File(tmp.root, "lib"),
            plugins = listOf("nimaz.android.library"),
            buildScript = """
                extensions.getByType(com.android.build.api.dsl.LibraryExtension::class.java)
                    .namespace = "com.arshadshah.nimaz.fixture"

                tasks.register("printConventions") {
                    val android = project.extensions.getByType(com.android.build.api.dsl.LibraryExtension::class.java)
                    val kotlin = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension::class.java)
                    val compileSdk = android.compileSdk
                    val minSdk = android.defaultConfig.minSdk
                    val source = android.compileOptions.sourceCompatibility.toString()
                    val target = android.compileOptions.targetCompatibility.toString()
                    val args = kotlin.compilerOptions.freeCompilerArgs.get().joinToString(",")
                    val hasKotlinAndroid = project.pluginManager.hasPlugin("org.jetbrains.kotlin.android")
                    val hasAgpLibrary = project.pluginManager.hasPlugin("com.android.library")
                    doLast {
                        println("REPORT compileSdk=" + compileSdk)
                        println("REPORT minSdk=" + minSdk)
                        println("REPORT sourceCompatibility=" + source)
                        println("REPORT targetCompatibility=" + target)
                        println("REPORT freeCompilerArgs=" + args)
                        println("REPORT hasKotlinAndroidPlugin=" + hasKotlinAndroid)
                        println("REPORT hasAgpLibrary=" + hasAgpLibrary)
                    }
                }
            """.trimIndent()
        )
        return result.reported()
    }

    @Test
    fun `applies the AGP library plugin and the shared SDK and Java levels`() {
        val report = reportOfLibraryFixture()
        assertThat(report["hasAgpLibrary"]).isEqualTo("true")
        assertThat(report["compileSdk"]).isEqualTo("37")
        assertThat(report["minSdk"]).isEqualTo("29")
        assertThat(report["sourceCompatibility"]).isEqualTo("21")
        assertThat(report["targetCompatibility"]).isEqualTo("21")
    }

    @Test
    fun `adds the param-property annotation default target compiler arg`() {
        assertThat(reportOfLibraryFixture()["freeCompilerArgs"])
            .contains("-Xannotation-default-target=param-property")
    }

    /**
     * The AGP 9 constraint, asserted as a negative because it is the one regression a future edit
     * reintroduces silently: AGP 9 compiles Kotlin through built-in support, so applying the
     * standalone `org.jetbrains.kotlin.android` plugin alongside it breaks the build.
     */
    @Test
    fun `does not apply the standalone kotlin-android plugin`() {
        assertThat(reportOfLibraryFixture()["hasKotlinAndroidPlugin"]).isEqualTo("false")
    }

    /**
     * The layering guard has to be *attached*, not merely defined. `check` is what `fastlane
     * android test` and the PR lane run, so a task nobody depends on enforces nothing.
     * [ModuleBoundaryRuleTest] covers the rule the task applies.
     */
    @Test
    fun `wires the module boundary guard into check`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "boundary-wiring"),
            plugins = listOf("nimaz.android.library"),
            buildScript = """
                android { namespace = "com.arshadshah.nimaz.fixture" }
                tasks.register("printConventions") {
                    val checkDeps = tasks.named("check").get().taskDependencies
                        .getDependencies(null).map { it.name }.sorted().joinToString(",")
                    doLast { println("REPORT checkDependsOn=" + checkDeps) }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["checkDependsOn"]).contains("moduleBoundary")
    }

    /**
     * Robolectric-executed code has to reach the coverage report.
     *
     * Robolectric loads the classes under test through its own instrumenting classloader and the
     * classes it hands back carry no source location, so JaCoCo's agent skips them by default.
     * The test still runs and still passes; it simply contributes nothing — and a class with no
     * execution data is indistinguishable from a class no test ever touched.
     *
     * That is not hypothetical. Before `configureRobolectricCoverage` existed,
     * `:core:database`'s exec file held 42 `com/arshadshah/nimaz` class records;
     * `LegacyUserDataImport` — 94 lines, twelve passing Robolectric tests — appeared in it zero
     * times and reported **0%**. With the flag the same run records 181 classes and the class
     * reports **94/94**. Across the repository the correction moved the merged report from
     * 22.8% to 39.3% of lines without a single new test being written.
     *
     * Asserted here rather than trusted because the failure mode is silence in both directions:
     * losing the flag does not break a build, does not fail a test, and shows up only as a
     * coverage number that quietly drops — the same shape as #464.
     */
    @Test
    fun `configures jacoco to record Robolectric-executed classes`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "robolectric-coverage"),
            plugins = listOf("nimaz.android.library", "jacoco"),
            buildScript = """
                android { namespace = "com.arshadshah.nimaz.fixture" }
                tasks.register("printConventions") {
                    val jacoco = tasks.named("testDebugUnitTest").get()
                        .extensions.getByType(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class.java)
                    val includeNoLocation = jacoco.isIncludeNoLocationClasses
                    val excludes = jacoco.excludes?.joinToString(",") ?: ""
                    doLast {
                        println("REPORT includeNoLocationClasses=" + includeNoLocation)
                        println("REPORT jacocoExcludes=" + excludes)
                    }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["includeNoLocationClasses"]).isEqualTo("true")
        // `jdk.internal.*` is the mandatory companion, not an optimisation: those classes also
        // have no location, and instrumenting them fails at class-load time on a sealed JDK
        // module with `IllegalAccessError`.
        assertThat(report["jacocoExcludes"]).contains("jdk.internal.*")
    }

    /**
     * And it must not require the `jacoco` plugin. Only the nineteen production modules apply it;
     * a convention plugin that configured the extension eagerly would fail every module that
     * does not, which is the cheapest way to make a future module opt out of coverage entirely.
     */
    @Test
    fun `does not require the jacoco plugin`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "no-jacoco"),
            plugins = listOf("nimaz.android.library"),
            buildScript = """
                android { namespace = "com.arshadshah.nimaz.fixture" }
                tasks.register("printConventions") {
                    val hasJacoco = project.pluginManager.hasPlugin("jacoco")
                    doLast { println("REPORT hasJacoco=" + hasJacoco) }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["hasJacoco"]).isEqualTo("false")
    }
}
