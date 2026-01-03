package com.arshadshah.nimaz.ui.components.quran

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun JuzListUI(
    juz: ArrayList<LocalJuz>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean,
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        userScrollEnabled = !loading,
        modifier = Modifier.testTag(TEST_TAG_QURAN_JUZ)
    ) {
        items(juz.size) { index ->
            JuzListItem(
                juz = juz[index],
                isLoading = loading,
                navigateToAyatScreen = onNavigateToAyatScreen
            )
        }
    }
}

/**
 * Juz list item following the design system.
 *
 * Design System Alignment:
 * - ElevatedCard with extraLarge shape
 * - 4dp elevation
 * - 8dp inner padding
 * - 12dp section spacing
 * - Header: primaryContainer with 16dp corners
 * - Content: surfaceVariant @ 0.5 alpha with 16dp corners
 * - Number badge: 44dp icon container with 12dp corners
 */
@Composable
fun JuzListItem(
    juz: LocalJuz,
    isLoading: Boolean,
    navigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val translationType = remember {
        when (PrivateSharedPreferences(context).getData(
            AppConstants.TRANSLATION_LANGUAGE,
            "English"
        )) {
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
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = !isLoading,
                onClick = { navigateToAyatScreen(juz.number.toString(), false, translationType, 0) }
            ),
        shape = MaterialTheme.shapes.extraLarge,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Juz Number Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${juz.number}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.placeholder(
                                        visible = isLoading,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                                )
                            }
                        }

                        // Title + Subtitle
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Juz ${juz.number}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.placeholder(
                                    visible = isLoading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                            )
                            Text(
                                text = juz.tname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.placeholder(
                                    visible = isLoading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                            )
                        }
                    }

                    // Arabic Name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = juz.name,
                            fontFamily = utmaniQuranFont,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .placeholder(
                                    visible = isLoading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                    }
                }
            }

            // Content Section - Metadata (optional, add if you have more juz info)
            // For now, keeping it minimal since Juz doesn't have as much metadata as Surah
        }
    }
}

/**
 * Compact Juz card for dense list views.
 * Follows the Compact Row Component pattern from design system.
 */
@Composable
fun CompactJuzCard(
    juz: LocalJuz,
    isLoading: Boolean,
    navigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val translationType = remember {
        when (PrivateSharedPreferences(context).getData(
            AppConstants.TRANSLATION_LANGUAGE,
            "English"
        )) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLoading,
                onClick = { navigateToAyatScreen(juz.number.toString(), false, translationType, 0) }
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Juz Number Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${juz.number}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.placeholder(
                            visible = isLoading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Juz ${juz.number}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.placeholder(
                        visible = isLoading,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
                Text(
                    text = juz.tname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.placeholder(
                        visible = isLoading,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            }

            // Arabic Name
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = juz.name,
                        fontFamily = utmaniQuranFont,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.placeholder(
                            visible = isLoading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

private val previewJuz = LocalJuz(
    number = 1,
    name = "آلم",
    tname = "Alif Lam Meem",
    juzStartAyaInQuran = 1,
)

private val previewJuz2 = LocalJuz(
    number = 30,
    name = "عَمَّ",
    tname = "Amma",
    juzStartAyaInQuran = 1,
)

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun JuzListItemPreview() {
    MaterialTheme {
        JuzListItem(
            juz = previewJuz,
            isLoading = false,
            navigateToAyatScreen = { _, _, _, _ -> },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun JuzListItemPreview_Juz30() {
    MaterialTheme {
        JuzListItem(
            juz = previewJuz2,
            isLoading = false,
            navigateToAyatScreen = { _, _, _, _ -> },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun JuzListItemPreview_Loading() {
    MaterialTheme {
        JuzListItem(
            juz = previewJuz,
            isLoading = true,
            navigateToAyatScreen = { _, _, _, _ -> },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun CompactJuzCardPreview() {
    MaterialTheme {
        CompactJuzCard(
            juz = previewJuz,
            isLoading = false,
            navigateToAyatScreen = { _, _, _, _ -> },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun CompactJuzCardPreview_List() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactJuzCard(
                juz = previewJuz,
                isLoading = false,
                navigateToAyatScreen = { _, _, _, _ -> }
            )
            CompactJuzCard(
                juz = LocalJuz(
                    number = 15, name = "سُبْحَانَ", tname = "Subhana",
                    juzStartAyaInQuran = 1,
                ),
                isLoading = false,
                navigateToAyatScreen = { _, _, _, _ -> }
            )
            CompactJuzCard(
                juz = previewJuz2,
                isLoading = false,
                navigateToAyatScreen = { _, _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JuzListItemPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            JuzListItem(
                juz = previewJuz,
                isLoading = false,
                navigateToAyatScreen = { _, _, _, _ -> },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CompactJuzCardPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CompactJuzCard(
                juz = previewJuz,
                isLoading = false,
                navigateToAyatScreen = { _, _, _, _ -> },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}