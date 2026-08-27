package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazToneColorsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every tone resolves a foreground colour`() {
        val resolved = mutableListOf<Color>()
        composeRule.setThemedContent {
            NimazTone.entries.forEach { resolved.add(NimazToneColors.foreground(it)) }
        }
        composeRule.waitForIdle()
        assertThat(resolved).hasSize(NimazTone.entries.size)
    }

    @Test
    fun `every tone resolves a container colour`() {
        val resolved = mutableListOf<Color>()
        composeRule.setThemedContent {
            NimazTone.entries.forEach { resolved.add(NimazToneColors.container(it)) }
        }
        composeRule.waitForIdle()
        assertThat(resolved).hasSize(NimazTone.entries.size)
    }

    @Test
    fun `transparent tone resolves a transparent container`() {
        var container: Color? = null
        composeRule.setThemedContent {
            container = NimazToneColors.container(NimazTone.TRANSPARENT)
        }
        composeRule.waitForIdle()
        assertThat(container).isEqualTo(Color.Transparent)
    }

    @Test
    fun `transparent tone resolves a transparent outline`() {
        var outline: Color? = null
        composeRule.setThemedContent {
            outline = NimazToneColors.outline(NimazTone.TRANSPARENT)
        }
        composeRule.waitForIdle()
        assertThat(outline).isEqualTo(Color.Transparent)
    }

    @Test
    fun `success and error resolve to different foregrounds`() {
        var success: Color? = null
        var error: Color? = null
        composeRule.setThemedContent {
            success = NimazToneColors.foreground(NimazTone.SUCCESS)
            error = NimazToneColors.foreground(NimazTone.ERROR)
        }
        composeRule.waitForIdle()
        assertThat(success).isNotEqualTo(error)
    }

    @Test
    fun `a tone's outline follows its foreground for the coloured tones`() {
        var foreground: Color? = null
        var outline: Color? = null
        composeRule.setThemedContent {
            foreground = NimazToneColors.foreground(NimazTone.WARNING)
            outline = NimazToneColors.outline(NimazTone.WARNING)
        }
        composeRule.waitForIdle()
        assertThat(outline).isEqualTo(foreground)
    }
}
