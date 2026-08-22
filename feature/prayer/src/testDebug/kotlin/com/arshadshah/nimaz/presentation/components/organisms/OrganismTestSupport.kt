package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The Compose test harness for this module's `components.organisms` tests — `CompassQiblaViewTest`,
 * whose directory says `qibla` but whose package does not.
 *
 * These ten lines now exist **fourteen** times: five in `:core:ui`, one in `:app`, one in
 * `:feature:calendar`, two in `:feature:tracker`, two in `:feature:quran` and four here —
 * one per test package, because the helpers are `internal` (so no module sees another's) and
 * are imported by package (so one module needs one each).
 *
 * Four in a single module is the point at which this stops being a footnote. **PR 22's
 * `core/ui/src/testFixtures/` item is what collapses all fourteen**, the way `:core:domain`'s
 * fakes already have; it has been deferred since PR 15 because it is a guardrail change rather
 * than a feature move, and the cost of deferring it is now this comment.
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
