import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthorDetails() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Author Name Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Designed and Developed By",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Arshad Shah",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Professional Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfessionalDetail(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Work,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = "Software Engineer",
                        detail = "HMHco (Houghton Mifflin Harcourt)"
                    )

                    ProfessionalDetail(
                        icon ={
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = "Education",
                        detail = "BSc in Computer Science"
                    )

                    ProfessionalDetail(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = "Location",
                        detail = "Dublin, Ireland"
                    )
                }
            }

            // Project Description
            Text(
                buildAnnotatedString {
                    withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle()) {
                        append("Nimaz is my passion project, born from a desire to create something meaningful for the Muslim community while expanding my Android development expertise. ")
                        append("Starting as my final year project, it has evolved into a comprehensive prayer companion that I hope serves as a valuable resource for Muslims worldwide.")
                    }
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Skills Section
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                TechnologyChip("Android")
                TechnologyChip("Kotlin")
                TechnologyChip("Jetpack Compose")
                TechnologyChip("Material Design")
                TechnologyChip("Clean Architecture")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Social Links
            AuthorLinks()
        }
    }
}

@Composable
private fun ProfessionalDetail(
    title: String,
    detail: String,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
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
private fun TechnologyChip(text: String) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered)
            MaterialTheme.colorScheme.secondary
        else
            MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "backgroundColor"
    )

    Surface(
        modifier = Modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = { isHovered = !isHovered }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isHovered)
                MaterialTheme.colorScheme.onSecondary
            else
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

object SocialPlatforms {
    fun github(url: String) = SocialLinkData(
        icon = R.drawable.github_icon,  // Representing GitHub with code icon
        url = url,
        description = "GitHub",
        backgroundColor = Color(0xFF24292E)
    )

    fun linkedin(url: String) = SocialLinkData(
        icon = R.drawable.linkedin_icon,  // Professional network icon
        url = url,
        description = "LinkedIn",
        backgroundColor = Color(0xFF0A66C2)
    )

    fun email(email: String) = SocialLinkData(
        icon = R.drawable.mail_icon,  // Email icon
        url = "mailto:$email",
        description = "Email",
        backgroundColor = Color(0xFF4CAF50)
    )

    fun portfolio(url: String) = SocialLinkData(
        icon = R.drawable.web,  // Portfolio/work icon
        url = url,
        description = "Portfolio",
        backgroundColor = Color(0xFF9C27B0)
    )

    // Custom platform with specified icon
    fun custom(
        url: String,
        icon: Int,
        description: String,
        backgroundColor: Color
    ) = SocialLinkData(
        icon = icon,
        url = url,
        description = description,
        backgroundColor = backgroundColor
    )
}

@Composable
private fun AuthorLinks() {
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
    Row(
        modifier = modifier.fillMaxWidth(),
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

@Composable
fun SocialLinkButton(
    data: SocialLinkData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered)
            data.backgroundColor
        else
            data.backgroundColor.copy(alpha = 0.8f),
        label = "backgroundColor"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 4f,
        label = "elevation"
    )

    Surface(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
            context.startActivity(intent)
        },
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = elevation.dp,
                shape = CircleShape,
                spotColor = data.backgroundColor.copy(alpha = 0.5f)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = backgroundColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(data.icon),
                contentDescription = data.description,
                tint = data.iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Tooltip on hover
    if (isHovered) {
        TooltipBox(
            text = data.description,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TooltipBox(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}

data class SocialLinkData(
    val icon: Int,
    val url: String,
    val description: String,
    val backgroundColor: Color,
    val iconTint: Color = Color.White
)