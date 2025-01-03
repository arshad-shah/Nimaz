package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush

@Composable
fun SurahHeader(
    surah: LocalSurah,
    loading: Boolean = false,
) {
    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // English Name with decorative background
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = surah.englishName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholderWithShimmer(
                            visible = loading,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }

            // Arabic Name with elegant styling
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = surah.name,
                    style = MaterialTheme.typography.displaySmall.copy(
                        lineHeight = 52.sp
                    ),
                    fontFamily = utmaniQuranFont,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .placeholderWithShimmer(
                            visible = loading,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }

            // English Translation with refined styling
            Text(
                text = surah.englishNameTranslation,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.25.sp,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .placeholderWithShimmer(
                        visible = loading,
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            // Metadata section with enhanced visuals
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "Surah Information",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${surah.numberOfAyahs} Verses • ${surah.revelationType}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.placeholderWithShimmer(
                            visible = loading,
                            shape = RoundedCornerShape(6.dp)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.placeholderWithShimmer(
    visible: Boolean,
    shape: Shape = RoundedCornerShape(4.dp)
) = this.placeholder(
    visible = visible,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    shape = shape,
    highlight = PlaceholderHighlight.shimmer(
        highlightColor = MaterialTheme.colorScheme.surface
    )
)
@Preview
@Composable
fun SurahHeaderPreview() {
    SurahHeader(
        surah = LocalSurah(
            1, 7, 1, "الفاتحة", "Al-Faatiha",
            "The Opening", "Meccan", 5, 1
        ),
        loading = false
    )
}