package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * Wraps organism content in a bare [MaterialTheme] so the components resolve
 * their default colour-scheme / typography values without pulling in the app's
 * NimazTheme (which performs Activity/window side effects irrelevant to
 * exercising the organisms under Robolectric).
 *
 * Mirrors the helpers used by the atom and molecule test suites so all three
 * layers share one convention.
 */
internal fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}
