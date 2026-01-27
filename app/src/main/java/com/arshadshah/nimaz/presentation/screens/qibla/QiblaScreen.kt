package com.arshadshah.nimaz.presentation.screens.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.QiblaEvent
import com.arshadshah.nimaz.presentation.viewmodel.QiblaViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onNavigateBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val state by viewModel.qiblaState.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val animatedRotation by animateFloatAsState(
        targetValue = state.rotationToQibla,
        animationSpec = tween(300),
        label = "compass_rotation"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Qibla Direction",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(QiblaEvent.ShowLocationPicker) }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location Info
            state.qiblaInfo?.let { info ->
                LocationInfoCard(
                    locationName = info.locationName,
                    bearing = info.direction.bearing,
                    distance = info.distanceToMecca
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Compass
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Compass Background
                CompassBackground(
                    modifier = Modifier.fillMaxSize()
                )

                // Compass Rose
                CompassRose(
                    rotation = -state.compassData.azimuth,
                    modifier = Modifier
                        .size(280.dp)
                        .rotate(-state.compassData.azimuth)
                )

                // Qibla Indicator
                QiblaIndicator(
                    qiblaBearing = state.qiblaDirection?.bearing?.toFloat() ?: 0f,
                    currentAzimuth = state.compassData.azimuth,
                    isFacingQibla = state.isFacingQibla,
                    modifier = Modifier.size(280.dp)
                )

                // Center Mosque Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isFacingQibla) {
                                NimazColors.StatusColors.Prayed.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = "Qibla",
                        tint = if (state.isFacingQibla) {
                            NimazColors.StatusColors.Prayed
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text
            if (state.isFacingQibla) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NimazColors.StatusColors.Prayed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "You are facing the Qibla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.StatusColors.Prayed,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            } else {
                Text(
                    text = "Turn ${if (state.rotationToQibla > 0) "right" else "left"} ${kotlin.math.abs(state.rotationToQibla).toInt()}°",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calibration Warning
            if (state.needsCalibration) {
                CalibrationWarning()
            }

            // Accuracy Info
            AccuracyInfo(accuracy = state.compassData.accuracy)
        }
    }
}

@Composable
private fun LocationInfoCard(
    locationName: String,
    bearing: Double,
    distance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(distance / 1000).toInt()} km to Mecca",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${bearing.toInt()}°",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.Primary
                )
                Text(
                    text = getCompassDirection(bearing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompassBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    NimazColors.Primary.copy(alpha = 0.1f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Main circle
        drawCircle(
            color = NimazColors.Primary.copy(alpha = 0.2f),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun CompassRose(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 20.dp.toPx()

        // Draw cardinal directions
        val directions = listOf("N", "E", "S", "W")
        directions.forEachIndexed { index, _ ->
            val angle = Math.toRadians((index * 90).toDouble())
            val endX = center.x + (radius * sin(angle)).toFloat()
            val endY = center.y - (radius * cos(angle)).toFloat()

            drawLine(
                color = if (index == 0) NimazColors.StatusColors.Missed else Color.Gray,
                start = center,
                end = Offset(
                    center.x + ((radius - 30.dp.toPx()) * sin(angle)).toFloat(),
                    center.y - ((radius - 30.dp.toPx()) * cos(angle)).toFloat()
                ),
                strokeWidth = if (index == 0) 4.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw minor ticks
        for (i in 0 until 360 step 15) {
            if (i % 90 != 0) {
                val angle = Math.toRadians(i.toDouble())
                val startRadius = radius - 10.dp.toPx()
                val endRadius = radius

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(
                        center.x + (startRadius * sin(angle)).toFloat(),
                        center.y - (startRadius * cos(angle)).toFloat()
                    ),
                    end = Offset(
                        center.x + (endRadius * sin(angle)).toFloat(),
                        center.y - (endRadius * cos(angle)).toFloat()
                    ),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun QiblaIndicator(
    qiblaBearing: Float,
    currentAzimuth: Float,
    isFacingQibla: Boolean,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (isFacingQibla) {
        NimazColors.StatusColors.Prayed
    } else {
        NimazColors.Primary
    }

    Canvas(modifier = modifier.rotate(-currentAzimuth + qiblaBearing)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 40.dp.toPx()

        // Draw Qibla arrow pointing up (which will be rotated to correct direction)
        drawLine(
            color = indicatorColor,
            start = center,
            end = Offset(center.x, center.y - radius),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Arrow head
        val arrowHeadSize = 20.dp.toPx()
        val arrowTip = Offset(center.x, center.y - radius)

        drawLine(
            color = indicatorColor,
            start = arrowTip,
            end = Offset(arrowTip.x - arrowHeadSize / 2, arrowTip.y + arrowHeadSize),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = indicatorColor,
            start = arrowTip,
            end = Offset(arrowTip.x + arrowHeadSize / 2, arrowTip.y + arrowHeadSize),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CalibrationWarning(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.StatusColors.Late.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NimazColors.StatusColors.Late,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Column {
                Text(
                    text = "Calibration Needed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.StatusColors.Late
                )
                Text(
                    text = "Move your phone in a figure-8 pattern",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccuracyInfo(
    accuracy: CompassAccuracy,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (accuracy) {
        CompassAccuracy.HIGH -> "High Accuracy" to NimazColors.StatusColors.Prayed
        CompassAccuracy.MEDIUM -> "Medium Accuracy" to NimazColors.StatusColors.Late
        CompassAccuracy.LOW -> "Low Accuracy" to NimazColors.StatusColors.Missed
        CompassAccuracy.UNRELIABLE -> "Unreliable" to Color.Gray
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

private fun getCompassDirection(bearing: Double): String {
    return when {
        bearing < 22.5 || bearing >= 337.5 -> "N"
        bearing < 67.5 -> "NE"
        bearing < 112.5 -> "E"
        bearing < 157.5 -> "SE"
        bearing < 202.5 -> "S"
        bearing < 247.5 -> "SW"
        bearing < 292.5 -> "W"
        else -> "NW"
    }
}
