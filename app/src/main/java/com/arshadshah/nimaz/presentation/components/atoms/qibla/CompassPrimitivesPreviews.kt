package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

// Previews — individual atom layers

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Rings")
@Composable
private fun CompassRingsPreview() {
    NimazTheme {
        CompassRings(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Dial Face")
@Composable
private fun CompassDialFacePreview() {
    NimazTheme {
        CompassDialFace(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Needles")
@Composable
private fun CompassNeedlesPreview() {
    NimazTheme {
        CompassNeedles(
            qiblaScreenAngle = 45f,
            northScreenAngle = -30f,
            isFacingQibla = false,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Needles - Facing")
@Composable
private fun CompassNeedlesFacingPreview() {
    NimazTheme {
        CompassNeedles(
            qiblaScreenAngle = 0f,
            northScreenAngle = -119f,
            isFacingQibla = true,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Direction Markers")
@Composable
private fun DirectionMarkersPreview() {
    NimazTheme {
        DirectionMarkers(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, name = "Lubber Notch")
@Composable
private fun CompassLubberNotchPreview() {
    NimazTheme {
        CompassLubberNotch(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, name = "Center Dot")
@Composable
private fun CompassCenterDotPreview() {
    NimazTheme {
        CompassCenterDot(isFacingQibla = false, modifier = Modifier)
    }
}

@Preview(showBackground = true, name = "Center Dot - Facing")
@Composable
private fun CompassCenterDotFacingPreview() {
    NimazTheme {
        CompassCenterDot(isFacingQibla = true, modifier = Modifier)
    }
}

// Previews — full compass dial, assembled from the atoms above
// (mirrors how QiblaCompassWidget stacks the layers).

/** Preview-only assembly so the atom layers can be seen built up together. */
@Composable
private fun AssembledCompassDial(
    qiblaBearing: Float,
    isFacingQibla: Boolean,
    azimuth: Float,
    size: Dp = 280.dp,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        CompassRings(modifier = Modifier.fillMaxSize())
        CompassDialFace(modifier = Modifier.size(size - 50.dp))
        DirectionMarkers(modifier = Modifier.size(size - 20.dp))

        CompassNeedles(
            qiblaScreenAngle = qiblaBearing - azimuth,
            northScreenAngle = -azimuth,
            isFacingQibla = isFacingQibla,
            modifier = Modifier.size(size - 50.dp)
        )

        CompassCenterDot(isFacingQibla = isFacingQibla)

        CompassFacingGlow(visible = isFacingQibla, modifier = Modifier.size(size - 20.dp))

        CompassLubberNotch(modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, name = "Full Compass Dial")
@Composable
private fun FullCompassDialPreview() {
    NimazTheme {
        AssembledCompassDial(qiblaBearing = 119f, isFacingQibla = false, azimuth = 30f)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, name = "Full Compass Dial - Facing")
@Composable
private fun FullCompassDialFacingPreview() {
    NimazTheme {
        AssembledCompassDial(qiblaBearing = 0f, isFacingQibla = true, azimuth = 0f)
    }
}
