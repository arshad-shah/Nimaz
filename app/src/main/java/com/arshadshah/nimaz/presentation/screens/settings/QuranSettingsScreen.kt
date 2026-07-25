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
import com.arshadshah.nimaz.domain.model.MushafScript
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
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelectReciter: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val quranState by viewModel.quranState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // === ADDING NEW TRANSLATIONS ===
    // To add a new translation:
    // 1. Ensure the translation data exists in the database (quran_translations table)
    //    with the translator_id matching the value below
    // 2. Add a new Pair to translationOptions: "Display Name" to "translator_id"
    // 3. The QuranRepository.getSurahWithAyahs() will automatically load the selected
    //    translator's text into Ayah.translation
    //
    // Available translator IDs in the Islamic Network API:
    // "en.sahih" - Sahih International (English)
    // "en.asad" - Muhammad Asad (English)
    // "en.pickthall" - Pickthall (English)
    // "en.yusufali" - Yusuf Ali (English)
    // Add more from: https://api.alquran.cloud/v1/edition?format=text&type=translation
    val translationOptions = listOf(
        "Sahih International" to "sahih_international"
    )

    // === ADDING NEW ARABIC FONTS ===
    // The font picker is driven entirely by the QuranArabicFont enum in
    // presentation/theme/Type.kt — it is the single source of truth. To add one:
    // 1. Add the font file(s) (.ttf/.otf) to app/src/main/res/font/
    // 2. In Type.kt, declare a FontFamily and add a QuranArabicFont entry
    //    (id + displayName + fontFamily)
    // That's it — this screen, the preview, and the reader all derive from the
    // enum, and the selected id persists via PreferencesDataStore.quranArabicFont.

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

                    // Mushaf script / layout selector — chooses which edition the Mushaf (page)
                    // reader renders: the default Uthmani/Madani 604-page layout, or the
                    // line-accurate 16-line IndoPak 548-page layout (issue #270). The choice
                    // persists via PreferencesDataStore.quranMushafScript and the reader picks
                    // its renderer + page count from it live.
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 14.dp
                        )
                    ) {
                        val mushafScriptLabels = mapOf(
                            MushafScript.MADANI to stringResource(R.string.mushaf_script_madani),
                            MushafScript.INDOPAK_16 to stringResource(R.string.mushaf_script_indopak16)
                        )
                        NimazDropdownField(
                            label = stringResource(R.string.mushaf_layout),
                            items = MushafScript.entries.map { script ->
                                NimazDropdownItem(
                                    value = script.name,
                                    label = mushafScriptLabels[script] ?: script.name,
                                )
                            },
                            selected = quranState.mushafScript.name,
                            onSelected = {
                                viewModel.onEvent(
                                    SettingsEvent.SetMushafScript(MushafScript.fromName(it))
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
                    NimazSettingsItem(
                        title = stringResource(R.string.show_tajweed_colors),
                        subtitle = stringResource(R.string.show_tajweed_colors_subtitle),
                        checked = quranState.showTajweed,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTajweed(!quranState.showTajweed)) }
                    )
                }
            }

            // Translation Section
            item {
                NimazSectionHeader(title = stringResource(R.string.translation))
            }
            item {
                NimazMenuGroup {
                    translationOptions.forEachIndexed { index, (displayName, value) ->
                        val isSelected = quranState.selectedTranslatorId == value
                        TranslationItem(
                            name = displayName,
                            language = stringResource(R.string.language_english),
                            isSelected = isSelected,
                            onClick = { viewModel.onEvent(SettingsEvent.SetTranslator(value)) }
                        )
                        if (index < translationOptions.lastIndex) {
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
                            Text(
                                text = quranState.selectedReciterId?.let { id ->
                                    when (id) {
                                        "sudais" -> "Abdul Rahman Al-Sudais"
                                        "ghamdi" -> "Saad Al-Ghamdi"
                                        "muaiqly" -> "Maher Al-Muaiqly"
                                        "abdulbasit" -> "Abdul Basit Abdul Samad"
                                        else -> id
                                    }
                                } ?: "Abdul Rahman Al-Sudais",
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
