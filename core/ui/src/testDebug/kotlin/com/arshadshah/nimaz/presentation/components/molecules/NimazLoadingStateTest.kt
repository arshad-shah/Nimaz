package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazLoadingStateTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders spinner`() {
        composeRule.setThemedContent { NimazLoadingState() }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
