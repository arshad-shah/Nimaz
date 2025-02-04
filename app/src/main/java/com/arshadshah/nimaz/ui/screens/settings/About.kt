package com.arshadshah.nimaz.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.settings.getAppVersion
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
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
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.displaySmall,
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
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(
                    elevation = if (scrollProgress > 0) 4.dp else 0.dp,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // App Info Section
                EnhancedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppLogoSection(onImageClicked)
                        AppInfoSection()
                    }
                }

                // Features Section
                EnhancedCard {
                    FeaturesSection()
                }

                // Developer Section
                EnhancedCard {
                    DeveloperSection()
                }

                // Social Links Section
                EnhancedCard {
                    SocialLinksSection()
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Scroll to Top FAB
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
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_up_icon),
                        contentDescription = "Scroll to top",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = scrollProgress * 360f }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedCard(
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        content()
    }
}

@Composable
private fun AppLogoSection(onImageClicked: () -> Unit) {
    val context = LocalContext.current
    val clickCount = remember { mutableIntStateOf(0) }
    val imageScale = remember { Animatable(1f) }
    val rotationState = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.size(120.dp),
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
                .graphicsLayer {
                    scaleX = imageScale.value
                    scaleY = imageScale.value
                    rotationZ = rotationState.value
                }
                .clip(CircleShape)
                .clickable {
                    scope.launch {
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
                        launch {
                            rotationState.animateTo(
                                rotationState.value + 360f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                    }
                    clickCount.intValue++
                    handleLogoClick(clickCount.intValue, context, onImageClicked)
                }
        )
    }
}

@Composable
private fun AppInfoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nimaz",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "Version ${getAppVersion(LocalContext.current)}",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            text = "A comprehensive Islamic companion app with accurate prayer times, qibla direction, and more.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun FeaturesSection() {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Key Features",
            icon = Icons.Rounded.Widgets
        )

        // Features list with enhanced cards similar to prayer times settings
        val features = listOf(
            FeatureItem(
                icon = R.drawable.praying,
                title = "Prayer Times",
                description = "Accurate prayer times for your location"
            ),
            FeatureItem(
                icon = R.drawable.qibla,
                title = "Qibla Direction",
                description = "Find the direction of the Kaaba in Mecca"
            ),
            FeatureItem(
                icon = R.drawable.quran,
                title = "Quran",
                description = "Read, listen, and search the Holy Quran"
            ),
            FeatureItem(
                icon = R.drawable.dua,
                title = "Duas",
                description = "Collection of authentic duas for various occasions"
            ),

            )

        features.forEach { feature ->
            FeatureCard(feature)
        }
    }
}

@Composable
private fun DeveloperSection() {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Developer",
            icon = Icons.Rounded.Code
        )

        // Professional cards similar to prayer times settings
        listOf(
            ProfessionalDetail(
                icon = Icons.Rounded.Work,
                title = "Software Engineer",
                subtitle = "HMH, Dublin",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            ProfessionalDetail(
                icon = Icons.Rounded.School,
                title = "Education",
                subtitle = "BSc Computer Science, Technological University Dublin",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            ProfessionalDetail(
                icon = Icons.Rounded.LocationOn,
                title = "Location",
                subtitle = "Dublin, Ireland",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ).forEach { detail ->
            ProfessionalCard(detail)
        }
    }
}

@Composable
private fun SocialLinksSection() {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Connect",
            icon = Icons.Rounded.Widgets
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                SocialLink(
                    icon = R.drawable.github_icon,
                    url = "https://github.com/arshad-shah",
                    backgroundColor = Color(0xFF24292E)
                ),
                SocialLink(
                    icon = R.drawable.linkedin_icon,
                    url = "https://www.linkedin.com/in/arshad-shah/",
                    backgroundColor = Color(0xFF0A66C2)
                ),
                SocialLink(
                    icon = R.drawable.mail_icon,
                    url = "mailto:arshad@arshadshah.com",
                    backgroundColor = MaterialTheme.colorScheme.primary
                ),
                SocialLink(
                    icon = R.drawable.web,
                    url = "https://arshadshah.com",
                    backgroundColor = MaterialTheme.colorScheme.secondary
                )
            ).forEach { socialLink ->
                SocialButton(socialLink)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureItem) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
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

            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfessionalCard(detail: ProfessionalDetail) {
    val isHovered by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = detail.containerColor.copy(alpha = if (isHovered) 1f else 0.8f),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isHovered) 8.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = detail.containerColor.copy(alpha = 0.5f)
            )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = detail.icon,
                    contentDescription = null,
                    tint = detail.contentColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = detail.contentColor
                )
                Text(
                    text = detail.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = detail.contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SocialButton(
    socialLink: SocialLink,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(socialLink.url))
                context.startActivity(intent)
            },
            shape = CircleShape,
            color = socialLink.backgroundColor,
            border = if (isHovered) BorderStroke(2.dp, Color.White.copy(alpha = 0.2f)) else null,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (isHovered) 12.dp else 6.dp,
                    shape = CircleShape,
                    spotColor = socialLink.backgroundColor
                )
        ) {
            Icon(
                painter = painterResource(id = socialLink.icon),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (isHovered) 1f else 0.9f),
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp)
            )
        }
    }
}

// Data classes for our components
private data class FeatureItem(
    @DrawableRes val icon: Int,
    val title: String,
    val description: String
)

private data class ProfessionalDetail(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val containerColor: Color,
    val contentColor: Color
)

private data class SocialLink(
    @DrawableRes val icon: Int,
    val url: String,
    val backgroundColor: Color
)

private fun handleLogoClick(
    clickCount: Int,
    context: Context,
    onImageClicked: () -> Unit
) {
    when (clickCount) {
        5 -> {
            Toasty.success(context, "Debug Mode Enabled").show()
            onImageClicked()
        }

        in 2..4 -> {
            Toasty.info(
                context,
                "Click ${6 - clickCount} more times to enable debug mode"
            ).show()
        }
    }
}