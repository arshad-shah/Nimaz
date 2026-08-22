package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLetterFormsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun connectingLetter() = QaidaLetter(
        id = 2,
        letterArabic = "ب",
        nameArabic = "باء",
        nameTransliteration = "Ba",
        isolatedForm = "ب",
        initialForm = "بـ",
        medialForm = "ـبـ",
        finalForm = "ـب",
        isConnecting = true,
        makhrajArea = MakhrajArea.SHAFATAIN,
        makhrajDetail = "From the lips",
        phoneticHint = "like 'b' in 'book'",
        audioKey = "ba",
        audioPath = "",
        displayOrder = 2,
    )

    private fun nonConnectingLetter() = QaidaLetter(
        id = 1,
        letterArabic = "ا",
        nameArabic = "ألف",
        nameTransliteration = "Alif",
        isolatedForm = "ا",
        initialForm = null,
        medialForm = null,
        finalForm = null,
        isConnecting = false,
        makhrajArea = MakhrajArea.JAWF,
        makhrajDetail = "From the empty space",
        phoneticHint = null,
        audioKey = "alif",
        audioPath = "",
        displayOrder = 1,
    )

    @Test
    fun `connecting letter shows all four form labels`() {
        composeRule.setThemedContent { QaidaLetterForms(letter = connectingLetter()) }
        composeRule.onNodeWithText("start").assertExists()
        composeRule.onNodeWithText("middle").assertExists()
        composeRule.onNodeWithText("end").assertExists()
        composeRule.onNodeWithText("alone").assertExists()
    }

    @Test
    fun `non-connecting letter shows only the alone form`() {
        composeRule.setThemedContent { QaidaLetterForms(letter = nonConnectingLetter()) }
        composeRule.onNodeWithText("alone").assertExists()
        composeRule.onNodeWithText("start").assertDoesNotExist()
    }
}
