package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazContainersTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `bottom sheet handle renders`() {
        composeRule.setThemedContent {
            BottomSheetHandle(modifier = Modifier.testTag("handle"))
        }
        composeRule.onNodeWithTag("handle").assertExists()
    }
}
