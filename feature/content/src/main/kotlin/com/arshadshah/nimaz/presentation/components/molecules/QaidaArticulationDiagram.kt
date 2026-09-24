package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.Image
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

internal fun articulationSite(glyph: String): ArticulationSite? = when (glyph.filterNot { it in '\u064B'..'\u065F' || it == '\u0670' || it == '\u0640' }.trim()) {
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

/** A neutral anatomical reference, never a simulated phoneme posture. */
@Composable
fun QaidaArticulationDiagram(glyph: String, detail: String, modifier: Modifier = Modifier) {
    val site = articulationSite(glyph) ?: return
    val view = stringResource(R.string.qaida_anatomy_side)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(view, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Image(painterResource(R.drawable.qaida_anatomy_side_art),
            contentDescription = stringResource(R.string.qaida_anatomy_description, glyph, view, detail),
            modifier = Modifier.fillMaxWidth().aspectRatio(1.5f))
        Text(stringResource(R.string.qaida_anatomy_key), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        // The text identifies the target; the resting anatomy does not pretend to demonstrate it.
        Text(detail, style = MaterialTheme.typography.bodyMedium)
        if (site == ArticulationSite.ROUNDED_LIPS || glyph == "ي")
            Text(stringResource(R.string.qaida_anatomy_consonant), style = MaterialTheme.typography.bodySmall)
        if (glyph == "م" || glyph == "ن")
            Text(stringResource(R.string.qaida_anatomy_nasal), style = MaterialTheme.typography.bodySmall)
    }
}
