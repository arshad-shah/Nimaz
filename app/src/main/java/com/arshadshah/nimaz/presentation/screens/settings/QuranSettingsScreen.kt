package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranReciter
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownField
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsSlider
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.TajweedLegendSheet
import com.arshadshah.nimaz.presentation.foundation.text.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.theme.asTranslationText
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel

/**
 * Quran reading preferences.
 *
 * ## Ordering
 * The screen is ordered the way a reader actually thinks about the page, top to bottom:
 *
 * 1. **Preview** — the sample ayah, so every control below has something to change.
 * 2. **Mushaf script** — the widest-reaching choice on the screen. It decides which edition
 *    the reader renders, and it *gates* tajweed (only Madani carries per-letter spans), so it
 *    has to be settled before the settings it constrains. It used to sit third inside "Arabic
 *    Text", below the font controls it silently overrides.
 * 3. **Arabic text** — face and size for the Arabic itself.
 * 4. **Translation** — which translation, whether to show it, and how big. Previously
 *    "show translation" lived under Display Options while the translation *picker* was two
 *    sections further down, so the two halves of one decision were never on screen together.
 * 5. **Tajweed** — its own section rather than three rows buried at the end of a
 *    seven-toggle list, with the availability note attached to the section.
 * 6. **Audio** — the reciter.
 * 7. **Reading** — behaviour that is neither look nor content (continuous reading, screen on).
 *
 * ## Adding new translations
 * The translation picker is driven entirely by the `QuranTranslation` enum in
 * `domain/model/QuranTranslation.kt` — it is the single source of truth. To add one:
 * 1. Add the edition in the arshad-shah/nimaz-data repository and run the importer, which
 *    writes a `tr.<id>` collection into the artifact the app fetches.
 * 2. Add a matching entry to the `QuranTranslation` enum.
 *
 * ## Adding new Arabic fonts
 * Driven entirely by the `QuranArabicFont` enum in `presentation/theme/Type.kt`:
 * 1. Add the font file(s) to `app/src/main/res/font/`.
 * 2. Declare a `FontFamily` and add a `QuranArabicFont` entry (id + displayName + fontFamily).
 *
 * ## Adding new reciters
 * Driven by the `QuranReciter` enum in `domain/model/QuranReciter.kt`, plus its CDN edition in
 * `QuranAudioManager.RECITER_CDN_MAP`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelectReciter: () -> Unit = {},
    onNavigateToSelectTranslation: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val quranState by viewModel.quranState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showTajweedLegend by remember { mutableStateOf(false) }

    val selectedTranslation = QuranTranslation.fromId(quranState.selectedTranslatorId)
    val selectedFont = QuranArabicFont.fromId(quranState.selectedArabicFontId)
    val selectedReciter = QuranReciter.fromId(quranState.selectedReciterId)

    // Tajweed colours are only available for the Madani (Uthmani) script — the IndoPak
    // layouts have no per-letter spans (see MushafLineLayout). Disable the toggles with a
    // reason rather than let them silently do nothing (#293).
    val tajweedAvailable = quranState.mushafScript == MushafScript.MADANI

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

            // ── 1. Live preview ──────────────────────────────────────────────────────────
            item {
                PreviewCard(
                    arabicFontSize = quranState.arabicFontSize,
                    arabicFontFamily = selectedFont.fontFamily,
                    showTransliteration = quranState.showTransliteration,
                    showTranslation = quranState.showTranslation,
                    translationText = quranState.previewTranslation,
                    translationFontSize = quranState.translationFontSize,
                    translationLanguage = selectedTranslation.language
                )
            }

            // ── 2. Mushaf script — the choice the rest of the screen hangs off ───────────
            item { NimazSectionHeader(title = stringResource(R.string.mushaf_layout)) }
            item {
                NimazMenuGroup {
                    // Chooses which edition the Mushaf (page) reader renders: the default
                    // ayah-flow Uthmani/Madani 604-page layout, or one of the line-accurate
                    // IndoPak editions (#270). Persists via
                    // PreferencesDataStore.quranMushafScript; the reader picks its renderer
                    // and page count from it live.
                    Column(modifier = Modifier.padding(16.dp)) {
                        val mushafScriptLabels = mapOf(
                            MushafScript.MADANI to stringResource(R.string.mushaf_script_madani),
                            MushafScript.INDOPAK_16 to stringResource(R.string.mushaf_script_indopak16),
                            MushafScript.INDOPAK_15 to stringResource(R.string.mushaf_script_indopak15),
                            MushafScript.INDOPAK_13 to stringResource(R.string.mushaf_script_indopak13)
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

            // ── 3. Arabic text ──────────────────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.arabic_text)) }
            item {
                NimazMenuGroup {
                    // The options come straight from QuranArabicFont.entries, each rendered
                    // in its own typeface. The live preview is the card at the top.
                    Column(modifier = Modifier.padding(16.dp)) {
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

                    NimazSettingsSlider(
                        title = stringResource(R.string.arabic_font_size),
                        valueLabel = stringResource(
                            R.string.arabic_font_size_value,
                            quranState.arabicFontSize.toInt()
                        ),
                        value = quranState.arabicFontSize,
                        onValueChange = { viewModel.onEvent(SettingsEvent.SetArabicFontSize(it)) },
                        valueRange = 18f..42f,
                        contentDescription = stringResource(R.string.arabic_font_size)
                    )
                }
            }

            // ── 4. Translation: which one, whether to show it, how big ──────────────────
            item { NimazSectionHeader(title = stringResource(R.string.translation)) }
            item {
                NimazMenuGroup {
                    // One row into a dedicated screen, in the shape of the reciter picker,
                    // rather than 15 rows inlined here. That screen has room for the live
                    // Bismillah preview that makes a translation judgeable by reading it.
                    // Translator *and* language on one line. This row used to pass the
                    // translator as `value` alongside a `subtitle`, and NimazSettingsItem
                    // rendered `subtitle ?: value` — so it showed "English" and the
                    // translator's name, the one thing the row exists to report, never
                    // appeared at all.
                    NimazSettingsItem(
                        title = stringResource(R.string.translation),
                        subtitle = stringResource(
                            R.string.settings_value_with_qualifier,
                            selectedTranslation.translator,
                            selectedTranslation.language.englishName
                        ),
                        icon = Icons.Default.Translate,
                        onClick = onNavigateToSelectTranslation,
                        showArrow = true
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.show_translation),
                        subtitle = stringResource(R.string.show_translation_subtitle),
                        checked = quranState.showTranslation,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetShowTranslation(!quranState.showTranslation)
                            )
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // The Quran reader has always read this preference (QuranViewModel ->
                    // AyahItem.fontSize) but no screen ever offered a control for it, unlike
                    // the Dua and Hadith settings screens — so Quran translation size was
                    // stuck at its 16sp default for everyone.
                    NimazSettingsSlider(
                        title = stringResource(R.string.translation_font_size),
                        valueLabel = stringResource(
                            R.string.arabic_font_size_value,
                            quranState.translationFontSize.toInt()
                        ),
                        value = quranState.translationFontSize,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetTranslationFontSize(it))
                        },
                        valueRange = 12f..28f,
                        enabled = quranState.showTranslation,
                        contentDescription = stringResource(R.string.translation_font_size)
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.show_transliteration),
                        subtitle = stringResource(R.string.show_transliteration_subtitle),
                        checked = quranState.showTransliteration,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetShowTransliteration(!quranState.showTransliteration)
                            )
                        }
                    )
                }
            }

            // ── 5. Tajweed ──────────────────────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.tajweed_section)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.show_tajweed_colors),
                        subtitle = if (tajweedAvailable) {
                            stringResource(R.string.show_tajweed_colors_subtitle)
                        } else {
                            stringResource(R.string.show_tajweed_colors_unavailable)
                        },
                        checked = quranState.showTajweed,
                        enabled = tajweedAvailable,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetShowTajweed(!quranState.showTajweed))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Colour-blind-friendly mode: underline rule spans so they are marked by
                    // a non-hue channel too (#294).
                    NimazSettingsItem(
                        title = stringResource(R.string.tajweed_underline),
                        subtitle = stringResource(R.string.tajweed_underline_subtitle),
                        checked = quranState.tajweedUnderline,
                        enabled = tajweedAvailable && quranState.showTajweed,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetTajweedUnderline(!quranState.tajweedUnderline)
                            )
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Reachable regardless of script so users can learn what the colours
                    // mean even on a layout that cannot show them (#294).
                    NimazSettingsItem(
                        title = stringResource(R.string.tajweed_colour_guide),
                        subtitle = stringResource(R.string.tajweed_colour_guide_subtitle),
                        showArrow = true,
                        onClick = { showTajweedLegend = true }
                    )
                }
            }

            // ── 6. Audio ────────────────────────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.audio)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.reciter),
                        subtitle = stringResource(
                            R.string.settings_value_with_qualifier,
                            selectedReciter.displayName,
                            selectedReciter.country
                        ),
                        icon = Icons.Default.Mic,
                        onClick = onNavigateToSelectReciter,
                        showArrow = true
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Under Audio, where it belongs. Called "Continuous Reading" and filed
                    // under Reading behaviour, it read as a twin of the player's "Follow
                    // along" — which is the *scroll*. This one is the playlist: whether the
                    // recitation carries on past the end of a verse.
                    NimazSettingsItem(
                        title = stringResource(R.string.continuous_reading),
                        subtitle = stringResource(R.string.continuous_reading_subtitle),
                        checked = quranState.continuousReading,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetContinuousReading(!quranState.continuousReading)
                            )
                        }
                    )
                }
            }

            // ── 7. Reading behaviour ────────────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.reading_section)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.keep_screen_on),
                        subtitle = stringResource(R.string.keep_screen_on_subtitle),
                        checked = quranState.keepScreenOn,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetKeepScreenOn(!quranState.keepScreenOn)
                            )
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
    showTranslation: Boolean,
    translationText: String?,
    translationFontSize: Float,
    translationLanguage: TranslationLanguage
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
                text = BISMILLAH_TEXT,
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = arabicFontFamily,
                fontSize = arabicFontSize.sp,
                lineHeight = (arabicFontSize * 2.4f).sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation - styled like reader
            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                NimazCard(
                    shape = RoundedCornerShape(10.dp),
                    style = NimazCardStyle.ELEVATED,
                    tone = NimazTone.NEUTRAL
                ) {
                    Text(
                        // The real text of the selected translation, so switching in the
                        // picker is visible here immediately. Falls back to the bundled
                        // sample only before the first load resolves.
                        text = translationText
                            ?: stringResource(R.string.quran_settings_preview_translation),
                        // Face, direction and leading all resolve from the translation's
                        // language — the reader uses the same helper, so the preview cannot
                        // promise a rendering the reader then fails to deliver.
                        style = MaterialTheme.typography.bodyMedium
                            .asTranslationText(translationLanguage, translationFontSize.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            // Transliteration - styled like reader
            if (showTransliteration) {
                Spacer(modifier = Modifier.height(10.dp))
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
