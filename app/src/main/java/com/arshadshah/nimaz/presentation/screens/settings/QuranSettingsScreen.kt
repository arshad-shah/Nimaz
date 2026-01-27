package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

    val translationOptions = listOf(
        "Sahih International" to "en.sahih",
        "Muhammad Asad" to "en.asad",
        "Pickthall" to "en.pickthall",
        "Yusuf Ali" to "en.yusufali"
    )

    val arabicFontOptions = listOf(
        "Amiri Quran",
        "KFGQPC Uthmanic Script",
        "Scheherazade"
    )

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
                SectionTitle(title = "ARABIC TEXT")
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

                    // Font Options
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        arabicFontOptions.forEachIndexed { index, fontName ->
                            val isSelected = index == 0 // First option selected by default
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                border = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp, 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Radio circle
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                else
                                                    MaterialTheme.colorScheme.background,
                                                shape = CircleShape
                                            )
                                            .then(
                                                if (isSelected) Modifier
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.0f
                                                    ) else MaterialTheme.colorScheme.background,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            // Outer ring
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outline,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.1f
                                                            ) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    shape = CircleShape
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(15.dp))

                                    Column {
                                        Text(
                                            text = fontName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u064E\u0651\u0647\u0650",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Display Options Section
            item {
                SectionTitle(title = "DISPLAY OPTIONS")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    DisplayToggleItem(
                        label = "Show Transliteration",
                        subtitle = "Roman letters pronunciation",
                        isEnabled = quranState.showTransliteration,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetShowTransliteration(!quranState.showTransliteration)) }
                    )
                    SettingDivider()
                    DisplayToggleItem(
                        label = "Show Translation",
                        subtitle = "Meaning in your language",
                        isEnabled = quranState.showTranslation,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetShowTranslation(!quranState.showTranslation)) }
                    )
                    SettingDivider()
                    DisplayToggleItem(
                        label = "Continuous Reading",
                        subtitle = "Continue reading between surahs",
                        isEnabled = quranState.continuousReading,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetContinuousReading(!quranState.continuousReading)) }
                    )
                    SettingDivider()
                    DisplayToggleItem(
                        label = "Keep Screen On",
                        subtitle = "Prevent screen from turning off",
                        isEnabled = quranState.keepScreenOn,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetKeepScreenOn(!quranState.keepScreenOn)) }
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Translation Section
            item {
                SectionTitle(title = "TRANSLATION")
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
                            SettingDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Audio Section
            item {
                SectionTitle(title = "AUDIO")
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
                    SettingDivider()
                    // Auto-play toggle
                    DisplayToggleItem(
                        label = "Auto-play Next Verse",
                        subtitle = "Continue to next verse",
                        isEnabled = quranState.continuousReading,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetContinuousReading(!quranState.continuousReading)) }
                    )
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
            Text(
                text = "PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u064E\u0651\u0647\u0650 \u0627\u0644\u0631\u064E\u0651\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u064E\u0651\u062D\u0650\u064A\u0645\u0650",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = arabicFontSize.sp,
                lineHeight = (arabicFontSize * 2.4f).sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            if (showTransliteration) {
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "Bismillahir-Rahmanir-Rahim",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "In the name of Allah, the Most Gracious, the Most Merciful",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 5.dp, bottom = 12.dp)
    )
}

@Composable
private fun DisplayToggleItem(
    label: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
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

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}
