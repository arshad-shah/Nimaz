package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranMushafPageBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        id: Int,
        juzNumber: Int = 1,
        // The bar derives hizb from the 1..240 quarter counter, never from `hizbNumber` (which
        // holds the quarter index in the shipped data). Quarter 5 is the opening of hizb 2.
        rubNumber: Int = 5,
        pageNumber: Int = 15
    ) = Ayah(
        id = id,
        surahNumber = 2,
        ayahNumber = id,
        textArabic = "",
        textSimple = "",
        juzNumber = juzNumber,
        hizbNumber = 0,
        rubNumber = rubNumber,
        pageNumber = pageNumber,
        sajdaType = null,
        sajdaNumber = null
    )

    @Test
    fun `single page renders page label juz and hizb`() {
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 15,
                totalPages = 604,
                ayahs = listOf(ayah(id = 100, juzNumber = 1, rubNumber = 5)),
                isKhatamActive = false,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = {},
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        composeRule.onNodeWithText("Page 15").assertExists()
        // Juz and hizb are one muted run appended to the page label, not separate chips.
        composeRule.onNodeWithText("Juz 1", substring = true).assertExists()
        composeRule.onNodeWithText("Hizb 2", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Next page").assertExists()
        composeRule.onNodeWithContentDescription("Previous page").assertExists()
    }

    @Test
    fun `two page spread renders range label`() {
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 14,
                secondPageNumber = 15,
                totalPages = 604,
                ayahs = listOf(ayah(id = 100)),
                isKhatamActive = false,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = {},
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        composeRule.onNodeWithText("Pages 14–15").assertExists()
    }

    @Test
    fun `empty ayahs hides juz and hizb`() {
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 1,
                totalPages = 604,
                ayahs = emptyList(),
                isKhatamActive = true,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = {},
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        composeRule.onNodeWithText("Page 1").assertExists()
        // juz/hizb are 0 -> not rendered
        composeRule.onNodeWithText("Juz 0", substring = true).assertDoesNotExist()
        // khatam active but ayahs empty -> toggle button not rendered
        composeRule.onNodeWithContentDescription("Mark page as read").assertDoesNotExist()
    }

    @Test
    fun `navigation enabled boundaries`() {
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 1,
                totalPages = 604,
                ayahs = listOf(ayah(id = 1)),
                isKhatamActive = false,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = {},
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        // page 1 -> previous disabled, next enabled (1 < 604)
        composeRule.onNodeWithContentDescription("Previous page").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Next page").assertIsEnabled()
    }

    @Test
    fun `next disabled at last page and previous enabled`() {
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 604,
                totalPages = 604,
                ayahs = listOf(ayah(id = 6000, juzNumber = 30)),
                isKhatamActive = false,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = {},
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        composeRule.onNodeWithContentDescription("Next page").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Previous page").assertIsEnabled()
    }

    @Test
    fun `navigation callbacks invoked`() {
        var prev = false
        var next = false
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 15,
                totalPages = 604,
                ayahs = listOf(ayah(id = 100)),
                isKhatamActive = false,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = {},
                onNavigatePrevious = { prev = true },
                onNavigateNext = { next = true }
            )
        }
        composeRule.onNodeWithContentDescription("Previous page").performClick()
        composeRule.onNodeWithContentDescription("Next page").performClick()
        assertThat(prev).isTrue()
        assertThat(next).isTrue()
    }

    @Test
    fun `khatam toggle unread page shows mark action and invokes callback`() {
        var toggled: List<Ayah>? = null
        val ayahs = listOf(ayah(id = 100), ayah(id = 101))
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 15,
                totalPages = 604,
                ayahs = ayahs,
                isKhatamActive = true,
                khatamReadAyahIds = emptySet(),
                onKhatamTogglePage = { toggled = it },
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        composeRule.onNodeWithContentDescription("Mark page as read").assertExists()
        composeRule.onNodeWithContentDescription("Mark page as read").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Mark page as read").performClick()
        assertThat(toggled).isEqualTo(ayahs)
    }

    @Test
    fun `khatam toggle fully read page shows read state and is disabled`() {
        val ayahs = listOf(ayah(id = 100), ayah(id = 101))
        composeRule.setThemedContent {
            MushafPageBar(
                pageNumber = 15,
                totalPages = 604,
                ayahs = ayahs,
                isKhatamActive = true,
                khatamReadAyahIds = setOf(100, 101),
                onKhatamTogglePage = {},
                onNavigatePrevious = {},
                onNavigateNext = {}
            )
        }
        composeRule.onNodeWithContentDescription("Page read").assertExists()
        composeRule.onNodeWithContentDescription("Page read").assertIsNotEnabled()
    }
}
