package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition
import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownField
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.TajweedLegendSheet
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelectReciter: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val quranState by viewModel.quranState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showTajweedLegend by remember { mutableStateOf(false) }

    // === ADDING NEW TRANSLATIONS ===
    // The translation picker is driven entirely by QuranEditions.translations — the content
    // registry is the single source of truth. To add one, see docs/quran/content-registry.md;
    // nothing in this screen needs to change.

    // === ADDING NEW ARABIC FONTS ===
    // The font picker derives from the content registry. To add one, see
    // docs/quran/content-registry.md; nothing in this screen needs to change. The
    // selected id persists via PreferencesDataStore.quranArabicFont.

    val selectedFont = QuranArabicFont.fromId(quranState.selectedArabicFontId)

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.quran_settings),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Preview Card
            item {
                PreviewCard(
                    arabicFontSize = quranState.arabicFontSize,
                    arabicFontFamily = selectedFont.fontFamily,
                    showTransliteration = quranState.showTransliteration,
                    showTranslation = quranState.showTranslation
                )
            }

            // Arabic Text Section
            item {
                NimazSectionHeader(title = stringResource(R.string.arabic_text))
            }
            item {
                NimazMenuGroup {
                    // Arabic Font Size Slider
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.arabic_font_size),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.arabic_font_size_value,
                                    quranState.arabicFontSize.toInt()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(15.dp))
                        Slider(
                            value = quranState.arabicFontSize,
                            onValueChange = { viewModel.onEvent(SettingsEvent.SetArabicFontSize(it)) },
                            valueRange = 18f..42f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Arabic Font selector — Nimaz anchored dropdown. The list of
                    // options comes straight from QuranArabicFont.entries, so adding
                    // a font (see comment above) automatically adds a menu item, each
                    // rendered in its own typeface. The live preview lives in the
                    // PREVIEW card at the top of the screen.
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 14.dp
                        )
                    ) {
                        NimazDropdownField(
                            label = stringResource(R.string.arabic_font),
                            items = QuranArabicFont.entries.map { font ->
                                NimazDropdownItem(
                                    value = font.id,
                                    label = font.displayName,
                                    textFontFamily = font.fontFamily,
                                )
                            },
                            selected = selectedFont.id,
                            onSelected = { viewModel.onEvent(SettingsEvent.SetArabicFont(it)) }
                        )
                    }

                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Mushaf layout selector — chooses which edition the Mushaf (page) reader
                    // renders (issue #270). The options are derived from
                    // QuranEditions.mushafLayouts, so shipping a new layout needs no edit
                    // here. The choice persists via PreferencesDataStore.quranMushafScript
                    // and the reader picks its renderer + page count from it live.
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 14.dp
                        )
                    ) {
                        NimazDropdownField(
                            label = stringResource(R.string.mushaf_layout),
                            items = QuranEditions.mushafLayouts.map { layout ->
                                NimazDropdownItem(
                                    value = layout.id,
                                    label = layout.displayName,
                                )
                            },
                            selected = quranState.mushafScript.id,
                            onSelected = {
                                viewModel.onEvent(
                                    SettingsEvent.SetMushafScript(QuranEditions.layout(it))
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.mushaf_layout_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Display Options Section
            item {
                NimazSectionHeader(title = stringResource(R.string.display_options))
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.show_transliteration),
                        subtitle = stringResource(R.string.show_transliteration_subtitle),
                        checked = quranState.showTransliteration,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTransliteration(!quranState.showTransliteration)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.show_translation),
                        subtitle = stringResource(R.string.show_translation_subtitle),
                        checked = quranState.showTranslation,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTranslation(!quranState.showTranslation)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.continuous_reading),
                        subtitle = stringResource(R.string.continuous_reading_subtitle),
                        checked = quranState.continuousReading,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetContinuousReading(!quranState.continuousReading)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.keep_screen_on),
                        subtitle = stringResource(R.string.keep_screen_on_subtitle),
                        checked = quranState.keepScreenOn,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetKeepScreenOn(!quranState.keepScreenOn)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Tajweed colours need per-letter spans, which only exist for the
                    // Uthmani text — an IndoPak-sourced layout has none (see
                    // MushafLineLayout). The edition declares this, so a new layout
                    // needs no edit here. Disable the toggle with a reason rather than
                    // let it silently do nothing (#293).
                    val tajweedAvailable = quranState.mushafScript.supportsTajweed
                    NimazSettingsItem(
                        title = stringResource(R.string.show_tajweed_colors),
                        subtitle = if (tajweedAvailable) {
                            stringResource(R.string.show_tajweed_colors_subtitle)
                        } else {
                            stringResource(R.string.show_tajweed_colors_unavailable)
                        },
                        checked = quranState.showTajweed,
                        enabled = tajweedAvailable,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTajweed(!quranState.showTajweed)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Colour-blind-friendly mode: underline rule spans so they are
                    // marked by a non-hue channel too (#294).
                    NimazSettingsItem(
                        title = stringResource(R.string.tajweed_underline),
                        subtitle = stringResource(R.string.tajweed_underline_subtitle),
                        checked = quranState.tajweedUnderline,
                        enabled = tajweedAvailable && quranState.showTajweed,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetTajweedUnderline(!quranState.tajweedUnderline)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Colour legend — reachable regardless of script so users can
                    // learn what the colours mean (#294).
                    NimazSettingsItem(
                        title = stringResource(R.string.tajweed_colour_guide),
                        subtitle = stringResource(R.string.tajweed_colour_guide_subtitle),
                        showArrow = true,
                        onClick = { showTajweedLegend = true }
                    )
                }
            }

            // Translation Section
            item {
                NimazSectionHeader(title = stringResource(R.string.translation))
            }
            item {
                NimazMenuGroup {
                    val selectedTranslation =
                        QuranEditions.translation(quranState.selectedTranslatorId)
                    QuranEditions.translations.forEachIndexed { index, translation ->
                        TranslationItem(
                            name = translation.displayName,
                            // Derived from the edition's BCP-47 tag, so a non-English
                            // translation labels itself correctly with no new string resource.
                            language = Locale.forLanguageTag(translation.languageTag)
                                .getDisplayLanguage(Locale.getDefault())
                                .replaceFirstChar { it.uppercase(Locale.getDefault()) },
                            isSelected = selectedTranslation.id == translation.id,
                            onClick = {
                                viewModel.onEvent(SettingsEvent.SetTranslator(translation.id))
                            }
                        )
                        if (index < QuranEditions.translations.lastIndex) {
                            NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            // Audio Section
            item {
                NimazSectionHeader(title = stringResource(R.string.audio))
            }
            item {
                NimazMenuGroup {
                    // Reciter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSelectReciter() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reciter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Resolved through the registry. This was a fourth hand-kept copy
                            // of the reciter names, and the shortest: it covered five ids and
                            // fell through to `else -> id`, so choosing e.g. Ali Al-Hudhaify
                            // in the picker made this row read "hudhaify". It also named a
                            // reciter ("ghamdi") that no other copy could select.
                            Text(
                                text = QuranEditions.reciter(quranState.selectedReciterId)
                                    .displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        NimazIcon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            variant = NimazIconVariant.MUTED,
                            size = NimazIconSize.MEDIUM
                        )
                    }
                    // Continuous reading also controls auto-play of next verse in audio mode
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTajweedLegend) {
        TajweedLegendSheet(onDismiss = { showTajweedLegend = false })
    }
}

@Composable
private fun PreviewCard(
    arabicFontSize: Float,
    arabicFontFamily: FontFamily,
    showTransliteration: Boolean,
    showTranslation: Boolean
) {
    NimazMenuGroup {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header row with verse number badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quran_settings_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
                // Verse number badge (like in reader)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Arabic text - matching reader style
            Text(
                text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u064E\u0651\u0647\u0650 \u0627\u0644\u0631\u064E\u0651\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u064E\u0651\u062D\u0650\u064A\u0645\u0650",
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = arabicFontFamily,
                fontSize = arabicFontSize.sp,
                lineHeight = (arabicFontSize * 2.4f).sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Transliteration - styled like reader
            if (showTransliteration) {
                Spacer(modifier = Modifier.height(12.dp))
                NimazCard(
                    shape = RoundedCornerShape(10.dp),
                    tone = NimazTone.SUCCESS
                ) {
                    Text(
                        text = stringResource(R.string.quran_settings_preview_transliteration),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Translation - styled like reader
            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                NimazCard(
                    shape = RoundedCornerShape(10.dp),
                    style = NimazCardStyle.ELEVATED,
                    tone = NimazTone.NEUTRAL
                ) {
                    Text(
                        text = stringResource(R.string.quran_settings_preview_translation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Juz/Page info like in reader
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.quran_settings_preview_location),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun TranslationItem(
    name: String,
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(6.dp)
                )
                .then(
                    if (!isSelected) Modifier.background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(6.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            if (isSelected) {
                NimazIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    variant = NimazIconVariant.ON_ACCENT,
                    iconSize = 14.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = language,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
