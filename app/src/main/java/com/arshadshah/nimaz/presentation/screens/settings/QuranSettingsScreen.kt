package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val quranState by viewModel.quranState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val translationOptions = listOf(
        "Sahih International" to "en.sahih",
        "Yusuf Ali" to "en.yusufali",
        "Pickthall" to "en.pickthall",
        "Muhammad Asad" to "en.asad",
        "Dr. Mustafa Khattab" to "en.khattab"
    )

    val reciterOptions = listOf(
        "Mishary Rashid Alafasy" to "alafasy",
        "Abdul Rahman Al-Sudais" to "sudais",
        "Saad Al-Ghamdi" to "ghamdi",
        "Maher Al-Muaiqly" to "muaiqly",
        "Abdul Basit Abdul Samad" to "abdulbasit"
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reading Settings
            item {
                SectionHeader(
                    title = "Reading",
                    icon = Icons.Default.MenuBook
                )
            }

            item {
                QuranToggleCard(
                    title = "Show Translation",
                    subtitle = "Display English translation below Arabic",
                    icon = Icons.Default.Translate,
                    isEnabled = quranState.showTranslation,
                    onToggle = { viewModel.onEvent(SettingsEvent.SetShowTranslation(!quranState.showTranslation)) }
                )
            }

            item {
                QuranToggleCard(
                    title = "Show Transliteration",
                    subtitle = "Display phonetic transliteration",
                    icon = Icons.Default.FormatSize,
                    isEnabled = quranState.showTransliteration,
                    onToggle = { viewModel.onEvent(SettingsEvent.SetShowTransliteration(!quranState.showTransliteration)) }
                )
            }

            item {
                QuranToggleCard(
                    title = "Continuous Reading",
                    subtitle = "Continue reading between surahs",
                    icon = Icons.Default.MenuBook,
                    isEnabled = quranState.continuousReading,
                    onToggle = { viewModel.onEvent(SettingsEvent.SetContinuousReading(!quranState.continuousReading)) }
                )
            }

            item {
                QuranToggleCard(
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off while reading",
                    icon = Icons.Default.ScreenLockPortrait,
                    isEnabled = quranState.keepScreenOn,
                    onToggle = { viewModel.onEvent(SettingsEvent.SetKeepScreenOn(!quranState.keepScreenOn)) }
                )
            }

            // Font Size
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Font Size",
                    icon = Icons.Default.FormatSize
                )
            }

            item {
                FontSizeCard(
                    label = "Arabic Font Size",
                    size = quranState.arabicFontSize,
                    onSizeChange = { viewModel.onEvent(SettingsEvent.SetArabicFontSize(it)) }
                )
            }

            item {
                FontSizeCard(
                    label = "Translation Font Size",
                    size = quranState.translationFontSize,
                    onSizeChange = { viewModel.onEvent(SettingsEvent.SetTranslationFontSize(it)) }
                )
            }

            // Translation
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Translation",
                    icon = Icons.Default.Translate
                )
            }

            item {
                SelectionCard(
                    options = translationOptions,
                    selectedOption = quranState.selectedTranslatorId,
                    onOptionSelected = { viewModel.onEvent(SettingsEvent.SetTranslator(it)) }
                )
            }

            // Audio Settings
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Audio",
                    icon = Icons.Default.Headphones
                )
            }

            item {
                SelectionCard(
                    title = "Reciter",
                    options = reciterOptions,
                    selectedOption = quranState.selectedReciterId ?: "",
                    onOptionSelected = { viewModel.onEvent(SettingsEvent.SetReciter(it)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NimazColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QuranToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) NimazColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun FontSizeCard(
    label: String,
    size: Float,
    onSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${size.toInt()}sp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = size,
                onValueChange = onSizeChange,
                valueRange = 14f..40f,
                steps = 25
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Small",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Large",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectionCard(
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            options.forEach { (displayName, value) ->
                SelectionOption(
                    displayName = displayName,
                    isSelected = selectedOption == value,
                    onClick = { onOptionSelected(value) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionOption(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            NimazColors.Primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NimazColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
