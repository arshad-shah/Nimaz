package com.arshadshah.nimaz.presentation.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The design system's numeric scales, and the one property that makes them scales.
 *
 * These are token objects — corner radii, elevations, spacings, icon sizes — and the reason to test
 * them is not that a constant might change, it is that they must stay **ordered**. Every one of
 * them is chosen from by name at hundreds of call sites (`NimazSpacing.Small` here,
 * `NimazSpacing.Large` there), and the names only mean anything if small really is smaller than
 * large. A value edited in isolation — a `Medium` nudged past `Large`, an `ExtraSmall` typed as
 * `40.dp` — compiles, renders, and silently inverts the visual hierarchy of every screen that
 * relies on the pair.
 *
 * `NimazShapes` is the same claim across a boundary: Material reads it for `MaterialTheme.shapes`,
 * so it has to agree with `NimazCornerRadius`, which is what the app's own components read. Two
 * tables of the same numbers is exactly the shape that drifts.
 */
class DesignScaleTest {

    @Test
    fun `corner radii increase with their names`() {
        val scale = listOf(
            NimazCornerRadius.ExtraSmall,
            NimazCornerRadius.Small,
            NimazCornerRadius.Medium,
            NimazCornerRadius.Large,
            NimazCornerRadius.ExtraLarge,
        )

        assertThat(scale).isInOrder()
        assertThat(scale.toSet()).hasSize(scale.size)
        // `Full` is the pill radius — deliberately far past the top of the scale so a rounded
        // rectangle can never accidentally be a pill.
        assertThat(NimazCornerRadius.Full).isGreaterThan(NimazCornerRadius.ExtraLarge)
    }

    @Test
    fun `elevations increase with their levels and start at nothing`() {
        val scale = listOf(
            NimazElevation.None,
            NimazElevation.Level1,
            NimazElevation.Level2,
            NimazElevation.Level3,
            NimazElevation.Level4,
            NimazElevation.Level5,
        )

        assertThat(scale).isInOrder()
        assertThat(scale.toSet()).hasSize(scale.size)
        assertThat(NimazElevation.None.value).isEqualTo(0f)
    }

    @Test
    fun `spacings increase with their names`() {
        val scale = listOf(
            NimazSpacing.ExtraSmall,
            NimazSpacing.Small,
            NimazSpacing.Medium,
            NimazSpacing.Large,
            NimazSpacing.ExtraLarge,
            NimazSpacing.XXLarge,
            NimazSpacing.XXXLarge,
        )

        assertThat(scale).isInOrder()
        assertThat(scale.toSet()).hasSize(scale.size)
    }

    @Test
    fun `icon sizes increase with their names and clear the smallest tap target`() {
        val scale = listOf(
            NimazIconSize.ExtraSmall,
            NimazIconSize.Small,
            NimazIconSize.Medium,
            NimazIconSize.Large,
            NimazIconSize.ExtraLarge,
            NimazIconSize.XXLarge,
        )

        assertThat(scale).isInOrder()
        assertThat(scale.toSet()).hasSize(scale.size)
    }

    @Test
    fun `the Material shape scale is the app's corner scale`() {
        // `MaterialTheme.shapes` is what stock M3 components round themselves with, and
        // `NimazCornerRadius` is what the app's own do. The two being separate tables of the same
        // numbers is how a Material dialog ends up a different radius from the card behind it.
        assertThat(NimazShapes.extraSmall)
            .isEqualTo(roundedBy(NimazCornerRadius.ExtraSmall))
        assertThat(NimazShapes.small).isEqualTo(roundedBy(NimazCornerRadius.Small))
        assertThat(NimazShapes.medium).isEqualTo(roundedBy(NimazCornerRadius.Medium))
        assertThat(NimazShapes.large).isEqualTo(roundedBy(NimazCornerRadius.Large))
        assertThat(NimazShapes.extraLarge).isEqualTo(roundedBy(NimazCornerRadius.ExtraLarge))
    }

    private fun roundedBy(radius: androidx.compose.ui.unit.Dp) =
        androidx.compose.foundation.shape.RoundedCornerShape(radius)
}
