package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaCelebrationBurstTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders a still frame without crashing`() {
        composeRule.setThemedContent {
            QaidaCelebrationBurst(
                modifier = Modifier.size(200.dp),
                play = false,
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
