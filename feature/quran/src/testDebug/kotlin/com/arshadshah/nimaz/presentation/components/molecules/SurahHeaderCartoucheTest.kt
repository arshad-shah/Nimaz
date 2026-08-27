package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurahHeaderCartoucheTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun showsSurahNamesAndRevelationBadge_withoutAyahCountBadge() {
        composeRule.setThemedContent {
            SurahHeaderCartouche(
                surah = Surah(
                    number = 2,
                    nameArabic = "البقرة",
                    nameEnglish = "Al-Baqarah",
                    nameTransliteration = "The Cow",
                    revelationType = RevelationType.MEDINAN,
                    ayahCount = 286,
                    orderInMushaf = 2,
                ),
            )
        }

        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("Al-Baqarah").assertExists()
        composeRule.onNodeWithText("البقرة").assertExists()
        composeRule.onNodeWithText("Medinan").assertExists()
        composeRule.onNodeWithText("286 Ayahs").assertDoesNotExist()
    }
}
