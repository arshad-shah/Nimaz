package com.arshadshah.nimaz.buildlogic

import org.gradle.api.Project

/**
 * Ordering for tasks that *read* a generated assets directory.
 *
 * `:app:fetchNimazData` writes into a source-set assets directory that is registered statically
 * (AGP 9 refuses a Provider there). Everything that reads that directory has to be ordered after
 * the task that fills it — not just the asset merge. Lint builds a model of every source set,
 * assets included, so `generateReleaseLintVitalReportModel` consumes the directory too and Gradle
 * fails the build outright rather than racing:
 *
 *     Task ':app:generateReleaseLintVitalReportModel' uses this output of task
 *     ':app:fetchNimazData' without declaring an explicit or implicit dependency.
 *
 * Only release ever hit it, because lint-vital runs for release and not for debug — so every debug
 * build and both PR check lanes were green while the deploy lane could not build at all. That is
 * the regression this helper exists to make un-repeatable.
 *
 * The task *registration* deliberately stays in the consuming project — today only `:app`, in
 * `app/build.gradle.kts`; the task type itself is [FetchNimazDataTask], here. A convention plugin
 * that reached for `:app:fetchNimazData` would point a library at the app, which is exactly the
 * inversion the multi-module epic exists to remove.
 */
object GeneratedAssetOrdering {

    /**
     * True for a task name that reads a source set's assets directory.
     *
     * `lint` is matched case-insensitively on purpose: AGP names these tasks both ways —
     * `generateReleaseLintVitalReportModel` and `lintAnalyzeDebug` — and matching only the
     * capitalised form once fixed the release lane while leaving the debug one to fail on the
     * next PR, which is exactly what happened.
     */
    fun consumesAssets(taskName: String): Boolean =
        (taskName.startsWith("merge") && taskName.endsWith("Assets")) ||
            taskName.contains("lint", ignoreCase = true)
}

/**
 * Makes every asset-consuming task in this project depend on [producerTaskName].
 *
 * @param producerTaskName a task registered in *this* project — never a cross-project path.
 */
fun Project.orderAssetConsumersAfter(producerTaskName: String) {
    tasks.matching { GeneratedAssetOrdering.consumesAssets(it.name) }
        .configureEach { dependsOn(producerTaskName) }
}
