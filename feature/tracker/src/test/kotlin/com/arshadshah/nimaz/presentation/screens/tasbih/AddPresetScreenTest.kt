package com.arshadshah.nimaz.presentation.screens.tasbih

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
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
 * The custom-dhikr form, in both of the modes one composable serves.
 *
 * The edit mode is the interesting one, and its seeding rule is the reason this file exists.
 * `LaunchedEffect` copies the loaded preset into the fields **once** — the presets flow re-emits
 * on every write to that table, so a form that re-seeded on each emission would silently discard
 * whatever the user had typed the moment anything else touched the presets. That is a data-loss
 * bug with no error message, and the `seeded` flag guarding it is checkable only from here.
 *
 * The rest are the ordinary ways a form goes wrong and stays plausible: creating when it should
 * update (which leaves a duplicate rather than an edit), a blank name accepted, and a target of
 * `""` silently becoming something other than the documented 33.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class AddPresetScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val existing = TasbihPreset(
        id = 42L,
        name = "My own dhikr",
        arabicText = "ذِكْر",
        transliteration = "Dhikr",
        translation = "Remembrance",
        targetCount = 100,
        category = TasbihCategory.EVENING,
        reference = "a note to keep",
        isDefault = false,
        displayOrder = 3,
        createdAt = 1_000L,
        updatedAt = 2_000L,
    )

    private val presetsState = MutableStateFlow(
        TasbihPresetsUiState(customPresets = listOf(existing), isLoading = false)
    )
    private val events = mutableListOf<TasbihEvent>()
    private var backs = 0

    private val viewModel: TasbihViewModel = mockk(relaxed = true) {
        every { this@mockk.presetsState } returns this@AddPresetScreenTest.presetsState
        every { onEvent(any()) } answers { events += firstArg<TasbihEvent>() }
    }

    private fun setContent(presetId: Long? = null) {
        composeRule.setThemedContent {
            AddPresetScreen(
                onNavigateBack = { backs++ },
                presetId = presetId,
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /**
     * The form's fields, in the order they are laid out: name, Arabic, transliteration,
     * translation.
     *
     * Addressed by position rather than by placeholder because `preset_name_placeholder` and
     * `transliteration_placeholder` are **the same string** — "e.g., SubhanAllah wa Bihamdihi" —
     * so a placeholder finder matches two fields and fails on the count, which is a collision in
     * the strings rather than anything wrong with the form.
     */
    private fun field(index: Int) = composeRule.onAllNodes(hasSetTextAction())[index]

    private fun nameField() = field(0)

    private fun createdPreset(): TasbihPreset =
        (events.single() as TasbihEvent.CreateCustomPreset).preset

    private fun updatedPreset(): TasbihPreset =
        (events.single() as TasbihEvent.UpdateCustomPreset).preset

    @Test
    fun `a filled-in form creates the dhikr it describes and leaves`() {
        setContent()

        nameField()
            .performTextInput("  Alhamdulillah  ")
        field(1)
            .performTextInput("الحمد لله")
        field(2)
            .performTextInput("Alhamdulillah")
        field(3)
            .performTextInput("All praise is due to Allah")
        composeRule.onNodeWithText(string(R.string.tasbih_category_morning)).performClick()
        composeRule.onNodeWithText(string(R.string.create_tasbih)).performClick()

        val preset = createdPreset()
        // Trimmed, because a trailing space is invisible and would sort and match differently.
        assertThat(preset.name).isEqualTo("Alhamdulillah")
        assertThat(preset.translation).isEqualTo("All praise is due to Allah")
        assertThat(preset.category).isEqualTo(TasbihCategory.MORNING)
        assertThat(preset.id).isEqualTo(0L)
        assertThat(preset.isDefault).isFalse()
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `empty optional fields are stored as absent rather than as blanks`() {
        setContent()

        nameField()
            .performTextInput("Bare")
        composeRule.onNodeWithText(string(R.string.create_tasbih)).performClick()

        // A blank string and "no Arabic text" render very differently — the row draws an empty
        // Arabic line for one and nothing at all for the other.
        val preset = createdPreset()
        assertThat(preset.arabicText).isNull()
        assertThat(preset.transliteration).isNull()
        assertThat(preset.translation).isNull()
        assertThat(preset.targetCount).isEqualTo(33)
    }

    @Test
    fun `a nameless dhikr is refused, and saying so clears once typing resumes`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.create_tasbih)).performClick()

        assertThat(events).isEmpty()
        assertThat(backs).isEqualTo(0)
        composeRule.onNodeWithText(string(R.string.name_required_error)).assertExists()

        nameField()
            .performTextInput("Now named")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.name_required_error)).assertDoesNotExist()
    }

    @Test
    fun `the target stepper will not go below one`() {
        setContent()

        nameField()
            .performTextInput("Bounded")
        repeat(40) {
            composeRule.onNodeWithContentDescription(string(R.string.cd_decrease)).performClick()
        }
        composeRule.onNodeWithText(string(R.string.create_tasbih)).performClick()

        // A target of zero makes the counter's progress arithmetic divide by it.
        assertThat(createdPreset().targetCount).isEqualTo(1)
    }

    @Test
    fun `editing seeds the form from the preset and updates rather than duplicates`() {
        setContent(presetId = existing.id)
        composeRule.waitForIdle()

        // The title changes with the mode, and the button with it — a form that says "Create"
        // over a loaded preset is one tap from a duplicate.
        composeRule.onNodeWithText(string(R.string.edit_tasbih)).assertExists()
        composeRule.onNodeWithText(existing.name).assertExists()
        composeRule.onNodeWithText(existing.translation!!).assertExists()

        composeRule.onNodeWithText(string(R.string.save_tasbih)).performClick()

        val preset = updatedPreset()
        assertThat(preset.id).isEqualTo(existing.id)
        assertThat(preset.name).isEqualTo(existing.name)
        assertThat(preset.targetCount).isEqualTo(existing.targetCount)
        assertThat(preset.category).isEqualTo(TasbihCategory.EVENING)
        // Carried through untouched: neither is editable here, and losing them on every save
        // would quietly strip the reference off a dhikr the user had cited.
        assertThat(preset.reference).isEqualTo(existing.reference)
        assertThat(preset.createdAt).isEqualTo(existing.createdAt)
        assertThat(preset.displayOrder).isEqualTo(existing.displayOrder)
    }

    @Test
    fun `editing a dhikr that has only a name leaves the other fields empty`() {
        val bare = existing.copy(
            arabicText = null,
            transliteration = null,
            translation = null,
            category = null,
        )
        presetsState.value = presetsState.value.copy(customPresets = listOf(bare))
        setContent(presetId = bare.id)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.save_tasbih)).performClick()

        // Seeding must turn a null field into an empty box, not into the string "null" — and it
        // must come back out as null rather than as a blank the row would draw a gap for.
        val preset = updatedPreset()
        assertThat(preset.arabicText).isNull()
        assertThat(preset.transliteration).isNull()
        assertThat(preset.translation).isNull()
        // A dhikr saved with no category is the user's own, so the form defaults it there rather
        // than leaving it uncategorised and unfindable under any tab.
        assertThat(preset.category).isEqualTo(TasbihCategory.CUSTOM)
    }

    @Test
    fun `every category the form offers can be chosen`() {
        setContent()

        nameField().performTextInput("Categorised")
        composeRule.onNodeWithText(string(R.string.tasbih_category_daily)).performClick()
        composeRule.onNodeWithText(string(R.string.create_tasbih)).performClick()

        // Five chips over five enum entries, each with its own label: a `when` arm that fell
        // through would leave a category the user can pick and cannot read.
        assertThat(createdPreset().category).isEqualTo(TasbihCategory.DAILY)
    }

    @Test
    fun `a re-emission of the presets flow does not overwrite what has been typed`() {
        setContent(presetId = existing.id)
        composeRule.waitForIdle()

        nameField().performTextReplacement("Renamed by hand")

        // Anything writing to the presets table re-emits this flow — creating another dhikr,
        // toggling a favourite, the seeder running. Re-seeding on that emission would throw the
        // user's edit away mid-sentence, with nothing on screen to explain it.
        presetsState.value = presetsState.value.copy(
            customPresets = listOf(existing),
            defaultPresets = presetsState.value.defaultPresets + existing.copy(id = 99L),
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.save_tasbih)).performClick()

        assertThat(updatedPreset().name).isEqualTo("Renamed by hand")
    }

    @Test
    fun `an unknown preset id falls back to creating`() {
        setContent(presetId = 404L)
        composeRule.waitForIdle()

        // Nothing to seed from, so the form must be the blank create form rather than a save
        // button pointed at a preset that does not exist.
        composeRule.onNodeWithText(string(R.string.new_tasbih)).assertExists()
        nameField()
            .performTextInput("Fresh")
        composeRule.onNodeWithText(string(R.string.create_tasbih)).performClick()

        assertThat(createdPreset().id).isEqualTo(0L)
    }

    @Test
    fun `the back arrow leaves without saving anything`() {
        setContent()

        nameField()
            .performTextInput("Abandoned")
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(events).isEmpty()
        assertThat(backs).isEqualTo(1)
    }
}
