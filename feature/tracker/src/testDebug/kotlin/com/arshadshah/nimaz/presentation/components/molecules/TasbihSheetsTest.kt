package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.molecules.tasbih.BeadDesignPickerSheet
import com.arshadshah.nimaz.presentation.components.molecules.tasbih.CurrentTasbihSheet
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TasbihSheetsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun samplePreset() = TasbihPreset(
        id = 1L,
        name = "SubhanAllah",
        arabicText = "سُبْحَانَ ٱللَّٰهِ",
        transliteration = "SubhanAllah",
        translation = "Glory be to Allah",
        targetCount = 33,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 0,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `bead design picker sheet renders`() {
        composeRule.setThemedContent {
            BeadDesignPickerSheet(
                selectedKey = "wood",
                onSelect = {},
                leftHanded = false,
                onToggleHanded = {},
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()
        // The modal bottom sheet renders in its own window, so onRoot() is
        // ambiguous; assert a distinctive piece of the sheet's content instead.
        composeRule.onNodeWithText("Left-handed").assertIsDisplayed()
    }

    @Test
    fun `current tasbih sheet renders with a preset`() {
        composeRule.setThemedContent {
            CurrentTasbihSheet(
                preset = samplePreset(),
                targetCount = 33,
                totalToday = 66,
                laps = 2,
                onChangeDhikr = {},
                onTargetChange = {},
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()
        // The preset's translation and the always-present Change Dhikr action
        // prove the sheet composed its preset branch and rendered.
        composeRule.onNodeWithText("Glory be to Allah").assertIsDisplayed()
        composeRule.onNodeWithText("Change Dhikr").assertIsDisplayed()
    }

    @Test
    fun `current tasbih sheet renders free-count branch when preset is null`() {
        composeRule.setThemedContent {
            CurrentTasbihSheet(
                preset = null,
                targetCount = 100,
                totalToday = 0,
                laps = 0,
                onChangeDhikr = {},
                onTargetChange = {},
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()
        // The "Free Count" label only appears when preset is null, proving the
        // free-count branch composed.
        composeRule.onNodeWithText("Free Count").assertIsDisplayed()
        composeRule.onNodeWithText("Change Dhikr").assertIsDisplayed()
    }

    @Test
    fun `the current sheet shows a preset's reference and transliteration`() {
        composeRule.setThemedContent {
            CurrentTasbihSheet(
                preset = samplePreset(),
                targetCount = 33,
                totalToday = 66,
                laps = 2,
                onChangeDhikr = {},
                onTargetChange = {},
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()

        // The reference is what makes a dhikr checkable — "Sahih Muslim" is the difference
        // between a remembrance and a claim about one. It is optional, so it renders only when
        // the preset carries it, which is exactly the arm that goes unnoticed when it breaks.
        composeRule.onNodeWithText("Sahih Muslim").assertIsDisplayed()
        composeRule.onNodeWithText("SubhanAllah").assertIsDisplayed()
    }

    @Test
    fun `a preset with no reference or transliteration renders neither`() {
        composeRule.setThemedContent {
            CurrentTasbihSheet(
                preset = samplePreset().copy(
                    transliteration = null,
                    reference = null,
                    arabicText = null,
                ),
                targetCount = 33,
                totalToday = 0,
                laps = 0,
                onChangeDhikr = {},
                onTargetChange = {},
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()

        // A bare custom dhikr is the common case — no blank lines where the optional parts
        // would be, and the translation still standing in as the name.
        composeRule.onNodeWithText("Sahih Muslim").assertDoesNotExist()
        composeRule.onNodeWithText("Glory be to Allah").assertIsDisplayed()
    }

    @Test
    fun `the free count sheet dials the target`() {
        var target = 100
        composeRule.setThemedContent {
            CurrentTasbihSheet(
                preset = null,
                targetCount = 100,
                totalToday = 0,
                laps = 0,
                onChangeDhikr = {},
                onTargetChange = { target = it },
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()

        // The stepper is offered only for a free count — a preset's target is the preset's, and
        // this is the one place a user can set their own.
        composeRule.onNodeWithContentDescription("Increase").performClick()

        assertThat(target).isGreaterThan(100)
    }

    @Test
    fun `the bead sheet says which way the beads advance`() {
        composeRule.setThemedContent {
            BeadDesignPickerSheet(
                selectedKey = "jade",
                onSelect = {},
                leftHanded = true,
                onToggleHanded = {},
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()

        // The handedness switch has no other confirmation on screen: the strand is a Canvas with
        // no text in it, so this line is the only thing that tells the user what they just
        // changed.
        composeRule.onNodeWithText("Beads advance left → right").assertIsDisplayed()
        composeRule.onNodeWithText("Jade").assertIsDisplayed()
    }
}
