package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithTag
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
class QaidaCourseHeaderTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders english title`() {
        composeRule.setThemedContent {
            QaidaCourseHeader(
                titleArabic = "القاعدة النورانية",
                titleEnglish = "Noorani Qaida",
                lessonIndex = 4,
                totalLessons = 17,
                totalStars = 9,
                overallFraction = 0.35f,
                continueLabel = "Lesson 4",
                onContinue = {},
            )
        }
        composeRule.onNodeWithText("Noorani Qaida").assertExists()
    }

    @Test
    fun `continue button fires callback`() {
        var fired = false
        composeRule.setThemedContent {
            QaidaCourseHeader(
                titleArabic = "القاعدة النورانية",
                titleEnglish = "Noorani Qaida",
                lessonIndex = 4,
                totalLessons = 17,
                totalStars = 9,
                overallFraction = 0.35f,
                continueLabel = "Lesson 4",
                onContinue = { fired = true },
            )
        }
        composeRule.onNodeWithTag("qaida_continue").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `hides continue button when label is null`() {
        composeRule.setThemedContent {
            QaidaCourseHeader(
                titleArabic = "القاعدة النورانية",
                titleEnglish = "Noorani Qaida",
                lessonIndex = 1,
                totalLessons = 17,
                totalStars = 0,
                overallFraction = 0f,
                continueLabel = null,
                onContinue = {},
            )
        }
        composeRule.onNodeWithTag("qaida_continue").assertDoesNotExist()
    }
}
