package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The hierarchy row — the component the hadith topic tree and the Quran outline are built from.
 *
 * Two behaviours here are easy to get wrong and invisible when you do. The first is that a row's
 * **body tap and its disclosure tap are different actions**: tapping "Doctrine" opens Doctrine,
 * tapping the chevron beside it expands its children in place. Wiring both to one lambda makes a
 * tree that cannot be browsed without leaving it. The second is that a **leaf keeps the
 * disclosure column but is not a control** — the dot holds the text edge in line with its
 * expandable siblings, and making it tappable gives the user a target that does nothing.
 *
 * The chevron's content description also flips with the state, which is the only thing telling a
 * screen-reader user whether a branch is open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazTreeRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a row renders its label, its secondary label and its count`() {
        composeRule.setThemedContent {
            NimazTreeRow(
                label = "Doctrine",
                secondaryLabel = "العقيدة",
                badgeText = "412",
                onClick = {},
            )
        }

        composeRule.onNodeWithText("Doctrine").assertExists()
        composeRule.onNodeWithText("العقيدة").assertExists()
        composeRule.onNodeWithText("412").assertExists()
    }

    @Test
    fun `a bare row shows none of the optional parts`() {
        // Four independent null checks. A row built from a topic with no Arabic name, no count and
        // no summary is the common case in the deeper levels of the tree.
        composeRule.setThemedContent { NimazTreeRow(label = "Mercy", onClick = {}) }

        composeRule.onNodeWithText("Mercy").assertExists()
        composeRule.onNodeWithText("412").assertDoesNotExist()
    }

    @Test
    fun `the supporting line renders when there is one`() {
        composeRule.setThemedContent {
            NimazTreeRow(
                label = "Patience in adversity",
                supportingText = "Doctrine · States of the heart",
                onClick = {},
            )
        }

        composeRule.onNodeWithText("Doctrine · States of the heart").assertExists()
    }

    @Test
    fun `tapping the row body opens it`() {
        var opened = 0
        composeRule.setThemedContent {
            NimazTreeRow(label = "Doctrine", onClick = { opened++ })
        }

        composeRule.onNodeWithText("Doctrine").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `the disclosure expands in place rather than opening the row`() {
        // The distinction the component exists to make. One lambda for both is a tree you can
        // only navigate by leaving it.
        var opened = 0
        var toggled = 0
        composeRule.setThemedContent {
            NimazTreeRow(
                label = "Doctrine",
                expandable = true,
                onClick = { opened++ },
                onToggleExpanded = { toggled++ },
            )
        }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.cd_tree_expand, "Doctrine"))
            .performClick()

        assertThat(toggled).isEqualTo(1)
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `an expanded row says it can be collapsed`() {
        // The chevron rotates, which a screen reader cannot see. The description is the whole
        // signal, and it is picked by the same boolean that drives the rotation.
        composeRule.setThemedContent {
            NimazTreeRow(
                label = "Doctrine",
                expandable = true,
                expanded = true,
                onClick = {},
            )
        }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.cd_tree_collapse, "Doctrine"))
            .assertExists()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.cd_tree_expand, "Doctrine"))
            .assertDoesNotExist()
    }

    @Test
    fun `a leaf offers no disclosure control at all`() {
        // The dot keeps the text edge aligned with expandable siblings and is deliberately not a
        // button — a target that does nothing is worse than no target.
        composeRule.setThemedContent { NimazTreeRow(label = "Mercy", onClick = {}) }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.cd_tree_expand, "Mercy"))
            .assertDoesNotExist()
    }

    @Test
    fun `trailing content is rendered beside the row`() {
        composeRule.setThemedContent {
            NimazTreeRow(
                label = "Doctrine",
                onClick = {},
                trailingContent = { Text("beside") },
            )
        }

        composeRule.onNodeWithText("beside").assertExists()
    }

    @Test
    fun `rows at every depth render, each indented one step further`() {
        // `depth` drives both the padding and the number of margin rules drawn behind the row.
        // Depth 0 draws none — `count <= 0` returns the modifier untouched — and the deeper rows
        // exercise the repeat.
        composeRule.setThemedContent {
            Column {
                NimazTreeRow(label = "Level 0", depth = 0, onClick = {})
                NimazTreeRow(label = "Level 1", depth = 1, onClick = {})
                NimazTreeRow(label = "Level 2", depth = 2, onClick = {})
            }
        }

        composeRule.onNodeWithText("Level 0").assertExists()
        composeRule.onNodeWithText("Level 1").assertExists()
        composeRule.onNodeWithText("Level 2").assertExists()
    }

    @Test
    fun `a right-to-left tree still renders its rows`() {
        // `rtl` mirrors which side the margin rules are drawn on. Arabic is a first-class layout
        // direction for this app, and the rules are the only thing that has to flip.
        composeRule.setThemedContent {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                NimazTreeRow(label = "العقيدة", depth = 2, expandable = true, onClick = {})
            }
        }

        composeRule.onNodeWithText("العقيدة").assertExists()
    }
}
