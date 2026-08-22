package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The **eighth** copy of these ten lines, and the second in this module alone — one for the
 * `screens` package, one for `components.organisms`, because the helpers are `internal` and
 * `internal` is scoped to a module *and* imported per package.
 *
 * The count so far: five in `:core:ui`, one in `:app`, one in `:feature:calendar`, two here.
 * `:feature:quran` and `:feature:prayer` will each add at least one more.
 *
 * **This is the case for the PR 22 item**, first flagged in PR 15: publish one from
 * `core/ui/src/testFixtures/` and all of them collapse, the way `:core:domain`'s fakes already
 * have. It is a guardrail change rather than a feature move, which is why it keeps being deferred
 * — but the cost of deferring it is now visible and growing.
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
