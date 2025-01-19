package com.arshadshah.nimaz.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.settings.getAppVersion
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch


// Enhanced SocialLinkData with additional properties for better customization
data class SocialLinkData(
    val icon: Int,
    val url: String,
    val description: String,
    val backgroundColor: Color,
    val iconTint: Color = Color.White,
    val hoverRotation: Float = 8f,
    val pressScale: Float = 0.9f
)

// Enhanced platform definitions
object SocialPlatforms {
    fun github(url: String) = SocialLinkData(
        icon = R.drawable.github_icon,
        url = url,
        description = "GitHub",
        backgroundColor = Color(0xFF24292E),
        hoverRotation = 12f
    )

    fun linkedin(url: String) = SocialLinkData(
        icon = R.drawable.linkedin_icon,
        url = url,
        description = "LinkedIn",
        backgroundColor = Color(0xFF0A66C2),
        hoverRotation = -8f
    )

    fun email(email: String) = SocialLinkData(
        icon = R.drawable.mail_icon,
        url = "mailto:$email",
        description = "Email",
        backgroundColor = Color(0xFF4CAF50),
        hoverRotation = 0f
    )

    fun portfolio(url: String) = SocialLinkData(
        icon = R.drawable.web,
        url = url,
        description = "Portfolio",
        backgroundColor = Color(0xFF9C27B0),
        hoverRotation = 15f
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutTopBar(
    scrollProgress: Float,
    onNavigateBack: () -> Unit
) {
    val containerAlpha by animateFloatAsState(
        targetValue = (1f - (scrollProgress * 0.3f)).coerceIn(0.7f, 1f),
        label = "containerAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (scrollProgress > 0) 4.dp else 0.dp,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha)
    ) {
        LargeTopAppBar(
            title = {
                Column {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Developer and app information",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("backButton")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = "Back",
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha)
            ),
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                )
            )
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedAppLogo(
    onImageClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPref = PrivateSharedPreferences(context)
    val clickCount = remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Enhanced animation states
    val imageScale = remember { Animatable(1f) }
    val rotationState = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated glow effect
        Box(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer {
                    alpha = glowAlpha.value
                    scaleX = imageScale.value + 0.2f
                    scaleY = imageScale.value + 0.2f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main logo
        Surface(
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                .graphicsLayer {
                    scaleX = imageScale.value
                    scaleY = imageScale.value
                    rotationZ = rotationState.value
                }
                .combinedClickable(
                    onClick = {
                        scope.launch {
                            // Scale animation
                            launch {
                                imageScale.animateTo(
                                    0.8f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                imageScale.animateTo(
                                    1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                            // Rotation animation
                            launch {
                                rotationState.animateTo(
                                    rotationState.value + 360f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                            // Glow animation
                            launch {
                                glowAlpha.animateTo(0.8f, tween(200))
                                glowAlpha.animateTo(0f, tween(500))
                            }
                        }

                        if (clickCount.intValue == 5) {
                            Toasty
                                .success(context, "Debug Mode Enabled")
                                .show()
                            sharedPref.saveDataBoolean("debug", true)
                            onImageClicked()
                            clickCount.intValue = 0
                        } else {
                            clickCount.intValue++
                            if (clickCount.intValue > 1) {
                                Toasty
                                    .info(
                                        context,
                                        "Click ${6 - clickCount.intValue} more times to enable debug mode"
                                    )
                                    .show()
                            }
                        }
                    },
                    onLongClick = {
                        scope.launch {
                            // Vibrant disable animation
                            launch {
                                rotationState.animateTo(
                                    rotationState.value - 360f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                            launch {
                                imageScale.animateTo(
                                    1.2f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                imageScale.animateTo(
                                    1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                        Toasty
                            .info(context, "Debug Mode Disabled")
                            .show()
                        sharedPref.saveDataBoolean("debug", false)
                        clickCount.intValue = 0
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                2.dp,
                Brush.sweepGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.4f)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun EnhancedAppInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App name with enhanced typography
        Text(
            text = "Nimaz",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            color = MaterialTheme.colorScheme.primary
        )

        // Version badge
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version ${getAppVersion(LocalContext.current)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // App description
        Text(
            text = "A free, Ad-free app that helps Muslims stay connected to their faith with accurate prayer times, precise qibla direction, beautiful Quran recitations, personalized reminders, and comprehensive Islamic calendar - all in one elegant and easy-to-use interface.",
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun EnhancedFeaturesList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Key Features",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val features = listOf(
            FeatureItem(
                icon = R.drawable.praying,
                title = "Accurate Prayer Times",
                subtitle = "Precisely calculated prayer times for your location",
                iconTint = MaterialTheme.colorScheme.primary,
                backgroundGradient = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.surface
                )
            ),
            FeatureItem(
                icon = R.drawable.qibla,
                title = "Qibla Direction",
                subtitle = "Find the exact direction of the Qibla using your device's compass",
                iconTint = MaterialTheme.colorScheme.secondary,
                backgroundGradient = listOf(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.surface
                )
            ),
            FeatureItem(
                icon = R.drawable.tracker_icon,
                title = "Prayer Tracking",
                subtitle = "Keep track of your daily prayers with beautiful statistics",
                iconTint = MaterialTheme.colorScheme.tertiary,
                backgroundGradient = listOf(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.surface
                )
            ),
            FeatureItem(
                icon = R.drawable.adhan,
                title = "Beautiful Adhans",
                subtitle = "Choose from a collection of high-quality adhan recordings",
                iconTint = MaterialTheme.colorScheme.secondary,
                backgroundGradient = listOf(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.surface
                )
            )
        )

        features.forEach { feature ->
            EnhancedFeatureCard(feature)
        }
    }
}

private data class FeatureItem(
    @DrawableRes val icon: Int,
    val title: String,
    val subtitle: String,
    val iconTint: Color,
    val backgroundGradient: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedFeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val elevation by animateFloatAsState(
        targetValue = when {
            isHovered -> 8f
            isExpanded -> 6f
            else -> 2f
        },
        label = "elevation"
    )

    val contentColor = MaterialTheme.colorScheme.onSurface
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Surface(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = feature.iconTint.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = elevation.dp,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(feature.backgroundGradient)
                )
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with animated container
                Surface(
                    shape = CircleShape,
                    color = feature.iconTint.copy(alpha = 0.1f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = feature.iconTint.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = feature.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = contentColor
                    )
                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnhancedProfessionalDetails() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Professional Details",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        // Professional cards grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EnhancedProfessionalCard(
                icon = Icons.Rounded.Work,
                title = "Software Engineer",
                company = "HMHco",
                subtitle = "Houghton Mifflin Harcourt",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            EnhancedProfessionalCard(
                icon = Icons.Rounded.School,
                title = "Education",
                company = "BSc Computer Science",
                subtitle = "Technical University Dublin",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            EnhancedProfessionalCard(
                icon = Icons.Rounded.LocationOn,
                title = "Location",
                company = "Dublin",
                subtitle = "Ireland",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        // Project description section
        EnhancedProjectDescription()
    }
}

@Composable
private fun EnhancedProfessionalCard(
    icon: ImageVector,
    title: String,
    company: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    val elevation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 4f,
        label = "elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = containerColor.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = containerColor.copy(alpha = if (isHovered) 1f else 0.8f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHovered)
                contentColor.copy(alpha = 0.2f)
            else
                Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with animated background
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = contentColor
                )
                Text(
                    text = company,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EnhancedProjectDescription() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Project Story",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        ).toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        append("Nimaz is my passion project, born from a desire to create something meaningful for the Muslim community while expanding my Android development expertise. ")
                        append("Starting as my final year project, it has evolved into a comprehensive prayer companion that I hope serves as a valuable resource for Muslims worldwide.")
                    }
                },
                modifier = Modifier.alpha(0.9f)
            )
        }
    }
}


@Composable
fun EnhancedAboutScreen(
    navController: NavHostController,
    onImageClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Calculate scroll progress for animations
    val scrollProgress = if (scrollState.maxValue == 0) 0f
    else scrollState.value.toFloat() / scrollState.maxValue

    Scaffold(
        topBar = {
            AboutTopBar(
                scrollProgress = scrollProgress,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Logo and Info
                        EnhancedAppLogo(onImageClicked)
                        EnhancedAppInfo()
                    }
                }

                // Features Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    EnhancedFeaturesList()
                }

                // Professional Details Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        EnhancedProfessionalDetails()
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        EnhancedSkillsSection()
                    }
                }

                // Social Links Section
                EnhancedSocialLinks()

                // Bottom spacing
                Spacer(modifier = Modifier.height(80.dp))
            }

            // Scroll to top FAB
            AnimatedVisibility(
                visible = scrollState.value > 100,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                enter = fadeIn() + slideInVertically { it * 2 },
                exit = fadeOut() + slideOutVertically { it * 2 }
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            scrollState.animateScrollTo(
                                0,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_up_icon),
                        contentDescription = "Scroll to top",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = scrollProgress * 360f
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnhancedSkillsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Technology Stack",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            listOf(
                SkillItem(
                    "Android",
                    Icons.Rounded.Android,
                    MaterialTheme.colorScheme.primary
                ),
                SkillItem(
                    "Kotlin",
                    Icons.Rounded.Code,
                    MaterialTheme.colorScheme.secondary
                ),
                SkillItem(
                    "Compose",
                    Icons.Rounded.Widgets,
                    MaterialTheme.colorScheme.tertiary
                ),
                SkillItem(
                    "Material Design",
                    Icons.Rounded.Palette,
                    MaterialTheme.colorScheme.error
                ),
                SkillItem(
                    "Clean Architecture",
                    Icons.Rounded.Architecture,
                    MaterialTheme.colorScheme.secondary
                )
            ).forEach { skill ->
                EnhancedSkillChip(skill)
            }
        }
    }
}

private data class SkillItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedSkillChip(
    skill: SkillItem,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isHovered) 5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isHovered)
            skill.color.copy(alpha = 0.2f)
        else
            skill.color.copy(alpha = 0.1f),
        label = "containerColor"
    )

    Surface(
        onClick = {},
//        onPointerEvent(PointerEventType.Enter) { isHovered = true },
//        onPointerEvent(PointerEventType.Exit) { isHovered = false },
        modifier = modifier
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (isHovered) skill.color.copy(0.5f) else skill.color.copy(0.2f)
        ),
        tonalElevation = if (isHovered) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = skill.icon,
                contentDescription = null,
                tint = skill.color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = skill.name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = skill.color
            )
        }
    }
}

@Composable
private fun EnhancedSocialLinks() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Connect With Me",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    SocialPlatforms.github("https://github.com/arshad-shah"),
                    SocialPlatforms.linkedin("https://www.linkedin.com/in/arshad-shah/"),
                    SocialPlatforms.email("arshad@arshadshah.com"),
                    SocialPlatforms.portfolio("https://arshadshah.com")
                ).forEach { socialData ->
                    EnhancedSocialButton(socialData)
                }
            }
        }
    }
}

@Composable
private fun EnhancedSocialButton(
    data: SocialLinkData,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isHovered) data.hoverRotation else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated glow
        if (isHovered) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                data.backgroundColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Surface(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                context.startActivity(intent)
            },
//            onPointerEvent(PointerEventType.Enter) { isHovered = true },
//            onPointerEvent(PointerEventType.Exit) { isHovered = false },
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .shadow(
                    elevation = if (isHovered) 12.dp else 6.dp,
                    shape = CircleShape,
                    spotColor = data.backgroundColor
                ),
            shape = CircleShape,
            color = data.backgroundColor,
            border = if (isHovered) {
                BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
            } else null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                data.backgroundColor.copy(alpha = 0.7f),
                                data.backgroundColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(data.icon),
                    contentDescription = data.description,
                    tint = data.iconTint.copy(
                        alpha = if (isHovered) 1f else 0.9f
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Enhanced tooltip
        AnimatedVisibility(
            visible = isHovered,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 64.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = data.backgroundColor.copy(alpha = 0.95f)
            ) {
                Text(
                    text = data.description,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = data.iconTint,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}