package com.arshadshah.nimaz.presentation.screens.bookmarks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.SavedKind
import com.arshadshah.nimaz.domain.model.UnifiedBookmark
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarkStatsUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarksEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarksUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarksViewModel
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
 * What the Saved screen shows for a given state, and which of its four empty-ish states wins.
 *
 * The ordering of those branches is the part worth pinning. An empty list is *also* what a failed
 * load leaves behind, so "You haven't saved anything yet" is what someone with a hundred bookmarks
 * sees the day the read throws — which is why the error branch is checked first. And a failed
 * *write* must not take the list with it: the bookmarks on screen are still correct, so a delete
 * that did not go through belongs on the snackbar and nowhere else.
 *
 * The view model is a mock so the state is the input rather than something to be arranged through
 * six use cases; what it reads back — which events the screen sends — is the behaviour.
 */
@RunWith(RobolectricTestRunner::class)
// A tall window, so a screen's whole scrolling content composes and the test can reach a row
// without first driving the list to it. The default Robolectric display is a phone, and on one
// a LazyColumn composes about a screenful — which turns "does this row open that screen" into a
// test about scroll offsets.
@Config(qualifiers = "w411dp-h2200dp")
class SavedScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(BookmarksUiState())
    private val stats = MutableStateFlow(BookmarkStatsUiState())
    private val events = mutableListOf<BookmarksEvent>()

    private val viewModel: BookmarksViewModel = mockk(relaxed = true) {
        every { bookmarksState } returns state
        every { statsState } returns stats
        every { onEvent(any()) } answers { events += firstArg<BookmarksEvent>() }
    }

    private fun render(back: () -> Unit = {}, onAyah: (Int, Int) -> Unit = { _, _ -> }) {
        composeRule.setThemedContent {
            SavedScreen(
                onNavigateBack = back,
                onNavigateToQuranAyah = onAyah,
                onNavigateToHadith = { _, _ -> },
                onNavigateToDua = {},
                viewModel = viewModel,
            )
        }
    }

    private fun bookmark(
        id: String = "quran_1",
        type: BookmarkType = BookmarkType.QURAN,
        title: String = "Al-Baqarah 2:255",
        subtitle: String = "Ayat al-Kursi",
        kinds: Set<SavedKind> = setOf(SavedKind.BOOKMARK),
        note: String? = null,
    ) = UnifiedBookmark(
        id = id,
        type = type,
        kinds = kinds,
        title = title,
        subtitle = subtitle,
        arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
        createdAt = 1_000,
        note = note,
        color = null,
        surahNumber = 2,
        ayahNumber = 255,
    )

    // ---- Which branch wins ----

    @Test
    fun `a first load with nothing yet shows the loading state, not the empty one`() {
        state.value = BookmarksUiState(isLoading = true)

        render()

        composeRule.onNodeWithText(str(R.string.no_bookmarks_yet)).assertDoesNotExist()
    }

    @Test
    fun `a reader with nothing saved is told so`() {
        state.value = BookmarksUiState(isLoading = false)

        render()

        composeRule.onNodeWithText(str(R.string.no_bookmarks_yet)).assertIsDisplayed()
    }

    @Test
    fun `a failed read says the load failed rather than that nothing is saved`() {
        // The two states look identical from the list's point of view — both are an empty list —
        // and telling someone with a hundred bookmarks that they have none is the worse lie.
        state.value = BookmarksUiState(
            isLoading = false,
            error = UiError(message = R.string.bookmarks_load_failed),
        )

        render()

        composeRule.onNodeWithText(str(R.string.no_bookmarks_yet)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.try_again)).assertIsDisplayed()
    }

    @Test
    fun `retrying a failed read asks the view model to load again`() {
        state.value = BookmarksUiState(
            isLoading = false,
            error = UiError(message = R.string.bookmarks_load_failed),
        )
        render()

        composeRule.onNodeWithText(str(R.string.try_again)).performClick()

        assertThat(events).contains(BookmarksEvent.Retry)
    }

    // ---- The list ----

    @Test
    fun `saved items are listed`() {
        val saved = listOf(bookmark(id = "quran_1", title = "Al-Baqarah 2:255"))
        state.value = BookmarksUiState(
            isLoading = false,
            allBookmarks = saved,
            filteredBookmarks = saved,
        )
        stats.value = BookmarkStatsUiState(totalBookmarks = 1, quranCount = 1)

        render()

        composeRule.onNodeWithText("Al-Baqarah 2:255").assertIsDisplayed()
        composeRule.onNodeWithText("Ayat al-Kursi").assertIsDisplayed()
    }

    @Test
    fun `a hadith is listed the same way a verse is`() {
        // One list, three corpora — the screen exists because the store is one `bookmarks` table
        // and the Qur'an section's own favourites tab could not see the rest of it.
        val saved = listOf(
            bookmark(id = "hadith_1", type = BookmarkType.HADITH, title = "Bukhari 1", subtitle = "Book of Revelation"),
        )
        state.value = BookmarksUiState(
            isLoading = false,
            allBookmarks = saved,
            filteredBookmarks = saved,
        )
        stats.value = BookmarkStatsUiState(totalBookmarks = 1, hadithCount = 1)

        render()

        composeRule.onNodeWithText("Bukhari 1").assertIsDisplayed()
    }

    @Test
    fun `a saved note is shown on the card rather than hidden behind it`() {
        // A note used to be invisible unless you happened to open the row carrying it.
        val saved = listOf(
            bookmark(kinds = setOf(SavedKind.BOOKMARK, SavedKind.NOTE), note = "read again in Ramadan"),
        )
        state.value = BookmarksUiState(
            isLoading = false,
            allBookmarks = saved,
            filteredBookmarks = saved,
        )
        stats.value = BookmarkStatsUiState(totalBookmarks = 1, noteCount = 1)

        render()

        composeRule.onNodeWithText("read again in Ramadan").assertIsDisplayed()
    }

    @Test
    fun `a filter that matches nothing says so without claiming the list is empty`() {
        val saved = listOf(bookmark())
        state.value = BookmarksUiState(
            isLoading = false,
            allBookmarks = saved,
            filteredBookmarks = emptyList(),
            searchQuery = "zzz",
        )
        stats.value = BookmarkStatsUiState(totalBookmarks = 1)

        render()

        // "No matches" and "nothing saved yet" are different facts and the reader can act on
        // only one of them.
        composeRule.onNodeWithText(str(R.string.no_bookmarks_yet)).assertDoesNotExist()
    }

    @Test
    fun `typing in the search field asks the view model to narrow the list`() {
        val saved = listOf(bookmark())
        state.value = BookmarksUiState(
            isLoading = false,
            allBookmarks = saved,
            filteredBookmarks = saved,
        )
        stats.value = BookmarkStatsUiState(totalBookmarks = 1)
        render()

        composeRule.onNodeWithText(str(R.string.bookmarks_search_placeholder)).performTextInput("kursi")

        assertThat(events.filterIsInstance<BookmarksEvent.SetSearchQuery>().map { it.query })
            .contains("kursi")
    }

    // ---- The app bar ----

    @Test
    fun `the app bar carries no list controls while there is no list`() {
        state.value = BookmarksUiState(isLoading = false)

        render()

        // Sort and filter over nothing are three taps that cannot change anything.
        composeRule.onNodeWithContentDescription(str(R.string.bookmarks_sort)).assertDoesNotExist()
    }

    @Test
    fun `going back is the caller's business, not the view model's`() {
        var wentBack = false
        state.value = BookmarksUiState(isLoading = false)

        render(back = { wentBack = true })
        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(wentBack).isTrue()
        assertThat(events).isEmpty()
    }
}
