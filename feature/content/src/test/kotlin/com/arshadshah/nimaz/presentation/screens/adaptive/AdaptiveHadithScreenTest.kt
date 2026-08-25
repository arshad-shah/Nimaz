package com.arshadshah.nimaz.presentation.screens.adaptive

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithBookmarksUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithChaptersUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.HadithCollectionUiState
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

/**
 * The hadith library on a phone and on a tablet — the same three screens, wired two ways.
 *
 * On a phone, opening a collection is a **route push**; on a tablet the identical tap must not
 * navigate at all, because the chapters belong in the scaffold's detail pane beside the list
 * that is still on screen. Get it backwards and a tablet stacks a full-screen chapter list over
 * a layout built to show both, while a phone's rows do nothing. Neither failure crashes, and
 * `HadithCollectionScreen`'s own tests cannot see either: the lambda they assert on is the one
 * this file chooses.
 *
 * The detail pane also carries a **second** decision — chapters or reader, from whether the
 * pane's args have a chapter — so one pane renders two different screens, and the argument that
 * decides is built by the list pane two taps earlier.
 *
 * The screens inside resolve their ViewModel through `hiltViewModel()`, and none of them is
 * handed one from here. Per #604's playbook item 8 that does not need Hilt: an owner whose
 * `ViewModelStore` already holds the mock answers before any factory is consulted, which is
 * what [seededOwner] is for.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveHadithScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val collectionState = MutableStateFlow(
        HadithCollectionUiState(
            books = listOf(bukhari, muslim),
            isLoading = false,
        )
    )
    private val chaptersState = MutableStateFlow(
        HadithChaptersUiState(
            book = bukhari,
            chapters = listOf(revelation),
            isLoading = false,
        )
    )
    private val readerState = MutableStateFlow(
        HadithReaderUiState(
            chapter = revelation,
            hadiths = listOf(firstHadith),
            isLoading = false,
        )
    )
    private val events = mutableListOf<HadithEvent>()

    private val viewModel: HadithViewModel = mockk(relaxed = true) {
        every { this@mockk.collectionState } returns this@AdaptiveHadithScreenTest.collectionState
        every { this@mockk.chaptersState } returns this@AdaptiveHadithScreenTest.chaptersState
        every { this@mockk.readerState } returns this@AdaptiveHadithScreenTest.readerState
        every { bookmarksState } returns MutableStateFlow(HadithBookmarksUiState(isLoading = false))
        every { onEvent(any()) } answers { events += firstArg<HadithEvent>() }
        every { isHadithBookmarked(any()) } returns flowOf(false)
    }

    private val navigated = mutableListOf<Route>()
    private val openedGrades = mutableListOf<HadithGrade>()

    private fun setContent() {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides seededOwner()) {
                AdaptiveHadithScreen(
                    onNavigate = { navigated += it },
                    onNavigateBack = {},
                    onNavigateToSearch = {},
                    onNavigateToBookmarks = {},
                    onNavigateToGrade = { openedGrades += it },
                )
            }
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone the collection is the whole screen`() {
        setContent()

        composeRule.onNodeWithText("Sahih al-Bukhari").assertExists()
        composeRule.onNodeWithText(string(R.string.kutub_al_sittah)).assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone opening a collection pushes its own destination`() {
        setContent()

        composeRule.onNodeWithText("Sahih Muslim").performClick()

        assertThat(navigated).containsExactly(Route.HadithBook("muslim"))
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet opening a collection moves the detail pane instead of navigating`() {
        // The list stays on screen; a push here would cover it.
        setContent()

        composeRule.onNodeWithText("Sahih al-Bukhari").performClick()
        composeRule.waitForIdle()

        assertThat(navigated).isEmpty()
        composeRule.onNodeWithText("Revelation").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet the detail pane shows the reader once a chapter is chosen`() {
        // Two taps, and the second is what puts a chapter id into the pane's args — the only
        // thing separating "show the chapters" from "show the hadith".
        setContent()

        composeRule.onNodeWithText("Sahih al-Bukhari").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Revelation").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Actions are but by intention").assertExists()
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet the detail pane is empty until something is chosen`() {
        setContent()

        composeRule.onNodeWithText("Actions are but by intention").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `browsing a grade is handed straight out on either size`() {
        // Grade browsing is not a pane: it replaces the reader's whole list, so it navigates
        // from both branches rather than opening beside the collection.
        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).performClick()

        assertThat(openedGrades).containsExactly(HadithGrade.SAHIH)
    }

    private fun seededOwner(): ViewModelStoreOwner {
        val store = ViewModelStore()
        seed(store, HadithViewModel::class.java, viewModel)
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    private fun <VM : ViewModel> seed(store: ViewModelStore, type: Class<VM>, instance: VM) {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                instance as T
        }
        ViewModelProvider.create(store, factory)[type]
    }

    private companion object {
        val bukhari = HadithBook(
            id = "bukhari",
            nameArabic = "صحيح البخاري",
            nameEnglish = "Sahih al-Bukhari",
            authorName = "Imam al-Bukhari",
            authorArabic = "البخاري",
            totalHadiths = 7563,
            totalChapters = 97,
            description = null,
            displayOrder = 1,
        )
        val muslim = bukhari.copy(
            id = "muslim",
            nameEnglish = "Sahih Muslim",
            authorName = "Imam Muslim",
        )
        val revelation = HadithChapter(
            id = "bukhari_1",
            bookId = "bukhari",
            chapterNumber = 1,
            nameArabic = "بدء الوحي",
            nameEnglish = "Revelation",
            hadithCount = 7,
            hadithStartNumber = 1,
            hadithEndNumber = 7,
        )
        val firstHadith = Hadith(
            id = "bukhari_1_1",
            bookId = "bukhari",
            chapterId = "1",
            hadithNumber = 1,
            hadithNumberInBook = 1,
            textArabic = "إنما الأعمال بالنيات",
            textEnglish = "Actions are but by intention",
            narratorChain = null,
            narratorName = "Umar ibn al-Khattab",
            grade = HadithGrade.SAHIH,
            gradeArabic = null,
            reference = "Sahih al-Bukhari 1",
        )
    }
}
