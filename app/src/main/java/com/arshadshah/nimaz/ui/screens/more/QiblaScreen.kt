package com.arshadshah.nimaz.ui.screens.qibla

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_INPUT
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QIBLA
import com.arshadshah.nimaz.ui.components.compass.rememberSensorData
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QiblaViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun QiblaScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = QIBLA_VIEWMODEL_KEY,
        initializer = { QiblaViewModel(context) },
        viewModelStoreOwner = context as ComponentActivity
    )

    val qiblaState by viewModel.qiblaState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sharedPreferences = PrivateSharedPreferences(context)
    val selectedImageIndex =
        remember { mutableStateOf(sharedPreferences.getDataInt("QiblaImageIndex")) }
    val compassImage = painterResource(qiblaImages[selectedImageIndex.value] ?: R.drawable.qibla1)

    LaunchedEffect(Unit) {
        viewModel.loadQibla(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .testTag(TEST_TAG_QIBLA)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QiblaHeader(qiblaState, isLoading, errorMessage)

            AnimatedVisibility(
                visible = !isLoading && errorMessage.isEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                QiblaCompass(qiblaState, isLoading, errorMessage, compassImage)
            }

            QiblaImageSelector(
                selectedIndex = selectedImageIndex.value,
                onImageSelected = { index ->
                    selectedImageIndex.value = index
                    sharedPreferences.saveDataInt("QiblaImageIndex", index)
                }
            )
        }

        if (isLoading) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun QiblaHeader(
    qiblaState: Double,
    isLoading: Boolean,
    errorMessage: String
) {
    val sharedPref = PrivateSharedPreferences(LocalContext.current)
    val location = sharedPref.getData(LOCATION_INPUT, "")
    val bearing = qiblaState.roundToInt()
    val compassDirection = bearingToCompassDirection(qiblaState.toFloat())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderInfo(
                title = "Location",
                value = if (isLoading) "Loading..." else if (errorMessage.isNotEmpty()) "Error" else location,
                icon = Icons.Rounded.LocationOn
            )

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            HeaderInfo(
                title = "Heading",
                value = if (isLoading) "Loading..." else if (errorMessage.isNotEmpty()) "Error" else "$bearing° $compassDirection",
                icon = Icons.Rounded.Navigation
            )
        }
    }
}

@Composable
private fun HeaderInfo(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun QiblaCompass(
    bearing: Double,
    isLoading: Boolean,
    errorMessage: String,
    compassImage: Painter,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sensorData = rememberSensorData(context, scope)
    val degree = (Math.toDegrees(sensorData?.yaw?.toDouble() ?: 0.0) + 360) % 360
    val target = (bearing - degree).toFloat()
    val rotateAnim = remember { Animatable(0f) }
    val pointingToQibla = abs(target) < 5f
    val vibrator =
        remember { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager }
    val directionState = remember { mutableStateOf(QiblaDirection.getDirection(target)) }

    LaunchedEffect(target) {
        directionState.value = QiblaDirection.getDirection(target)
        if (pointingToQibla) {
            rotateAnim.animateTo(0f, tween(100))
            rotateAnim.stop()
            vibrator.vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            )
        } else {
            rotateAnim.animateTo(target, tween(200))
            vibrator.cancel()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        color = when {
            pointingToQibla -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DirectionIndicator(directionState.value, pointingToQibla)
            Spacer(modifier = Modifier.height(24.dp))
            CompassView(compassImage, rotateAnim.value, pointingToQibla)
        }
    }
}

@Composable
private fun DirectionIndicator(direction: QiblaDirection, pointingToQibla: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = direction.message,
            style = MaterialTheme.typography.headlineMedium,
            color = if (pointingToQibla)
                MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (direction.icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = direction.icon,
                contentDescription = null,
                tint = if (pointingToQibla)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompassView(
    compassImage: Painter,
    rotation: Float,
    pointingToQibla: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = compassImage,
            contentDescription = "Qibla compass",
            modifier = Modifier
                .fillMaxSize(0.9f)
                .rotate(rotation)
                .scale(
                    animateFloatAsState(
                        if (pointingToQibla) 1.1f else 1f,
                        spring(stiffness = Spring.StiffnessLow)
                    ).value
                )
        )
    }
}

@Composable
fun QiblaImageSelector(
    selectedIndex: Int,
    onImageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(qiblaImages.toList()) { (index, imageRes) ->
                QiblaImageOption(
                    imageRes = imageRes,
                    isSelected = index == selectedIndex,
                    onSelected = { onImageSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun QiblaImageOption(
    imageRes: Int,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Image(
        painter = painterResource(imageRes),
        contentDescription = "Compass style",
        modifier = Modifier
            .padding(start = 6.dp)
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                onClick = onSelected
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                shape = CircleShape
            )
    )
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private enum class QiblaDirection(
    val message: String,
    val icon: ImageVector? = null
) {
    ALIGNED("Facing Qibla", Icons.Rounded.CheckCircle),
    RIGHT("Turn Right", Icons.AutoMirrored.Rounded.ArrowForward),
    LEFT("Turn Left", Icons.AutoMirrored.Rounded.ArrowBack);

    companion object {
        fun getDirection(target: Float) = when {
            abs(target) < 5f -> ALIGNED
            target > 0f -> RIGHT
            else -> LEFT
        }
    }
}

private val qiblaImages = mapOf(
    0 to R.drawable.qibla1,
    1 to R.drawable.qibla2,
    2 to R.drawable.qibla3,
    3 to R.drawable.qibla4,
    4 to R.drawable.qibla5,
    5 to R.drawable.qibla6
)

private fun bearingToCompassDirection(bearing: Float): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
    return directions[((bearing + 22.5f) / 45f).toInt() % 8]
}