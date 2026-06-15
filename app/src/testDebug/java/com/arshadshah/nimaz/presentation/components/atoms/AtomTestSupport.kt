package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * Wraps atom content in a [MaterialTheme] so the components resolve their
 * default colour-scheme / typography values. Using the bare MaterialTheme
 * (instead of the app's NimazTheme) keeps the Robolectric harness free of the
 * Activity/window side effects that NimazTheme performs, which are irrelevant
 * to exercising the atoms.
 */
internal fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}
