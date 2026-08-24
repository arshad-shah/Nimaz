package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.model.SurahOverviewGroup
import com.arshadshah.nimaz.domain.model.SurahOverviewSection
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahBackgroundState
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahPassagesState
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahSubjectsState
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicViewModel
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
 * The three screens that answer "what is this surah about": its background, its passage outline,
 * and the subjects it is cited under.
 *
 * All three share one distinction that is easy to collapse and expensive to get wrong:
 * **"this install has no thematic content" is not "this surah has none".** The migration that
 * creates the thematic tables ships before the artifact that fills them, so for a window every
 * surah looks like it has nothing to say — and "Nothing indexed here" is a claim about the
 * corpus, not about the download.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class SurahThematicScreensTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val backgroundState = MutableStateFlow(SurahBackgroundState())
    private val passagesState = MutableStateFlow(SurahPassagesState())
    private val subjectsState = MutableStateFlow(SurahSubjectsState())
    private val thematicEvents = mutableListOf<SurahThematicEvent>()
    private val topicEvents = mutableListOf<QuranTopicsEvent>()

    private val thematicViewModel: SurahThematicViewModel = mockk(relaxed = true) {
        every { backgroundState } returns this@SurahThematicScreensTest.backgroundState
        every { passagesState } returns this@SurahThematicScreensTest.passagesState
        every { onEvent(any()) } answers { thematicEvents += firstArg<SurahThematicEvent>() }
    }

    private val topicsViewModel: QuranTopicsViewModel = mockk(relaxed = true) {
        every { surahSubjects } returns subjectsState
        every { onEvent(any()) } answers { topicEvents += firstArg<QuranTopicsEvent>() }
    }

    private val cave = Surah(
        number = 18,
        nameArabic = "الكهف",
        nameEnglish = "The Cave",
        nameTransliteration = "Al-Kahf",
        revelationType = RevelationType.MECCAN,
        ayahCount = 110,
        orderInMushaf = 18,
        startPage = 293,
    )

    // ---- Background ----

    private fun renderBackground(onOpenAyah: (Int, Int) -> Unit = { _, _ -> }) {
        composeRule.setThemedContent {
            SurahBackgroundScreen(
                surahNumber = 18,
                onNavigateBack = {},
                onOpenAyah = onOpenAyah,
                onOpenTopic = {},
                viewModel = thematicViewModel,
            )
        }
    }

    @Test
    fun `arriving asks for this surah's background`() {
        renderBackground()

        assertThat(thematicEvents).contains(SurahThematicEvent.Load(18))
    }

    @Test
    fun `the background's sections are shown under their own headings`() {
        backgroundState.value = SurahBackgroundState(
            isLoading = false,
            surah = cave,
            overview = SurahOverview(
                surahNumber = 18,
                summary = "A Meccan surah about trial and shelter.",
                sections = listOf(
                    SurahOverviewSection(
                        position = 1,
                        heading = "Why it is called The Cave",
                        group = SurahOverviewGroup.NAME,
                        body = "Named for the sleepers who took shelter in it.",
                    ),
                ),
            ),
        )

        renderBackground()

        composeRule.onNodeWithText("Why it is called The Cave").assertIsDisplayed()
        composeRule.onNodeWithText("Named for the sleepers", substring = true).assertIsDisplayed()
    }

    @Test
    fun `an install without the thematic layer is told so, not shown an empty page`() {
        backgroundState.value = SurahBackgroundState(isLoading = false, surah = cave, overview = null)

        renderBackground()

        composeRule.onNodeWithText(str(R.string.quran_topics_unavailable_title)).assertIsDisplayed()
    }

    @Test
    fun `a failed background load offers to try again`() {
        backgroundState.value = SurahBackgroundState(
            isLoading = false,
            surah = cave,
            error = UiError(message = R.string.surah_thematic_load_failed),
        )
        renderBackground()

        composeRule.onNodeWithText(str(R.string.try_again)).performClick()

        // Two loads: the one on arrival and the retry.
        assertThat(thematicEvents.count { it == SurahThematicEvent.Load(18) }).isEqualTo(2)
    }

    // ---- Passages ----

    private fun renderPassages(currentAyah: Int? = null, onOpenAyah: (Int, Int) -> Unit = { _, _ -> }) {
        composeRule.setThemedContent {
            SurahPassagesScreen(
                surahNumber = 18,
                currentAyah = currentAyah,
                onNavigateBack = {},
                onOpenAyah = onOpenAyah,
                viewModel = thematicViewModel,
            )
        }
    }

    private val passages = listOf(
        AyahTheme(surahNumber = 18, ayahFrom = 1, ayahTo = 8, theme = "The purpose of the Book", ayahCount = 8),
        AyahTheme(surahNumber = 18, ayahFrom = 9, ayahTo = 26, theme = "The People of the Cave", ayahCount = 18),
    )

    @Test
    fun `the passage outline is listed`() {
        passagesState.value = SurahPassagesState(isLoading = false, surah = cave, passages = passages)

        renderPassages()

        // Found by description, not by text: `NimazRangeRow` clears its subtree's semantics and
        // publishes one label, so a screen reader hears "Open the passage …" rather than four
        // fragments — and a test that looked for the fragments would be asserting the opposite
        // of what the row promises.
        composeRule.onNodeWithContentDescription(passageLabel("The People of the Cave"))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(passageLabel("The purpose of the Book"))
            .assertIsDisplayed()
    }

    @Test
    fun `opening a passage opens the verse it starts on`() {
        passagesState.value = SurahPassagesState(isLoading = false, surah = cave, passages = passages)
        var opened: Pair<Int, Int>? = null
        renderPassages(onOpenAyah = { s, a -> opened = s to a })

        composeRule.onNodeWithContentDescription(passageLabel("The People of the Cave"))
            .performClick()

        assertThat(opened).isEqualTo(18 to 9)
    }

    @Test
    fun `arriving mid-surah still lists every passage, not only the one being read`() {
        passagesState.value = SurahPassagesState(isLoading = false, surah = cave, passages = passages)

        renderPassages(currentAyah = 12)

        // The reader's own passage is marked and scrolled to; the outline is still an outline.
        composeRule.onNodeWithContentDescription(passageLabel("The People of the Cave"))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(passageLabel("The purpose of the Book"))
            .assertIsDisplayed()
    }

    @Test
    fun `filtering by a verse number finds the passage that contains it`() {
        // A table of contents, and the thing a reader most often has in hand is a verse number:
        // "12" should find "The People of the Cave" without their knowing where it starts.
        passagesState.value = SurahPassagesState(
            isLoading = false,
            surah = cave,
            passages = passages,
            query = "12",
        )

        renderPassages()

        composeRule.onNodeWithContentDescription(passageLabel("The People of the Cave"))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(passageLabel("The purpose of the Book"))
            .assertDoesNotExist()
    }

    private fun passageLabel(theme: String) = str(R.string.cd_passage_open, theme)

    @Test
    fun `typing a filter hands it to the view model`() {
        passagesState.value = SurahPassagesState(isLoading = false, surah = cave, passages = passages)
        renderPassages()

        composeRule.onNodeWithText(str(R.string.surah_passages_filter_hint))
            .performTextInput("cave")

        assertThat(thematicEvents.filterIsInstance<SurahThematicEvent.Filter>().map { it.query })
            .contains("cave")
    }

    @Test
    fun `a filter matching nothing says so`() {
        passagesState.value = SurahPassagesState(
            isLoading = false,
            surah = cave,
            passages = passages,
            query = "zzz",
        )

        renderPassages()

        composeRule.onNodeWithText(str(R.string.surah_passages_none_title)).assertIsDisplayed()
    }

    // ---- Subjects ----

    private fun renderSubjects(onBrowseAll: () -> Unit = {}) {
        composeRule.setThemedContent {
            SurahSubjectsScreen(
                surahNumber = 18,
                onNavigateBack = {},
                onOpenTopic = { _, _ -> },
                onBrowseAllSubjects = onBrowseAll,
                viewModel = topicsViewModel,
            )
        }
    }

    private fun subject(id: Int, name: String, verses: Int) = SurahTopic(
        topic = QuranTopic(
            id = id,
            name = name,
            arabicName = "موضوع",
            description = "",
            wikiLink = "",
            ayahCount = verses,
            parentId = null,
            thematicParentId = null,
            ontologyParentId = null,
            isThematic = true,
            isOntology = false,
            relatedTopicIds = emptyList(),
        ),
        versesInSurah = verses,
    )

    @Test
    fun `arriving asks for this surah's subjects`() {
        renderSubjects()

        assertThat(topicEvents).contains(QuranTopicsEvent.LoadSurahSubjects(18))
    }

    @Test
    fun `the surah's subjects are listed`() {
        subjectsState.value = SurahSubjectsState(
            isLoading = false,
            surahNumber = 18,
            surahName = "The Cave",
            subjects = listOf(subject(1, "Trial", 20), subject(2, "Shelter", 6)),
        )

        renderSubjects()

        composeRule.onNodeWithText("Trial").assertIsDisplayed()
        composeRule.onNodeWithText("Shelter").assertIsDisplayed()
    }

    @Test
    fun `a surah the index does not cite is told the index is still there`() {
        subjectsState.value = SurahSubjectsState(
            isLoading = false,
            surahNumber = 18,
            subjects = emptyList(),
            isAvailable = true,
        )
        var browsedAll = false

        renderSubjects(onBrowseAll = { browsedAll = true })
        composeRule.onNodeWithText(str(R.string.surah_subjects_none_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.surah_subjects_browse_all)).performClick()

        assertThat(browsedAll).isTrue()
    }

    @Test
    fun `an install without the index is told that instead, and offered nothing to browse`() {
        subjectsState.value = SurahSubjectsState(
            isLoading = false,
            surahNumber = 18,
            subjects = emptyList(),
            isAvailable = false,
        )

        renderSubjects()

        // Offering "All subjects" here would open onto the same emptiness.
        composeRule.onNodeWithText(str(R.string.quran_topics_unavailable_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.surah_subjects_browse_all)).assertDoesNotExist()
    }

    @Test
    fun `opening a subject is the caller's business`() {
        subjectsState.value = SurahSubjectsState(
            isLoading = false,
            surahNumber = 18,
            subjects = listOf(subject(1, "Trial", 20)),
        )
        var opened: Pair<Int, TopicTree>? = null
        composeRule.setThemedContent {
            SurahSubjectsScreen(
                surahNumber = 18,
                onNavigateBack = {},
                onOpenTopic = { id, tree -> opened = id to tree },
                onBrowseAllSubjects = {},
                viewModel = topicsViewModel,
            )
        }

        composeRule.onNodeWithText("Trial").performClick()

        assertThat(opened?.first).isEqualTo(1)
    }
}
