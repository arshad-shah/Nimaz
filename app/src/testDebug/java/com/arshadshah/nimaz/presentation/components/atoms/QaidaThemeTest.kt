package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.onRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaThemeTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `medallion state has three entries`() {
        assertThat(QaidaMedallionState.entries).hasSize(3)
        assertThat(QaidaMedallionState.entries).containsExactly(
            QaidaMedallionState.DONE,
            QaidaMedallionState.CURRENT,
            QaidaMedallionState.LOCKED,
        )
    }

    @Test
    fun `rememberQaidaPalette derives colours from material scheme`() {
        var captured: QaidaPalette? = null
        var scheme: ColorScheme? = null
        composeRule.setThemedContent {
            scheme = MaterialTheme.colorScheme
            captured = rememberQaidaPalette()
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()

        val palette = captured!!
        val c = scheme!!
        assertThat(palette.done).isEqualTo(c.secondary)
        assertThat(palette.current).isEqualTo(c.primary)
        assertThat(palette.locked).isEqualTo(c.surfaceVariant)
        assertThat(palette.trailLocked).isEqualTo(c.outline)
    }
}
