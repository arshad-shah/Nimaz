package com.arshadshah.nimaz.presentation.components.organisms.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaCalibrationBanner
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaCompassWidget
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaFactsRow
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaInstructionRow
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact

/**
 * The themed compass view: the rotating [QiblaCompassWidget], [QiblaInstructionRow],
 * [QiblaFactsRow], and [QiblaCalibrationBanner]. Responsive — a single column on
 * phones, a compass-left / info-right split on tablets. The location lives in the
 * top bar, so it is not repeated here.
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
    val needsCalibration = accuracy == CompassAccuracy.UNRELIABLE || accuracy == CompassAccuracy.LOW
    val compact = currentWindowSizeClass().isCompact

    if (compact) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            if (hasQiblaInfo) {
                QiblaInstructionRow(
                    isFacingQibla = isFacingQibla,
                    rotationToQibla = rotationToQibla,
                    currentAzimuth = animatedAzimuth,
                    isCompassReady = isCompassReady,
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                QiblaCompassWidget(
                    qiblaBearing = qiblaBearing,
                    isFacingQibla = isFacingQibla,
                    animatedAzimuth = animatedAzimuth,
                    rotationToQibla = rotationToQibla,
                    compassSize = 320.dp,
                )
            }

            Spacer(Modifier.height(12.dp))

            if (hasQiblaInfo) {
                QiblaFactsRow(
                    qiblaBearing = qiblaBearing,
                    currentAzimuth = animatedAzimuth,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            if (needsCalibration) {
                QiblaCalibrationBanner(
                    onCalibrate = onCalibrate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                QiblaCompassWidget(
                    qiblaBearing = qiblaBearing,
                    isFacingQibla = isFacingQibla,
                    animatedAzimuth = animatedAzimuth,
                    rotationToQibla = rotationToQibla,
                    compassSize = 360.dp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (hasQiblaInfo) {
                    QiblaInstructionRow(
                        isFacingQibla = isFacingQibla,
                        rotationToQibla = rotationToQibla,
                        currentAzimuth = animatedAzimuth,
                        isCompassReady = isCompassReady,
                    )
                    QiblaFactsRow(
                        qiblaBearing = qiblaBearing,
                        currentAzimuth = animatedAzimuth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (needsCalibration) {
                    QiblaCalibrationBanner(
                        onCalibrate = onCalibrate,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
