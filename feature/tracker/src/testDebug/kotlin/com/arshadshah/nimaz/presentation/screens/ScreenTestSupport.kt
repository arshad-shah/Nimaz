package com.arshadshah.nimaz.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The Compose test harness for this module's screen tests.
 *
 * **The seventh copy of these ten lines**, and the reason has not changed: the helpers are
 * `internal`, and `internal` is scoped to a module. `:app` keeps its own for the screen tests that
 * have not moved yet; `:core:ui` has five, one per component test package; this is `:feature:tracker`'s.
 *
 * **Candidate for PR 22**, flagged since PR 15. Publishing one from `core/ui/src/testFixtures/`
 * would collapse all seven, the way `:core:domain`'s fakes already are. Every further feature
 * module with a Compose test adds a copy until that happens — `:feature:quran` and
 * `:feature:prayer` both will.
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
