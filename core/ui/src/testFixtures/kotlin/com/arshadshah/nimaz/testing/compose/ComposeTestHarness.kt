package com.arshadshah.nimaz.testing.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The one Compose test harness, published from `:core:ui`'s test fixtures.
 *
 * **It replaces twenty copies of these ten lines.** They multiplied for a reason that is worth
 * stating, because it is not carelessness: the helpers were `internal`, so no module could see
 * another's, *and* they were imported by package, so a module needed one per test package.
 * `:feature:prayer` alone carried four. Every feature module extracted during #551 added at least
 * one, and the count grew with each PR — five in `:core:ui`, three in `:app`, and the rest spread
 * across seven feature modules.
 *
 * Publishing them from `testFixtures` collapses all of it, the way `:core:domain`'s fakes already
 * had. Consumers add `testImplementation(testFixtures(project(":core:ui")))` and one import; the
 * helpers are public here rather than `internal`, which is the whole point — `internal` is scoped
 * to a module, and that scoping is what made twenty copies necessary.
 *
 * This was deferred from PR 15 to PR 22 of #551 as a guardrail change rather than a feature move.
 * The cost of deferring it was fifteen further copies.
 */
fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}

/**
 * Shared factory for the JUnit4 Compose rule.
 *
 * Centralises the (deprecated) [createComposeRule] call so the suppression lives in one place —
 * one place, now, rather than twenty. Migrating to the v2 rule switches the test dispatcher and is
 * a separate change.
 */
@Suppress("DEPRECATION")
fun createComponentComposeRule(): ComposeContentTestRule = createComposeRule()
