package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranReciter
import com.arshadshah.nimaz.presentation.viewmodel.settings.QuranSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.ReciterPreviewUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reciter picker: search, a "currently selected" hero, and a card per voice.
 *
 * The preview button is the reason this file exists. It never made a sound before PR 21 of #551 —
 * the screen took a second `hiltViewModel<QuranViewModel>()`, which on this destination is a
 * *fresh* instance whose reader state is empty, so the playlist it built was empty and
 * `playFromAyah` returned without playing. The button set its spinner and played silence, and
 * nothing about that was visible from the screen. What is asserted here is that the button
 * dispatches `PreviewReciter` **for the reciter whose row it is in**, and that a second tap on a
 * playing row stops it rather than restarting it.
 *
 * The hero is the other half: it resolves through `QuranReciter.fromId`, which accepts aliases
 * from older builds, so a stored id the enum has since renamed still highlights the right voice
 * instead of silently falling back to the default.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class SelectReciterScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private val first = QuranReciter.entries.first()
    private val second = QuranReciter.entries[1]

    private fun setContent(
        state: QuranSettingsUiState = QuranSettingsUiState(),
        preview: ReciterPreviewUiState = ReciterPreviewUiState(),
    ) {
        viewModel.quranState.value = state
        viewModel.reciterPreview.value = preview
        composeRule.setThemedContent {
            SelectReciterScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the hero names the selected reciter and marks it active`() {
        setContent(QuranSettingsUiState(selectedReciterId = second.id))

        composeRule.onAllNodesWithText(second.displayName).onFirst().assertExists()
        composeRule.onNodeWithText(string(R.string.active)).assertExists()
        composeRule.onNodeWithText(string(R.string.select_reciter_currently_selected))
            .assertIsDisplayed()
    }

    @Test
    fun `a stored id the enum has since renamed still resolves to a real reciter`() {
        // `QuranReciter.fromId` accepts aliases from older builds. Without that, an install
        // that predates a rename silently reverts to the default voice on this screen.
        setContent(QuranSettingsUiState(selectedReciterId = "not-a-reciter"))

        composeRule.onAllNodesWithText(QuranReciter.fromId("not-a-reciter").displayName)
            .onFirst().assertExists()
    }

    @Test
    fun `every reciter is listed with its style and country`() {
        setContent()

        composeRule.onAllNodesWithText(first.displayName).onFirst().assertExists()
        composeRule.onAllNodesWithText(first.country).onFirst().assertExists()
        composeRule.onAllNodesWithText(recitationStyleLabelFor(first)).onFirst().assertExists()
    }

    @Test
    fun `searching narrows the list to matching reciters`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.select_reciter_search_hint))
            .performTextInput(second.displayName)

        val expected = QuranReciter.search(second.displayName)
        assertThat(expected).contains(second)
        composeRule.onAllNodesWithText(second.displayName).onFirst().assertExists()
    }

    @Test
    fun `tapping a reciter's card selects it`() {
        setContent(QuranSettingsUiState(selectedReciterId = first.id))

        composeRule.onAllNodesWithText(second.displayName).onFirst().performClick()

        assertThat(viewModel.only<SettingsEvent.SetReciter>().reciterId).isEqualTo(second.id)
    }

    @Test
    fun `the preview button auditions the reciter whose row it is in`() {
        // The bug this replaces played silence from a fresh ViewModel's empty playlist. The
        // assertion that matters is the id, not that a preview happened.
        setContent()

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_preview)).onFirst()
            .performClick()

        assertThat(viewModel.only<SettingsEvent.PreviewReciter>().reciterId).isEqualTo(first.id)
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetReciter>()).isEmpty()
    }

    @Test
    fun `tapping preview on the reciter that is already playing stops it`() {
        // Otherwise the only way to stop a running preview is to leave the screen.
        composeRule.mainClock.autoAdvance = false
        setContent(
            preview = ReciterPreviewUiState(reciterId = first.id, isPlaying = true)
        )

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_preview)).onFirst()
            .performClick()

        assertThat(viewModel.events).contains(SettingsEvent.StopReciterPreview)
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.PreviewReciter>()).isEmpty()
    }

    @Test
    fun `a preview that is still buffering is not treated as playing`() {
        // `isPlaying` and `isDownloading` are separate for this reason: tapping again while a
        // sample is still arriving must start it, not stop something that never began.
        composeRule.mainClock.autoAdvance = false
        setContent(
            preview = ReciterPreviewUiState(
                reciterId = first.id,
                isPlaying = false,
                isDownloading = true,
            )
        )

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_preview)).onFirst()
            .performClick()

        assertThat(viewModel.events.filterIsInstance<SettingsEvent.PreviewReciter>()).isNotEmpty()
    }

    @Test
    fun `a preview running on another reciter does not make this row a stop button`() {
        // `preview.reciterId == reciter.id` rather than `preview.isPlaying` alone — otherwise
        // every row on the screen becomes a stop button the moment any one of them plays.
        composeRule.mainClock.autoAdvance = false
        setContent(
            preview = ReciterPreviewUiState(reciterId = second.id, isPlaying = true)
        )

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_preview)).onFirst()
            .performClick()

        assertThat(viewModel.only<SettingsEvent.PreviewReciter>().reciterId).isEqualTo(first.id)
    }

    @Test
    fun `the title is Select Reciter and the back button navigates back`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.select_reciter_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    private fun recitationStyleLabelFor(reciter: QuranReciter): String = context.getString(
        when (reciter.style) {
            com.arshadshah.nimaz.domain.model.RecitationStyle.MURATTAL ->
                R.string.recitation_style_murattal
            com.arshadshah.nimaz.domain.model.RecitationStyle.MUJAWWAD ->
                R.string.recitation_style_mujawwad
        }
    )
}
