package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaInfo
import kotlin.math.roundToInt

/**
 * Bottom HUD for the AR Qibla view: a vertically-stacked column containing
 * [QiblaStatusCapsule] and [QiblaAccuracyPill], centred horizontally with
 * navigation-bar padding applied.
 */
@Composable
fun QiblaArHud(
    qiblaInfo: QiblaInfo,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    isCompassReady: Boolean,
    compassAccuracy: CompassAccuracy,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QiblaStatusCapsule(
            qiblaBearing = qiblaInfo.direction.bearing.roundToInt(),
            isFacingQibla = isFacingQibla,
            rotationToQibla = rotationToQibla,
            isCompassReady = isCompassReady,
            onCamera = true
        )
        QiblaAccuracyPill(accuracy = compassAccuracy)
    }
}
