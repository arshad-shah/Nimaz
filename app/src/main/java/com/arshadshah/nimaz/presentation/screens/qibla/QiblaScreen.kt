package com.arshadshah.nimaz.presentation.screens.qibla

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.QiblaEvent
import com.arshadshah.nimaz.presentation.viewmodel.QiblaViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onNavigateBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val state by viewModel.qiblaState.collectAsState()
    val context = LocalContext.current

    val animatedAzimuth by animateFloatAsState(
        targetValue = state.animatedAzimuth,
        animationSpec = tween(150),
        label = "compass_rotation"
    )

    val goldColor = Color(0xFFEAB308)
    val greenColor = Color(0xFF22C55E)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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

    if (state.showCalibrationDialog) {
        CalibrationDialog(
            accuracy = state.compassData.accuracy,
            onDismiss = { viewModel.onEvent(QiblaEvent.DismissCalibrationDialog) }
        )
    }

    // Camera rationale dialog
    if (showCameraRationale) {
        AlertDialog(
            onDismissRequest = { showCameraRationale = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Camera access is needed to show the AR Qibla view. The camera feed is used to overlay the Qibla direction on the real world. No images are captured or stored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCameraRationale = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraRationale = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    if (state.isArMode) Color.Black
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
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                return@Box
            }

            // Main content with Crossfade
            Crossfade(
                targetState = state.isArMode,
                animationSpec = tween(400),
                label = "qibla_mode_crossfade"
            ) { isArMode ->
                if (isArMode && cameraPermissionGranted) {
                    // AR View
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
                    // Compass View
                    CompassQiblaView(
                        state = state,
                        animatedAzimuth = animatedAzimuth,
                        goldColor = goldColor,
                        greenColor = greenColor,
                        onCalibrate = { viewModel.onEvent(QiblaEvent.ShowCalibrationDialog) }
                    )
                }
            }

            // Mode toggle - floating at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = if (state.isArMode) 64.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                NimazPillTabs(
                    tabs = listOf("Compass", "AR"),
                    selectedIndex = if (state.isArMode) 1 else 0,
                    onTabSelect = { index ->
                        if (index == 1) {
                            // Switching to AR mode
                            if (cameraPermissionGranted) {
                                viewModel.onEvent(QiblaEvent.SetArMode(true))
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            viewModel.onEvent(QiblaEvent.SetArMode(false))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CompassQiblaView(
    state: com.arshadshah.nimaz.presentation.viewmodel.QiblaUiState,
    animatedAzimuth: Float,
    goldColor: Color,
    greenColor: Color,
    onCalibrate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Info
        state.qiblaInfo?.let { info ->
            Text(
                text = info.locationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format("%.4f", info.latitude)}\u00B0 ${if (info.latitude >= 0) "N" else "S"}, ${
                    String.format("%.4f", abs(info.longitude))
                }\u00B0 ${if (info.longitude >= 0) "E" else "W"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Compass
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CompassRings(modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .rotate(-animatedAzimuth),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    qiblaBearing = state.qiblaDirection?.bearing?.toFloat() ?: 0f,
                    isFacingQibla = state.isFacingQibla,
                    goldColor = goldColor,
                    modifier = Modifier.size(250.dp)
                )
                DirectionMarkers(modifier = Modifier.fillMaxSize())
            }

            // Center dot
            Box(
                modifier = Modifier
                    .size(if (state.isFacingQibla) 28.dp else 20.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.isFacingQibla) greenColor
                        else MaterialTheme.colorScheme.outline
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isFacingQibla) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Facing Qibla glow overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = state.isFacingQibla,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Canvas(modifier = Modifier.size(280.dp)) {
                    drawCircle(
                        color = greenColor.copy(alpha = 0.15f),
                        radius = size.minDimension / 2
                    )
                }
            }

            // Static north indicator
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, 0f)
                val triSize = 8.dp.toPx()
                val path = Path().apply {
                    moveTo(center.x, triSize + 2.dp.toPx())
                    lineTo(center.x - triSize / 2, 2.dp.toPx())
                    lineTo(center.x + triSize / 2, 2.dp.toPx())
                    close()
                }
                drawPath(path = path, color = Color(0xFFEF4444))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Facing Qibla banner
        AnimatedVisibility(
            visible = state.isFacingQibla,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = greenColor.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = greenColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Facing Qibla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = greenColor
                    )
                }
            }
        }

        // Turn direction hint
        AnimatedVisibility(
            visible = !state.isFacingQibla && state.isCompassReady,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val turnRight = state.rotationToQibla > 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (turnRight)
                        Icons.AutoMirrored.Filled.RotateRight
                    else
                        Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Turn ${if (turnRight) "right" else "left"} ${abs(state.rotationToQibla).toInt()}\u00B0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Qibla Direction Info
        state.qiblaInfo?.let { info ->
            Text(
                text = "${info.direction.bearing.toInt()}\u00B0",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                fontWeight = FontWeight.Bold,
                color = if (state.isFacingQibla) greenColor else goldColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${getCompassDirection(info.direction.bearing)} \u2022 Qibla Direction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Distance pill
            Surface(
                shape = RoundedCornerShape(25.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format("%,d", (info.distanceToMecca / 1000).toInt())} km to Mecca",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Accuracy Bar
        AccuracyBar(
            accuracy = state.compassData.accuracy,
            greenColor = greenColor,
            onCalibrate = onCalibrate,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CalibrationDialog(
    accuracy: CompassAccuracy,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Calibrate Compass",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Current accuracy: ${accuracy.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "To improve compass accuracy:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                CalibrationStep("1", "Hold your phone away from metal objects and magnets")
                CalibrationStep("2", "Slowly move your phone in a figure-8 pattern")
                CalibrationStep("3", "Rotate the figure-8 in all three axes")
                CalibrationStep("4", "Repeat until accuracy improves to Medium or High")
                Text(
                    text = "Tip: If accuracy remains low, try moving to a different location away from electronic devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun CalibrationStep(
    number: String,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompassRings(modifier: Modifier = Modifier) {
    val outerRingColor = MaterialTheme.colorScheme.outlineVariant
    val innerRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2

        drawCircle(
            color = outerRingColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = innerRingColor,
            radius = outerRadius - 10.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun DirectionMarkers(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val northColor = Color(0xFFEF4444) // Red for North
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        val directions = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        directions.forEach { (label, angleDeg) ->
            val isNorth = label == "N"
            val textColor = if (isNorth) northColor else onSurfaceVariantColor
            val style = TextStyle(
                fontSize = if (isNorth) 16.sp else 14.sp,
                fontWeight = if (isNorth) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor
            )
            val textResult = textMeasurer.measure(label, style)
            val angle = Math.toRadians(angleDeg.toDouble())
            val markerRadius = radius - 30.dp.toPx()
            val x = center.x + (markerRadius * sin(angle)).toFloat() - textResult.size.width / 2
            val y = center.y - (markerRadius * cos(angle)).toFloat() - textResult.size.height / 2

            drawText(textLayoutResult = textResult, topLeft = Offset(x, y))
        }
    }
}

@Composable
private fun CompassDial(
    qiblaBearing: Float,
    isFacingQibla: Boolean,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    val dialBackground = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainer
        )
    )
    val tickColorMajor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val tickColorMinor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val arrowColor = if (isFacingQibla) Color(0xFF22C55E) else goldColor

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        drawCircle(brush = dialBackground, radius = radius, center = center)

        for (i in 0 until 360 step 5) {
            val isMajor = i % 30 == 0
            val tickLength = if (isMajor) 15.dp.toPx() else 8.dp.toPx()
            val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            val tickColor = if (isMajor) tickColorMajor else tickColorMinor
            val angle = Math.toRadians(i.toDouble())
            val startR = radius - tickLength
            val endR = radius

            drawLine(
                color = tickColor,
                start = Offset(
                    center.x + (startR * sin(angle)).toFloat(),
                    center.y - (startR * cos(angle)).toFloat()
                ),
                end = Offset(
                    center.x + (endR * sin(angle)).toFloat(),
                    center.y - (endR * cos(angle)).toFloat()
                ),
                strokeWidth = tickWidth
            )
        }

        // Qibla arrow
        val qiblaAngleRad = Math.toRadians(qiblaBearing.toDouble())
        val arrowLength = radius - 30.dp.toPx()
        val arrowBaseWidth = 15.dp.toPx()

        val tipX = center.x + (arrowLength * sin(qiblaAngleRad)).toFloat()
        val tipY = center.y - (arrowLength * cos(qiblaAngleRad)).toFloat()

        val perpAngle = qiblaAngleRad + Math.PI / 2
        val baseLeftX = center.x + (arrowBaseWidth * sin(perpAngle)).toFloat()
        val baseLeftY = center.y - (arrowBaseWidth * cos(perpAngle)).toFloat()
        val baseRightX = center.x - (arrowBaseWidth * sin(perpAngle)).toFloat()
        val baseRightY = center.y + (arrowBaseWidth * cos(perpAngle)).toFloat()

        val arrowPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseLeftX, baseLeftY)
            lineTo(baseRightX, baseRightY)
            close()
        }

        drawPath(path = arrowPath, color = arrowColor)

        // Kaaba icon at tip
        val kaabaSize = 16.dp.toPx()
        val kaabaOffset = 10.dp.toPx()
        val kaabaCenterX = center.x + ((arrowLength + kaabaOffset) * sin(qiblaAngleRad)).toFloat()
        val kaabaCenterY = center.y - ((arrowLength + kaabaOffset) * cos(qiblaAngleRad)).toFloat()

        drawRect(
            color = arrowColor,
            topLeft = Offset(kaabaCenterX - kaabaSize / 2, kaabaCenterY - kaabaSize / 2),
            size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize),
            style = Stroke(width = 1.5f.dp.toPx())
        )
    }
}

@Composable
private fun AccuracyBar(
    accuracy: CompassAccuracy,
    greenColor: Color,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf(
        CompassAccuracy.UNRELIABLE,
        CompassAccuracy.LOW,
        CompassAccuracy.MEDIUM,
        CompassAccuracy.HIGH
    )
    val activeIndex = levels.indexOf(accuracy)

    val (label, color, hint) = when (accuracy) {
        CompassAccuracy.HIGH -> Triple("High", greenColor, "Compass is accurate")
        CompassAccuracy.MEDIUM -> Triple("Medium", Color(0xFFFACC15), "Accuracy is acceptable")
        CompassAccuracy.LOW -> Triple("Low", MaterialTheme.colorScheme.error, "Calibration recommended")
        CompassAccuracy.UNRELIABLE -> Triple(
            "Unreliable",
            MaterialTheme.colorScheme.error,
            "Calibration needed"
        )
    }

    val needsCalibration = accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.UNRELIABLE

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (needsCalibration) color.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compass Accuracy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }

            // Segmented accuracy indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                levels.forEachIndexed { index, _ ->
                    val segmentColor = if (index <= activeIndex) color else
                        MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(segmentColor)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (needsCalibration) Icons.Default.Warning
                    else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (needsCalibration) {
                    Button(
                        onClick = onCalibrate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Calibrate",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun getCompassDirection(bearing: Double): String {
    return when {
        bearing < 22.5 || bearing >= 337.5 -> "North"
        bearing < 67.5 -> "Northeast"
        bearing < 112.5 -> "East"
        bearing < 157.5 -> "Southeast"
        bearing < 202.5 -> "South"
        bearing < 247.5 -> "Southwest"
        bearing < 292.5 -> "West"
        else -> "Northwest"
    }
}

// Previews

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - High")
@Composable
private fun AccuracyBarHighPreview() {
    NimazTheme {
        AccuracyBar(
            accuracy = CompassAccuracy.HIGH,
            greenColor = Color(0xFF22C55E),
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - Medium")
@Composable
private fun AccuracyBarMediumPreview() {
    NimazTheme {
        AccuracyBar(
            accuracy = CompassAccuracy.MEDIUM,
            greenColor = Color(0xFF22C55E),
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - Low")
@Composable
private fun AccuracyBarLowPreview() {
    NimazTheme {
        AccuracyBar(
            accuracy = CompassAccuracy.LOW,
            greenColor = Color(0xFF22C55E),
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - Unreliable")
@Composable
private fun AccuracyBarUnreliablePreview() {
    NimazTheme {
        AccuracyBar(
            accuracy = CompassAccuracy.UNRELIABLE,
            greenColor = Color(0xFF22C55E),
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Calibration Dialog")
@Composable
private fun CalibrationDialogPreview() {
    NimazTheme {
        CalibrationDialog(
            accuracy = CompassAccuracy.LOW,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Rings")
@Composable
private fun CompassRingsPreview() {
    NimazTheme {
        CompassRings(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Dial")
@Composable
private fun CompassDialPreview() {
    NimazTheme {
        CompassDial(
            qiblaBearing = 45f,
            isFacingQibla = false,
            goldColor = Color(0xFFD4A853),
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Calibration Step")
@Composable
private fun CalibrationStepPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalibrationStep("1", "Hold your phone away from metal objects and magnets")
            CalibrationStep("2", "Slowly move your phone in a figure-8 pattern")
        }
    }
}
