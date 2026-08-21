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
}
