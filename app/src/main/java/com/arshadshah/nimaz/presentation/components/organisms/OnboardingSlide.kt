package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/**
 * Data class for onboarding slide.
 */
data class OnboardingSlideData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val accentColor: Color
)

/**
 * Default onboarding slides for Nimaz app.
 */
val defaultOnboardingSlides = listOf(
    OnboardingSlideData(
        title = "Prayer Times",
        description = "Get accurate prayer times based on your location with customizable calculation methods",
        icon = Icons.Default.Mosque,
        backgroundColor = NimazColors.PrayerColors.Fajr,
        accentColor = NimazColors.PrayerColors.FajrGradientEnd
    ),
    OnboardingSlideData(
        title = "Quran & Hadith",
        description = "Read the Quran with translations, listen to recitations, and explore authentic Hadith collections",
        icon = Icons.Default.Book,
        backgroundColor = NimazColors.QuranColors.Meccan,
        accentColor = NimazColors.QuranColors.Medinan
    ),
    OnboardingSlideData(
        title = "Location",
        description = "Allow location access for accurate prayer times and Qibla direction",
        icon = Icons.Default.LocationOn,
        backgroundColor = Color(0xFF14B8A6),
        accentColor = Color(0xFF2DD4BF)
    ),
    OnboardingSlideData(
        title = "Notifications",
        description = "Never miss a prayer with timely Adhan notifications",
        icon = Icons.Default.Notifications,
        backgroundColor = Color(0xFFEAB308),
        accentColor = Color(0xFFFACC15)
    )
)

/**
 * Complete onboarding screen with pager.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    slides: List<OnboardingSlideData> = defaultOnboardingSlides,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = onComplete
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = pagerState.currentPage < slides.lastIndex,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingSlide(
                    data = slides[page],
                    isActive = pagerState.currentPage == page
                )
            }

            // Bottom controls
            OnboardingControls(
                pagerState = pagerState,
                totalPages = slides.size,
                onNextClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < slides.lastIndex) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onComplete()
                        }
                    }
                },
                onComplete = onComplete
            )
        }
    }
}

/**
 * Single onboarding slide.
 */
@Composable
fun OnboardingSlide(
    data: OnboardingSlideData,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        animationSpec = tween(300),
        label = "slide_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            data.accentColor,
                            data.backgroundColor
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = data.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = data.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Onboarding controls with page indicators.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingControls(
    pagerState: PagerState,
    totalPages: Int,
    onNextClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLastPage = pagerState.currentPage == totalPages - 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalPages) { index ->
                PageIndicator(
                    isSelected = pagerState.currentPage == index,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action button
        Button(
            onClick = if (isLastPage) onComplete else onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isLastPage) "Get Started" else "Next",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}

/**
 * Page indicator dot.
 */
@Composable
private fun PageIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val width by animateFloatAsState(
        targetValue = if (isSelected) 24f else 8f,
        animationSpec = tween(300),
        label = "indicator_width"
    )

    Box(
        modifier = modifier
            .height(8.dp)
            .width(width.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
    )
}

/**
 * Permission request slide for onboarding.
 */
@Composable
fun PermissionSlide(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) {
                        NimazColors.StatusColors.Prayed.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isGranted) {
                    NimazColors.StatusColors.Prayed
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NimazColors.StatusColors.Prayed.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NimazColors.StatusColors.Prayed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Permission Granted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.StatusColors.Prayed
                    )
                }
            }
        } else {
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Grant Permission",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Feature highlight card for onboarding.
 */
@Composable
fun FeatureHighlightCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Welcome slide for first launch.
 */
@Composable
fun WelcomeSlide(
    appName: String = "Nimaz",
    tagline: String = "Your Complete Islamic Companion",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mosque,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tagline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Preview(showBackground = true, name = "OnboardingSlide")
@Composable
private fun OnboardingSlidePreview() {
    NimazTheme {
        OnboardingSlide(
            data = defaultOnboardingSlides.first()
        )
    }
}

@Preview(showBackground = true, name = "PermissionSlide - Not Granted")
@Composable
private fun PermissionSlideNotGrantedPreview() {
    NimazTheme {
        PermissionSlide(
            title = "Location Access",
            description = "Allow location access for accurate prayer times and Qibla direction",
            icon = Icons.Default.LocationOn,
            isGranted = false,
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "PermissionSlide - Granted")
@Composable
private fun PermissionSlideGrantedPreview() {
    NimazTheme {
        PermissionSlide(
            title = "Location Access",
            description = "Allow location access for accurate prayer times and Qibla direction",
            icon = Icons.Default.LocationOn,
            isGranted = true,
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "FeatureHighlightCard")
@Composable
private fun FeatureHighlightCardPreview() {
    NimazTheme {
        FeatureHighlightCard(
            title = "Prayer Times",
            description = "Get accurate prayer times based on your location",
            icon = Icons.Default.Mosque,
            color = NimazColors.PrayerColors.Fajr,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "WelcomeSlide")
@Composable
private fun WelcomeSlidePreview() {
    NimazTheme {
        WelcomeSlide()
    }
}
