package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaAccuracyPill
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QiblaAccuracyPillTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `pill renders accuracy label`() {
        composeRule.setThemedContent {
            QiblaAccuracyPill(accuracy = CompassAccuracy.HIGH)
        }
        composeRule.onNodeWithText("High").assertExists()
    }
}
