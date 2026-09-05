package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The grid cell every names catalogue is made of.
 *
 * Two of its four inputs are optional and only the Prophets tab passes them, so the difference
 * between "a name" and "a prophet" lives entirely in whether `titleLabel` and `eraChip` are null.
 * That branch is the one worth holding: the Prophets tab silently losing its era is not a crash
 * and not a layout change, just a fact that stops being on screen.
 */
@RunWith(RobolectricTestRunner::class)
class NameMedallionCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var clicks = 0
    private var favouriteToggles = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun card(
        isFavorite: Boolean = false,
        titleLabel: String? = null,
        eraChip: String? = null,
    ): @Composable () -> Unit = {
        NameMedallionCard(
            number = 1,
            arabicName = "الرَّحْمَٰن",
            primaryLabel = "Ar-Rahman",
            secondaryLabel = "The Most Compassionate",
            isFavorite = isFavorite,
            accent = NamesAccents.allah(),
            onClick = { clicks++ },
            onFavoriteClick = { favouriteToggles++ },
            titleLabel = titleLabel,
            eraChip = eraChip,
        )
    }

    @Test
    fun `the card carries the arabic, the transliteration and the meaning`() {
        composeRule.setThemedContent(card())

        composeRule.onNodeWithText("الرَّحْمَٰن").assertIsDisplayed()
        composeRule.onNodeWithText("Ar-Rahman").assertIsDisplayed()
        composeRule.onNodeWithText("The Most Compassionate").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `the whole cell opens the name`() {
        composeRule.setThemedContent(card())

        composeRule.onNodeWithText("Ar-Rahman").assertHasClickAction()
        composeRule.onNodeWithText("Ar-Rahman").performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `an unfavourited name offers to add it`() {
        composeRule.setThemedContent(card(isFavorite = false))

        composeRule.onNodeWithContentDescription(
            str(R.string.add_to_favorites)
        ).performClick()
        assertThat(favouriteToggles).isEqualTo(1)
    }

    @Test
    fun `a favourited name offers to remove it`() {
        composeRule.setThemedContent(card(isFavorite = true))

        composeRule.onNodeWithContentDescription(
            str(R.string.remove_from_favorites)
        ).assertIsDisplayed()
    }

    /** The Prophets variant: both optional lines present. */
    @Test
    fun `a prophet carries its title and era`() {
        composeRule.setThemedContent(
            card(titleLabel = "The first man", eraChip = "Beginning")
        )

        composeRule.onNodeWithText("The first man").assertIsDisplayed()
        composeRule.onNodeWithText("Beginning").assertIsDisplayed()
    }

    /** And a name that is not a prophet shows neither, rather than an empty line. */
    @Test
    fun `a name without a title or era shows neither`() {
        composeRule.setThemedContent(card())

        composeRule.onNodeWithText("The first man").assertDoesNotExist()
        composeRule.onNodeWithText("Beginning").assertDoesNotExist()
    }
}
