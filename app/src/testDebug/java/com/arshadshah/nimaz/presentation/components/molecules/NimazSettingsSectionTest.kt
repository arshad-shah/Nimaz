package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSettingsSectionTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders title and content with card`() {
        composeRule.setThemedContent {
            NimazSettingsSection(title = "PRAYER SETTINGS") {
                Text("Calculation Method")
            }
        }
        composeRule.onNodeWithText("PRAYER SETTINGS").assertExists()
        composeRule.onNodeWithText("Calculation Method").assertExists()
    }

    @Test
    fun `renders title and content without card`() {
        composeRule.setThemedContent {
            NimazSettingsSection(title = "GENERAL", showCard = false) {
                Text("Theme")
            }
        }
        composeRule.onNodeWithText("GENERAL").assertExists()
        composeRule.onNodeWithText("Theme").assertExists()
    }
}
