package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NumberSelector
import com.arshadshah.nimaz.ui.components.common.RadioListItem
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.viewModel.AyatViewModel

//QuranBottomBar.kt
@Composable
fun QuranBottomBar(
    displaySettings: DisplaySettings,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog visibility states
    var showTranslationDialog by remember { mutableStateOf(false) }
    var showArabicSizeDialog by remember { mutableStateOf(false) }
    var showTranslationSizeDialog by remember { mutableStateOf(false) }
    var showFontStyleDialog by remember { mutableStateOf(false) }

    // Available options
    val translationOptions = listOf("English", "Urdu")
    val fontOptions = listOf("Default", "Quranme", "Hidayat", "Amiri", "IndoPak")

    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Translation Button
            IconButton(onClick = { showTranslationDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Translation Language",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Arabic Font Size Button
            IconButton(onClick = { showArabicSizeDialog = true }) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Arabic Font Size"
                )
            }

            // Translation Font Size Button
            IconButton(onClick = { showTranslationSizeDialog = true }) {
                Icon(
                    imageVector = Icons.Default.FontDownload,
                    contentDescription = "Translation Font Size"
                )
            }

            // Font Style Button
            IconButton(onClick = { showFontStyleDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = "Font Style"
                )
            }
        }
    }

    // Translation Language Dialog
    if (showTranslationDialog) {
        AlertDialogNimaz(
            topDivider = false,
            bottomDivider = false,
            dismissButtonText = "Close",
            contentHeight = 250.dp,
            contentDescription = "Translation Language",
            title = "Select Translation Language",
            contentToShow = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    translationOptions.forEach { option ->
                        RadioListItem(
                            item = option,
                            isSelected = displaySettings.translation == option,
                            index = translationOptions.indexOf(option),
                            onSelected = {
                                onEvent(
                                    AyatViewModel.AyatEvent.UpdateDisplaySettings(
                                        displaySettings.copy(translation = option)
                                    )
                                )
                                showTranslationDialog = false
                            }
                        )
                    }
                }
            },
            onDismissRequest = { showTranslationDialog = false },
            onConfirm = { showTranslationDialog = false },
            onDismiss = { showTranslationDialog = false }
        )
    }

    // Arabic Font Size Dialog
    if (showArabicSizeDialog) {
        AlertDialogNimaz(
            topDivider = false,
            bottomDivider = false,
            dismissButtonText = "Close",
            contentHeight = 200.dp,
            contentDescription = "Arabic Font Size",
            title = "Adjust Arabic Font Size",
            contentToShow = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NumberSelector(
                        value = displaySettings.arabicFontSize,
                        onValueChange = { newValue ->
                            onEvent(
                                AyatViewModel.AyatEvent.UpdateDisplaySettings(
                                    displaySettings.copy(arabicFontSize = newValue)
                                )
                            )
                        },
                        minValue = if (displaySettings.arabicFont == "IndoPak") 32f else 24f,
                        maxValue = if (displaySettings.arabicFont == "IndoPak") 60f else 46f
                    )
                }
            },
            onDismissRequest = { showArabicSizeDialog = false },
            onConfirm = { showArabicSizeDialog = false },
            onDismiss = { showArabicSizeDialog = false }
        )
    }

    // Translation Font Size Dialog
    if (showTranslationSizeDialog) {
        AlertDialogNimaz(
            topDivider = false,
            bottomDivider = false,
            dismissButtonText = "Close",
            contentHeight = 200.dp,
            contentDescription = "Translation Font Size",
            title = "Adjust Translation Font Size",
            contentToShow = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NumberSelector(
                        value = displaySettings.translationFontSize,
                        onValueChange = { newValue ->
                            onEvent(
                                AyatViewModel.AyatEvent.UpdateDisplaySettings(
                                    displaySettings.copy(translationFontSize = newValue)
                                )
                            )
                        },
                        minValue = 16f,
                        maxValue = 40f
                    )
                }
            },
            onDismissRequest = { showTranslationSizeDialog = false },
            onConfirm = { showTranslationSizeDialog = false },
            onDismiss = { showTranslationSizeDialog = false }
        )
    }

    // Font Style Dialog
    if (showFontStyleDialog) {
        AlertDialogNimaz(
            topDivider = false,
            bottomDivider = false,
            dismissButtonText = "Close",
            contentHeight = 250.dp,
            contentDescription = "Font Style",
            title = "Select Font Style",
            contentToShow = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontOptions.forEach { option ->
                        RadioListItem(
                            item = option,
                            isSelected = displaySettings.arabicFont == option,
                            index = fontOptions.indexOf(option),
                            onSelected = {
                                val newSettings = displaySettings.copy(arabicFont = option)
                                // Adjust font sizes based on font style
                                val adjustedSettings = when (option) {
                                    "IndoPak" -> newSettings.copy(
                                        arabicFontSize = maxOf(32f, newSettings.arabicFontSize)
                                    )

                                    else -> newSettings.copy(
                                        arabicFontSize = minOf(46f, newSettings.arabicFontSize)
                                    )
                                }
                                onEvent(
                                    AyatViewModel.AyatEvent.UpdateDisplaySettings(
                                        adjustedSettings
                                    )
                                )
                                showFontStyleDialog = false
                            }
                        )
                    }
                }
            },
            onDismissRequest = { showFontStyleDialog = false },
            onConfirm = { showFontStyleDialog = false },
            onDismiss = { showFontStyleDialog = false }
        )
    }
}