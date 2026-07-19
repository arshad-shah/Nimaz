package com.arshadshah.nimaz.presentation.screens.dua

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.DuaArabicText
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownField
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Reading preferences for the Dua reader. Deliberately mirrors
 * [com.arshadshah.nimaz.presentation.screens.settings.QuranSettingsScreen] so the
 * two readers feel consistent, but persists an independent set of dua prefs. The
 * Arabic font picker is driven by the shared [QuranArabicFont] enum.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val duaState by viewModel.duaState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val selectedFont = QuranArabicFont.fromId(duaState.selectedArabicFontId)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.dua_settings),
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

            // Live preview
            item {
                DuaPreviewCard(
                    arabicFontSize = duaState.arabicFontSize,
                    arabicFontFamily = selectedFont.fontFamily,
                    translationFontSize = duaState.translationFontSize,
                    showArabic = duaState.showArabic,
                    showTransliteration = duaState.showTransliteration,
                    showTranslation = duaState.showTranslation
                )
            }

            // Arabic text — size + font
            item { NimazSectionHeader(title = stringResource(R.string.arabic_text)) }
            item {
                NimazMenuGroup {
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
                                    duaState.arabicFontSize.toInt()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(15.dp))
                        Slider(
                            value = duaState.arabicFontSize,
                            onValueChange = { viewModel.onEvent(SettingsEvent.SetDuaArabicFontSize(it)) },
                            valueRange = 18f..42f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                            onSelected = { viewModel.onEvent(SettingsEvent.SetDuaArabicFont(it)) }
                        )
                    }
                }
            }

            // Translation size
            item { NimazSectionHeader(title = stringResource(R.string.translation)) }
            item {
                NimazMenuGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.translation_font_size),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.arabic_font_size_value,
                                    duaState.translationFontSize.toInt()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(15.dp))
                        Slider(
                            value = duaState.translationFontSize,
                            onValueChange = { viewModel.onEvent(SettingsEvent.SetDuaTranslationFontSize(it)) },
                            valueRange = 12f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // Display options
            item { NimazSectionHeader(title = stringResource(R.string.display_options)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.show_arabic),
                        subtitle = stringResource(R.string.show_arabic_subtitle),
                        checked = duaState.showArabic,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetDuaShowArabic(!duaState.showArabic)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.show_transliteration),
                        subtitle = stringResource(R.string.show_transliteration_subtitle),
                        checked = duaState.showTransliteration,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetDuaShowTransliteration(!duaState.showTransliteration)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.show_translation),
                        subtitle = stringResource(R.string.show_translation_subtitle),
                        checked = duaState.showTranslation,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetDuaShowTranslation(!duaState.showTranslation)) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DuaPreviewCard(
    arabicFontSize: Float,
    arabicFontFamily: FontFamily,
    translationFontSize: Float,
    showArabic: Boolean,
    showTransliteration: Boolean,
    showTranslation: Boolean
) {
    NimazMenuGroup {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.quran_settings_preview),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )

            if (showArabic) {
                Spacer(modifier = Modifier.height(15.dp))
                DuaArabicText(
                    text = stringResource(R.string.dua_settings_preview_arabic),
                    customFontSize = arabicFontSize,
                    fontFamily = arabicFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showTransliteration) {
                Spacer(modifier = Modifier.height(12.dp))
                NimazCard(
                    style = NimazCardStyle.OUTLINED,
                    shape = RoundedCornerShape(10.dp),
                    tone = NimazTone.SUCCESS,
                    elevation = 0.dp
                ) {
                    Text(
                        text = stringResource(R.string.dua_settings_preview_transliteration),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = translationFontSize.sp,
                            lineHeight = (translationFontSize * 1.5f).sp
                        ),
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                NimazCard(
                    style = NimazCardStyle.OUTLINED,
                    shape = RoundedCornerShape(10.dp),
                    tone = NimazTone.NEUTRAL,
                    elevation = 0.dp
                ) {
                    Text(
                        text = stringResource(R.string.dua_settings_preview_translation),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = translationFontSize.sp,
                            lineHeight = (translationFontSize * 1.5f).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
