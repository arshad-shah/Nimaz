package com.arshadshah.nimaz.buildlogic

import java.io.File
import java.util.Properties
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * A throwaway Gradle project that applies one convention plugin and prints the *effects* back.
 *
 * TestKit spawns a real Gradle build per fixture, which is slow, so there is exactly one fixture
 * per plugin. Asserting on printed effects (`compileSdk=37`) rather than on the plugin's source
 * is deliberate: it is the effect that regresses, and a refactor of how the plugin sets the value
 * should not have to touch the test.
 */
object ConventionFixture {

    private val APPLY_FALSE_PREAMBLE = listOf(
        "libs.plugins.android.application",
        "libs.plugins.android.library",
        "libs.plugins.kotlin.jvm",
        "libs.plugins.kotlin.compose",
        "libs.plugins.ksp",
        "libs.plugins.hilt",
    )

    /** `build-logic` itself, injected by the `test` task — see build-logic/convention/build.gradle.kts. */
    private val buildLogicRoot: File = File(System.getProperty("nimaz.buildLogic.root"))

    /** The repository root, which owns `gradle/libs.versions.toml` and `local.properties`. */
    private val repoRoot: File = File(System.getProperty("nimaz.repo.root"))

    private val androidSdkDir: String? by lazy {
        val fromProperties = File(repoRoot, "local.properties")
            .takeIf { it.isFile }
            ?.let { file -> Properties().apply { file.inputStream().use(::load) }.getProperty("sdk.dir") }
        fromProperties ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    }

    /**
     * Writes a single-project fixture into [dir] and runs [task] on it.
     *
     * @param buildScript the body of the fixture's `build.gradle.kts`, minus the plugins block.
     */
    fun run(
        dir: File,
        plugins: List<String>,
        buildScript: String,
        imports: List<String> = emptyList(),
        task: String = "printConventions",
    ): BuildResult {
        dir.mkdirs()
        File(dir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
                includeBuild("${buildLogicRoot.invariantSeparatorsPath}")
            }
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
                versionCatalogs {
                    create("libs") {
                        from(files("${File(repoRoot, "gradle/libs.versions.toml").invariantSeparatorsPath}"))
                    }
                }
            }
            rootProject.name = "fixture"
            """.trimIndent()
        )
        androidSdkDir?.let {
            File(dir, "local.properties").writeText("sdk.dir=$it\n")
        }
        File(dir, "gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
            android.useAndroidX=true
            android.nonTransitiveRClass=true
            """.trimIndent()
        )
        File(dir, "build.gradle.kts").writeText(
            buildString {
                imports.forEach { appendLine("import $it") }
                appendLine("plugins {")
                // Mirrors the real root build.gradle.kts: AGP and KGP have to be on the script
                // classpath before a convention plugin class that *references* their extension
                // types can even be loaded. Without these the fixture fails with
                // ClassNotFoundException at plugin-apply time rather than at assertion time.
                APPLY_FALSE_PREAMBLE.forEach { appendLine("    alias($it) apply false") }
                plugins.forEach { appendLine("    id(\"$it\")") }
                appendLine("}")
                appendLine(buildScript)
            }
        )
        // A source root has to exist or AGP's manifest/namespace wiring complains before the
        // fixture's own task ever runs.
        File(dir, "src/main/kotlin").mkdirs()

        return GradleRunner.create()
            .withProjectDir(dir)
            .withArguments(task, "--stacktrace")
            .forwardOutput()
            .build()
    }

    /** Reads the `REPORT key=value` lines a fixture printed. */
    fun BuildResult.reported(): Map<String, String> =
        output.lineSequence()
            .filter { it.startsWith("REPORT ") }
            .map { it.removePrefix("REPORT ").substringBefore("=") to it.substringAfter("=") }
            .toMap()
}
