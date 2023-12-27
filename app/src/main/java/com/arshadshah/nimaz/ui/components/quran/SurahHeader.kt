package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

//surah header component
@Composable
fun SurahHeader(
    surah: LocalSurah,
    loading: Boolean = false,
) {
    OutlinedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = surah.revelationType,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(4.dp)
                    .placeholder(
                        visible = loading,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = Color.White,
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = surah.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = utmaniQuranFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .placeholder(
                                visible = loading,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp),
                                highlight = PlaceholderHighlight.shimmer(
                                    highlightColor = Color.White,
                                )
                            )
                    )
                }

                Text(
                    text = surah.englishName,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .placeholder(
                            visible = loading,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        )
                )
                Text(
                    text = surah.englishNameTranslation,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .placeholder(
                            visible = loading,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        )
                )
            }

            Text(
                text = "${surah.numberOfAyahs} Verses",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .placeholder(
                        visible = loading,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = Color.White,
                        )
                    )
            )
        }
    }
}

@Preview
@Composable
fun SurahHeaderPreview() {
    SurahHeader(
        surah = LocalSurah(
            1,
            7,
            1,
            "الفاتحة",
            "Al-Faatiha",
            "The Opening",
            "Meccan",
            5,
            1,
        ),
        loading = false,
    )
}