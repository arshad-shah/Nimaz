package com.arshadshah.nimaz.ui.components.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R

@Composable
private fun EnhancedProfessionalDetail(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnhancedSkillsSection() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        listOf(
            "Android" to Icons.Rounded.Android,
            "Kotlin" to Icons.Rounded.Code,
            "Jetpack Compose" to Icons.Rounded.Widgets,
            "Material Design" to Icons.Rounded.Palette,
            "Clean Architecture" to Icons.Rounded.Architecture
        ).forEach { (text, icon) ->
            EnhancedTechnologyChip(text, icon)
        }
    }
}

@Composable
private fun EnhancedTechnologyChip(
    text: String,
    icon: ImageVector
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered)
            MaterialTheme.colorScheme.secondary
        else
            MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "backgroundColor"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 4f,
        label = "elevation"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .shadow(elevation.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = { isHovered = !isHovered }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHovered)
                    MaterialTheme.colorScheme.onSecondary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (isHovered)
                    MaterialTheme.colorScheme.onSecondary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun AuthorLinks() {
    val links = listOf(
        SocialPlatforms.github("https://github.com/arshad-shah"),
        SocialPlatforms.linkedin("https://www.linkedin.com/in/arshad-shah/"),
        SocialPlatforms.email("arshad@arshadshah.com"),
        SocialPlatforms.portfolio("https://arshadshah.com")
    )

    SocialLinks(
        links = links,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
fun SocialLinks(
    modifier: Modifier = Modifier,
    links: List<SocialLinkData>,
    arrangement: Arrangement.Horizontal = Arrangement.Center
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            links.forEach { linkData ->
                SocialLinkButton(
                    data = linkData,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SocialLinkButton(
    data: SocialLinkData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Enhanced animation states
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isHovered -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> data.backgroundColor.copy(alpha = 0.7f)
            isHovered -> data.backgroundColor
            else -> data.backgroundColor.copy(alpha = 0.8f)
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "backgroundColor"
    )

    val elevation by animateFloatAsState(
        targetValue = when {
            isPressed -> 2f
            isHovered -> 12f
            else -> 6f
        },
        label = "elevation"
    )

    val iconSize by animateFloatAsState(
        targetValue = if (isHovered) 28f else 24f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconSize"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                context.startActivity(intent)
            },
            interactionSource = interactionSource,
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = elevation.dp,
                    shape = CircleShape,
                    spotColor = data.backgroundColor.copy(alpha = 0.3f),
                    ambientColor = data.backgroundColor.copy(alpha = 0.1f)
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                },
            shape = CircleShape,
            color = backgroundColor,
            border = if (isHovered) BorderStroke(2.dp, Color.White.copy(alpha = 0.2f)) else null
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
                    modifier = Modifier.size(iconSize.dp)
                )
            }
        }

        // Enhanced tooltip
        AnimatedVisibility(
            visible = isHovered,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            TooltipBox(
                text = data.description,
                backgroundColor = data.backgroundColor.copy(alpha = 0.9f),
                textColor = data.iconTint
            )
        }
    }
}

@Composable
private fun TooltipBox(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        modifier = Modifier
            .padding(top = 64.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.9f),
                            backgroundColor
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = textColor
            )
        }
    }
}

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