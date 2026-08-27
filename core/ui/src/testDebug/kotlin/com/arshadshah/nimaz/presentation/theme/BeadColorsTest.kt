package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The tasbih bead palette — six materials, each a three-stop shading ramp.
 *
 * A bead is drawn as a radial gradient from highlight through mid to shadow. That is the only
 * thing making a flat circle read as a sphere, and it depends entirely on the three stops being
 * **ordered by luminance**. Swap a highlight and a shadow and the bead lights from the wrong side;
 * make two of them equal and it goes flat. Neither throws, neither shows up in a semantics tree,
 * and `:feature:tracker` (#613) draws the whole strand from these values without asserting on
 * them.
 *
 * The other property is that the six materials are actually distinguishable — they are a setting
 * the user chooses, so two that resolve to the same ramp is a choice that does nothing.
 */
class BeadColorsTest {

    private data class Material(
        val name: String,
        val highlight: Color,
        val mid: Color,
        val shadow: Color,
        val cord: Color,
    )

    private val materials = listOf(
        Material(
            "wood", BeadColors.WoodHighlight, BeadColors.WoodMid, BeadColors.WoodShadow,
            BeadColors.WoodCord,
        ),
        Material(
            "marble", BeadColors.MarbleHighlight, BeadColors.MarbleMid, BeadColors.MarbleShadow,
            BeadColors.MarbleCord,
        ),
        Material(
            "amethyst", BeadColors.AmethystHighlight, BeadColors.AmethystMid,
            BeadColors.AmethystShadow, BeadColors.AmethystCord,
        ),
        Material(
            "onyx", BeadColors.OnyxHighlight, BeadColors.OnyxMid, BeadColors.OnyxShadow,
            BeadColors.OnyxCord,
        ),
        Material(
            "pearl", BeadColors.PearlHighlight, BeadColors.PearlMid, BeadColors.PearlShadow,
            BeadColors.PearlCord,
        ),
        Material(
            "jade", BeadColors.JadeRestHighlight, BeadColors.JadeRestMid,
            BeadColors.JadeRestShadow, BeadColors.JadeCord,
        ),
    )

    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

    @Test
    fun `every material shades from highlight through mid to shadow`() {
        // The property that makes a circle read as a sphere. Ordered strictly, because two equal
        // stops draw a flat disc.
        materials.forEach { material ->
            assertThat(material.highlight.luminance())
                .isGreaterThan(material.mid.luminance())
            assertThat(material.mid.luminance())
                .isGreaterThan(material.shadow.luminance())
        }
    }

    @Test
    fun `the active gold bead shades the same way`() {
        // Gold is the crossing bead — the one under the user's thumb — and it takes its mid stop
        // from the shared palette rather than declaring its own.
        assertThat(BeadColors.GoldHighlight.luminance())
            .isGreaterThan(NimazPalette.Gold500.luminance())
        assertThat(NimazPalette.Gold500.luminance())
            .isGreaterThan(BeadColors.GoldShadow.luminance())
    }

    @Test
    fun `the imame shades the same way`() {
        // The lap marker — the bead that says a round of dhikr has closed.
        assertThat(BeadColors.JadeHighlight.luminance()).isGreaterThan(BeadColors.JadeMid.luminance())
        assertThat(BeadColors.JadeMid.luminance()).isGreaterThan(BeadColors.JadeShadow.luminance())
    }

    @Test
    fun `no two materials resolve to the same ramp`() {
        // The material is a setting the user picks. Two that render identically is a choice with
        // no effect, which reads as a broken setting rather than as a duplicated constant.
        val ramps = materials.map { Triple(it.highlight, it.mid, it.shadow) }

        assertThat(ramps.toSet()).hasSize(materials.size)
    }

    @Test
    fun `every cord is darker than the bead it threads`() {
        // The cord runs behind the beads; one lighter than the bead body would read as the strand
        // in front of them.
        materials.forEach { material ->
            assertThat(material.cord.luminance()).isLessThan(material.mid.luminance())
        }
    }

    @Test
    fun `the tray is darker than every bead on it`() {
        // The beads sit on the tray, and contrast against it is what makes the count readable at
        // a glance in the dark — which is when the tasbih is most used.
        materials.forEach { material ->
            assertThat(BeadColors.TrayBackground.luminance())
                .isLessThan(material.shadow.luminance())
        }
    }
}
