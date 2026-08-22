package com.arshadshah.nimaz.presentation.screens.qibla

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorAction
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaCalibrationSheet
import com.arshadshah.nimaz.presentation.components.organisms.qibla.ArQiblaView
import com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaTopBar
import com.arshadshah.nimaz.presentation.viewmodel.prayer.QiblaEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.QiblaViewModel

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
    val state by viewModel.qiblaState.collectAsStateWithLifecycle()
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
        QiblaCalibrationSheet(
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
                accuracy = state.compassData.accuracy,
                isArMode = isArMode,
                onCameraToggle = {
                    if (isArMode) viewModel.onEvent(QiblaEvent.SetArMode(false))
                    else requestArMode()
                },
                onCalibrate = { viewModel.onEvent(QiblaEvent.ShowCalibrationDialog) },
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
            // Only when there is no direction to draw: a compass already pointing somewhere
            // is more use than a message about a refresh that failed.
            val error = state.error
            if (error != null && state.qiblaInfo == null) {
                NimazErrorState(
                    title = stringResource(error.message),
                    message = stringResource(R.string.qibla_no_location_body),
                    kind = error.kind,
                    details = error.details,
                    // The screen offered no way out of this at all — a reader with no
                    // location set saw a red sentence telling them to go to settings, and
                    // no way to get there or to retry.
                    primaryAction = NimazErrorDefaults.retry(
                        onRetry = { viewModel.onEvent(QiblaEvent.RefreshLocation) },
                        label = stringResource(R.string.try_again),
                    ),
                    secondaryAction = NimazErrorAction(
                        label = stringResource(R.string.location_set_prompt),
                        onClick = { viewModel.onEvent(QiblaEvent.ShowLocationPicker) },
                    ),
                    modifier = Modifier.padding(20.dp),
                )
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
                        animatedAzimuth = animatedAzimuth,
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
