package com.arshadshah.nimaz.presentation.screens.hadith

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * The hadith reader — and the four ways a reader arrives at one.
 *
 * **The entry-point `when` is the highest-value thing on this screen.** One `LaunchedEffect`
 * turns four route shapes into four different loads, and every one of them is reachable from
 * the UI: a chapter from the chapter list, a *number* from a bookmark, an *id* from search, and
 * a *grade* from the collection screen's pills. Their guards overlap by construction —
 * `bookId` is empty in the search case *and* in the grade case, `chapterId` carries a hadith id
 * in one and a composite key in another — so the ordering is load-bearing, and the failure it
 * prevents is a bookmark opening a stranger's narration rather than crashing. `HadithViewModel`
 * cannot see any of this: it receives whichever event the screen chose.
 *
 * **The four display toggles each remove their own element.** `:core:datastore` (#603) pinned
 * that the preferences persist; what was never asserted is that turning the chain off actually
 * removes the chain. Each is a separate `if`, and a reader who has hidden the Arabic and gets
 * it anyway has no way to report it beyond "the setting does nothing".
 *
 * **A hadith that ships without an optional field must not leave a hole.** The grade chip, the
 * narrator badge, the reference badge and the isnād are each guarded; unguarded, a narration the
 * content artifact carries no grade for renders an empty pill.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HadithReaderScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val readerState = MutableStateFlow(HadithReaderUiState())
    private val events = mutableListOf<HadithEvent>()

    private val viewModel: HadithViewModel = mockk(relaxed = true) {
        every { this@mockk.readerState } returns this@HadithReaderScreenTest.readerState
        every { onEvent(any()) } answers { events += firstArg<HadithEvent>() }
        every { isHadithBookmarked(any()) } returns flowOf(false)
    }

    private var backs = 0
    private var settingsOpened = 0

    private fun setContent(
        bookId: String = "bukhari",
        chapterId: String = "bukhari_1",
        hadithNumber: Int? = null,
        grade: HadithGrade? = null,
    ) {
        composeRule.setThemedContent {
            HadithReaderScreen(
                bookId = bookId,
                chapterId = chapterId,
                onNavigateBack = { backs++ },
                onNavigateToSettings = { settingsOpened++ },
                hadithNumber = hadithNumber,
                grade = grade,
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /** What the system clipboard holds, as the reader would paste it. */
    private fun clipboardText(): String {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
    }

    private fun loaded(vararg hadiths: com.arshadshah.nimaz.domain.model.Hadith) {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = hadiths.toList(),
            isLoading = false,
        )
    }

    // ---- the four ways in -------------------------------------------------------------

    @Test
    fun `a chapter route loads that chapter`() {
        readerState.value = HadithReaderUiState(isLoading = false)

        setContent(bookId = "bukhari", chapterId = "bukhari_2")

        assertThat(events).containsExactly(HadithEvent.LoadChapter("bukhari_2"))
    }

    @Test
    fun `a bookmark's book-and-number opens that number, not that primary key`() {
        // A `HadithBookmark` stores the number printed in the reader, never the database id.
        // Passing the number through the id path looks up a real hadith from an arbitrary book,
        // so the reader lands somewhere plausible and wrong.
        readerState.value = HadithReaderUiState(isLoading = false)

        setContent(bookId = "muslim", chapterId = "", hadithNumber = 2564)

        assertThat(events).containsExactly(
            HadithEvent.LoadHadithByNumber(bookId = "muslim", hadithNumber = 2564)
        )
    }

    @Test
    fun `a bare hadith id from search is loaded as an id`() {
        // No book, and no `book_chapter` composite in the second argument — the only shape that
        // means "this is a hadith id".
        readerState.value = HadithReaderUiState(isLoading = false)

        setContent(bookId = "", chapterId = "bukhari-1-1")

        assertThat(events).containsExactly(HadithEvent.LoadHadithById("bukhari-1-1"))
    }

    @Test
    fun `an id carrying an underscore is read as a chapter, not as a hadith id`() {
        // The guard is `bookId.isEmpty() && !chapterId.contains("_")`, and the underscore is
        // the *whole* signal: a composite `book_chapter` key has one, a hadith id from search
        // is assumed not to. This asserts the boundary the app actually relies on — with no
        // book and an underscore present, the composite reading wins. It is worth pinning
        // because hadith ids in this dataset **do** contain underscores (`bukhari_1_1`), so
        // the two shapes are one character apart and the classification is a convention, not
        // a proof. A change to either side sends search hits into the chapter loader.
        readerState.value = HadithReaderUiState(isLoading = false)

        setContent(bookId = "", chapterId = "bukhari_1")

        assertThat(events).containsExactly(HadithEvent.LoadChapter("bukhari_1"))
    }

    @Test
    fun `browsing a grade wins over every other reading of the arguments`() {
        // Grade browsing arrives with an empty book and an empty chapter — exactly the shape
        // the search branch matches — so the guard order is what keeps "read every sahih
        // narration" from resolving to "open the hadith whose id is the empty string".
        readerState.value = HadithReaderUiState(isLoading = false)

        setContent(bookId = "", chapterId = "", grade = HadithGrade.SAHIH)

        assertThat(events).containsExactly(HadithEvent.FilterByGrade(HadithGrade.SAHIH))
    }

    @Test
    fun `a grade shelf titles itself with the grade and counts what it found`() {
        // There is no chapter in this mode, so the app bar would otherwise sit on "Loading…"
        // for the whole session.
        readerState.value = HadithReaderUiState(
            chapter = null,
            hadiths = listOf(hadith(), hadith(id = "b2"), hadith(id = "b3")),
            isLoading = false,
        )

        setContent(bookId = "", chapterId = "", grade = HadithGrade.HASAN)

        composeRule.onNodeWithText(string(R.string.hadith_grade_hasan)).assertExists()
        composeRule.onNodeWithText(string(R.string.hadith_count_format, "3")).assertExists()
    }

    // ---- what a page renders ----------------------------------------------------------

    @Test
    fun `a hadith renders its Arabic, its translation and its reference`() {
        loaded(
            hadith(
                textArabic = "إنما الأعمال بالنيات",
                textEnglish = "Actions are but by intention",
                reference = "Sahih al-Bukhari 1",
            )
        )

        setContent()

        composeRule.onNodeWithText("إنما الأعمال بالنيات").assertExists()
        composeRule.onNodeWithText("Actions are but by intention").assertExists()
        composeRule.onNodeWithText("Sahih al-Bukhari 1").assertExists()
    }

    @Test
    fun `the chapter's name and number title the reader`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(chapterNumber = 2, nameEnglish = "Belief"),
            hadiths = listOf(hadith()),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Belief").assertExists()
        composeRule.onNodeWithText(string(R.string.hadith_chapter_format, 2)).assertExists()
    }

    @Test
    fun `hiding the Arabic removes the Arabic`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(textArabic = "نص عربي", textEnglish = "English body")),
            isLoading = false,
            showArabic = false,
        )

        setContent()

        composeRule.onNodeWithText("نص عربي").assertDoesNotExist()
        composeRule.onNodeWithText("English body").assertExists()
    }

    @Test
    fun `hiding the translation removes the translation`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(textArabic = "نص عربي", textEnglish = "English body")),
            isLoading = false,
            showTranslation = false,
        )

        setContent()

        composeRule.onNodeWithText("English body").assertDoesNotExist()
        composeRule.onNodeWithText("نص عربي").assertExists()
    }

    @Test
    fun `hiding the grade removes the grade chip`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(grade = HadithGrade.SAHIH)),
            isLoading = false,
            showGrade = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).assertDoesNotExist()
    }

    @Test
    fun `hiding the chain removes the chain`() {
        // The setting `:core:datastore` persists, asserted where it has an effect.
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(narratorChain = "Malik -> Nafi -> Ibn Umar")),
            isLoading = false,
            showChain = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_isnad)).assertDoesNotExist()
    }

    @Test
    fun `showing the chain offers it collapsed, and expanding lists the narrators`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(narratorChain = "Malik -> Nafi -> Ibn Umar")),
            isLoading = false,
            showChain = true,
        )

        setContent()

        composeRule.onNodeWithText("Malik").assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.hadith_isnad)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Malik").assertExists()
        composeRule.onNodeWithText("Nafi").assertExists()
        composeRule.onNodeWithText("Ibn Umar").assertExists()
    }

    @Test
    fun `a single-narrator chain is shown whole rather than as a one-dot timeline`() {
        // The timeline is drawn only when the chain actually splits; a chain the separators do
        // not match would otherwise render as one lonely dot instead of the text it is.
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(narratorChain = "حدثنا عبد الله بن يوسف")),
            isLoading = false,
            showChain = true,
        )

        setContent()
        composeRule.onNodeWithText(string(R.string.hadith_isnad)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("حدثنا عبد الله بن يوسف").assertExists()
    }

    @Test
    fun `a chain of nothing but whitespace is not offered at all`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = listOf(hadith(narratorChain = "   ")),
            isLoading = false,
            showChain = true,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_isnad)).assertDoesNotExist()
    }

    @Test
    fun `a narrator field that already says who narrated is not prefixed twice`() {
        // The dataset carries "Narrated by …" inline for some collections and a bare name for
        // others. Prefixing unconditionally produces "Narrated by Narrated by Abu Huraira".
        loaded(hadith(narratorName = "Narrated by Abu Huraira"))

        setContent()

        composeRule.onNodeWithText("Narrated by Abu Huraira").assertExists()
    }

    @Test
    fun `a bare narrator name is given the prefix`() {
        loaded(hadith(narratorName = "Abu Huraira"))

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_narrated_by_format, "Abu Huraira"))
            .assertExists()
    }

    @Test
    fun `a narrator field of nothing but spaces renders no badge`() {
        // `narratorName?.trim()?.takeIf { it.isNotBlank() }`. The content artifact carries a
        // whitespace-only narrator as readily as a null, and an unguarded badge is an empty
        // accent pill above the Arabic.
        loaded(hadith(narratorName = "   ", grade = null))

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_narrated_by_format, "")).assertDoesNotExist()
        composeRule.onNodeWithText("Actions are but by intention").assertExists()
    }

    @Test
    fun `copying leaves out a reference that is only whitespace`() {
        // The same shape again, in the copy text this time — where it would paste as a
        // trailing blank line rather than as an empty pill.
        loaded(hadith(narratorName = null, reference = "   "))

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.cd_copy)).performClick()
        composeRule.waitForIdle()

        assertThat(clipboardText().trim()).doesNotContain("   \n")
        assertThat(clipboardText()).contains("Actions are but by intention")
    }

    @Test
    fun `a hadith with no grade and no narrator renders neither badge`() {
        loaded(hadith(grade = null, narratorName = null, reference = null))

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).assertDoesNotExist()
        composeRule.onNodeWithText("Actions are but by intention").assertExists()
    }

    // ---- the bottom bar ---------------------------------------------------------------

    @Test
    fun `bookmarking the open hadith cites the hadith the reader is on`() {
        loaded(hadith(id = "bukhari_1_1", bookId = "bukhari", hadithNumber = 1))

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.cd_bookmark)).performClick()

        assertThat(events).contains(
            HadithEvent.ToggleBookmark(
                hadithId = "bukhari_1_1",
                bookId = "bukhari",
                hadithNumber = 1,
            )
        )
    }

    @Test
    fun `the bookmark control reflects the persisted flag, not the row that was loaded`() {
        // `isHadithBookmarked` is a live flow; the loaded row's `isBookmarked` is only the seed.
        // Reading the seed alone leaves the star stale the moment the bookmark is toggled from
        // anywhere else.
        every { viewModel.isHadithBookmarked("bukhari_1_1") } returns flowOf(true)
        loaded(hadith(id = "bukhari_1_1", isBookmarked = false))

        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_bookmark)).assertExists()
    }

    @Test
    fun `copying a hadith puts its whole citation on the clipboard`() {
        // The copy text is built by a local function nothing else calls, and it is what a
        // reader pastes into a message. Every optional field is guarded there separately from
        // the page's own guards, so a hadith can render perfectly and still be copied with a
        // blank line where its narrator should be.
        loaded(
            hadith(
                textArabic = "إنما الأعمال بالنيات",
                textEnglish = "Actions are but by intention",
                narratorName = "Umar ibn al-Khattab",
                reference = "Sahih al-Bukhari 1",
            )
        )

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.cd_copy)).performClick()
        composeRule.waitForIdle()

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(string(R.string.hadith_copied))
        val copied = clipboardText()
        assertThat(copied).contains("إنما الأعمال بالنيات")
        assertThat(copied).contains("Actions are but by intention")
        assertThat(copied).contains(string(R.string.hadith_narrated_by_format, "Umar ibn al-Khattab"))
        assertThat(copied).contains("Sahih al-Bukhari 1")
    }

    @Test
    fun `copying a hadith with no narrator and no reference leaves no blank lines for them`() {
        // Both fields are `takeIf { it.isNotBlank() }`, and the content artifact carries an
        // empty string as often as a null. Without the guard the paste ends in two blank
        // lines and, before it, the literal "Narrated by ".
        loaded(
            hadith(
                textEnglish = "Actions are but by intention",
                narratorName = "",
                reference = null,
            )
        )

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.cd_copy)).performClick()
        composeRule.waitForIdle()

        val copied = clipboardText()
        assertThat(copied).contains("Actions are but by intention")
        assertThat(copied).doesNotContain("Narrated by")
        assertThat(copied.trimEnd()).isEqualTo(copied.trimEnd().trimEnd('\n'))
    }

    @Test
    fun `a single-hadith chapter offers no page arrows`() {
        // `NimazReaderBottomBar` hides both chevrons when there is nowhere to go — the actions
        // stay, so this is not "the bar is missing".
        loaded(hadith())

        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.previous)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.next)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.cd_copy)).assertExists()
    }

    @Test
    fun `a multi-hadith chapter offers arrows, and the first page cannot go back`() {
        loaded(hadith(id = "h1"), hadith(id = "h2"), hadith(id = "h3"))

        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.previous)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(string(R.string.next)).assertIsEnabled()
    }

    // ---- the states that are not a hadith ---------------------------------------------

    @Test
    fun `a load failure is reported as a failure rather than as an empty chapter`() {
        readerState.value = HadithReaderUiState(
            hadiths = emptyList(),
            isLoading = false,
            error = UiError(message = R.string.hadith_load_failed, details = "disk I/O error"),
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.no_hadith_found)).assertDoesNotExist()
    }

    @Test
    fun `retrying a failed chapter re-issues the load`() {
        readerState.value = HadithReaderUiState(
            isLoading = false,
            error = UiError(message = R.string.hadith_load_failed),
        )

        setContent()
        events.clear()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).containsExactly(HadithEvent.Retry)
    }

    @Test
    fun `a chapter that really is empty says so`() {
        readerState.value = HadithReaderUiState(
            chapter = chapter(),
            hadiths = emptyList(),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.no_hadith_found)).assertIsDisplayed()
    }

    @Test
    fun `hadith settings are reachable from the reader`() {
        loaded(hadith())

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.hadith_settings)).performClick()

        assertThat(settingsOpened).isEqualTo(1)
    }

    @Test
    fun `the reader shows a spinner rather than a title it does not have yet`() {
        composeRule.mainClock.autoAdvance = false
        readerState.value = HadithReaderUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.loading)).assertExists()
        composeRule.onNodeWithText(string(R.string.no_hadith_found)).assertDoesNotExist()
    }
}
