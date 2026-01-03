package com.arshadshah.nimaz.ui.components.quran.aya.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.utils.QuranUtils.getArabicFont

/**
 * Special Ayat card for Bismillah or other highlighted verses.
 * Displayed prominently at the start of surahs.
 *
 * Design System Alignment:
 * - ElevatedCard with extraLarge shape
 * - 4dp elevation
 * - 8dp inner padding
 * - 12dp section spacing
 * - Header: primaryContainer with 16dp corners
 * - Content: surfaceVariant @ 0.5 alpha with 16dp corners
 * - Uses semantic colors instead of hardcoded gold
 */
@Composable
fun SpecialAyat(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section - Arabic Text
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Arabic Text
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = aya.ayaArabic,
                            fontFamily = getArabicFont(displaySettings.arabicFont),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = displaySettings.arabicFontSize.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = (displaySettings.arabicFontSize * 1.5f).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .placeholder(
                                    visible = loading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                    }

                    // Decorative Separator
                    DecorativeSeparator()
                }
            }

            // Content Section - Translation
            if (displaySettings.translation == "English" || displaySettings.translation == "Urdu") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (displaySettings.translation == "English") {
                            Text(
                                text = aya.translationEnglish,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = displaySettings.translationFontSize.sp,
                                    lineHeight = (displaySettings.translationFontSize * 1.4f).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .placeholder(
                                        visible = loading,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                            )
                        }

                        if (displaySettings.translation == "Urdu") {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = aya.translationUrdu,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = displaySettings.translationFontSize.sp,
                                        fontFamily = urduFont,
                                        lineHeight = (displaySettings.translationFontSize * 1.4f).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
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
    }
}

/**
 * Decorative separator using semantic colors.
 * Uses tertiary color scheme for accent.
 */
@Composable
private fun DecorativeSeparator(
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left line with gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.5f),
                            accentColor
                        )
                    )
                )
        )

        // Center ornament
        Surface(
            shape = CircleShape,
            color = accentColor,
            modifier = Modifier.size(12.dp)
        ) {
            Box(
                modifier = Modifier.size(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(6.dp)
                ) {}
            }
        }

        // Right line with gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Compact version of special ayat for inline display.
 */
@Composable
fun CompactSpecialAyat(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Arabic Text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = aya.ayaArabic,
                    fontFamily = getArabicFont(displaySettings.arabicFont),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = (displaySettings.arabicFontSize * 0.8f).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }

            // Small separator
            Row(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(4.dp)
                ) {}
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                )
            }

            // Translation
            if (displaySettings.translation == "English") {
                Text(
                    text = aya.translationEnglish,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

private val previewAya = LocalAya(
    ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
    translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
    translationUrdu = "اللہ کے نام سے جو بہت مہربان نہایت رحم والا ہے۔",
    suraNumber = 1,
    ayaNumberInSurah = 0,
    bookmark = false,
    favorite = false,
    note = "",
    audioFileLocation = "",
    sajda = false,
    sajdaType = "",
    ruku = 1,
    juzNumber = 1,
    ayaNumberInQuran = 0
)

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SpecialAyatPreview_English() {
    MaterialTheme {
        SpecialAyat(
            aya = previewAya,
            displaySettings = DisplaySettings(translation = "English"),
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SpecialAyatPreview_Urdu() {
    MaterialTheme {
        SpecialAyat(
            aya = previewAya,
            displaySettings = DisplaySettings(translation = "Urdu"),
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SpecialAyatPreview_Loading() {
    MaterialTheme {
        SpecialAyat(
            aya = previewAya,
            displaySettings = DisplaySettings(translation = "English"),
            loading = true,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun CompactSpecialAyatPreview() {
    MaterialTheme {
        CompactSpecialAyat(
            aya = previewAya,
            displaySettings = DisplaySettings(translation = "English"),
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun DecorativeSeparatorPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(16.dp)
        ) {
            DecorativeSeparator(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpecialAyatPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SpecialAyat(
                aya = previewAya,
                displaySettings = DisplaySettings(translation = "English"),
                loading = false,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CompactSpecialAyatPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CompactSpecialAyat(
                aya = previewAya,
                displaySettings = DisplaySettings(translation = "English"),
                loading = false,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}