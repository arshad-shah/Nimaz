package com.arshadshah.nimaz.presentation.screens.bookmarks

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.SavedKind
import com.arshadshah.nimaz.domain.model.UnifiedBookmark
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarkSortOrder
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
 * Saved's three app-bar controls, and the one that cannot be undone.
 *
 * They are three and not one on purpose. A single menu holding the corpus filter, the sort order
 * *and* an irreversible wipe made the reader read a list of unrelated options to find any of
 * them — and put "Clear everything saved" one slip below "A–Z". So the wipe lives on its own,
 * behind a confirmation, and the filter row states its current value rather than being a toggle
 * whose state you have to remember.
 *
 * `SavedScreenTest` covers which branch the body renders; this is the chrome above it.
 */
@RunWith(RobolectricTestRunner::class)
// Wider than a phone: the app bar carries three action buttons and their dropdowns, and on a
// 411dp window the third is clipped past the edge — which reads as "failed to inject touch
// input" rather than as a layout problem.
@Config(qualifiers = "w800dp-h2200dp")
class SavedScreenMenusTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(BookmarksUiState())
    private val stats = MutableStateFlow(BookmarkStatsUiState())
    private val events = mutableListOf<BookmarksEvent>()
    private var navigatedToAyah: Pair<Int, Int>? = null
    private var navigatedToHadith: Pair<String, Int>? = null
    private var navigatedToDua: String? = null

    private val viewModel: BookmarksViewModel = mockk(relaxed = true) {
        every { bookmarksState } returns state
        every { statsState } returns stats
        every { onEvent(any()) } answers { events += firstArg<BookmarksEvent>() }
    }

    /**
     * Click through the semantics tree rather than the touch surface.
     *
     * The three app-bar dropdowns are composed in the same `Box` and overlap, so a synthetic tap
     * at a row's coordinates can land on whichever one is on top. The row's own `onClick` is the
     * thing under test either way.
     */
    private fun SemanticsNodeInteraction.tap() =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun render() {
        composeRule.setThemedContent {
            SavedScreen(
                onNavigateBack = {},
                onNavigateToQuranAyah = { s, a -> navigatedToAyah = s to a },
                onNavigateToHadith = { b, n -> navigatedToHadith = b to n },
                onNavigateToDua = { navigatedToDua = it },
                viewModel = viewModel,
            )
        }
    }

    private fun bookmark(
        id: String = "quran_1",
        type: BookmarkType = BookmarkType.QURAN,
        title: String = "Al-Baqarah 2:255",
        kinds: Set<SavedKind> = setOf(SavedKind.BOOKMARK),
    ) = UnifiedBookmark(
        id = id,
        type = type,
        kinds = kinds,
        title = title,
        subtitle = "a subtitle",
        arabicText = "نص",
        createdAt = 1_000,
        note = null,
        color = null,
        surahNumber = 2,
        ayahNumber = 255,
        hadithBookId = "bukhari",
        hadithNumber = 1,
        duaId = "dua-1",
    )

    private fun seed(vararg saved: UnifiedBookmark) {
        state.value = BookmarksUiState(
            isLoading = false,
            allBookmarks = saved.toList(),
            filteredBookmarks = saved.toList(),
        )
        stats.value = BookmarkStatsUiState(
            totalBookmarks = saved.size,
            quranCount = saved.count { it.type == BookmarkType.QURAN },
            hadithCount = saved.count { it.type == BookmarkType.HADITH },
            duaCount = saved.count { it.type == BookmarkType.DUA },
            bookmarkCount = saved.count { SavedKind.BOOKMARK in it.kinds },
            favouriteCount = saved.count { SavedKind.FAVOURITE in it.kinds },
            noteCount = saved.count { SavedKind.NOTE in it.kinds },
        )
    }

    // ---- The corpus filter ----

    @Test
    fun `the filter menu offers every corpus, with what is in it`() {
        seed(bookmark(), bookmark(id = "hadith_1", type = BookmarkType.HADITH, title = "Bukhari 1"))
        render()

        composeRule.onNodeWithContentDescription(str(R.string.saved_show)).performClick()

        // Each row carries its count, which is what tells a reader there is nothing under
        // Hadith before they tap it.
        composeRule.onNodeWithText("${str(R.string.quran_type)}  1").assertIsDisplayed()
        composeRule.onNodeWithText("${str(R.string.hadith_type)}  1").assertIsDisplayed()
        composeRule.onNodeWithText("${str(R.string.dua_type)}  0").assertIsDisplayed()
    }

    @Test
    fun `choosing a corpus asks the view model to narrow the list`() {
        seed(bookmark(id = "hadith_1", type = BookmarkType.HADITH, title = "Bukhari 1"))
        render()

        composeRule.onNodeWithContentDescription(str(R.string.saved_show)).performClick()
        composeRule.onNodeWithText("${str(R.string.hadith_type)}  1").performClick()

        assertThat(events.filterIsInstance<BookmarksEvent.SetFilter>()).isNotEmpty()
    }

    // ---- The sort order ----

    @Test
    fun `the sort menu offers every order`() {
        seed(bookmark())
        render()

        composeRule.onNodeWithContentDescription(str(R.string.bookmarks_sort)).performClick()

        // All four, so a reader can find "A–Z" without knowing it is not the default.
        composeRule.onNodeWithText(str(R.string.bookmarks_sort_newest)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.bookmarks_sort_oldest)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.bookmarks_sort_type)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.bookmarks_sort_alphabetical)).assertIsDisplayed()
    }

    @Test
    fun `choosing an order asks the view model to re-sort`() {
        seed(bookmark())
        render()

        composeRule.onNodeWithContentDescription(str(R.string.bookmarks_sort)).performClick()
        composeRule.onNodeWithText(str(R.string.bookmarks_sort_alphabetical)).performClick()

        assertThat(events).contains(BookmarksEvent.SetSortOrder(BookmarkSortOrder.ALPHABETICAL))
    }

    // ---- The wipe ----

    @Test
    fun `the wipe is not offered while there is nothing to wipe`() {
        state.value = BookmarksUiState(isLoading = false)

        render()

        // Three app-bar controls over an empty list are three taps that cannot do anything —
        // and one of them is irreversible.
        composeRule.onAllNodesWithContentDescription(str(R.string.cd_more_options))
            .assertCountEquals(0)
    }

    // ---- Opening what was saved ----

    @Test
    fun `opening a saved verse opens the reader at it`() {
        seed(bookmark())
        render()

        composeRule.onNodeWithText("Al-Baqarah 2:255").performClick()

        assertThat(navigatedToAyah).isEqualTo(2 to 255)
    }

    @Test
    fun `opening a saved hadith opens its book`() {
        seed(bookmark(id = "hadith_1", type = BookmarkType.HADITH, title = "Bukhari 1"))
        render()

        composeRule.onNodeWithText("Bukhari 1").performClick()

        assertThat(navigatedToHadith).isEqualTo("bukhari" to 1)
    }

    @Test
    fun `opening a saved dua opens it`() {
        seed(bookmark(id = "dua_1", type = BookmarkType.DUA, title = "On waking"))
        render()

        composeRule.onNodeWithText("On waking").performClick()

        assertThat(navigatedToDua).isEqualTo("dua-1")
    }

    // ---- The kind tabs ----

    @Test
    fun `the kind tabs carry their counts`() {
        seed(
            bookmark(kinds = setOf(SavedKind.BOOKMARK)),
            bookmark(id = "quran_2", title = "Al-Fatihah 1:1", kinds = setOf(SavedKind.FAVOURITE)),
        )

        render()

        // Kind and count on one chip: "Favorites  1".
        composeRule.onNodeWithText("${str(R.string.favorites)}  1").assertIsDisplayed()
        composeRule.onNodeWithText("${str(R.string.bookmarks)}  1").assertIsDisplayed()
    }

    @Test
    fun `choosing a kind asks the view model for it`() {
        seed(bookmark(kinds = setOf(SavedKind.NOTE)))
        render()

        composeRule.onNodeWithText("${str(R.string.notes)}  1").performClick()

        assertThat(events.filterIsInstance<BookmarksEvent.SetKind>()).isNotEmpty()
    }
}
