package com.arshadshah.nimaz.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The Compose test harness for screen-level tests that stay in `:app`.
 *
 * A near-copy of the `*TestSupport.kt` files that sit one per test package — the pattern this
 * codebase already follows, because the helpers are `internal` and each package imports its own.
 * It exists as a *separate* copy because the design system's copies left for `:core:ui` in PR 10
 * of #551, and `internal` is scoped to a module: the one screen test that had been importing
 * `components.atoms.createComponentComposeRule` could no longer see it. Borrowing the atoms'
 * harness was always a little odd; a screen test is not an atom test.
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
