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
 * One category's duas, and the row component the occasion screen shares with it.
 *
 * **`DuaListItem` is the same row in two screens with one difference**: given an
 * `onOccasionClick` the occasion under the title becomes a tappable badge into that occasion's
 * whole list; without one it is plain text. Both branches ship — the category screen passes the
 * lambda, the occasion screen does not, because a chip that navigates to the list you are
 * already looking at is a dead end. Nothing but a rendering test can tell the two apart.
 *
 * **The occasion label must come from the resources, not from the domain model.**
 * `DuaOccasion.displayName()` returns hardcoded English, which is right for a log line and
 * wrong on a chip a German reader is looking at; `duaOccasionLabelRes` is the presentation-layer
 * mapping that exists for it. A row rendering the enum's own name would look correct in every
 * English test.
 *
 * **The repeat badge is guarded twice** — null, and zero. The content artifact carries `0` for
 * duas with no prescribed count, and an unguarded badge reads "0x".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class DuaCategoryScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val categoryState = MutableStateFlow(DuaCategoryUiState())
    private val events = mutableListOf<DuaEvent>()

    private val viewModel: DuaViewModel = mockk(relaxed = true) {
        every { this@mockk.categoryState } returns this@DuaCategoryScreenTest.categoryState
        every { onEvent(any()) } answers { events += firstArg<DuaEvent>() }
    }

    private val openedDuas = mutableListOf<String>()
    private val openedOccasions = mutableListOf<DuaOccasion>()

    private fun setContent(categoryId: String = "morning") {
        composeRule.setThemedContent {
            DuaCategoryScreen(
                categoryId = categoryId,
                onNavigateBack = {},
                onNavigateToDua = { openedDuas += it },
                onNavigateToOccasion = { openedOccasions += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `opening the screen asks for the category named in the route`() {
        categoryState.value = DuaCategoryUiState(isLoading = false)

        setContent(categoryId = "before_sleep")

        assertThat(events).containsExactly(DuaEvent.LoadCategory("before_sleep"))
    }

    @Test
    fun `each dua renders its number, title, Arabic and translation`() {
        categoryState.value = DuaCategoryUiState(
            category = category(),
            duas = listOf(
                dua(
                    id = "d1",
                    displayOrder = 1,
                    titleEnglish = "Upon waking",
                    textArabic = "الحمد لله",
                    textEnglish = "Praise be to Allah",
                ),
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Upon waking").assertIsDisplayed()
        composeRule.onNodeWithText("الحمد لله").assertExists()
        composeRule.onNodeWithText("Praise be to Allah").assertExists()
        composeRule.onNodeWithText("1").assertExists()
    }

    @Test
    fun `the header card carries the category's Arabic name and description`() {
        categoryState.value = DuaCategoryUiState(
            category = category(
                nameArabic = "أذكار الصباح",
                description = "Said between dawn and sunrise",
            ),
            duas = listOf(dua()),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("أذكار الصباح").assertExists()
        composeRule.onNodeWithText("Said between dawn and sunrise").assertExists()
    }

    @Test
    fun `a category with no description renders its header without one`() {
        categoryState.value = DuaCategoryUiState(
            category = category(nameArabic = "أدعية", description = null),
            duas = listOf(dua()),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("أدعية").assertExists()
    }

    @Test
    fun `tapping a dua opens that dua`() {
        categoryState.value = DuaCategoryUiState(
            category = category(),
            duas = listOf(
                dua(id = "d1", titleEnglish = "Upon waking"),
                dua(id = "d2", titleEnglish = "Upon leaving home"),
            ),
            isLoading = false,
        )

        setContent()
        composeRule.onNodeWithText("Upon leaving home").performClick()

        assertThat(openedDuas).containsExactly("d2")
    }

    @Test
    fun `the occasion under a title is a way into that occasion's whole list`() {
        // Only this screen passes `onOccasionClick`, and the badge it turns the label into is
        // the sole route to `DuaOccasionScreen`.
        categoryState.value = DuaCategoryUiState(
            category = category(),
            duas = listOf(dua(id = "d1", occasion = DuaOccasion.TRAVELING)),
            isLoading = false,
        )

        setContent()
        composeRule.onNodeWithText(string(R.string.dua_occasion_traveling)).performClick()

        assertThat(openedOccasions).containsExactly(DuaOccasion.TRAVELING)
        // Tapping the occasion is not tapping the row.
        assertThat(openedDuas).isEmpty()
    }

    @Test
    fun `the occasion label is the localised one, not the domain model's English`() {
        // `DuaOccasion.BEFORE_SLEEP.displayName()` and the resource differ in wording; rendering
        // the model's own string would pass unnoticed in English and ship untranslated
        // everywhere else.
        categoryState.value = DuaCategoryUiState(
            category = category(),
            duas = listOf(dua(id = "d1", occasion = DuaOccasion.BEFORE_SLEEP)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_occasion_before_sleep)).assertExists()
    }

    @Test
    fun `a dua filed under no occasion renders no occasion line`() {
        categoryState.value = DuaCategoryUiState(
            category = category(),
            duas = listOf(dua(id = "d1", titleEnglish = "Unfiled", occasion = null)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Unfiled").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.dua_occasion_general)).assertDoesNotExist()
    }

    @Test
    fun `a prescribed repeat count is shown, and a count of zero is not`() {
        categoryState.value = DuaCategoryUiState(
            category = category(),
            duas = listOf(
                dua(id = "d1", titleEnglish = "Thrice", repeatCount = 3),
                dua(id = "d2", titleEnglish = "Unprescribed", repeatCount = 0),
                dua(id = "d3", titleEnglish = "Unknown", repeatCount = null),
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_repeat_count_format, 3)).assertExists()
        composeRule.onNodeWithText(string(R.string.dua_repeat_count_format, 0)).assertDoesNotExist()
    }

    @Test
    fun `the app bar counts the category's duas`() {
        categoryState.value = DuaCategoryUiState(
            category = category(nameEnglish = "Morning Adhkar", duaCount = 12),
            duas = listOf(dua()),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Morning Adhkar").assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.duas_count_format, 12, 12)
        ).assertExists()
    }

    @Test
    fun `a failed load is reported, and retrying reloads the same category`() {
        categoryState.value = DuaCategoryUiState(
            isLoading = false,
            error = UiError(message = R.string.dua_category_load_failed, details = "disk I/O"),
        )

        setContent(categoryId = "evening")
        events.clear()
        composeRule.onNodeWithText(string(R.string.dua_category_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        // The route's id, not whatever the ViewModel happened to load last.
        assertThat(events).containsExactly(DuaEvent.LoadCategory("evening"))
    }

    @Test
    fun `the app bar falls back to a generic title until the category is known`() {
        composeRule.mainClock.autoAdvance = false
        categoryState.value = DuaCategoryUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.duas)).assertExists()
    }
}
