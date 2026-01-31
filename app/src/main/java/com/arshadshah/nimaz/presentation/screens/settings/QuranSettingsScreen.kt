package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
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
    // 1. Add the font file (.ttf/.otf) to app/src/main/res/font/
    // 2. In presentation/theme/Type.kt, create a new FontFamily:
    //    val NewFontFamily = FontFamily(Font(R.font.new_font_regular, FontWeight.Normal))
    // 3. In presentation/components/atoms/ArabicText.kt, add the font to fontFamilyOptions map
    // 4. Add a new entry to the arabicFontOptions list below
    // 5. Add a SettingsEvent.SetArabicFontFamily(id) event and wire it through
    //    SettingsViewModel -> PreferencesDataStore -> QuranViewModel -> ArabicText
    //
    // Currently using: Amiri (AmiriFontFamily from Type.kt)
    // Font files: res/font/amiri_regular.ttf, res/font/amiri_bold.ttf

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Quran Settings",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Preview Card
            item {
                PreviewCard(
                    arabicFontSize = quranState.arabicFontSize,
                    showTransliteration = quranState.showTransliteration,
                    showTranslation = quranState.showTranslation
                )
                Spacer(modifier = Modifier.height(25.dp))
            }

            // Arabic Text Section
            item {
                NimazSectionTitle(
                    text = "ARABIC TEXT",
                    modifier = Modifier.padding(start = 5.dp, bottom = 12.dp),
                    uppercase = false
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    // Arabic Font Size Slider
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arabic Font Size",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${quranState.arabicFontSize.toInt()}px",
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

                    // Font: Amiri (see comments above for adding new fonts)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arabic Font",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Amiri",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Display Options Section
            item {
                NimazSectionTitle(
                    text = "DISPLAY OPTIONS",
                    modifier = Modifier.padding(start = 5.dp, bottom = 12.dp),
                    uppercase = false
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    NimazSettingsItem(
                        title = "Show Transliteration",
                        subtitle = "Roman letters pronunciation",
                        checked = quranState.showTransliteration,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTransliteration(!quranState.showTransliteration)) }
                    )
                    NimazDivider()
                    NimazSettingsItem(
                        title = "Show Translation",
                        subtitle = "Meaning in your language",
                        checked = quranState.showTranslation,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTranslation(!quranState.showTranslation)) }
                    )
                    NimazDivider()
                    NimazSettingsItem(
                        title = "Continuous Reading",
                        subtitle = "Continue reading between surahs and auto-play next verse",
                        checked = quranState.continuousReading,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetContinuousReading(!quranState.continuousReading)) }
                    )
                    NimazDivider()
                    NimazSettingsItem(
                        title = "Keep Screen On",
                        subtitle = "Prevent screen from turning off",
                        checked = quranState.keepScreenOn,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetKeepScreenOn(!quranState.keepScreenOn)) }
                    )
                    NimazDivider()
                    NimazSettingsItem(
                        title = "Show Tajweed Colors",
                        subtitle = "Color-coded pronunciation rules",
                        checked = quranState.showTajweed,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetShowTajweed(!quranState.showTajweed)) }
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Translation Section
            item {
                NimazSectionTitle(
                    text = "TRANSLATION",
                    modifier = Modifier.padding(start = 5.dp, bottom = 12.dp),
                    uppercase = false
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    translationOptions.forEachIndexed { index, (displayName, value) ->
                        val isSelected = quranState.selectedTranslatorId == value
                        TranslationItem(
                            name = displayName,
                            language = "English",
                            isSelected = isSelected,
                            onClick = { viewModel.onEvent(SettingsEvent.SetTranslator(value)) }
                        )
                        if (index < translationOptions.lastIndex) {
                            NimazDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Audio Section
            item {
                NimazSectionTitle(
                    text = "AUDIO",
                    modifier = Modifier.padding(start = 5.dp, bottom = 12.dp),
                    uppercase = false
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
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
                                text = "Reciter",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = quranState.selectedReciterId?.let { id ->
                                    when (id) {
                                        "alafasy" -> "Mishary Rashid Alafasy"
                                        "sudais" -> "Abdul Rahman Al-Sudais"
                                        "ghamdi" -> "Saad Al-Ghamdi"
                                        "muaiqly" -> "Maher Al-Muaiqly"
                                        "abdulbasit" -> "Abdul Basit Abdul Samad"
                                        else -> id
                                    }
                                } ?: "Mishary Rashid Alafasy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Continuous reading also controls auto-play of next verse in audio mode
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PreviewCard(
    arabicFontSize: Float,
    showTransliteration: Boolean,
    showTranslation: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header row with verse number badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PREVIEW",
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
                fontSize = arabicFontSize.sp,
                lineHeight = (arabicFontSize * 2.4f).sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Transliteration - styled like reader
            if (showTransliteration) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "Bismillahir-Rahmanir-Rahim",
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "In the name of Allah, the Most Gracious, the Most Merciful",
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
                text = "Juz 1 \u2022 Page 1",
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
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
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
