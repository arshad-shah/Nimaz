package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamDetailUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamFormMode
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamFormUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamListUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamViewModel
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
 * The three khatam screens: the list, one khatam's detail, and the form that makes or edits one.
 *
 * A khatam exists to assign a **daily portion**, and the detail screen used to lead with
 * "resume" — which answers a question about the past. What the plan asks for today is the thing
 * to look for here.
 *
 * The form's two destructive actions carry the other risk. Archive and delete are one row apart
 * in the same overflow menu, both take a khatam out of the list, and only one of them is
 * recoverable — so each has to confirm, and neither may fire from the menu itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class KhatamScreensTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val listState = MutableStateFlow(KhatamListUiState())
    private val detailState = MutableStateFlow(KhatamDetailUiState())
    private val formState = MutableStateFlow(KhatamFormUiState())
    private val events = mutableListOf<KhatamEvent>()

    private val viewModel: KhatamViewModel = mockk(relaxed = true) {
        every { listState } returns this@KhatamScreensTest.listState
        every { detailState } returns this@KhatamScreensTest.detailState
        every { formState } returns this@KhatamScreensTest.formState
        every { onEvent(any()) } answers { events += firstArg<KhatamEvent>() }
    }

    private fun khatam(
        id: Long = 1,
        name: String = "Ramadan",
        status: KhatamStatus = KhatamStatus.ACTIVE,
        read: Int = 0,
        active: Boolean = false,
    ) = Khatam(
        id = id,
        name = name,
        status = status,
        isActive = active,
        dailyTarget = 20,
        totalAyahsRead = read,
        createdAt = 0,
        updatedAt = 0,
    )

    // ---- The list ----

    private fun renderList(
        onDetail: (Long) -> Unit = {},
        onCreate: () -> Unit = {},
        onRead: (Int, Int) -> Unit = { _, _ -> },
    ) {
        composeRule.setThemedContent {
            KhatamListScreen(
                onNavigateBack = {},
                onNavigateToDetail = onDetail,
                onNavigateToCreate = onCreate,
                onNavigateToRead = onRead,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `a reader with no khatam is invited to start one`() {
        listState.value = KhatamListUiState(isLoading = false)
        var created = false

        renderList(onCreate = { created = true })
        composeRule.onNodeWithText(str(R.string.khatam_no_started)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.khatam_start_journey)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.khatam_start_new)).performClick()

        assertThat(created).isTrue()
    }

    @Test
    fun `khatams in progress are listed`() {
        listState.value = KhatamListUiState(
            isLoading = false,
            inProgressKhatams = listOf(khatam(1, "Ramadan"), khatam(2, "Second read")),
        )

        renderList()

        composeRule.onNodeWithText("Ramadan").assertIsDisplayed()
        composeRule.onNodeWithText("Second read").assertIsDisplayed()
    }

    @Test
    fun `opening a khatam is the caller's business`() {
        listState.value = KhatamListUiState(
            isLoading = false,
            inProgressKhatams = listOf(khatam(7, "Ramadan")),
        )
        var opened: Long? = null
        renderList(onDetail = { opened = it })

        composeRule.onNodeWithText("Ramadan").performClick()

        assertThat(opened).isEqualTo(7)
    }

    @Test
    fun `finished and archived khatams live under their own tabs`() {
        listState.value = KhatamListUiState(
            isLoading = false,
            inProgressKhatams = listOf(khatam(1, "Ramadan")),
            completedKhatams = listOf(khatam(2, "Last year", status = KhatamStatus.COMPLETED)),
            abandonedKhatams = listOf(khatam(3, "Abandoned", status = KhatamStatus.ABANDONED)),
        )

        renderList()
        // A finished khatam is not in progress, and the reader should not have to read past it.
        composeRule.onNodeWithText("Last year").assertDoesNotExist()

        composeRule.onNodeWithText(str(R.string.khatam_section_completed)).performClick()
        composeRule.onNodeWithText("Last year").assertIsDisplayed()

        composeRule.onNodeWithText(str(R.string.khatam_section_archived)).performClick()
        composeRule.onNodeWithText("Abandoned").assertIsDisplayed()
    }

    @Test
    fun `the hero continues from where the plan says, not from the top`() {
        listState.value = KhatamListUiState(
            isLoading = false,
            activeKhatam = khatam(1, "Ramadan", read = 1000, active = true),
            activeInsights = KhatamInsights(daysActive = 5, currentStreak = 3),
            inProgressKhatams = listOf(khatam(1, "Ramadan", read = 1000, active = true)),
            nextUnreadSurah = 18,
            nextUnreadAyah = 10,
            nextUnreadSurahName = "The Cave",
        )
        var read: Pair<Int, Int>? = null
        renderList(onRead = { s, a -> read = s to a })

        composeRule.onNodeWithText(str(R.string.khatam_continue_at, "The Cave", 10)).performClick()

        assertThat(read).isEqualTo(18 to 10)
    }

    @Test
    fun `a first load shows neither the list nor the invitation`() {
        listState.value = KhatamListUiState(isLoading = true)

        renderList()

        composeRule.onNodeWithText(str(R.string.khatam_no_started)).assertDoesNotExist()
    }

    // ---- The detail ----

    private fun renderDetail(
        khatamId: Long = 1,
        onBack: () -> Unit = {},
        onRead: (Int, Int) -> Unit = { _, _ -> },
        onEdit: (Long) -> Unit = {},
    ) {
        composeRule.setThemedContent {
            KhatamDetailScreen(
                khatamId = khatamId,
                onNavigateBack = onBack,
                onNavigateToRead = onRead,
                onNavigateToEdit = onEdit,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `arriving asks for this khatam`() {
        renderDetail(khatamId = 7)

        assertThat(events).contains(KhatamEvent.LoadKhatamDetail(7))
    }

    @Test
    fun `a khatam that has been deleted pops rather than spinning`() {
        detailState.value = KhatamDetailUiState(isLoading = false, khatam = null, notFound = true)
        var back = false

        renderDetail(onBack = { back = true })

        assertThat(back).isTrue()
    }

    @Test
    fun `the detail leads with what the plan asks for today`() {
        detailState.value = KhatamDetailUiState(
            isLoading = false,
            khatam = khatam(1, "Ramadan", read = 1000),
            insights = KhatamInsights(daysActive = 5, currentStreak = 3, juzCompleted = 4),
            nextUnreadSurah = 18,
            nextUnreadAyah = 10,
            nextUnreadSurahName = "The Cave",
            todaysPortionLabel = "Al-Kahf 18:1 → An-Nur 24:12",
        )

        renderDetail()

        composeRule.onNodeWithText("Al-Kahf 18:1 → An-Nur 24:12").assertIsDisplayed()
    }

    @Test
    fun `the reader's streak and juz are on the detail`() {
        detailState.value = KhatamDetailUiState(
            isLoading = false,
            khatam = khatam(1, "Ramadan", read = 1000),
            insights = KhatamInsights(daysActive = 5, currentStreak = 3, juzCompleted = 4),
        )

        renderDetail()

        composeRule.onNodeWithText(str(R.string.khatam_stat_streak)).assertIsDisplayed()
    }

    @Test
    fun `editing a khatam is the caller's business`() {
        detailState.value = KhatamDetailUiState(
            isLoading = false,
            khatam = khatam(7, "Ramadan"),
        )
        var edited: Long? = null
        renderDetail(khatamId = 7, onEdit = { edited = it })

        composeRule.onNodeWithContentDescription(str(R.string.khatam_edit)).performClick()

        assertThat(edited).isEqualTo(7)
    }

    @Test
    fun `a khatam that is not the active one can be made active`() {
        detailState.value = KhatamDetailUiState(
            isLoading = false,
            khatam = khatam(7, "Ramadan", active = false),
        )
        renderDetail(khatamId = 7)

        composeRule.onNodeWithContentDescription(str(R.string.khatam_more_actions)).performClick()
        composeRule.onNodeWithText(str(R.string.khatam_set_active)).performClick()

        assertThat(events).contains(KhatamEvent.SetActiveKhatam(7))
    }

    // ---- The form ----

    private fun renderForm(khatamId: Long? = null, onBack: () -> Unit = {}) {
        composeRule.setThemedContent {
            KhatamFormScreen(
                khatamId = khatamId,
                onNavigateBack = onBack,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `arriving with no id starts a blank form`() {
        renderForm(khatamId = null)

        assertThat(events.filterIsInstance<KhatamEvent.StartCreate>()).isNotEmpty()
    }

    @Test
    fun `arriving with an id loads that khatam into the form`() {
        renderForm(khatamId = 7)

        assertThat(events).contains(KhatamEvent.StartEdit(7))
    }

    @Test
    fun `a new khatam's form says it is new`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Create)

        renderForm()

        composeRule.onNodeWithText(str(R.string.khatam_new)).assertIsDisplayed()
    }

    @Test
    fun `an existing khatam's form says it is an edit`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Edit(7), name = "Ramadan")

        renderForm(khatamId = 7)

        composeRule.onNodeWithText(str(R.string.khatam_edit_title)).assertIsDisplayed()
    }

    @Test
    fun `naming the khatam hands the name up`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Create)
        renderForm()

        // The name field is the first thing on the form that takes text.
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Ramadan")

        assertThat(events.filterIsInstance<KhatamEvent.UpdateName>().map { it.name })
            .contains("Ramadan")
    }

    @Test
    fun `a validation failure is shown rather than swallowed`() {
        formState.value = KhatamFormUiState(
            mode = KhatamFormMode.Create,
            errorRes = R.string.khatam_error_name_required,
        )

        renderForm()

        composeRule.onNodeWithText(str(R.string.khatam_error_name_required)).assertIsDisplayed()
    }

    @Test
    fun `a saved khatam leaves the form, once`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Create, saveComplete = true)
        var back = false

        renderForm(onBack = { back = true })

        // The flag is consumed so a recomposition cannot navigate a second time.
        assertThat(back).isTrue()
        assertThat(events).contains(KhatamEvent.ConsumeSaveComplete)
    }

    @Test
    fun `archiving asks first, and only then archives`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Edit(7), name = "Ramadan")
        renderForm(khatamId = 7)

        composeRule.onNodeWithContentDescription(str(R.string.khatam_more_actions)).performClick()
        composeRule.onNodeWithText(str(R.string.khatam_action_archive)).performClick()

        // Opening the menu row must not be the act itself: it is one row above "Delete
        // permanently" and both take the khatam out of the list.
        assertThat(events.filterIsInstance<KhatamEvent.AbandonKhatam>()).isEmpty()
        composeRule.onNodeWithText(str(R.string.khatam_archive_title)).assertIsDisplayed()
    }

    @Test
    fun `deleting asks first, and only then deletes`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Edit(7), name = "Ramadan")
        renderForm(khatamId = 7)

        composeRule.onNodeWithContentDescription(str(R.string.khatam_more_actions)).performClick()
        composeRule.onNodeWithText(str(R.string.khatam_action_delete)).performClick()

        assertThat(events.filterIsInstance<KhatamEvent.DeleteKhatam>()).isEmpty()
        composeRule.onNodeWithText(str(R.string.khatam_delete_title)).assertIsDisplayed()

        composeRule.onNodeWithText(str(R.string.delete)).performClick()
        assertThat(events).contains(KhatamEvent.DeleteKhatam(7))
    }

    @Test
    fun `a new khatam is offered no destructive actions at all`() {
        formState.value = KhatamFormUiState(mode = KhatamFormMode.Create)

        renderForm()

        composeRule.onNodeWithContentDescription(str(R.string.khatam_more_actions))
            .assertDoesNotExist()
    }

    @Test
    fun `editing shows what has already been read, so the reader can see it is untouched`() {
        formState.value = KhatamFormUiState(
            mode = KhatamFormMode.Edit(7),
            name = "Ramadan",
            totalAyahsRead = 1000,
        )

        renderForm(khatamId = 7)

        composeRule.onNodeWithText("1000", substring = true).assertIsDisplayed()
    }
}
