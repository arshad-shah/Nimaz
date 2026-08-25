package com.arshadshah.nimaz.presentation.screens.hadith

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithChaptersUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithViewModel
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
 * One collection's chapters — the step between the library and the reader.
 *
 * **Three empty-looking states are three different things here**, and the screen's `when`
 * ordering is the whole of what keeps them apart: still loading, failed to load, and a
 * collection that genuinely lists no chapters. `chapters.isEmpty()` is true in all three. The
 * comment in the source says the error branch sits before the empty branch *because* a failure
 * once reported itself as "No chapters found" — a message that tells the reader to give up on
 * a collection that is fine.
 *
 * **The search filter is derived, never stored.** `filteredChapters` recomputes from
 * `searchQuery`, which is what lets a Room re-emission — a content refresh, a bookmark write —
 * repaint the list without wiping what the reader typed. The screen is where that shows: the
 * rows it renders come from `filteredChapters`, and rendering `chapters` instead would look
 * correct until the moment someone searched.
 *
 * **A chapter is opened by book *and* chapter.** `onNavigateToChapter(bookId, chapter.id)`
 * carries both because the reader keys on the composite `bookId_chapterId`; passing the
 * chapter's own id alone resolves the header to null, which is the defect
 * `Hadith.chapterKey` exists to prevent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HadithChaptersScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val chaptersState = MutableStateFlow(HadithChaptersUiState())
    private val events = mutableListOf<HadithEvent>()

    private val viewModel: HadithViewModel = mockk(relaxed = true) {
        every { this@mockk.chaptersState } returns this@HadithChaptersScreenTest.chaptersState
        every { onEvent(any()) } answers { events += firstArg<HadithEvent>() }
    }

    private val openedChapters = mutableListOf<Pair<String, String>>()
    private var backs = 0

    private fun setContent(bookId: String = "bukhari") {
        composeRule.setThemedContent {
            HadithChaptersScreen(
                bookId = bookId,
                onNavigateBack = { backs++ },
                onNavigateToChapter = { book, chapter -> openedChapters += book to chapter },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `opening the screen asks for the book named in the route`() {
        // The screen owns no id of its own: everything below depends on this dispatch carrying
        // the route's `bookId` rather than whatever the ViewModel last loaded.
        chaptersState.value = HadithChaptersUiState(isLoading = false)

        setContent(bookId = "tirmidhi")

        assertThat(events).containsExactly(HadithEvent.LoadBook("tirmidhi"))
    }

    @Test
    fun `the chapters render with their number, name and hadith count`() {
        chaptersState.value = HadithChaptersUiState(
            book = book(),
            chapters = listOf(
                chapter(id = "bukhari_1", chapterNumber = 1, nameEnglish = "Revelation", hadithCount = 7),
                chapter(id = "bukhari_2", chapterNumber = 2, nameEnglish = "Belief", hadithCount = 51),
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Revelation").assertIsDisplayed()
        composeRule.onNodeWithText("Belief").assertIsDisplayed()
        composeRule.onNodeWithText("51").assertExists()
    }

    @Test
    fun `a chapter with no Arabic name renders without a blank second line`() {
        // `nameArabic.isNotBlank()` guards the Arabic line. The content artifact does not carry
        // one for every chapter of every collection.
        chaptersState.value = HadithChaptersUiState(
            book = book(),
            chapters = listOf(chapter(nameEnglish = "Ablution", nameArabic = "")),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Ablution").assertIsDisplayed()
    }

    @Test
    fun `tapping a chapter opens it under its own book`() {
        chaptersState.value = HadithChaptersUiState(
            book = book(),
            chapters = listOf(
                chapter(id = "bukhari_1", nameEnglish = "Revelation"),
                chapter(id = "bukhari_2", nameEnglish = "Belief"),
            ),
            isLoading = false,
        )

        setContent(bookId = "bukhari")
        composeRule.onNodeWithText("Belief").performClick()

        assertThat(openedChapters).containsExactly("bukhari" to "bukhari_2")
    }

    @Test
    fun `the header card reports the chapters actually loaded, not the book's own total`() {
        // `totalChapters` is what the collection claims; `chapters.size` is what arrived. A
        // partial content artifact makes the two disagree, and the count under the title is the
        // only place a reader would ever see that.
        chaptersState.value = HadithChaptersUiState(
            book = book(totalChapters = 97, totalHadiths = 7563),
            chapters = List(3) { chapter(id = "bukhari_$it", chapterNumber = it + 1) },
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.hadith_chapters_count_format, 3, 3)
        ).assertExists()
        composeRule.onNodeWithText(string(R.string.hadith_count_format, "7563")).assertExists()
    }

    @Test
    fun `a search narrows the list without touching the chapters underneath`() {
        // `filteredChapters` is derived from `searchQuery` on every read. Rendering `chapters`
        // instead would pass every test that does not set a query.
        chaptersState.value = HadithChaptersUiState(
            book = book(),
            chapters = listOf(
                chapter(id = "bukhari_1", nameEnglish = "Revelation"),
                chapter(id = "bukhari_2", nameEnglish = "Belief"),
                chapter(id = "bukhari_3", nameEnglish = "Knowledge"),
            ),
            searchQuery = "bel",
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Belief").assertIsDisplayed()
        composeRule.onNodeWithText("Revelation").assertDoesNotExist()
        composeRule.onNodeWithText("Knowledge").assertDoesNotExist()
    }

    @Test
    fun `a search that matches nothing still shows the book's header`() {
        // The header comes from `chapters`, the rows from `filteredChapters`. Driving both off
        // the filtered list would blank the whole screen on a typo.
        chaptersState.value = HadithChaptersUiState(
            book = book(nameEnglish = "Sahih al-Bukhari"),
            chapters = listOf(chapter(nameEnglish = "Revelation")),
            searchQuery = "zzzz",
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Imam al-Bukhari").assertExists()
        composeRule.onNodeWithText("Revelation").assertDoesNotExist()
    }

    @Test
    fun `a collection that lists no chapters says so`() {
        chaptersState.value = HadithChaptersUiState(
            book = book(),
            chapters = emptyList(),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.no_chapters_found)).assertIsDisplayed()
    }

    @Test
    fun `a failed load is reported as a failure, not as an empty collection`() {
        // The ordering this pins is the fix: `chapters.isEmpty()` is true here too, and the
        // empty branch used to win.
        chaptersState.value = HadithChaptersUiState(
            book = book(),
            chapters = emptyList(),
            isLoading = false,
            error = UiError(
                message = R.string.hadith_chapters_load_failed,
                details = "disk I/O error",
            ),
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_chapters_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.no_chapters_found)).assertDoesNotExist()
    }

    @Test
    fun `retrying a failed chapter list re-issues the load`() {
        chaptersState.value = HadithChaptersUiState(
            isLoading = false,
            error = UiError(message = R.string.hadith_chapters_load_failed),
        )

        setContent()
        events.clear()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).containsExactly(HadithEvent.Retry)
    }

    @Test
    fun `the app bar falls back to a generic title until the book is known`() {
        // `state.book` is null for the whole first frame of every navigation. Showing "Chapters"
        // rather than an empty bar is what stops the screen from looking broken while it loads.
        composeRule.mainClock.autoAdvance = false
        chaptersState.value = HadithChaptersUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_chapters_title)).assertExists()
    }

    @Test
    fun `once the book is known the app bar carries its name`() {
        // Asserted on an empty collection deliberately: with chapters present the header card
        // renders the same name a second time, and "found 2 nodes" would pass for the wrong
        // reason — the generic title could still be sitting in the bar.
        chaptersState.value = HadithChaptersUiState(
            book = book(nameEnglish = "Sunan an-Nasa'i"),
            chapters = emptyList(),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Sunan an-Nasa'i").assertExists()
        composeRule.onNodeWithText(string(R.string.hadith_chapters_title)).assertDoesNotExist()
    }
}
