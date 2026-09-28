package com.arshadshah.nimaz.presentation.screens.qaida

import com.arshadshah.nimaz.presentation.components.molecules.ArticulationSite
import com.arshadshah.nimaz.presentation.components.molecules.articulationSite
import com.arshadshah.nimaz.presentation.components.molecules.articulationPose
import com.arshadshah.nimaz.presentation.components.molecules.ArticulationPose
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QaidaArticulationTest {
    @Test fun `all 29 alphabet glyphs have a guide while unknown content has none`() {
        "ابتثجحخدذرزسشصضطظعغفقكلمنهويء".forEach { assertThat(articulationSite(it.toString())).isNotNull() }
        assertThat(articulationSite("unknown")).isNull()
    }
    @Test fun `a hamza carrier is not taught as a long vowel`() {
        assertThat(articulationSite("أَ")).isEqualTo(ArticulationSite.DEEP_THROAT)
        assertThat(articulationSite("ا")).isEqualTo(ArticulationSite.CAVITY)
    }
    @Test fun `qaf and kaf keep their distinct back tongue locations`() {
        assertThat(articulationSite("ق")).isNotEqualTo(articulationSite("ك"))
    }
    @Test fun `consonant waw and ya do not use the long vowel illustration`() {
        assertThat(articulationSite("و")).isEqualTo(ArticulationSite.ROUNDED_LIPS)
        assertThat(articulationSite("ي")).isEqualTo(ArticulationSite.MID_TONGUE)
    }
    @Test fun `lip tooth contact and tongue side use distinct anatomical views`() {
        assertThat(articulationSite("ف")).isEqualTo(ArticulationSite.LOWER_LIP)
        assertThat(articulationSite("ض")).isEqualTo(ArticulationSite.TONGUE_SIDE)
        assertThat(articulationSite("ب")).isEqualTo(ArticulationSite.CLOSED_LIPS)
    }
    @Test fun `all letters have raster poses and the explorer cannot regress to a single reference`() {
        val poses = "ابتثجحخدذرزسشصضطظعغفقكلمنهويء".map { articulationPose(it.toString()) }
        assertThat(poses).doesNotContain(null)
        assertThat(poses.map { it!!.image }.distinct()).hasSize(24)
        assertThat(articulationPose("unknown")).isNull()
    }
    @Test fun `nasal glottal and emphatic distinctions survive shared articulation sites`() {
        listOf("ب" to "م", "ه" to "ء", "س" to "ص", "ت" to "ط", "ذ" to "ظ", "ق" to "ك").forEach { (a, b) ->
            assertThat(articulationPose(a)!!.image).isNotEqualTo(articulationPose(b)!!.image)
        }
    }
    @Test fun `diacritics and hamza carriers select the same consonant pose`() {
        assertThat(articulationPose("مَ")).isEqualTo(ArticulationPose.BILABIAL_NASAL)
        assertThat(articulationPose("أَ")).isEqualTo(ArticulationPose.GLOTTAL_CLOSED)
        assertThat(articulationPose("أَ")).isNotEqualTo(articulationPose("ا"))
    }
}
