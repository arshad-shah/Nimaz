package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

// The `:core:ui` copy of this package's Compose test harness.
//
// A copy, not a move, because `presentation.components.molecules` is split across two modules by PR 10
// of #551: the generic `Nimaz*` members live here, the feature-specific ones stay in `:app` until
// they leave for their own feature modules in Milestone 5. The helpers are `internal`, and
// `internal` is scoped to a module, so each side needs its own — the same reason this codebase
// already carried five near-identical `*TestSupport.kt` files, one per test package, before any of
// this. The duplication ends when the staying half moves on.

/**
 * Wraps molecule content in a bare [MaterialTheme] so the components resolve
 * their default colour-scheme / typography values without pulling in the app's
 * NimazTheme (which performs Activity/window side effects irrelevant to
 * exercising the molecules under Robolectric).
 *
 * Mirrors the helper used by the atom test suite so both layers share one
 * convention.
 */
internal fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}

/**
 * Shared factory for the JUnit4 Compose test rule. Centralises the (currently
 * deprecated) [createComposeRule] call so the suppression lives in one place;
 * migrating to the v2 rule would switch the test dispatcher and is out of scope.
 */
@Suppress("DEPRECATION")
internal fun createComponentComposeRule(): ComposeContentTestRule = createComposeRule()
