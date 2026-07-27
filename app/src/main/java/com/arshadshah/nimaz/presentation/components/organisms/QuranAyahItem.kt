package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.arshadshah.nimaz.domain.model.quran.catalogue.TranslationEdition
import com.arshadshah.nimaz.presentation.theme.fontFamily
import com.arshadshah.nimaz.presentation.theme.textDirection
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.NimazActionPill
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.QuranVerseText
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.components.atoms.appendAyahEndMarker
import com.arshadshah.nimaz.presentation.components.atoms.getDisplayArabicText
import com.arshadshah.nimaz.presentation.components.atoms.hasLeadingBismillah
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
internal fun AyahItem(
    ayah: Ayah,
    showTranslation: Boolean,
    showTransliteration: Boolean = false,
    arabicFontSize: Float,
    arabicFontFamily: FontFamily = AmiriFontFamily,
    fontSize: Float,
    /** The active translation edition — drives the translation's direction and font. */
    translationEdition: TranslationEdition = QuranEditions.defaultTranslation,
    isHighlighted: Boolean = false,
    isAudioPlaying: Boolean = false,
    isFavorite: Boolean = false,
    isKhatamRead: Boolean = false,
    isKhatamMode: Boolean = false,
    showTajweed: Boolean = false,
    tajweedUnderline: Boolean = false,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onPlayAyahClick: () -> Unit = {},
    onTafseerClick: () -> Unit = {},
    onKhatamToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
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
                        NimazIcon(
                            imageVector = if (isKhatamRead) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (isKhatamRead) stringResource(R.string.cd_mark_as_unread) else stringResource(
                                R.string.cd_mark_as_read
                            ),
                            tint = if (isKhatamRead) NimazColors.Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = NimazIconSize.MEDIUM
                        )
                    }
                }
            }

            NimazActionPill {
                NimazPillActionButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    onClick = onFavoriteClick,
                    active = isFavorite,
                    activeColor = NimazPalette.Red500,
                )
                NimazPillActionButton(
                    icon = if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(R.string.cd_bookmark),
                    onClick = onBookmarkClick,
                    active = ayah.isBookmarked,
                    activeColor = NimazColors.QuranColors.BookmarkPrimary,
                )
                NimazPillActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    onClick = {
                        shareScope.launch {
                            ContentShareManager.shareBranded(
                                context,
                                Shareables.ayah(context, ayah)
                            )
                        }
                    },
                )
                NimazPillActionButton(
                    icon = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isAudioPlaying) stringResource(R.string.pause) else stringResource(
                        R.string.action_play
                    ),
                    onClick = onPlayAyahClick,
                    active = isAudioPlaying || isHighlighted,
                )
                NimazPillActionButton(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.cd_tafseer),
                    onClick = onTafseerClick,
                    active = true,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text with ayah end marker (with optional tajweed colors)
        val displayText = ayah.getDisplayArabicText()
        val textColor = MaterialTheme.colorScheme.onBackground
        // Ayah end-marker: gold brackets + teal number, matching the ornament language.
        val markerBracketColor = NimazColors.Gold500
        val markerNumberColor = MaterialTheme.colorScheme.primary

        if (showTajweed && ayah.textTajweed != null) {
            // Render with tajweed colors using BasicText
            val tajweedAnnotated = remember(
                ayah.textTajweed, isDarkTheme, ayah.numberInSurah,
                textColor, markerBracketColor, markerNumberColor, tajweedUnderline
            ) {
                val parsed = TajweedParser.parse(
                    tajweedText = ayah.textTajweed,
                    isDarkTheme = isDarkTheme,
                    defaultColor = textColor,
                    stripPrefix = if (ayah.hasLeadingBismillah) BISMILLAH_TEXT else null,
                    annotateRules = true,  // enable tap-to-explain (#294)
                    underlineRules = tajweedUnderline
                )
                // Append the coloured end marker to the tajweed text
                buildAnnotatedString {
                    append(parsed)
                    append(" ")
                    appendAyahEndMarker(ayah.numberInSurah, markerBracketColor, markerNumberColor)
                }
            }
            // Tap a coloured tajweed word to explain its rule (#294) — mirrors the
            // continuous mushaf page, so the verse-list reader is no longer dead.
            var tappedRuleCode by remember { mutableStateOf<String?>(null) }
            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            BasicText(
                text = tajweedAnnotated,
                onTextLayout = { result -> textLayoutResult = result },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(tajweedAnnotated) {
                        detectTapGestures { position ->
                            val layout = textLayoutResult ?: return@detectTapGestures
                            val offset = layout.getOffsetForPosition(position)
                            tajweedAnnotated.getStringAnnotations(
                                tag = TajweedParser.RULE_TAG, start = offset, end = offset
                            ).firstOrNull()?.let { tappedRuleCode = it.item }
                        }
                    },
                style = TextStyle(
                    fontFamily = arabicFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2).sp,
                    textDirection = TextDirection.Rtl,
                    color = textColor
                )
            )
            tappedRuleCode?.let { code ->
                TajweedRuleSheet(ruleCode = code, onDismiss = { tappedRuleCode = null })
            }
        } else {
            QuranVerseText(
                arabicText = displayText,
                verseNumber = ayah.numberInSurah,
                customFontSize = arabicFontSize.sp.value,
                fontFamily = arabicFontFamily
            )
        }

        // Translation. Direction and font come from the edition, so an RTL translation
        // (Urdu, Persian, Arabic) aligns and wraps from the right rather than sitting flush
        // left in an LTR layout — TextAlign.Start resolves against textDirection.
        if (showTranslation && ayah.translation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            NimazCard(style = NimazCardStyle.OUTLINED, tone = NimazTone.NEUTRAL, elevation = 0.dp) {
                Text(
                    text = ayah.translation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp,
                        fontFamily = translationEdition.fontFamily,
                        textDirection = translationEdition.textDirection
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Transliteration
        if (showTransliteration && ayah.transliteration != null) {
            Spacer(modifier = Modifier.height(8.dp))
            NimazCard(style = NimazCardStyle.OUTLINED, tone = NimazTone.SUCCESS, elevation = 0.dp) {
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
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
                    NimazBadge(
                        text = if (ayah.sajdaType == SajdaType.OBLIGATORY) stringResource(R.string.sajdah_wajib) else stringResource(
                            R.string.sajdah
                        ),
                        shape = NimazBadgeShape.ROUNDED,
                        size = NimazBadgeSize.SMALL,
                        colors = NimazBadgeDefaults.feature(
                            color = NimazPalette.Red600,
                            emphasis = NimazBadgeEmphasis.SOFT
                        )
                    )
                }

                if (ayah.rubNumber > 0 && ayah.numberInSurah == 1 || (ayah.rubNumber > 0)) {
                    val quarterLabel = when (ayah.rubNumber) {
                        1 -> stringResource(R.string.hizb_format, ayah.hizbNumber)
                        2 -> stringResource(R.string.hizb_quarter_format, ayah.hizbNumber)
                        3 -> stringResource(R.string.hizb_half_format, ayah.hizbNumber)
                        4 -> stringResource(R.string.hizb_three_quarter_format, ayah.hizbNumber)
                        else -> ""
                    }
                    if (quarterLabel.isNotEmpty()) {
                        NimazBadge(
                            text = quarterLabel,
                            tone = NimazTone.SUCCESS,
                            emphasis = NimazBadgeEmphasis.SOFT,
                            shape = NimazBadgeShape.ROUNDED,
                            size = NimazBadgeSize.SMALL
                        )
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
            ayah = ayah(
                4,
                transliteration = "Bismi ll\u0101hi r-ra\u1E25m\u0101ni r-ra\u1E25\u012Bm"
            ),
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
