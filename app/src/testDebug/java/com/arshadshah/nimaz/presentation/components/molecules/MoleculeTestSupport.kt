package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule

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
