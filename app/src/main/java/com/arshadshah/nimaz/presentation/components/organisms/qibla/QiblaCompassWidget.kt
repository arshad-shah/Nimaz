package com.arshadshah.nimaz.presentation.components.organisms.qibla

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassCenterDot
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassDialFace
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassFacingGlow
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassLubberNotch
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassNeedles
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassRings
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CompassTurnArc
import com.arshadshah.nimaz.presentation.components.atoms.qibla.DirectionMarkers
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The full Qibla compass: stacks the compass primitive atoms into one widget.
 * Like a real handheld compass, the **face stays still** — the rings, degree
 * ticks and N/E/S/W cardinals never spin — while two **needles** pivot on the
 * center: a quiet red north needle and the gold Qibla needle. The caller bakes
 * [animatedAzimuth] into each needle's screen angle, so turning the phone swings
 * the needles, not the dial. Facing the Qibla = the gold needle reaches the top
 * "AIM" notch. Shared by the compact and tablet compass layouts.
 */
@Composable
fun QiblaCompassWidget(
    qiblaBearing: Float,
    isFacingQibla: Boolean,
    animatedAzimuth: Float,
    rotationToQibla: Float,
    modifier: Modifier = Modifier,
    compassSize: Dp = 360.dp,
) {
    Box(
        modifier = modifier.size(compassSize),
        contentAlignment = Alignment.Center
    ) {
        CompassRings(modifier = Modifier.fillMaxSize())
        CompassDialFace(modifier = Modifier.size(compassSize - 35.dp))
        DirectionMarkers(modifier = Modifier.size(compassSize - 20.dp))

        CompassTurnArc(
            rotationToQibla = rotationToQibla,
            isFacingQibla = isFacingQibla,
            modifier = Modifier.fillMaxSize(),
        )

        CompassNeedles(
            qiblaScreenAngle = qiblaBearing - animatedAzimuth,
            northScreenAngle = -animatedAzimuth,
            isFacingQibla = isFacingQibla,
            modifier = Modifier.size(compassSize - 50.dp)
        )

        CompassCenterDot(isFacingQibla = isFacingQibla)

        CompassFacingGlow(
            visible = isFacingQibla,
            modifier = Modifier.size(compassSize - 20.dp)
        )

        CompassLubberNotch(modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true, widthDp = 340, heightDp = 340, name = "Qibla Compass - Seeking")
@Composable
private fun QiblaCompassWidgetPreview() {
    NimazTheme {
        QiblaCompassWidget(
            qiblaBearing = 119f,
            isFacingQibla = false,
            animatedAzimuth = 30f,
            rotationToQibla = 34f,
            compassSize = 300.dp
        )
    }
}

@Preview(showBackground = true, widthDp = 340, heightDp = 340, name = "Qibla Compass - Facing")
@Composable
private fun QiblaCompassWidgetFacingPreview() {
    NimazTheme {
        QiblaCompassWidget(
            qiblaBearing = 0f,
            isFacingQibla = true,
            animatedAzimuth = 0f,
            rotationToQibla = 0f,
            compassSize = 300.dp
        )
    }
}
