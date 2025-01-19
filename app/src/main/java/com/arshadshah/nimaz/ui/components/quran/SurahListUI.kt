package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.QuranItemNumber
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun SurahListUI(
    surahs: ArrayList<LocalSurah>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean,
) {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        userScrollEnabled = !loading,
    ) {
        items(surahs.size) { index ->
            SurahCard(
                surah = surahs[index],
                loading = loading,
                onNavigate = onNavigateToAyatScreen
            )
        }
    }
}


@Composable
fun SurahCard(
    surah: LocalSurah,
    onNavigate: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean
) {
    val context = LocalContext.current
    val language = remember {
        when (PrivateSharedPreferences(context)
            .getData(AppConstants.TRANSLATION_LANGUAGE, "English")) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = !loading,
                onClick = { onNavigate(surah.number.toString(), true, language, 0) }
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Surah number and name
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = surah.englishName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        ).padding(start = 8.dp)
                    )

                    QuranItemNumber(
                        number = surah.number,
                        loading = loading
                    )
                }
            }

            // Content Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Info Container
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = surah.englishNameTranslation,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.placeholder(
                                visible = loading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${surah.numberOfAyahs} Verses",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .placeholder(
                                        visible = loading,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = surah.revelationType,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .placeholder(
                                        visible = loading,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                            )
                        }
                    }

                    // Arabic Name Container
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = surah.name,
                            fontFamily = utmaniQuranFont,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .placeholder(
                                    visible = loading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CompactSurahCard(
    surah: LocalSurah,
    onNavigate: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean
) {
    val context = LocalContext.current
    val language = remember {
        when (PrivateSharedPreferences(context)
            .getData(AppConstants.TRANSLATION_LANGUAGE, "English")) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .height(64.dp)
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = !loading,
                onClick = { onNavigate(surah.number.toString(), true, language, 0) }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Surah Number
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    QuranItemNumber(
                        number = surah.number,
                        loading = loading,
                    )
                }
            }

            // Middle Section (Names and Verses)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = surah.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .placeholder(
                                visible = loading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${surah.numberOfAyahs} Verses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .placeholder(
                                    visible = loading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = surah.englishNameTranslation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .placeholder(
                                visible = loading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = surah.revelationType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .placeholder(
                                    visible = loading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                    }
                }
            }

            // Arabic Name
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight(0.8f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.name,
                        fontFamily = utmaniQuranFont,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SurahCardPreview() {
    SurahCard(
        surah = LocalSurah(
            number = 1,
            name = "الفاتحة",
            englishName = "Al-Fatihah",
            englishNameTranslation = "The Opening",
            numberOfAyahs = 7,
            revelationType = "Meccan",
            startAya = 1,
            revelationOrder = 1,
            rukus = 1
        ),
        onNavigate = { _, _, _, _ -> },
        loading = false
    )
}

@Preview
@Composable
fun CompactSurahCardPreview() {
    CompactSurahCard(
        surah = LocalSurah(
            number = 1,
            name = "الفاتحة",
            englishName = "Al-Fatihah",
            englishNameTranslation = "The Opening",
            numberOfAyahs = 7,
            revelationType = "Meccan",
            startAya = 1,
            revelationOrder = 1,
            rukus = 1
        ),
        onNavigate = { _, _, _, _ -> },
        loading = false
    )
}