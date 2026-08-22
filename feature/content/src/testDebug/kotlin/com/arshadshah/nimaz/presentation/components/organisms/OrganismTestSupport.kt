package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The Compose test harness for this module's `components.organisms` tests.
 *
 * Added when PR 19 swept up the tests PR 17 left behind in `app/src/testDebug`: they kept
 * compiling there, because `:app` depends on this module and none of their subjects is
 * `internal` — a test that still compiles is not evidence it is in the right module.
 *
 * Another copy of the same ten lines. **PR 22's `core/ui/src/testFixtures/` item collapses all
 * of them**, the way `:core:domain`'s fakes already have.
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
