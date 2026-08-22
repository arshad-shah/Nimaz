package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The **ninth** copy of these ten lines. Five in `:core:ui`, one in `:app`, one in
 * `:feature:calendar`, two in `:feature:tracker`, and now this. `:feature:prayer` will add more.
 *
 * The cause has not changed since PR 15 flagged it: the helpers are `internal`, so a module cannot
 * see another module's, and they are imported by package, so one module needs one per test
 * package. Publishing a single copy from `core/ui/src/testFixtures/` collapses all nine — a
 * **PR 22** item, deferred because it is a guardrail change rather than a feature move.
 */
internal fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}

/** Centralises the (deprecated) [createComposeRule] call so the suppression lives in one place. */
@Suppress("DEPRECATION")
internal fun createComponentComposeRule(): ComposeContentTestRule = createComposeRule()
