package com.arshadshah.nimaz.presentation.foundation.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

internal fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent {
        MaterialTheme {
            content()
        }
    }
}

@Suppress("DEPRECATION")
internal fun createComponentComposeRule(): ComposeContentTestRule = createComposeRule()
