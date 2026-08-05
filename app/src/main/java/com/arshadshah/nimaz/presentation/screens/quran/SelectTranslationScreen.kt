package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.theme.asLanguageLabel
import com.arshadshah.nimaz.presentation.theme.asTranslationText
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel

/**
 * Picks the Quran translation, as a dedicated screen in the shape of [SelectReciterScreen]:
 * search, a "currently selected" hero card, then the full list.
 *
 * It gets its own screen rather than living in Quran settings because the catalogue is 15
 * translations across 11 languages — inlined, that dominated a screen that already carries
 * font, script, tajweed and audio sections.
 *
 * The hero card previews the Bismillah in the selected translation and **updates as you tap
 * down the list**, so a translation can be judged by reading it rather than by its
 * translator's name. That preview is driven by
 * [SettingsViewModel.observeQuranPreviewTranslation];
 * because a translation's first read is also what seeds it, the card doubles as
 * confirmation the content actually landed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTranslationScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val quranState by viewModel.quranState.collectAsStateWithLifecycle()
    val selected = QuranTranslation.fromId(quranState.selectedTranslatorId)
    val arabicFont = QuranArabicFont.fromId(quranState.selectedArabicFontId)
    var searchQuery by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Translator and both language names are all searchable, so "Urdu", "اردو" and
    // "Maududi" all reach the same row.
    val grouped = remember(searchQuery) {
        QuranTranslation.entries
            .filter { translation ->
                searchQuery.isBlank() ||
                        translation.translator.contains(searchQuery, ignoreCase = true) ||
                        translation.language.englishName.contains(searchQuery, ignoreCase = true) ||
                        translation.language.nativeName.contains(searchQuery, ignoreCase = true)
            }
            .groupBy { it.language }
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.translation),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NimazSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" },
                    placeholder = stringResource(R.string.select_translation_search_hint)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.select_translation_currently_selected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SelectedTranslationCard(
                    translation = selected,
                    previewTranslation = quranState.previewTranslation,
                    arabicFontFamily = arabicFont.fontFamily
                )
            }

            item {
                Text(
                    text = stringResource(R.string.select_translation_all),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (grouped.isEmpty()) {
                item {
                    NimazEmptyState(
                        title = stringResource(R.string.picker_no_matches),
                        message = stringResource(R.string.no_results_hint),
                        icon = Icons.Default.Translate,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            grouped.forEach { (language, translations) ->
                item(key = "lang-${language.code}") {
                    // The endonym half of this header is written in its own script — "اردو"
                    // in a Latin body font falls back to whatever Naskh face the system has.
                    // Splitting it in two lets each half carry its own face rather than
                    // relying on glyph fallback inside a single run.
                    Row(
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.englishName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.labelMedium
                                .asLanguageLabel(language),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item(key = "group-${language.code}") {
                    NimazMenuGroup {
                        translations.forEachIndexed { index, translation ->
                            NimazMenuItem(
                                title = translation.translator,
                                subtitle = language.nativeName,
                                // The endonym again — Urdu needs Nastaliq and the extra
                                // leading that comes with it, even at subtitle size.
                                subtitleStyle = MaterialTheme.typography.bodySmall
                                    .asLanguageLabel(language),
                                trailingIcon = null,
                                selected = translation == selected,
                                onClick = {
                                    viewModel.onEvent(
                                        SettingsEvent.SetTranslator(translation.id)
                                    )
                                }
                            )
                            if (index < translations.lastIndex) {
                                NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/**
 * The hero card: which translation is active, and what it actually reads like.
 *
 * Shows the Bismillah — the verse every reader knows by heart — in Arabic above the selected
 * translation's rendering of it, so the comparison is immediate. The translation text keeps
 * the previous value while a newly tapped one resolves (its first read seeds 6,236 rows), so
 * the card never flashes empty mid-browse.
 */
@Composable
private fun SelectedTranslationCard(
    translation: QuranTranslation,
    previewTranslation: String?,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = Modifier.fillMaxWidth(),
        tone = NimazTone.ACCENT
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translation.translator,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${translation.language.englishName} · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = translation.language.nativeName,
                            style = MaterialTheme.typography.bodySmall
                                .asLanguageLabel(translation.language),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                NimazBadge(
                    text = stringResource(R.string.active),
                    tone = NimazTone.ACCENT,
                    emphasis = NimazBadgeEmphasis.SOFT,
                    size = NimazBadgeSize.LARGE,
                    icon = Icons.Filled.FiberManualRecord
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = BISMILLAH_TEXT,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = arabicFontFamily,
                lineHeight = 52.sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            NimazDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = previewTranslation
                    ?: stringResource(R.string.quran_settings_preview_translation),
                // Face, direction and leading all resolve from the translation's language —
                // the same helper the reader uses, so this preview cannot promise a rendering
                // the reader then fails to deliver.
                style = MaterialTheme.typography.bodyMedium
                    .asTranslationText(translation.language),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
