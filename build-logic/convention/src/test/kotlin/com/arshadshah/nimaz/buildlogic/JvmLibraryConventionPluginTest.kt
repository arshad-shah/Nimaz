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
}
