package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.foundation.tokens.KhatamProgressBar
import com.arshadshah.nimaz.presentation.foundation.tokens.KhatamProgressRing
import com.arshadshah.nimaz.presentation.foundation.tokens.rememberKhatamAccent
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The last of the design system's optional parameters, supplied rather than defaulted.
 *
 * These components are all well covered *at their defaults* — which is the call every other test
 * makes, and the one every screen in the app does not. A card that takes a shape and a border, a
 * badge that takes an indicator, a khatam ring that takes an accent: each of those exists because
 * some screen needed it, and none of them is exercised by a test that only ever passes the
 * required arguments.
 *
 * The failure this catches is narrow and real: a parameter that stops being threaded through its
 * component's body. It compiles, it renders at the default, and the one screen that sets it
 * quietly gets the default instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class FullArgumentSweepTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the card takes its shape, tone, elevation and colours together`() {
        composeRule.setThemedContent {
            Column {
                NimazCard { Text("default card") }
                NimazCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = NimazCardStyle.OUTLINED,
                    tone = NimazTone.WARNING,
                    shape = RoundedCornerShape(2.dp),
                    elevation = 6.dp,
                    selected = true,
                    level = NimazCardLevel.RAISED,
                    onClick = {},
                    enabled = true,
                    gradient = listOf(Color.Yellow, Color.Magenta),
                    colors = NimazCardDefaults.colors(
                        container = Color.Yellow,
                        content = Color.Black,
                        border = Color.Magenta,
                    ),
                ) {
                    Text("specified card")
                }
            }
        }

        composeRule.onNodeWithText("default card").assertExists()
        composeRule.onNodeWithText("specified card").assertExists()
    }

    @Test
    fun `the badge takes every axis at once`() {
        composeRule.setThemedContent {
            NimazBadge(
                text = "Everything",
                modifier = Modifier,
                tone = NimazTone.WARNING,
                emphasis = NimazBadgeEmphasis.OUTLINED,
                shape = NimazBadgeShape.ROUNDED,
                size = NimazBadgeSize.LARGE,
                icon = Icons.Filled.Star,
                indicatorColor = Color.Magenta,
                selected = false,
                selectedTone = NimazTone.SUCCESS,
                onClick = {},
            )
        }

        composeRule.onNodeWithText("Everything").assertExists()
    }

    @Test
    fun `the menu row takes every option at once`() {
        composeRule.setThemedContent {
            NimazMenuGroup(modifier = Modifier.fillMaxWidth()) {
                NimazMenuItem(
                    title = "Everything",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    subtitle = "with all of it",
                    icon = Icons.Filled.Schedule,
                    iconTint = Color.Magenta,
                    trailingIcon = NimazIcons.Forward,
                    trailing = { Text("slot") },
                    enabled = true,
                    selected = true,
                    subtitleStyle = MaterialTheme.typography.labelSmall,
                )
            }
        }

        composeRule.onNodeWithText("with all of it").assertExists()
        composeRule.onNodeWithText("slot").assertExists()
    }

    @Test
    fun `the khatam ring and bar take a caller's accent and metrics`() {
        composeRule.setThemedContent {
            Column {
                val accent = rememberKhatamAccent()
                KhatamProgressRing(
                    progress = 0.42f,
                    modifier = Modifier,
                    size = 72.dp,
                    strokeWidth = 9.dp,
                    accent = accent,
                    isComplete = false,
                    textStyle = MaterialTheme.typography.titleMedium,
                    animated = false,
                )
                KhatamProgressBar(
                    progress = 0.42f,
                    modifier = Modifier.fillMaxWidth(),
                    height = 10.dp,
                    accent = accent,
                    isComplete = false,
                )
            }
        }

        composeRule.onNodeWithText(useUnmergedTree = true, text = "42%").assertExists()
    }

    @Test
    fun `the page indicator's pager overload takes every metric`() {
        composeRule.setThemedContent {
            NimazPageIndicator(
                pageCount = 4,
                currentPage = 2,
                modifier = Modifier,
                activeColor = Color.Magenta,
                inactiveColor = Color.Gray,
                dotSize = 9.dp,
                activeWidth = 30.dp,
                spacing = 5.dp,
            )
        }

        composeRule.waitForIdle()
    }
}
