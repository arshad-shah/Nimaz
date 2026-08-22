package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaMakhrajHelperTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders the detail text`() {
        composeRule.setThemedContent {
            QaidaMakhrajHelper(
                area = MakhrajArea.SHAFATAIN,
                detail = "Pressing the lips together gently.",
            )
        }
        composeRule.onNodeWithText("Pressing the lips together gently.").assertExists()
    }

    @Test
    fun `MakhrajArea has five entries`() {
        assertThat(MakhrajArea.entries).hasSize(5)
    }
}
