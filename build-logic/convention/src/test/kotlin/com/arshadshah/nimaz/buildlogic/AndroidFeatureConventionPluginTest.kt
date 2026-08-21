package com.arshadshah.nimaz.buildlogic

import com.arshadshah.nimaz.buildlogic.ConventionFixture.reported
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * One fixture covers `nimaz.android.feature` and, through it, `nimaz.android.compose` and
 * `nimaz.android.hilt` — TestKit spawns a real Gradle build per fixture, so the transitive test
 * doubles as the compose and hilt test rather than adding two more builds.
 */
class AndroidFeatureConventionPluginTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `applies library, compose and hilt, and wires the hilt compiler onto ksp`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "feature"),
            plugins = listOf("nimaz.android.feature"),
            buildScript = """
                extensions.getByType(com.android.build.api.dsl.LibraryExtension::class.java)
                    .namespace = "com.arshadshah.nimaz.fixture"

                tasks.register("printConventions") {
                    val android = project.extensions.getByType(com.android.build.api.dsl.LibraryExtension::class.java)
                    val compose = android.buildFeatures.compose
                    val hasLibrary = project.pluginManager.hasPlugin("com.android.library")
                    val hasComposeCompiler = project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose")
                    val hasKsp = project.pluginManager.hasPlugin("com.google.devtools.ksp")
                    val hasHilt = project.pluginManager.hasPlugin("com.google.dagger.hilt.android")
                    val hasKotlinAndroid = project.pluginManager.hasPlugin("org.jetbrains.kotlin.android")
                    val kspDeps = project.configurations.getByName("ksp").dependencies
                        .joinToString(",") { it.group + ":" + it.name }
                    val implementationDeps = project.configurations.getByName("implementation").dependencies
                        .joinToString(",") { it.group + ":" + it.name }
                    val hasKspTask = project.tasks.findByName("kspDebugKotlin") != null
                    doLast {
                        println("REPORT compose=" + compose)
                        println("REPORT hasLibrary=" + hasLibrary)
                        println("REPORT hasComposeCompiler=" + hasComposeCompiler)
                        println("REPORT hasKsp=" + hasKsp)
                        println("REPORT hasHilt=" + hasHilt)
                        println("REPORT hasKotlinAndroidPlugin=" + hasKotlinAndroid)
                        println("REPORT kspDependencies=" + kspDeps)
                        println("REPORT implementationDependencies=" + implementationDeps)
                        println("REPORT hasKspDebugKotlinTask=" + hasKspTask)
                    }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["hasLibrary"]).isEqualTo("true")
        assertThat(report["hasComposeCompiler"]).isEqualTo("true")
        assertThat(report["hasKsp"]).isEqualTo("true")
        assertThat(report["hasHilt"]).isEqualTo("true")
        assertThat(report["compose"]).isEqualTo("true")
        assertThat(report["hasKotlinAndroidPlugin"]).isEqualTo("false")
        assertThat(report["hasKspDebugKotlinTask"]).isEqualTo("true")
        assertThat(report["kspDependencies"]).contains("com.google.dagger:hilt-compiler")
        assertThat(report["implementationDependencies"]).contains("com.google.dagger:hilt-android")
    }
}
