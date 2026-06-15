package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazIconButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `icon button enums are complete`() {
        assertThat(NimazIconButtonStyle.entries).hasSize(4)
        assertThat(NimazIconButtonSize.entries).hasSize(4)
    }

    private fun assertIconButton(style: NimazIconButtonStyle) {
        var clicked = false
        composeRule.setThemedContent {
            NimazIconButton(
                icon = Icons.Default.Star,
                onClick = { clicked = true },
                contentDescription = "btn",
                style = style,
                size = NimazIconButtonSize.LARGE
            )
        }
        composeRule.onNodeWithContentDescription("btn").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `standard icon button fires click`() = assertIconButton(NimazIconButtonStyle.STANDARD)

    @Test
    fun `filled icon button fires click`() = assertIconButton(NimazIconButtonStyle.FILLED)

    @Test
    fun `filled tonal icon button fires click`() =
        assertIconButton(NimazIconButtonStyle.FILLED_TONAL)

    @Test
    fun `outlined icon button fires click`() = assertIconButton(NimazIconButtonStyle.OUTLINED)

    @Test
    fun `icon button accepts custom colors and disabled state`() {
        composeRule.setThemedContent {
            NimazIconButton(
                icon = Icons.Default.Star,
                onClick = {},
                contentDescription = "custom-btn",
                enabled = false,
                colors = IconButtonDefaults.iconButtonColors()
            )
        }
        composeRule.onNodeWithContentDescription("custom-btn").assertExists()
    }
}
