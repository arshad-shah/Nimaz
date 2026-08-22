package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * The **tenth** copy of these ten lines, and the second in this module — one for
 * `components.molecules`, one for `components.organisms`, because the helpers are `internal`,
 * and `internal` is scoped to a module *and* imported per package.
 *
 * The count: five in `:core:ui`, one in `:app`, one in `:feature:calendar`, two in
 * `:feature:tracker`, and this one. `:feature:prayer` and `:feature:content` will each add more.
 *
 * This copy exists because PR 19 left eleven Quran component tests behind in `app/src/testDebug`
 * and CI caught it: two of them read members that became `internal` when their subjects moved
 * here, and `internal` does not cross a module. The tests compiled in `:app` right up to the
 * moment the visibility narrowed — a module's components being tested from another module is
 * invisible until exactly that happens.
 *
 * **This is the case for the PR 22 item**, first flagged in PR 15: publish one from
 * `core/ui/src/testFixtures/` and all of them collapse, the way `:core:domain`'s fakes already
 * have.
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
