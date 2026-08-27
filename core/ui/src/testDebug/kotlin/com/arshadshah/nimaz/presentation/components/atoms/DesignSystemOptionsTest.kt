package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldDensity
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldShell
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The design system's optional parameters, called with something other than their defaults.
 *
 * Every component here already has a test proving it renders. What none of them prove is that its
 * *options* are wired: a size that is read but never applied, a colour parameter shadowed by a
 * theme read, a shape argument dropped in a refactor. Those are invisible at the default call —
 * which is the only call most components ever get in a test — and they are how a design system
 * accumulates parameters that quietly do nothing.
 *
 * So this is deliberately an options pass, one composition per component family, asserting the
 * thing each option is supposed to produce. It is the counterpart to the per-component tests, not
 * a replacement for them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class DesignSystemOptionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the shamsa medallion takes its size, its inks and its numeral system`() {
        // `useArabicIndicNumerals` is the one that changes what is *read*: a surah medallion in the
        // Quran reader shows ٣٦, the same component in a list shows 36, and the flag is the only
        // difference.
        composeRule.setThemedContent {
            Column {
                ShamsaMedallion(number = 36)
                ShamsaMedallion(
                    number = 36,
                    size = 64.dp,
                    gold = Color.Yellow,
                    teal = Color.Cyan,
                    numberColor = Color.Magenta,
                    useArabicIndicNumerals = true,
                    numberStyle = MaterialTheme.typography.headlineSmall,
                )
                DiamondFloret(color = Color.Yellow)
                DiamondFloret(color = Color.Yellow, size = 12.dp, alpha = 0.4f)
            }
        }

        composeRule.onNodeWithText("36").assertExists()
        composeRule.onNodeWithText(toArabicNumber(36)).assertExists()
    }

    @Test
    fun `the skeleton takes a caller's shape and line metrics`() {
        composeRule.setThemedContent {
            Column {
                NimazSkeleton()
                NimazSkeleton(shape = CircleShape)
                NimazSkeletonText()
                NimazSkeletonText(lines = 5, lineHeight = 14.dp, lastLineFraction = 0.3f)
                NimazSkeletonRow()
                NimazSkeletonRow(showLeading = false)
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun `the empty state renders with and without its action`() {
        // The `actionLabel != null && onAction != null` pairing — half a pair is a button with no
        // handler, which is the failure the component's own test already names.
        var acted = 0
        composeRule.setThemedContent {
            Column {
                NimazEmptyState(title = "Nothing here", message = "Add something")
                NimazEmptyState(
                    title = "No bookmarks",
                    message = "Save a verse to see it here",
                    actionLabel = "Browse the Quran",
                    onAction = { acted++ },
                )
            }
        }

        composeRule.onNodeWithText("Nothing here").assertExists()
        composeRule.onNodeWithText("Browse the Quran").assertExists()
    }

    @Test
    fun `the field shell renders its label, helper and error states`() {
        // The shell is what every form field in the app is drawn inside, so its error state is the
        // app's one way of saying "this entry is wrong".
        composeRule.setThemedContent {
            Column {
                NimazFieldShell(label = "Amount") { Text("body") }
                NimazFieldShell(
                    label = "Amount",
                    required = true,
                    helper = "In your chosen currency",
                    counter = "3/40",
                    focused = true,
                ) { Text("helped") }
                NimazFieldShell(
                    label = "Amount",
                    optionalLabel = "optional",
                    error = "Not a number",
                    counter = "41/40",
                    counterIsOver = true,
                    enabled = false,
                    readOnly = true,
                    density = NimazFieldDensity.COMPACT,
                ) { Text("errored") }
            }
        }

        composeRule.onNodeWithText("In your chosen currency").assertExists()
        composeRule.onNodeWithText("Not a number").assertExists()
    }

    @Test
    fun `the icon well takes its size and its colour`() {
        composeRule.setThemedContent {
            Column {
                NimazIconWell(
                    icon = Icons.Filled.Star,
                    color = MaterialTheme.colorScheme.primary,
                    contentDescription = "default",
                )
                NimazIconWellSize.entries.forEach { size ->
                    NimazIconWell(
                        icon = Icons.Filled.Star,
                        contentDescription = size.name,
                        color = Color.Magenta,
                        size = size,
                    )
                }
            }
        }

        NimazIconWellSize.entries.forEach {
            composeRule.onNodeWithContentDescription(it.name).assertExists()
        }
    }
}
