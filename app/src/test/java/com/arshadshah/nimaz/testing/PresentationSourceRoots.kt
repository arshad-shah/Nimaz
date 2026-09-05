package com.arshadshah.nimaz.testing

import com.google.common.truth.Truth.assertThat
import java.io.File

/**
 * Every directory holding presentation code, across every module — one list, so the scanning
 * tests in `:app` cannot drift apart.
 *
 * Four tests walk presentation sources looking for something: a `NavController` outside a graph
 * (`NavControllerConfinementTest`), a route declared but never registered
 * (`EveryRouteIsRegisteredTest`), an analytics branch nothing dispatches
 * (`AnalyticsReachabilityTest`), a cross-module `hiltViewModel()`
 * (`CrossFeatureViewModelGuardTest`). Each kept its own hand-maintained list of roots, and each
 * PR of #551 that moves code has had to remember all four.
 *
 * It did not go well. PR 14 moved `screens/{about,help,more,onboarding}` into two feature modules
 * and **three of the four tests failed** — every one of them reporting a shrunken scan rather than
 * a real problem. They failed *loudly*, which is the only reason this is a tidy-up and not a
 * silent hole: each carries a floor, and the floors are what turned "found nothing" into a red
 * build. Compare `AnalyticsReachabilityTest` before PR 12, which filtered missing roots away and
 * had been scanning a directory that never existed for as long as it had existed.
 *
 * So: **adding a feature module means adding one line here.** Every scan picks it up, and
 * [assertAllExist] fails the moment a listed root stops being a directory.
 */
object PresentationSourceRoots {

    /**
     * Paths are relative to the `:app` module directory, which is the working directory of its
     * unit tests. `..` reaches the repository root.
     */
    val ALL: List<String> = listOf(
        // `:app` has no presentation package any more. It held `screens/home`, `viewmodel/home`
        // and 21 components until the `:feature:home` extraction; `NavGraph.kt` is under
        // `core/navigation/`, which is scanned below. The directory itself is gone — git does not
        // track an empty one — so listing it here fails `assertAllExist` on a fresh checkout
        // while passing in the working tree that did the move, which is exactly how it was found.
        // The design system, `presentation/model`, `theme/`, and the shared screen helpers.
        "../core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation",
        // The route vocabulary. No screen, but `taggedComposable` and every `Route` live here.
        "../core/navigation/src/main/kotlin/com/arshadshah/nimaz/core/navigation",
        // Feature modules, in extraction order.
        "../feature/widget/src/main/kotlin/com/arshadshah/nimaz/widget",
        "../feature/onboarding/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/about/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/tools/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/calendar/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/search/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/content/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/tracker/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/quran/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/prayer/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/settings/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "../feature/home/src/main/kotlin/com/arshadshah/nimaz/presentation",
    )

    /**
     * The roots that can register a destination — [ALL] minus `:feature:widget`, which draws
     * Glance surfaces rather than Compose destinations and holds no `NavGraphBuilder` extension.
     */
    val NAVIGABLE: List<String> = ALL.filterNot { it.contains("/feature/widget/") }

    /** Kotlin sources under [roots], deduplicated by path. */
    fun sources(roots: List<String> = ALL): List<File> =
        roots.map(::File)
            .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }
            .distinctBy { it.path }

    /**
     * Fails naming every root that is not a directory.
     *
     * An assertion rather than a filter, deliberately: filtering is what let
     * `AnalyticsReachabilityTest` scan a nonexistent `presentation/widget` in silence. Call this
     * before any scan whose result would otherwise look like "nothing to report".
     */
    fun assertAllExist(roots: List<String> = ALL) {
        assertThat(roots.filterNot { File(it).isDirectory }).isEmpty()
    }
}
