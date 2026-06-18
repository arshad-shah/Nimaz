package com.arshadshah.nimaz.presentation.components.organisms.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaStatusCapsule
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaAccuracyBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlin.math.roundToInt

/**
 * The themed compass view: the rotating [QiblaCompassWidget], the shared
 * [QiblaStatusCapsule] and the [QiblaAccuracyBar]. Responsive — a single
 * column on phones, a compass-left / info-right split on tablets. The location
 * lives in the top bar, so it is not repeated here.
 */
@Composable
fun CompassQiblaView(
    qiblaBearing: Float,
    animatedAzimuth: Float,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    isCompassReady: Boolean,
    accuracy: CompassAccuracy,
    hasQiblaInfo: Boolean,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = currentWindowSizeClass().isCompact

    if (compact) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            QiblaCompassWidget(
                qiblaBearing = qiblaBearing,
                isFacingQibla = isFacingQibla,
                animatedAzimuth = animatedAzimuth,
                compassSize = 300.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (hasQiblaInfo) {
                QiblaStatusCapsule(
                    qiblaBearing = qiblaBearing.roundToInt(),
                    isFacingQibla = isFacingQibla,
                    rotationToQibla = rotationToQibla,
                    isCompassReady = isCompassReady
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            QiblaAccuracyBar(
                accuracy = accuracy,
                onCalibrate = onCalibrate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QiblaCompassWidget(
                    qiblaBearing = qiblaBearing,
                    isFacingQibla = isFacingQibla,
                    animatedAzimuth = animatedAzimuth,
                    compassSize = 360.dp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasQiblaInfo) {
                    QiblaStatusCapsule(
                        qiblaBearing = qiblaBearing.roundToInt(),
                        isFacingQibla = isFacingQibla,
                        rotationToQibla = rotationToQibla,
                        isCompassReady = isCompassReady
                    )
                }
                QiblaAccuracyBar(
                    accuracy = accuracy,
                    onCalibrate = onCalibrate,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 720, name = "Compass View - Phone")
@Composable
private fun CompassQiblaViewPhonePreview() {
    NimazTheme {
        CompassQiblaView(
            qiblaBearing = 119f,
            animatedAzimuth = 30f,
            isFacingQibla = false,
            rotationToQibla = 12f,
            isCompassReady = true,
            accuracy = CompassAccuracy.HIGH,
            hasQiblaInfo = true,
            onCalibrate = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 500, name = "Compass View - Tablet")
@Composable
private fun CompassQiblaViewTabletPreview() {
    NimazTheme {
        CompassQiblaView(
            qiblaBearing = 0f,
            animatedAzimuth = 0f,
            isFacingQibla = true,
            rotationToQibla = 0f,
            isCompassReady = true,
            accuracy = CompassAccuracy.MEDIUM,
            hasQiblaInfo = true,
            onCalibrate = {}
        )
    }
}
