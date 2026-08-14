package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaFactCard
import com.arshadshah.nimaz.presentation.foundation.geometry.cardinalDirection
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.roundToInt

/**
 * A pair of [QiblaFactCard]s showing the Qibla bearing and the current device
 * heading side by side. Displayed below the compass dial on the main screen.
 */
@Composable
fun QiblaFactsRow(
    qiblaBearing: Float,
    currentAzimuth: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QiblaFactCard(
            label = stringResource(R.string.qibla_bearing_label),
            value = "${qiblaBearing.roundToInt()}° ${cardinalDirection(qiblaBearing)}",
            modifier = Modifier.weight(1f),
        )
        QiblaFactCard(
            label = stringResource(R.string.you_are_facing_label),
            value = "${currentAzimuth.roundToInt()}° ${cardinalDirection(currentAzimuth)}",
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Facts Row")
@Composable
private fun QiblaFactsRowPreview() {
    NimazTheme {
        QiblaFactsRow(qiblaBearing = 118f, currentAzimuth = 212f)
    }
}
