package com.arshadshah.nimaz.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The dependency rule `moduleBoundary` enforces, as a table.
 *
 * SPEC §4: *":core:* never depends on :feature:*; no :feature:* depends on another :feature:*;
 * only :app depends on features."* Nothing in the compiler objects to a violation — a sideways
 * `implementation(project(":feature:quran"))` from `:feature:prayer` compiles perfectly well and
 * simply reintroduces the coupling #551 exists to remove.
 *
 * Tested here rather than through TestKit because the risk lives in the rule, not the plumbing:
 * the prefix matching, and the easily-missed `:feature:` → `:feature:` case. Each TestKit case
 * costs a whole Gradle build; each of these costs nothing, so the table can be exhaustive. That
 * the task exists and is wired into `check` is asserted separately in
 * [AndroidLibraryConventionPluginTest].
 */
class ModuleBoundaryRuleTest {

    @Test
    fun `a core module may depend on another core module`() {
        assertThat(isForbiddenModuleDependency(":core:common", ":core:domain")).isFalse()
        assertThat(isForbiddenModuleDependency(":core:ui", ":core:common")).isFalse()
    }

    @Test
    fun `a core module may not reach up to app`() {
        assertThat(isForbiddenModuleDependency(":core:common", ":app")).isTrue()
        assertThat(isForbiddenModuleDependency(":core:domain", ":app")).isTrue()
    }

    @Test
    fun `a core module may not reach up to a feature`() {
        // The concrete case this was written for: `:core:audio` reads
        // `PrayerNotificationScheduler.CHANNEL_ID_ADHAN`, and if that scheduler's Android impl
        // lands in `:feature:prayer` the edge points the wrong way. Recorded on #551.
        assertThat(isForbiddenModuleDependency(":core:audio", ":feature:prayer")).isTrue()
    }

    @Test
    fun `a feature may depend on core but not sideways on another feature`() {
        assertThat(isForbiddenModuleDependency(":feature:quran", ":core:ui")).isFalse()
        assertThat(isForbiddenModuleDependency(":feature:quran", ":core:domain")).isFalse()
        // The mesh the epic exists to avoid. Every one of these that lands makes the graph
        // strictly worse than the monolith it replaced.
        assertThat(isForbiddenModuleDependency(":feature:prayer", ":feature:tracker")).isTrue()
        assertThat(isForbiddenModuleDependency(":feature:tracker", ":feature:prayer")).isTrue()
    }

    @Test
    fun `a feature may not reach up to app either`() {
        assertThat(isForbiddenModuleDependency(":feature:settings", ":app")).isTrue()
    }

    @Test
    fun `app may depend on anything`() {
        // :app is the shell. It is the one module allowed to know about every feature, which is
        // why `screens/adaptive` stays there rather than becoming a module of its own.
        assertThat(isForbiddenModuleDependency(":app", ":feature:quran")).isFalse()
        assertThat(isForbiddenModuleDependency(":app", ":core:domain")).isFalse()
    }

    @Test
    fun `a module does not depend on itself`() {
        // Every Android module's own test source sets appear on its compile and runtime
        // classpaths as a ProjectDependency on itself: `debugUnitTestCompileClasspath ->
        // :feature:widget`. `:feature:widget` starts with `:feature:`, so a naive
        // feature-to-feature rule calls that forbidden and the module cannot build at all.
        //
        // This lay dormant from PR 1 of #551 until PR 13, because until the first `:feature:*`
        // module existed there was nothing for the rule to misclassify — the `:core:*` branch
        // forbids `:app` and `:feature:*`, neither of which a `:core:` path matches. It failed on
        // `:feature:widget`'s very first `check`.
        assertThat(isForbiddenModuleDependency(":feature:widget", ":feature:widget")).isFalse()
        assertThat(isForbiddenModuleDependency(":core:ui", ":core:ui")).isFalse()
        assertThat(isForbiddenModuleDependency(":app", ":app")).isFalse()

        // …and the sideways case it exists for is still caught.
        assertThat(isForbiddenModuleDependency(":feature:widget", ":feature:quran")).isTrue()
    }

    @Test
    fun `the match is on a path segment, not a bare prefix`() {
        // `:coreutils` is not a `:core:` module and `:features` is not a `:feature:` module.
        // A `startsWith(":core")` — one character shorter — would classify both wrongly, and
        // the mistake would only surface the day someone adds such a module.
        assertThat(isForbiddenModuleDependency(":coreutils", ":app")).isFalse()
        assertThat(isForbiddenModuleDependency(":core:common", ":features")).isFalse()
        assertThat(isForbiddenModuleDependency(":core:common", ":appkit")).isFalse()
    }
}
