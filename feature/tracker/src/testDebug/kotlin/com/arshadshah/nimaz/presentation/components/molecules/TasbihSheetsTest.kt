package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.molecules.tasbih.BeadDesignPickerSheet
import com.arshadshah.nimaz.presentation.components.molecules.tasbih.CurrentTasbihSheet
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
}
