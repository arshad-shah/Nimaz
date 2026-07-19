package com.arshadshah.nimaz.presentation.screens.hadith

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.HadithArabicText
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
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Reading preferences for the Hadith reader. Mirrors the Dua/Quran settings
 * screens (preview · Arabic size + font · translation size · show toggles) and
 * adds the two Hadith-specific toggles: show grade badge and show chain of
 * narration. Persists an independent set of Hadith prefs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val hadithState by viewModel.hadithState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val selectedFont = QuranArabicFont.fromId(hadithState.selectedArabicFontId)

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.hadith_settings),
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

            item {
                HadithPreviewCard(
                    arabicFontSize = hadithState.arabicFontSize,
                    arabicFontFamily = selectedFont.fontFamily,
                    translationFontSize = hadithState.translationFontSize,
                    showArabic = hadithState.showArabic,
                    showTranslation = hadithState.showTranslation,
                    showGrade = hadithState.showGrade
                )
            }

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
                                    hadithState.arabicFontSize.toInt()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(15.dp))
                        Slider(
                            value = hadithState.arabicFontSize,
                            onValueChange = {
                                viewModel.onEvent(
                                    SettingsEvent.SetHadithArabicFontSize(
                                        it
                                    )
                                )
                            },
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
                            onSelected = { viewModel.onEvent(SettingsEvent.SetHadithArabicFont(it)) }
                        )
                    }
                }
            }

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
                                    hadithState.translationFontSize.toInt()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(15.dp))
                        Slider(
                            value = hadithState.translationFontSize,
                            onValueChange = {
                                viewModel.onEvent(
                                    SettingsEvent.SetHadithTranslationFontSize(
                                        it
                                    )
                                )
                            },
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

            item { NimazSectionHeader(title = stringResource(R.string.display_options)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.show_arabic),
                        subtitle = stringResource(R.string.show_arabic_subtitle),
                        checked = hadithState.showArabic,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetHadithShowArabic(!hadithState.showArabic)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.show_translation),
                        subtitle = stringResource(R.string.show_translation_subtitle),
                        checked = hadithState.showTranslation,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetHadithShowTranslation(
                                    !hadithState.showTranslation
                                )
                            )
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.hadith_show_grade),
                        subtitle = stringResource(R.string.hadith_show_grade_subtitle),
                        checked = hadithState.showGrade,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetHadithShowGrade(!hadithState.showGrade)) }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.hadith_show_chain),
                        subtitle = stringResource(R.string.hadith_show_chain_subtitle),
                        checked = hadithState.showChain,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetHadithShowChain(!hadithState.showChain)) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HadithPreviewCard(
    arabicFontSize: Float,
    arabicFontFamily: FontFamily,
    translationFontSize: Float,
    showArabic: Boolean,
    showTranslation: Boolean,
    showGrade: Boolean
) {
    NimazMenuGroup {
        Column(modifier = Modifier.padding(20.dp)) {
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
                if (showGrade) {
                    HadithGradeChip(
                        label = stringResource(R.string.hadith_grade_sahih),
                        color = SahihGreen
                    )
                }
            }

            if (showArabic) {
                Spacer(modifier = Modifier.height(15.dp))
                HadithArabicText(
                    text = stringResource(R.string.hadith_settings_preview_arabic),
                    customFontSize = arabicFontSize,
                    fontFamily = arabicFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showTranslation) {
                Spacer(modifier = Modifier.height(12.dp))
                NimazCard(
                    style = NimazCardStyle.OUTLINED,
                    shape = RoundedCornerShape(10.dp),
                    tone = NimazTone.NEUTRAL,
                    elevation = 0.dp
                ) {
                    Text(
                        text = stringResource(R.string.hadith_settings_preview_translation),
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
