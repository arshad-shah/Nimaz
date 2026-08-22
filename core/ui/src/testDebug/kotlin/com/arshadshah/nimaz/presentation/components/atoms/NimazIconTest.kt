package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazIconTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `icon enums are complete`() {
        assertThat(NimazIconVariant.entries).hasSize(6)
        assertThat(NimazIconType.entries).hasSize(2)
        assertThat(NimazIconSize.entries).hasSize(5)
        assertThat(NimazIconContainerShape.entries).hasSize(3)
    }

    @Test
    fun `plain icon renders with content description`() {
        composeRule.setThemedContent {
            NimazIcon(imageVector = Icons.Default.Star, contentDescription = "star")
        }
        composeRule.onNodeWithContentDescription("star").assertExists()
    }

    @Test
    fun `default plain icon size matches the LARGE preset 24dp`() {
        // NimazIconSize.LARGE is the default so a no-size NimazIcon mirrors a
        // bare Material Icon (24dp).
        composeRule.setThemedContent {
            NimazIcon(imageVector = Icons.Default.Star, contentDescription = "star")
        }
        composeRule.onNodeWithContentDescription("star")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun `size preset drives the glyph dimension`() {
        composeRule.setThemedContent {
            NimazIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "star",
                size = NimazIconSize.SMALL
            )
        }
        composeRule.onNodeWithContentDescription("star").assertWidthIsEqualTo(16.dp)
    }

    @Test
    fun `iconSize override beats the size preset`() {
        composeRule.setThemedContent {
            NimazIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "star",
                size = NimazIconSize.LARGE,
                iconSize = 18.dp
            )
        }
        composeRule.onNodeWithContentDescription("star").assertWidthIsEqualTo(18.dp)
    }

    @Test
    fun `every variant renders`() {
        // setContent may only be called once per test, so render one icon per
        // variant in a single composition and assert each is present by its
        // (unique-per-variant) content description.
        composeRule.setThemedContent {
            Column {
                NimazIconVariant.entries.forEach { variant ->
                    NimazIcon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "v-${variant.name}",
                        variant = variant
                    )
                }
            }
        }
        NimazIconVariant.entries.forEach { variant ->
            composeRule.onNodeWithContentDescription("v-${variant.name}").assertExists()
        }
    }

    @Test
    fun `tint escape hatch renders`() {
        composeRule.setThemedContent {
            NimazIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "tinted",
                tint = NimazColors.Success
            )
        }
        composeRule.onNodeWithContentDescription("tinted").assertExists()
    }

    private fun assertContainedRenders(shape: NimazIconContainerShape) {
        composeRule.setThemedContent {
            NimazIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "contained",
                type = NimazIconType.CONTAINED,
                variant = NimazIconVariant.PRIMARY,
                containerShape = shape,
                size = NimazIconSize.LARGE
            )
        }
        composeRule.onNodeWithContentDescription("contained").assertExists()
    }

    @Test
    fun `contained icon renders circle`() = assertContainedRenders(NimazIconContainerShape.CIRCLE)

    @Test
    fun `contained icon renders rounded square`() =
        assertContainedRenders(NimazIconContainerShape.ROUNDED_SQUARE)

    @Test
    fun `contained icon renders square`() = assertContainedRenders(NimazIconContainerShape.SQUARE)

    @Test
    fun `contained icon honours granular container and icon size`() {
        composeRule.setThemedContent {
            NimazIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "badge",
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = Color(0xFF22C55E),
                containerSize = 38.dp,
                iconSize = 20.dp,
                cornerRadius = 11.dp
            )
        }
        composeRule.onNodeWithContentDescription("badge")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun `contained icon accepts a solid container color override`() {
        composeRule.setThemedContent {
            NimazIcon(
                imageVector = Icons.Default.Star,
                contentDescription = "solid",
                type = NimazIconType.CONTAINED,
                variant = NimazIconVariant.ON_ACCENT,
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
        composeRule.onNodeWithContentDescription("solid").assertExists()
    }
}
