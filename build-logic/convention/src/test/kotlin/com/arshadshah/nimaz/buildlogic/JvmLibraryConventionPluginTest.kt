package com.arshadshah.nimaz.buildlogic

import com.arshadshah.nimaz.buildlogic.ConventionFixture.reported
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * `:core:domain` is a pure JVM module: no Android plugin, therefore no Android SDK on the
 * classpath, therefore a stray `import android.*` in the domain layer is a compile error.
 *
 * The compiler covers imports. It does not cover a `dependencies { }` line — a JAR-packaged
 * `androidx` artifact resolves and compiles perfectly well on a JVM classpath — so the plugin
 * also registers `androidFreeClasspath` and wires it into `check`. The last two tests here are
 * that guard's positive and negative cases; without the negative one, a guard that silently
 * matched nothing would look exactly like a guard that passed.
 */
class JvmLibraryConventionPluginTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `is Java 21 and carries no Android plugin`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "jvm"),
            plugins = listOf("nimaz.jvm.library"),
            buildScript = """
                tasks.register("printConventions") {
                    val java = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
                    val kotlin = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
                    val source = java.sourceCompatibility.toString()
                    val target = java.targetCompatibility.toString()
                    val args = kotlin.compilerOptions.freeCompilerArgs.get().joinToString(",")
                    val hasApp = project.pluginManager.hasPlugin("com.android.application")
                    val hasLibrary = project.pluginManager.hasPlugin("com.android.library")
                    doLast {
                        println("REPORT sourceCompatibility=" + source)
                        println("REPORT targetCompatibility=" + target)
                        println("REPORT freeCompilerArgs=" + args)
                        println("REPORT hasAndroidPlugin=" + (hasApp || hasLibrary))
                    }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["sourceCompatibility"]).isEqualTo("21")
        assertThat(report["targetCompatibility"]).isEqualTo("21")
        assertThat(report["freeCompilerArgs"]).contains("-Xannotation-default-target=param-property")
        assertThat(report["hasAndroidPlugin"]).isEqualTo("false")
    }

    @Test
    fun `wires the Android-free classpath guard into check`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "guard-wiring"),
            plugins = listOf("nimaz.jvm.library"),
            buildScript = """
                tasks.register("printConventions") {
                    val checkDeps = tasks.named("check").get().taskDependencies
                        .getDependencies(null).map { it.name }.sorted().joinToString(",")
                    doLast {
                        println("REPORT checkDependsOn=" + checkDeps)
                    }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["checkDependsOn"]).contains("androidFreeClasspath")
    }

    @Test
    fun `the guard fails on a JAR-packaged androidx artifact`() {
        // androidx.annotation, deliberately: an AAR such as androidx.core:core-ktx cannot resolve
        // on a JVM classpath at all, so it would fail *before* the guard ran and prove nothing
        // about the guard. This one resolves cleanly and has to be caught on purpose.
        val result = ConventionFixture.runAndFail(
            dir = File(tmp.root, "guard-trip"),
            plugins = listOf("nimaz.jvm.library"),
            buildScript = """
                dependencies { "implementation"("androidx.annotation:annotation:1.9.1") }
            """.trimIndent(),
            task = "androidFreeClasspath",
        )

        assertThat(result.output).contains("must stay Android-free")
        assertThat(result.output).contains("androidx.annotation")
    }
}
