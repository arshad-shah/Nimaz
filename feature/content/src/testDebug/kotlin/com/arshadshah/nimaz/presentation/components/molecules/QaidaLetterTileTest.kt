package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLetterTileTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun letter(
        id: Int = 2,
        letterArabic: String = "ب",
        name: String = "Ba",
    ) = QaidaLetter(
        id = id,
        letterArabic = letterArabic,
        nameArabic = "باء",
        nameTransliteration = name,
        isolatedForm = letterArabic,
        initialForm = null,
        medialForm = null,
        finalForm = null,
        isConnecting = true,
        makhrajArea = MakhrajArea.SHAFATAIN,
        makhrajDetail = "From the lips",
        phoneticHint = null,
        audioKey = name.lowercase(),
        audioPath = "",
        displayOrder = id,
    )

    @Test
    fun `renders content description with letter name`() {
        composeRule.setThemedContent {
            QaidaLetterTile(letter = letter(name = "Ba"), heard = false, onClick = {})
        }
        composeRule.onNodeWithContentDescription("Letter Ba").assertExists()
    }

    @Test
    fun `heard letter marks content description`() {
        composeRule.setThemedContent {
            QaidaLetterTile(letter = letter(name = "Ba"), heard = true, onClick = {})
        }
        composeRule.onNodeWithContentDescription("Letter Ba, heard").assertExists()
    }

    @Test
    fun `click fires onClick with the letter`() {
        var fired: QaidaLetter? = null
        val target = letter(name = "Ba")
        composeRule.setThemedContent {
            QaidaLetterTile(letter = target, heard = false, onClick = { fired = it })
        }
        composeRule.onNodeWithContentDescription("Letter Ba").performClick()
        assertThat(fired).isEqualTo(target)
    }
}
