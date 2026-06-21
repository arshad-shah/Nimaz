package com.arshadshah.nimaz.presentation.components.organisms

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.QuranVerseText
import com.arshadshah.nimaz.presentation.components.atoms.formatAyahEndMarker
import com.arshadshah.nimaz.presentation.components.atoms.getDisplayArabicText
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A single ayah action rendered as an individual circular "pill". The pill fill
 * and icon tint animate between an inactive neutral state and an [active] state
 * tinted with [activeColor] (e.g. red for favourite, gold for bookmark).
 */
@Composable
private fun AyahActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val tint by animateColorAsState(
        targetValue = if (active) activeColor
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "ayah_action_tint"
    )
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun AyahItem(
    ayah: Ayah,
    showTranslation: Boolean,
    showTransliteration: Boolean = false,
    arabicFontSize: Float,
    arabicFontFamily: FontFamily = AmiriFontFamily,
    fontSize: Float,
    isHighlighted: Boolean = false,
    isAudioPlaying: Boolean = false,
    isFavorite: Boolean = false,
    isKhatamRead: Boolean = false,
    isKhatamMode: Boolean = false,
    showTajweed: Boolean = false,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onPlayAyahClick: () -> Unit = {},
    onTafseerClick: () -> Unit = {},
    onKhatamToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val bgColor by animateColorAsState(
        targetValue = if (isHighlighted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "ayah_highlight"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 15.dp, vertical = 6.dp)
    ) {
        // Number badge + actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ayah.numberInSurah.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (isKhatamMode) {
                    IconButton(
                        onClick = onKhatamToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isKhatamRead) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (isKhatamRead) stringResource(R.string.cd_mark_as_unread) else stringResource(R.string.cd_mark_as_read),
                            tint = if (isKhatamRead) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(100),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                ),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                AyahActionButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    onClick = onFavoriteClick,
                    active = isFavorite,
                    activeColor = Color(0xFFEF4444),
                )
                AyahActionButton(
                    icon = if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(R.string.cd_bookmark),
                    onClick = onBookmarkClick,
                    active = ayah.isBookmarked,
                    activeColor = NimazColors.QuranColors.BookmarkPrimary,
                )
                AyahActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    onClick = {
                        val textToShare =
                            "${ayah.textArabic}\n\n${ayah.translation ?: ""}\n\n- Surah ${ayah.surahNumber}, Ayah ${ayah.numberInSurah}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Ayah"))
                    },
                )
                AyahActionButton(
                    icon = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isAudioPlaying) stringResource(R.string.pause) else stringResource(R.string.action_play),
                    onClick = onPlayAyahClick,
                    active = isAudioPlaying || isHighlighted,
                )
                AyahActionButton(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.cd_tafseer),
                    onClick = onTafseerClick,
                    active = true,
                )
              }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text with ayah end marker (with optional tajweed colors)
        val displayText = ayah.getDisplayArabicText()
        val textColor = MaterialTheme.colorScheme.onBackground

        if (showTajweed && ayah.textTajweed != null) {
            // Render with tajweed colors using BasicText
            val tajweedAnnotated = remember(ayah.textTajweed, isDarkTheme, ayah.numberInSurah) {
                val parsed = TajweedParser.parse(
                    tajweedText = ayah.textTajweed,
                    isDarkTheme = isDarkTheme,
                    defaultColor = textColor
                )
                // Append the end marker to the tajweed text
                buildAnnotatedString {
                    append(parsed)
                    append(" ")
                    append(formatAyahEndMarker(ayah.numberInSurah))
                }
            }
            BasicText(
                text = tajweedAnnotated,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontFamily = arabicFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2).sp,
                    textDirection = TextDirection.Rtl,
                    color = textColor
                )
            )
        } else {
            QuranVerseText(
                arabicText = displayText,
                verseNumber = ayah.numberInSurah,
                customFontSize = arabicFontSize.sp.value,
                fontFamily = arabicFontFamily
            )
        }

        // Translation
        if (showTranslation && ayah.translation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    text = ayah.translation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Transliteration
        if (showTransliteration && ayah.transliteration != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ) {
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Indicators row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ayah.sajdaType != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (ayah.sajdaType == SajdaType.OBLIGATORY) stringResource(R.string.sajdah_wajib) else stringResource(R.string.sajdah),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (ayah.rubNumber > 0 && ayah.numberInSurah == 1 || (ayah.rubNumber > 0)) {
                    val quarterLabel = when (ayah.rubNumber) {
                        1 -> "Hizb ${ayah.hizbNumber}"
                        2 -> "\u00BC Hizb ${ayah.hizbNumber}"
                        3 -> "\u00BD Hizb ${ayah.hizbNumber}"
                        4 -> "\u00BE Hizb ${ayah.hizbNumber}"
                        else -> ""
                    }
                    if (quarterLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = quarterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.juz_page_dot_format, ayah.juz, ayah.page),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}

/**
 * Showcase of every [AyahItem] state: plain, active bookmark/favourite, audio
 * playing + highlighted, transliteration shown, a sajdah verse, a hizb-marker
 * verse, and khatam (read-tracking) mode. Rendered for both themes below.
 */
@Composable
private fun AyahItemShowcase() {
    val baseArabic =
        "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064E\u0647\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650"

    fun ayah(
        n: Int,
        bookmarked: Boolean = false,
        hizb: Int = 1,
        rub: Int = 0,
        sajda: SajdaType? = null,
        transliteration: String? = null,
    ) = Ayah(
        id = n,
        surahNumber = 1,
        ayahNumber = n,
        textArabic = baseArabic,
        textSimple = "bismillah al-rahman al-raheem",
        juzNumber = 1,
        hizbNumber = hizb,
        rubNumber = rub,
        pageNumber = 1,
        sajdaType = sajda,
        sajdaNumber = if (sajda != null) 1 else null,
        translation = "In the name of Allah, the Most Gracious, the Most Merciful.",
        isBookmarked = bookmarked,
        transliteration = transliteration,
    )

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        // 1. Plain verse, translation only
        AyahItem(
            ayah = ayah(1),
            showTranslation = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
        HorizontalDivider()
        // 2. Active states: bookmarked + favourited
        AyahItem(
            ayah = ayah(2, bookmarked = true),
            showTranslation = true,
            isFavorite = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
        HorizontalDivider()
        // 3. Audio playing + highlighted row
        AyahItem(
            ayah = ayah(3),
            showTranslation = true,
            isAudioPlaying = true,
            isHighlighted = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
        HorizontalDivider()
        // 4. Transliteration shown
        AyahItem(
            ayah = ayah(4, transliteration = "Bismi ll\u0101hi r-ra\u1E25m\u0101ni r-ra\u1E25\u012Bm"),
            showTranslation = true,
            showTransliteration = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
        HorizontalDivider()
        // 5. Sajdah verse marker
        AyahItem(
            ayah = ayah(5, sajda = SajdaType.OBLIGATORY),
            showTranslation = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
        HorizontalDivider()
        // 6. Hizb / quarter marker
        AyahItem(
            ayah = ayah(6, hizb = 2, rub = 1),
            showTranslation = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
        HorizontalDivider()
        // 7. Khatam mode (read-tracking), marked read
        AyahItem(
            ayah = ayah(7),
            showTranslation = true,
            isKhatamMode = true,
            isKhatamRead = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
        )
    }
}

@Preview(name = "Ayah Item \u00B7 Light", showBackground = true, heightDp = 1400)
@Composable
private fun AyahItemShowcaseLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        AyahItemShowcase()
    }
}

@Preview(name = "Ayah Item \u00B7 Dark", showBackground = true, heightDp = 1400)
@Composable
private fun AyahItemShowcaseDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        AyahItemShowcase()
    }
}
