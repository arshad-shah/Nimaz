package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLetterBoardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun letter(id: Int, arabic: String, name: String) = QaidaLetter(
        id = id,
        letterArabic = arabic,
        nameArabic = "اسم",
        nameTransliteration = name,
        isolatedForm = arabic,
        initialForm = null,
        medialForm = null,
        finalForm = null,
        isConnecting = true,
        makhrajArea = MakhrajArea.LISAN,
        makhrajDetail = "",
        phoneticHint = null,
        audioKey = name.lowercase(),
        audioPath = "",
        displayOrder = id,
    )

    private val letters = listOf(
        letter(1, "ا", "Alif"),
        letter(2, "ب", "Ba"),
        letter(3, "ت", "Ta"),
        letter(4, "ث", "Tha"),
    )

    @Test
    fun rendersBoardSmoke() {
        composeRule.setThemedContent {
            QaidaLetterBoard(
                letters = letters,
                heardLetterIds = setOf(1, 2),
                onLetterClick = {},
                modifier = Modifier.height(400.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun rendersEmptyBoardSmoke() {
        composeRule.setThemedContent {
            QaidaLetterBoard(
                letters = emptyList(),
                heardLetterIds = emptySet(),
                onLetterClick = {},
                modifier = Modifier.height(400.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
