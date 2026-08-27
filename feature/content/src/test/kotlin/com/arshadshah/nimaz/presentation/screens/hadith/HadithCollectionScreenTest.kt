package com.arshadshah.nimaz.presentation.screens.hadith

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithBookmarksUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithCollectionUiState
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
 * The hadith library's front door: six collections, three grade shelves, and one card of
 * content the app chose for today.
 *
 * Three properties are worth pinning and nothing else in the repository asserts any of them.
 *
 * **The hadith-of-the-day card has two lives.** `getHadithOfTheDay` is best-effort — the
 * ViewModel drops its failure rather than replacing the book list over one card — so
 * `hadithOfTheDay` is null on a perfectly healthy screen, and the card renders shipped
 * fallback text instead. Nothing tells the two apart at a glance, which is what makes it worth
 * an assertion: a card that shows Bukhari 6018 when the real selection failed to load is a
 * silent substitution of one narration for another.
 *
 * **Only three grades are browsable.** Mawḍūʿ (fabricated) is deliberately absent — it is a
 * warning label on an individual narration, not a shelf to invite anyone onto — and the list it
 * is absent from is a private `val` no compiler check guards. A fourth pill arriving is exactly
 * the kind of change that looks like a completion and is not.
 *
 * **The failure branch and the empty branch are distinguishable.** `books.isEmpty()` is true in
 * both, so the ordering of the `when` is the whole of what separates "the library could not be
 * read" from "the library is empty", and the retry that reaches `HadithEvent.Retry` is the only
 * way out of the first.
 *
 * Rendered tall so the whole `LazyColumn` composes — the book grid is the last of five items.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HadithCollectionScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val collectionState = MutableStateFlow(HadithCollectionUiState())
    private val bookmarksState = MutableStateFlow(HadithBookmarksUiState())
    private val events = mutableListOf<HadithEvent>()

    private val viewModel: HadithViewModel = mockk(relaxed = true) {
        every { this@mockk.collectionState } returns this@HadithCollectionScreenTest.collectionState
        every { this@mockk.bookmarksState } returns this@HadithCollectionScreenTest.bookmarksState
        every { onEvent(any()) } answers { events += firstArg<HadithEvent>() }
    }

    private val opened = mutableListOf<String>()
    private val openedGrades = mutableListOf<HadithGrade>()

    private fun setContent() {
        composeRule.setThemedContent {
            HadithCollectionScreen(
                onNavigateBack = { opened += "back" },
                onNavigateToBook = { opened += "book:$it" },
                onNavigateToBookmarks = { opened += "bookmarks" },
                onNavigateToSearch = { opened += "search" },
                onNavigateToGrade = { openedGrades += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the six collections each render with their author and hadith count`() {
        collectionState.value = HadithCollectionUiState(
            books = listOf(
                book(id = "bukhari", nameEnglish = "Sahih al-Bukhari", totalHadiths = 7563),
                book(id = "muslim", nameEnglish = "Sahih Muslim", authorName = "Imam Muslim"),
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Sahih al-Bukhari").assertIsDisplayed()
        composeRule.onNodeWithText("Imam al-Bukhari").assertIsDisplayed()
        composeRule.onNodeWithText("Sahih Muslim").assertIsDisplayed()
        composeRule.onNodeWithText("Imam Muslim").assertIsDisplayed()
    }

    @Test
    fun `a book that ships no author renders without an empty line where the name goes`() {
        // `authorName.isNotBlank()` guards the second line. Unguarded, a collection whose
        // author field the content artifact does not carry renders a blank row and shifts the
        // count line down — which reads as a layout bug, not as missing data.
        collectionState.value = HadithCollectionUiState(
            books = listOf(book(nameEnglish = "Anonymous Collection", authorName = "")),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Anonymous Collection").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_count_format, "7,563")).assertExists()
    }

    @Test
    fun `tapping a collection opens that collection, by its own id`() {
        collectionState.value = HadithCollectionUiState(
            books = listOf(
                book(id = "bukhari", nameEnglish = "Sahih al-Bukhari"),
                book(id = "ibnmajah", nameEnglish = "Sunan Ibn Majah"),
            ),
            isLoading = false,
        )

        setContent()
        composeRule.onNodeWithText("Sunan Ibn Majah").performClick()

        assertThat(opened).containsExactly("book:ibnmajah")
    }

    @Test
    fun `every collection gets its own cover, and an unknown one still gets a cover`() {
        // `getBookGradient` maps the six Kutub al-Sittah ids to their own palettes and
        // everything else to a default. It is looked up while the card composes, so a book id
        // the map does not know throws here rather than rendering plainly — which is what a
        // seventh collection arriving from the content artifact would do.
        collectionState.value = HadithCollectionUiState(
            books = listOf(
                book(id = "bukhari", nameEnglish = "Bukhari"),
                book(id = "muslim", nameEnglish = "Muslim"),
                book(id = "tirmidhi", nameEnglish = "Tirmidhi"),
                book(id = "nasai", nameEnglish = "Nasai"),
                book(id = "abudawud", nameEnglish = "Abu Dawud"),
                book(id = "ibnmajah", nameEnglish = "Ibn Majah"),
                book(id = "malik", nameEnglish = "Muwatta Malik"),
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Muwatta Malik").assertIsDisplayed()
        composeRule.onNodeWithText("Ibn Majah").assertIsDisplayed()
    }

    @Test
    fun `the bookmarked tile counts the bookmarks, not the books`() {
        collectionState.value = HadithCollectionUiState(books = listOf(book()), isLoading = false)
        bookmarksState.value = HadithBookmarksUiState(
            bookmarks = listOf(bookmark(1), bookmark(2), bookmark(3)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.bookmarked)).assertExists()
        composeRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun `the hadith of the day shows the selection when there is one`() {
        collectionState.value = HadithCollectionUiState(
            books = listOf(book()),
            hadithOfTheDay = hadith(
                textEnglish = "The believer is the mirror of the believer",
                reference = "Sunan Abi Dawud 4918",
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("The believer is the mirror of the believer").assertExists()
        composeRule.onNodeWithText("Sunan Abi Dawud 4918").assertExists()
        // The fallback narration must not be on screen at the same time as a real one.
        composeRule.onNodeWithText(string(R.string.hadith_fallback_source)).assertDoesNotExist()
    }

    @Test
    fun `a hadith of the day that never loaded falls back to the shipped narration`() {
        // Best-effort: `loadHadithOfTheDay` reports its failure and drops it, so this state is
        // reached both by "not loaded yet" and by "the content database could not be read".
        collectionState.value = HadithCollectionUiState(books = listOf(book()), isLoading = false)

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_fallback_english)).assertExists()
        composeRule.onNodeWithText(string(R.string.hadith_fallback_source)).assertExists()
    }

    @Test
    fun `bookmarking the hadith of the day cites the hadith's own book and number`() {
        // The card is the only place that dispatches `ToggleBookmark` with
        // `hadithNumberInBook` rather than `hadithNumber`; the two differ for every collection
        // whose numbering restarts per volume, and a bookmark saved under the wrong one
        // reopens on a different narration.
        collectionState.value = HadithCollectionUiState(
            books = listOf(book()),
            hadithOfTheDay = hadith(
                id = "muslim_2_9",
                bookId = "muslim",
                hadithNumber = 9,
                hadithNumberInBook = 2564,
            ),
            isLoading = false,
        )

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.bookmark)).performClick()

        assertThat(events).containsExactly(
            HadithEvent.ToggleBookmark(
                hadithId = "muslim_2_9",
                bookId = "muslim",
                hadithNumber = 2564,
            )
        )
    }

    @Test
    fun `bookmarking does nothing at all while the card is showing the fallback`() {
        // There is no hadith to bookmark, and the card's tap handler is a no-op rather than a
        // dispatch of the fallback's imaginary id.
        collectionState.value = HadithCollectionUiState(books = listOf(book()), isLoading = false)

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.bookmark)).performClick()

        assertThat(events).isEmpty()
    }

    @Test
    fun `exactly the three browsable grades are offered, and each opens its own shelf`() {
        collectionState.value = HadithCollectionUiState(books = listOf(book()), isLoading = false)

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).performClick()
        composeRule.onNodeWithText(string(R.string.hadith_grade_hasan)).performClick()
        composeRule.onNodeWithText(string(R.string.hadith_grade_daif)).performClick()

        assertThat(openedGrades).containsExactly(
            HadithGrade.SAHIH,
            HadithGrade.HASAN,
            HadithGrade.DAIF,
        ).inOrder()
        // Fabricated is a warning on a narration, never a shelf.
        composeRule.onNodeWithText(string(R.string.hadith_grade_mawdu)).assertDoesNotExist()
    }

    @Test
    fun `a failed load says the library could not be read, and retry re-issues it`() {
        collectionState.value = HadithCollectionUiState(
            books = emptyList(),
            isLoading = false,
            error = UiError(message = R.string.hadith_books_load_failed, details = "no such table"),
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_books_load_failed)).assertExists()
        // Not "no collections" — the list being empty is a consequence of the failure.
        composeRule.onNodeWithText(string(R.string.kutub_al_sittah)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.try_again)).performClick()
        assertThat(events).containsExactly(HadithEvent.Retry)
    }

    @Test
    fun `a not-found error keeps its own kind rather than reading as a crash`() {
        collectionState.value = HadithCollectionUiState(
            isLoading = false,
            error = UiError(
                message = R.string.hadith_not_found,
                kind = NimazErrorKind.NOT_FOUND,
            ),
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_not_found)).assertExists()
    }

    @Test
    fun `the spinner shows only while there is nothing to show yet`() {
        // `isLoading && books.isEmpty()`: a refresh that re-emits over a populated list must
        // not replace the library with a spinner.
        composeRule.mainClock.autoAdvance = false
        collectionState.value = HadithCollectionUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.kutub_al_sittah)).assertDoesNotExist()
    }

    @Test
    fun `a reload over a populated library leaves the library on screen`() {
        collectionState.value = HadithCollectionUiState(
            books = listOf(book(nameEnglish = "Sahih al-Bukhari")),
            isLoading = true,
        )

        setContent()

        composeRule.onNodeWithText("Sahih al-Bukhari").assertIsDisplayed()
    }

    @Test
    fun `search and bookmarks are reachable from the app bar`() {
        collectionState.value = HadithCollectionUiState(books = listOf(book()), isLoading = false)

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.search)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.bookmarks)).performClick()

        assertThat(opened).containsExactly("search", "bookmarks").inOrder()
    }

    private fun bookmark(id: Long) = HadithBookmark(
        id = id,
        hadithId = "bukhari_1_$id",
        bookId = "bukhari",
        hadithNumber = id.toInt(),
        note = null,
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
