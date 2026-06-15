package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazIconTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `icon enums are complete`() {
        assertThat(NimazIconSize.entries).hasSize(5)
        assertThat(NimazIconContainerShape.entries).hasSize(3)
    }

    private fun assertContainedIcon(shape: NimazIconContainerShape) {
        composeRule.setThemedContent {
            ContainedIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "icon",
                containerShape = shape,
                size = NimazIconSize.LARGE
            )
        }
        composeRule.onNodeWithContentDescription("icon").assertExists()
    }

    @Test
    fun `contained icon renders circle`() = assertContainedIcon(NimazIconContainerShape.CIRCLE)

    @Test
    fun `contained icon renders rounded square`() =
        assertContainedIcon(NimazIconContainerShape.ROUNDED_SQUARE)

    @Test
    fun `contained icon renders square`() = assertContainedIcon(NimazIconContainerShape.SQUARE)

    @Test
    fun `prayer icon renders`() {
        composeRule.setThemedContent {
            PrayerIcon(
                imageVector = Icons.Default.Star,
                prayerColor = Color(0xFF5C6BC0),
                contentDescription = "prayer-icon"
            )
        }
        composeRule.onNodeWithContentDescription("prayer-icon").assertExists()
    }
}
