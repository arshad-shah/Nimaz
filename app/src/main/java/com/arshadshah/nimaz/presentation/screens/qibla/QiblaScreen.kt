package com.arshadshah.nimaz.presentation.screens.qibla

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaCalibrationDialog
import com.arshadshah.nimaz.presentation.components.organisms.qibla.ArQiblaView
import com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView
import com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaTopBar
import com.arshadshah.nimaz.presentation.viewmodel.QiblaEvent
import com.arshadshah.nimaz.presentation.viewmodel.QiblaViewModel

/**
 * Qibla screen — screen-level logic only. It owns the compass lifecycle, the
 * azimuth animation, the camera-permission flow and the Compass/AR mode switch.
 * All UI is delegated to the shared [QiblaTopBar], [CompassQiblaView] and
 * [ArQiblaView] components.
 */
@Composable
fun QiblaScreen(
    onNavigateBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val state by viewModel.qiblaState.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        viewModel.onEvent(QiblaEvent.StartCompass)
        onDispose {
            viewModel.onEvent(QiblaEvent.StopCompass)
        }
    }

    val animatedAzimuth by animateFloatAsState(
        targetValue = state.animatedAzimuth,
        animationSpec = tween(150),
        label = "compass_rotation"
    )

    // Camera permission state
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showCameraRationale by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) {
            viewModel.onEvent(QiblaEvent.SetArMode(true))
        } else {
            showCameraRationale = true
        }
    }

    fun requestArMode() {
        if (cameraPermissionGranted) {
            viewModel.onEvent(QiblaEvent.SetArMode(true))
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (state.showCalibrationDialog) {
        QiblaCalibrationDialog(
            accuracy = state.compassData.accuracy,
            onDismiss = { viewModel.onEvent(QiblaEvent.DismissCalibrationDialog) }
        )
    }

    if (showCameraRationale) {
        NimazConfirmDialog(
            title = stringResource(R.string.camera_permission_title),
            message = stringResource(R.string.camera_permission_message),
            confirmText = stringResource(R.string.grant_permission),
            cancelText = stringResource(R.string.not_now),
            titleIcon = Icons.Default.CameraAlt,
            onConfirm = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            onDismiss = { showCameraRationale = false },
        )
    }

    val isArMode = state.isArMode && cameraPermissionGranted

    NimazScreenScaffold(
        topBar = {
            QiblaTopBar(
                locationName = state.qiblaInfo?.locationName,
                latitude = state.qiblaInfo?.latitude,
                longitude = state.qiblaInfo?.longitude,
                fallbackTitle = stringResource(R.string.guide_qibla_title),
                tabs = listOf(stringResource(R.string.compass), stringResource(R.string.ar)),
                selectedIndex = if (isArMode) 1 else 0,
                onTabSelect = { index ->
                    if (index == 1) requestArMode()
                    else viewModel.onEvent(QiblaEvent.SetArMode(false))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    if (isArMode) Color.Black
                    else MaterialTheme.colorScheme.background
                )
        ) {
            // Error state
            if (state.error != null && state.qiblaInfo == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NimazIcon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        variant = NimazIconVariant.ERROR,
                        iconSize = 48.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: stringResource(R.string.unknown_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                return@Box
            }

            Crossfade(
                targetState = isArMode,
                animationSpec = tween(400),
                label = "qibla_mode_crossfade"
            ) { arMode ->
                if (arMode) {
                    ArQiblaView(
                        azimuth = state.compassData.azimuth,
                        qiblaInfo = state.qiblaInfo,
                        isFacingQibla = state.isFacingQibla,
                        rotationToQibla = state.rotationToQibla,
                        isCompassReady = state.isCompassReady,
                        compassAccuracy = state.compassData.accuracy,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CompassQiblaView(
                        qiblaBearing = (state.qiblaInfo?.direction?.bearing
                            ?: state.qiblaDirection?.bearing ?: 0.0).toFloat(),
                        animatedAzimuth = animatedAzimuth,
                        isFacingQibla = state.isFacingQibla,
                        rotationToQibla = state.rotationToQibla,
                        isCompassReady = state.isCompassReady,
                        accuracy = state.compassData.accuracy,
                        hasQiblaInfo = state.qiblaInfo != null,
                        onCalibrate = { viewModel.onEvent(QiblaEvent.ShowCalibrationDialog) }
                    )
                }
            }
        }
    }
}
