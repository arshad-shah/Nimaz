package com.arshadshah.nimaz.presentation.screens.dua

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaCategoryUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every dua for one occasion, gathered across the curated categories.
 *
 * The cross-cut shipped in the database, the repository and the event before anything could
 * dispatch it — so this screen is the only thing that makes `LoadDuasByOccasion` reachable, and
 * a regression here returns the feature to existing everywhere except where a reader can get to
 * it.
 *
 * It renders from `categoryState`, the same surface `LoadCategory` fills, so **the header card
 * must not appear**: `state.category` is null in this mode by construction, and a screen that
 * rendered the card unconditionally would show the last category the reader visited above an
 * unrelated occasion's duas. The row here also passes no `onOccasionClick` — a chip navigating
 * to the list you are already on is a dead end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class DuaOccasionScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val categoryState = MutableStateFlow(DuaCategoryUiState())
    private val events = mutableListOf<DuaEvent>()

    private val viewModel: DuaViewModel = mockk(relaxed = true) {
        every { this@mockk.categoryState } returns this@DuaOccasionScreenTest.categoryState
        every { onEvent(any()) } answers { events += firstArg<DuaEvent>() }
    }

    private val openedDuas = mutableListOf<String>()

    private fun setContent(occasion: DuaOccasion = DuaOccasion.MORNING) {
        composeRule.setThemedContent {
            DuaOccasionScreen(
                occasion = occasion,
                onNavigateBack = {},
                onNavigateToDua = { openedDuas += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `opening the screen asks for that occasion's duas`() {
        categoryState.value = DuaCategoryUiState(isLoading = false)

        setContent(occasion = DuaOccasion.DISTRESS)

        assertThat(events).containsExactly(
            DuaEvent.LoadDuasByOccasion(DuaOccasion.DISTRESS)
        )
    }

    @Test
    fun `the occasion's own name titles the screen and the count follows it`() {
        categoryState.value = DuaCategoryUiState(
            duas = listOf(dua(id = "d1"), dua(id = "d2")),
            isLoading = false,
        )

        setContent(occasion = DuaOccasion.FORGIVENESS)

        composeRule.onNodeWithText(string(R.string.dua_occasion_forgiveness)).assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.duas_count_format, 2, 2)
        ).assertExists()
    }

    @Test
    fun `the duas gathered for the occasion are listed`() {
        categoryState.value = DuaCategoryUiState(
            duas = listOf(
                dua(id = "d1", titleEnglish = "On leaving the house"),
                dua(id = "d2", titleEnglish = "On boarding a mount"),
            ),
            isLoading = false,
        )

        setContent(occasion = DuaOccasion.TRAVELING)

        composeRule.onNodeWithText("On leaving the house").assertIsDisplayed()
        composeRule.onNodeWithText("On boarding a mount").assertIsDisplayed()
    }

    @Test
    fun `no category header is rendered over an occasion's list`() {
        // `categoryState` is shared with the category screen, and a stale `category` left in it
        // would put another collection's Arabic name and description above these duas.
        categoryState.value = DuaCategoryUiState(
            category = category(nameArabic = "أذكار الصباح", description = "Morning adhkar"),
            duas = listOf(dua(id = "d1", titleEnglish = "Travel dua")),
            isLoading = false,
        )

        setContent(occasion = DuaOccasion.TRAVELING)

        composeRule.onNodeWithText("Travel dua").assertIsDisplayed()
        composeRule.onNodeWithText("Morning adhkar").assertDoesNotExist()
    }

    @Test
    fun `the occasion label under a row is text here, not a way back to this screen`() {
        // No `onOccasionClick` is passed, so the badge branch of `DuaListItem` must not run.
        categoryState.value = DuaCategoryUiState(
            duas = listOf(dua(id = "d1", occasion = DuaOccasion.RAIN)),
            isLoading = false,
        )

        setContent(occasion = DuaOccasion.RAIN)

        composeRule.onNodeWithText("Travel dua").assertDoesNotExist()
        composeRule.onNodeWithText(dua().titleEnglish).performClick()
        assertThat(openedDuas).containsExactly("d1")
    }

    @Test
    fun `an occasion nothing is filed under says so rather than looking broken`() {
        categoryState.value = DuaCategoryUiState(duas = emptyList(), isLoading = false)

        setContent(occasion = DuaOccasion.GRATITUDE)

        composeRule.onNodeWithText(string(R.string.dua_occasion_empty)).assertIsDisplayed()
    }

    @Test
    fun `a failed load is reported as a failure, and retrying re-asks for the occasion`() {
        categoryState.value = DuaCategoryUiState(
            duas = emptyList(),
            isLoading = false,
            error = UiError(message = R.string.dua_category_load_failed, details = "disk I/O"),
        )

        setContent(occasion = DuaOccasion.EATING)
        events.clear()

        composeRule.onNodeWithText(string(R.string.dua_category_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.dua_occasion_empty)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.try_again)).performClick()
        assertThat(events).containsExactly(DuaEvent.LoadDuasByOccasion(DuaOccasion.EATING))
    }

    @Test
    fun `while loading, neither the list nor the empty message is on screen`() {
        composeRule.mainClock.autoAdvance = false
        categoryState.value = DuaCategoryUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_occasion_empty)).assertDoesNotExist()
    }
}
