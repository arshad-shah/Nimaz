package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.ui.components.common.QuranItemNumber
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
                isLoading = loading,
                juzNumber = juz[index].number,
                name = juz[index].name,
                translatedName = juz[index].tname,
                navigateToAyatScreen = onNavigateToAyatScreen
            )
        }
    }
}

@Composable
fun JuzListItem(
    juzNumber: Int,
    isLoading: Boolean,
    name: String,
    translatedName: String,
    navigateToAyatScreen: (String, Boolean, String, Int?) -> Unit
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
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = !isLoading,
                onClick = { navigateToAyatScreen(juzNumber.toString(), false, translationType, 0) }
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
            // Header with Juz number
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
                        text = "Juz $juzNumber",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.placeholder(
                            visible = isLoading,
                            highlight = PlaceholderHighlight.shimmer()
                        ).padding(start = 8.dp)
                    )

                    QuranItemNumber(
                        number = juzNumber,
                        loading = isLoading
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
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = translatedName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .placeholder(
                                        visible = isLoading,
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
                            text = name,
                            fontFamily = utmaniQuranFont,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .placeholder(
                                    visible = isLoading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun JuzListItemPreview() {
    JuzListItem(
        juzNumber = 1,
        isLoading = false,
        name = "الفاتحة",
        translatedName = "Al-Fatihah",
        navigateToAyatScreen = { _, _, _, _ -> }
    )
}