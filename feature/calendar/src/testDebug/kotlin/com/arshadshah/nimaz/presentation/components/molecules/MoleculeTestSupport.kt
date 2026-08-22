package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

// The sixth copy of this 40-line harness, and the reason is always the same: the helpers are
// `internal`, and `internal` is scoped to a module. Before #551 the codebase already carried five
// — one per test package — and each module that takes a component test needs its own.
//
// `IslamicEventCard` is calendar-only (nothing else in the repo names it), so it moves into this
// module rather than to `:core:ui`, and its test comes with it.
//
// **Candidate for PR 22.** Publishing this from `core/ui/src/testFixtures/` would collapse all six
// into one, the way `:core:domain`'s fakes already are — see `docs/ARCHITECTURE.md` on
// `testFixtures`. Not done here because it is a guardrail change, not a feature move, and every
// further feature module with a component test adds a copy until it is.

/** Wraps content in a bare [MaterialTheme] so components resolve default colours and typography. */
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
