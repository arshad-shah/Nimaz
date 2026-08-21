package com.arshadshah.nimaz.buildlogic

import com.arshadshah.nimaz.buildlogic.ConventionFixture.reported
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * `:app` is `com.android.application`, so the shared config has to exist on the application side
 * too — the five plugins the issue named could not have been applied to `:app` at all.
 */
class AndroidApplicationConventionPluginTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `carries the same SDK, Java and compiler-arg config as the library plugin`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "app"),
            plugins = listOf("nimaz.android.application"),
            buildScript = """
                extensions.getByType(com.android.build.api.dsl.ApplicationExtension::class.java).apply {
                    namespace = "com.arshadshah.nimaz.fixture"
                    defaultConfig.applicationId = "com.arshadshah.nimaz.fixture"
                }

                tasks.register("printConventions") {
                    val android = project.extensions.getByType(com.android.build.api.dsl.ApplicationExtension::class.java)
                    val kotlin = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension::class.java)
                    val compileSdk = android.compileSdk
                    val minSdk = android.defaultConfig.minSdk
                    val source = android.compileOptions.sourceCompatibility.toString()
                    val args = kotlin.compilerOptions.freeCompilerArgs.get().joinToString(",")
                    val hasKotlinAndroid = project.pluginManager.hasPlugin("org.jetbrains.kotlin.android")
                    doLast {
                        println("REPORT compileSdk=" + compileSdk)
                        println("REPORT minSdk=" + minSdk)
                        println("REPORT sourceCompatibility=" + source)
                        println("REPORT freeCompilerArgs=" + args)
                        println("REPORT hasKotlinAndroidPlugin=" + hasKotlinAndroid)
                    }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["compileSdk"]).isEqualTo("37")
        assertThat(report["minSdk"]).isEqualTo("29")
        assertThat(report["sourceCompatibility"]).isEqualTo("21")
        assertThat(report["freeCompilerArgs"]).contains("-Xannotation-default-target=param-property")
        assertThat(report["hasKotlinAndroidPlugin"]).isEqualTo("false")
    }
}
