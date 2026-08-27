package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaCellTileTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun cell(
        id: Int = 1,
        textArabic: String = "بَ",
        transliteration: String = "ba",
    ) = QaidaCell(
        id = id,
        lineId = 1,
        lessonId = 1,
        position = id,
        textArabic = textArabic,
        transliteration = transliteration,
        tokenType = TokenType.SYLLABLE,
        audioKey = "ba",
        audioPath = "",
        highlightGroup = "fatha",
        letterId = 2,
        notes = null,
    )

    @Test
    fun `renders transliteration when enabled`() {
        composeRule.setThemedContent {
            QaidaCellTile(
                cell = cell(transliteration = "ba"),
                isPlaying = false,
                showTransliteration = true,
                onTap = {},
            )
        }
        composeRule.onNodeWithText("ba").assertExists()
    }

    @Test
    fun `tap fires onTap with the cell`() {
        var fired: QaidaCell? = null
        val target = cell(transliteration = "ba")
        composeRule.setThemedContent {
            QaidaCellTile(
                cell = target,
                isPlaying = false,
                showTransliteration = true,
                onTap = { fired = it },
            )
        }
        composeRule.onNodeWithText("ba").performClick()
        assertThat(fired).isEqualTo(target)
    }
}
