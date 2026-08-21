package com.arshadshah.nimaz.buildlogic

import com.arshadshah.nimaz.buildlogic.ConventionFixture.reported
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pins the exact regression that once broke the release lane while both PR lanes stayed green:
 * lint reads the generated assets directory, and only lint-vital (release-only) hit the missing
 * dependency.
 */
class GeneratedAssetOrderingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `matches asset merges and lint tasks in both of AGP's naming styles`() {
        assertThat(GeneratedAssetOrdering.consumesAssets("mergeDebugAssets")).isTrue()
        assertThat(GeneratedAssetOrdering.consumesAssets("mergeReleaseAssets")).isTrue()
        assertThat(GeneratedAssetOrdering.consumesAssets("lintAnalyzeDebug")).isTrue()
        // The capitalised form. Matching only the lowercase one fixed the release lane and left
        // the debug one to fail on the next PR, which is exactly what happened.
        assertThat(GeneratedAssetOrdering.consumesAssets("generateReleaseLintVitalReportModel")).isTrue()
        assertThat(GeneratedAssetOrdering.consumesAssets("compileDebugKotlin")).isFalse()
        assertThat(GeneratedAssetOrdering.consumesAssets("mergeDebugResources")).isFalse()
    }

    @Test
    fun `makes the real merge-assets and lint task graph depend on the producing task`() {
        val report = ConventionFixture.run(
            dir = File(tmp.root, "assets"),
            plugins = listOf("nimaz.android.library"),
            imports = listOf("com.arshadshah.nimaz.buildlogic.orderAssetConsumersAfter"),
            buildScript = """
                extensions.getByType(com.android.build.api.dsl.LibraryExtension::class.java)
                    .namespace = "com.arshadshah.nimaz.fixture"

                // Stands in for `:app:fetchNimazData`, which stays registered in the consuming
                // project on purpose — a library plugin must never reach for `:app:...`.
                tasks.register("fetchNimazData") { outputs.dir(layout.buildDirectory.dir("generated/assets")) }

                orderAssetConsumersAfter("fetchNimazData")

                tasks.register("printConventions") {
                    val merge = project.tasks.getByName("mergeDebugAssets")
                        .taskDependencies.getDependencies(null).map { it.name }
                    val lint = project.tasks.getByName("lintAnalyzeDebug")
                        .taskDependencies.getDependencies(null).map { it.name }
                    doLast {
                        println("REPORT mergeAssetsDependsOnFetch=" + merge.contains("fetchNimazData"))
                        println("REPORT lintDependsOnFetch=" + lint.contains("fetchNimazData"))
                    }
                }
            """.trimIndent()
        ).reported()

        assertThat(report["mergeAssetsDependsOnFetch"]).isEqualTo("true")
        assertThat(report["lintDependsOnFetch"]).isEqualTo("true")
    }
}
