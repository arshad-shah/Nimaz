package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `card styles enum is complete`() {
        assertThat(NimazCardStyle.entries).hasSize(4)
    }

    private fun assertClickableCard(style: NimazCardStyle) {
        var clicked = false
        composeRule.setThemedContent {
            NimazCard(style = style, onClick = { clicked = true }) {
                Text("Clickable")
            }
        }
        composeRule.onNodeWithText("Clickable").performClick()
        assertThat(clicked).isTrue()
    }

    private fun assertStaticCard(style: NimazCardStyle) {
        composeRule.setThemedContent {
            NimazCard(style = style) {
                Text("Static")
            }
        }
        composeRule.onNodeWithText("Static").assertExists()
    }

    @Test
    fun `filled card clickable`() = assertClickableCard(NimazCardStyle.FILLED)

    @Test
    fun `elevated card clickable`() = assertClickableCard(NimazCardStyle.ELEVATED)

    @Test
    fun `outlined card clickable`() = assertClickableCard(NimazCardStyle.OUTLINED)

    @Test
    fun `gradient style card clickable`() = assertClickableCard(NimazCardStyle.GRADIENT)

    @Test
    fun `filled card static`() = assertStaticCard(NimazCardStyle.FILLED)

    @Test
    fun `elevated card static`() = assertStaticCard(NimazCardStyle.ELEVATED)

    @Test
    fun `outlined card static`() = assertStaticCard(NimazCardStyle.OUTLINED)

    @Test
    fun `gradient style card static`() = assertStaticCard(NimazCardStyle.GRADIENT)

    @Test
    fun `gradient card renders and fires onClick`() {
        var clicked = false
        composeRule.setThemedContent {
            GradientCard(
                gradientColors = listOf(Color(0xFF5C6BC0), Color(0xFF9FA8DA)),
                onClick = { clicked = true }
            ) {
                Text("Gradient clickable")
            }
        }
        composeRule.onNodeWithText("Gradient clickable").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `gradient card renders without onClick`() {
        composeRule.setThemedContent {
            GradientCard(gradientColors = listOf(Color.Red, Color.Blue)) {
                Text("Gradient static")
            }
        }
        composeRule.onNodeWithText("Gradient static").assertExists()
    }

    @Test
    fun `prayer card renders content and fires onClick`() {
        var clicked = false
        composeRule.setThemedContent {
            PrayerCard(
                primaryColor = Color(0xFFFFB74D),
                secondaryColor = Color(0xFFFFE0B2),
                onClick = { clicked = true }
            ) {
                Text("Fajr")
            }
        }
        composeRule.onNodeWithText("Fajr").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `prayer card renders without onClick`() {
        composeRule.setThemedContent {
            PrayerCard(
                primaryColor = Color.Red,
                secondaryColor = Color.Blue
            ) {
                Text("Dhuhr")
            }
        }
        composeRule.onNodeWithText("Dhuhr").assertExists()
    }
}
