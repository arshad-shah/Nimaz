package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.theme.QaidaMedallionState
import com.arshadshah.nimaz.presentation.theme.QaidaPalette
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaMedallionTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Composable
    private fun palette(): QaidaPalette {
        val c = MaterialTheme.colorScheme
        return QaidaPalette(
            done = c.secondary,
            onDone = c.onSecondary,
            current = c.primary,
            onCurrent = c.onPrimary,
            locked = c.surfaceVariant,
            onLocked = c.onSurfaceVariant,
            trail = c.primary,
            trailLocked = c.outline,
            gold = c.secondary,
            surface = c.surface,
            surfaceContainer = c.surfaceContainerHigh,
            onSurface = c.onSurface,
            onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline,
        )
    }

    @Test
    fun `exposes content description for accessibility`() {
        composeRule.setThemedContent {
            QaidaMedallion(
                label = "١",
                state = QaidaMedallionState.CURRENT,
                contentDescription = "Lesson 2, current",
                palette = palette(),
                onClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Lesson 2, current").assertIsDisplayed()
    }

    @Test
    fun `current medallion fires click`() {
        var fired = false
        composeRule.setThemedContent {
            QaidaMedallion(
                label = "٢",
                state = QaidaMedallionState.CURRENT,
                contentDescription = "Lesson 2, current",
                palette = palette(),
                onClick = { fired = true },
            )
        }
        composeRule.onNodeWithContentDescription("Lesson 2, current").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `locked medallion does not fire click`() {
        var fired = false
        composeRule.setThemedContent {
            QaidaMedallion(
                label = "٣",
                state = QaidaMedallionState.LOCKED,
                contentDescription = "Lesson 3, locked",
                palette = palette(),
                onClick = { fired = true },
            )
        }
        composeRule.onNodeWithContentDescription("Lesson 3, locked").performClick()
        assertThat(fired).isFalse()
    }
}
