package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.feature.content.R

/** Locations, not a motion simulation. Shared makharij deliberately share a location. */
internal enum class ArticulationSite {
    CAVITY, DEEP_THROAT, MID_THROAT, UPPER_THROAT, BACK_TONGUE, FORWARD_BACK_TONGUE,
    MID_TONGUE, TONGUE_SIDE, TONGUE_EDGE, TONGUE_TIP, TONGUE_TIP_BACK,
    UPPER_GUM, LOWER_TEETH, UPPER_TEETH_EDGE, LOWER_LIP, CLOSED_LIPS, ROUNDED_LIPS
}

private fun baseGlyph(glyph: String) = glyph.filterNot { it in '\u064B'..'\u065F' || it == '\u0670' || it == '\u0640' }.trim()

internal fun articulationSite(glyph: String): ArticulationSite? = when (baseGlyph(glyph)) {
    "ا", "ى" -> ArticulationSite.CAVITY
    "ء", "أ", "إ", "ؤ", "ئ", "ه" -> ArticulationSite.DEEP_THROAT
    "ع", "ح" -> ArticulationSite.MID_THROAT
    "غ", "خ" -> ArticulationSite.UPPER_THROAT
    "ق" -> ArticulationSite.BACK_TONGUE
    "ك" -> ArticulationSite.FORWARD_BACK_TONGUE
    "ج", "ش", "ي" -> ArticulationSite.MID_TONGUE
    "ض" -> ArticulationSite.TONGUE_SIDE
    "ل" -> ArticulationSite.TONGUE_EDGE
    "ن" -> ArticulationSite.TONGUE_TIP
    "ر" -> ArticulationSite.TONGUE_TIP_BACK
    "ط", "د", "ت" -> ArticulationSite.UPPER_GUM
    "ص", "ز", "س" -> ArticulationSite.LOWER_TEETH
    "ظ", "ذ", "ث" -> ArticulationSite.UPPER_TEETH_EDGE
    "ف" -> ArticulationSite.LOWER_LIP
    "ب", "م" -> ArticulationSite.CLOSED_LIPS
    "و" -> ArticulationSite.ROUNDED_LIPS
    else -> null
}

/** Raster poses encode both place and visible posture; shared places need not share a pose. */
internal enum class ArticulationPose(val image: Int, val view: Int = R.string.qaida_anatomy_side) {
    CAVITY(R.drawable.qaida_pose_cavity),
    BILABIAL(R.drawable.qaida_pose_bilabial),
    DENTAL_STOP(R.drawable.qaida_pose_dental_stop),
    INTERDENTAL(R.drawable.qaida_pose_interdental),
    POSTALVEOLAR_STOP(R.drawable.qaida_pose_postalveolar_stop),
    PHARYNGEAL(R.drawable.qaida_pose_pharyngeal),
    UVULAR_FRICATIVE(R.drawable.qaida_pose_uvular_fricative),
    TAP(R.drawable.qaida_pose_tap),
    SIBILANT(R.drawable.qaida_pose_sibilant),
    POSTALVEOLAR_FRICATIVE(R.drawable.qaida_pose_postalveolar_fricative),
    EMPHATIC_SIBILANT(R.drawable.qaida_pose_emphatic_sibilant),
    LATERAL_EMPHATIC(R.drawable.qaida_pose_lateral_emphatic, R.string.qaida_anatomy_top),
    EMPHATIC_STOP(R.drawable.qaida_pose_emphatic_stop),
    EMPHATIC_INTERDENTAL(R.drawable.qaida_pose_emphatic_interdental),
    LABIODENTAL(R.drawable.qaida_pose_labiodental),
    UVULAR_STOP(R.drawable.qaida_pose_uvular_stop),
    VELAR_STOP(R.drawable.qaida_pose_velar_stop),
    LATERAL(R.drawable.qaida_pose_lateral),
    BILABIAL_NASAL(R.drawable.qaida_pose_bilabial_nasal),
    ALVEOLAR_NASAL(R.drawable.qaida_pose_alveolar_nasal),
    GLOTTAL_OPEN(R.drawable.qaida_pose_glottal_open),
    LABIAL_VELAR(R.drawable.qaida_pose_labial_velar, R.string.qaida_anatomy_side_front),
    PALATAL(R.drawable.qaida_pose_palatal),
    GLOTTAL_CLOSED(R.drawable.qaida_pose_glottal_closed),
}

internal fun articulationPose(glyph: String): ArticulationPose? = when (baseGlyph(glyph)) {
    "ا", "ى" -> ArticulationPose.CAVITY
    "ب" -> ArticulationPose.BILABIAL
    "ت", "د" -> ArticulationPose.DENTAL_STOP
    "ث", "ذ" -> ArticulationPose.INTERDENTAL
    "ج" -> ArticulationPose.POSTALVEOLAR_STOP
    "ح", "ع" -> ArticulationPose.PHARYNGEAL
    "خ", "غ" -> ArticulationPose.UVULAR_FRICATIVE
    "ر" -> ArticulationPose.TAP
    "ز", "س" -> ArticulationPose.SIBILANT
    "ش" -> ArticulationPose.POSTALVEOLAR_FRICATIVE
    "ص" -> ArticulationPose.EMPHATIC_SIBILANT
    "ض" -> ArticulationPose.LATERAL_EMPHATIC
    "ط" -> ArticulationPose.EMPHATIC_STOP
    "ظ" -> ArticulationPose.EMPHATIC_INTERDENTAL
    "ف" -> ArticulationPose.LABIODENTAL
    "ق" -> ArticulationPose.UVULAR_STOP
    "ك" -> ArticulationPose.VELAR_STOP
    "ل" -> ArticulationPose.LATERAL
    "م" -> ArticulationPose.BILABIAL_NASAL
    "ن" -> ArticulationPose.ALVEOLAR_NASAL
    "ه" -> ArticulationPose.GLOTTAL_OPEN
    "و" -> ArticulationPose.LABIAL_VELAR
    "ي" -> ArticulationPose.PALATAL
    "ء", "أ", "إ", "ؤ", "ئ" -> ArticulationPose.GLOTTAL_CLOSED
    else -> null
}

/** Highlighted, schematic articulation positions; see the corpus guidance for each sound. */
@Composable
fun QaidaArticulationDiagram(glyph: String, detail: String, modifier: Modifier = Modifier) {
    val pose = articulationPose(glyph) ?: return
    val view = stringResource(pose.view)
    val base = baseGlyph(glyph)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(view, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Image(painterResource(pose.image),
            contentDescription = stringResource(R.string.qaida_anatomy_description, glyph, view, detail),
            modifier = Modifier.fillMaxWidth().aspectRatio(1200f / 792f).clip(RoundedCornerShape(20.dp)))
        Text(stringResource(R.string.qaida_anatomy_key), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (base == "و" || base == "ي")
            Text(stringResource(R.string.qaida_anatomy_consonant), style = MaterialTheme.typography.bodySmall)
        if (base == "م" || base == "ن")
            Text(stringResource(R.string.qaida_anatomy_nasal), style = MaterialTheme.typography.bodySmall)
    }
}
