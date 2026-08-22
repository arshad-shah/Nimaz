package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazStatusDotTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `styles and sizes are complete`() {
        assertThat(NimazStatusDotStyle.entries).hasSize(2)
        assertThat(NimazStatusDotSize.entries).hasSize(3)
    }

    @Test
    fun `spec defaults to the filled style`() {
        assertThat(NimazStatusDotSpec(NimazTone.SUCCESS).style)
            .isEqualTo(NimazStatusDotStyle.FILLED)
    }

    @Test
    fun `a described dot is exposed to accessibility`() {
        composeRule.setThemedContent {
            NimazStatusDot(
                spec = NimazStatusDotSpec(NimazTone.SUCCESS),
                contentDescription = "fasted"
            )
        }
        composeRule.onNodeWithContentDescription("fasted").assertIsDisplayed()
    }

    @Test
    fun `an outlined dot renders without a description`() {
        composeRule.setThemedContent {
            NimazStatusDot(
                spec = NimazStatusDotSpec(NimazTone.NEUTRAL, NimazStatusDotStyle.OUTLINED)
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the colour-driven overload renders and is describable`() {
        composeRule.setThemedContent {
            NimazStatusDot(
                color = MaterialTheme.colorScheme.primary,
                style = NimazStatusDotStyle.OUTLINED,
                contentDescription = "ring"
            )
        }
        composeRule.onNodeWithContentDescription("ring").assertIsDisplayed()
    }

    @Test
    fun `sizes are ordered smallest to largest`() {
        assertThat(NimazStatusDotSize.SMALL.diameter.value)
            .isLessThan(NimazStatusDotSize.MEDIUM.diameter.value)
        assertThat(NimazStatusDotSize.MEDIUM.diameter.value)
            .isLessThan(NimazStatusDotSize.LARGE.diameter.value)
    }
}
