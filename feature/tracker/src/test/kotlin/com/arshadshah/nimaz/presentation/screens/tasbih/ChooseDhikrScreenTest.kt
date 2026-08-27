package com.arshadshah.nimaz.presentation.screens.tasbih

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihCounterUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihPresetsUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihViewModel
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
 * Choosing what to count.
 *
 * Two filters compose here — a tab and a search box — and they are `&&`-ed inside the screen
 * rather than derived in the ViewModel, so nothing outside this file has ever run them. The
 * failure they hide is silent: a list that quietly shows the wrong set still looks like a list
 * of adhkar, and the user's own custom dhikr simply appears to have vanished.
 *
 * Deleting is the other thing worth pinning. It is behind a confirmation because it is
 * irreversible, and the dialog's cancel arm — the one that must *not* delete — is exactly the
 * arm no manual pass ever exercises twice.
 *
 * Tall on purpose: the list is a `LazyColumn` and the tab strip a `LazyRow`, so at phone height
 * neither composes in full and an assertion about filtering would be an assertion about
 * viewport size instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ChooseDhikrScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun preset(
        id: Long,
        name: String,
        category: TasbihCategory?,
        translation: String? = null,
        target: Int = 33,
        arabic: String? = null,
    ) = TasbihPreset(
        id = id,
        name = name,
        arabicText = arabic,
        transliteration = null,
        translation = translation,
        targetCount = target,
        category = category,
        reference = null,
        isDefault = category != TasbihCategory.CUSTOM,
        displayOrder = 0,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val subhanAllah =
        preset(1, "SubhanAllah", TasbihCategory.AFTER_PRAYER, "Glory be", arabic = "سُبْحَانَ ٱللَّٰهِ")
    private val morningDua = preset(2, "Morning remembrance", TasbihCategory.MORNING)
    private val eveningDua = preset(3, "Evening remembrance", TasbihCategory.EVENING)
    private val mine = preset(9, "My own dhikr", TasbihCategory.CUSTOM, target = 7)

    private val presetsState = MutableStateFlow(
        TasbihPresetsUiState(
            defaultPresets = listOf(subhanAllah, morningDua, eveningDua),
            customPresets = listOf(mine),
            favorites = setOf(2L),
            isLoading = false,
        )
    )
    private val counterState = MutableStateFlow(TasbihCounterUiState())
    private val events = mutableListOf<TasbihEvent>()
    private var backs = 0
    private var addPresetTaps = 0
    private val edits = mutableListOf<Long>()

    private val viewModel: TasbihViewModel = mockk(relaxed = true) {
        every { this@mockk.presetsState } returns this@ChooseDhikrScreenTest.presetsState
        every { this@mockk.counterState } returns this@ChooseDhikrScreenTest.counterState
        every { onEvent(any()) } answers { events += firstArg<TasbihEvent>() }
    }

    private fun setContent() {
        composeRule.setThemedContent {
            ChooseDhikrScreen(
                onBack = { backs++ },
                onNavigateToAddPreset = { addPresetTaps++ },
                onEditPreset = { edits += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the All tab shows every default and every custom dhikr`() {
        setContent()

        composeRule.onNodeWithText(subhanAllah.name).assertExists()
        composeRule.onNodeWithText(morningDua.name).assertExists()
        composeRule.onNodeWithText(eveningDua.name).assertExists()
        composeRule.onNodeWithText(mine.name).assertExists()
    }

    @Test
    fun `a category tab keeps only that category`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.tasbih_category_morning)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(morningDua.name).assertExists()
        composeRule.onNodeWithText(subhanAllah.name).assertDoesNotExist()
        composeRule.onNodeWithText(eveningDua.name).assertDoesNotExist()
    }

    @Test
    fun `the Mine tab keeps only what the user created`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.tasbih_category_mine)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(mine.name).assertExists()
        composeRule.onNodeWithText(subhanAllah.name).assertDoesNotExist()
    }

    @Test
    fun `the star tab keeps only the favourites`() {
        setContent()

        // The tab's label is literally a star, which is the whole of its text.
        composeRule.onNodeWithText("★").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(morningDua.name).assertExists()
        composeRule.onNodeWithText(subhanAllah.name).assertDoesNotExist()
        composeRule.onNodeWithText(mine.name).assertDoesNotExist()
    }

    @Test
    fun `search matches a translation, not only a name`() {
        setContent()

        // `NimazSearchBar` carries its placeholder as a content description rather than as text.
        composeRule.onNodeWithContentDescription(string(R.string.tasbih_search_dhikr))
            .performTextInput("glory")
        composeRule.waitForIdle()

        // Case-insensitive, and against the English gloss — someone searching for what a dhikr
        // *means* has no other way to find it.
        composeRule.onNodeWithText(subhanAllah.name).assertExists()
        composeRule.onNodeWithText(morningDua.name).assertDoesNotExist()
    }

    @Test
    fun `search and tab narrow together rather than one replacing the other`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.tasbih_category_morning)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.tasbih_search_dhikr))
            .performTextInput("evening")
        composeRule.waitForIdle()

        // "Evening remembrance" matches the query but not the tab, so nothing survives. A screen
        // that let the query win would show a dhikr from a category the user filtered out.
        composeRule.onNodeWithText(eveningDua.name).assertDoesNotExist()
        composeRule.onNodeWithText(morningDua.name).assertDoesNotExist()
    }

    @Test
    fun `picking a dhikr selects it and goes back`() {
        setContent()

        composeRule.onNodeWithText(subhanAllah.name).performClick()

        assertThat(events).containsExactly(TasbihEvent.SelectPreset(subhanAllah))
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `free count clears the selection and goes back`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.tasbih_free_count_subtitle)).performClick()

        assertThat(events).containsExactly(TasbihEvent.ClearPreset)
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `the star on a row toggles that row's dhikr`() {
        setContent()

        // Every row publishes an identically-described star, so the list is narrowed to one row
        // first — an unqualified match would assert against whichever row came first and pass
        // whatever the tap actually reached.
        composeRule.onNodeWithText(string(R.string.tasbih_category_mine)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites)).performClick()

        assertThat(events).containsExactly(TasbihEvent.ToggleFavorite(mine.id))
    }

    @Test
    fun `only a custom dhikr offers an edit control`() {
        setContent()

        // Four rows are listed and exactly one of them is the user's own. The default adhkar are
        // not theirs to change, and an edit control on one would let a typo be saved over
        // shipped content.
        composeRule.onAllNodesWithContentDescription(string(R.string.tasbih_edit_preset))
            .assertCountEquals(1)

        composeRule.onNodeWithContentDescription(string(R.string.tasbih_edit_preset))
            .performClick()

        assertThat(edits).containsExactly(mine.id)
    }

    @Test
    fun `a row shows the dhikr's Arabic where it has some, and nothing where it has none`() {
        setContent()

        // The Arabic line is the dhikr as it is actually said; a row that dropped it would leave
        // the user matching adhkar by their English names alone.
        composeRule.onNodeWithText(subhanAllah.arabicText!!).assertExists()
        // And a custom dhikr saved without Arabic gets no empty line where one would be.
        composeRule.onNodeWithText(mine.name).assertExists()
        composeRule.onAllNodesWithText(subhanAllah.arabicText!!).assertCountEquals(1)
    }

    @Test
    fun `a row reports its target count`() {
        setContent()

        composeRule.onNodeWithText("${mine.targetCount}×").assertExists()
    }

    @Test
    fun `swiping a custom dhikr asks before deleting, and cancelling keeps it`() {
        setContent()

        swipeAwayRowNamed(mine.name)

        composeRule.onNodeWithText(string(R.string.tasbih_delete_preset_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()
        composeRule.waitForIdle()

        // The cancel arm is the one a manual pass never runs twice, and the one whose failure is
        // unrecoverable: the row is gone and the dhikr with it.
        assertThat(events).isEmpty()
        composeRule.onNodeWithText(mine.name).assertExists()
    }

    @Test
    fun `confirming the dialog deletes the dhikr that was swiped`() {
        setContent()

        swipeAwayRowNamed(mine.name)
        composeRule.onNodeWithText(string(R.string.delete)).performClick()

        assertThat(events).containsExactly(TasbihEvent.DeleteCustomPreset(mine.id))
    }

    @Test
    fun `the add row leads to the form`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.new_tasbih)).performClick()

        assertThat(addPresetTaps).isEqualTo(1)
    }

    /**
     * Swipes the row named [rowText] end-to-start, far enough to pass the dismiss threshold.
     *
     * Explicit coordinates rather than a bare `swipeLeft()`: the gesture travels across the node
     * it is addressed to, and a threshold measured against the row's width is not reached by a
     * default swipe that starts at its centre.
     */
    private fun swipeAwayRowNamed(rowText: String) {
        composeRule.onNodeWithText(rowText).performTouchInput {
            swipeLeft(startX = right - 1f, endX = left + 1f, durationMillis = 200)
        }
        composeRule.waitForIdle()
    }

}
