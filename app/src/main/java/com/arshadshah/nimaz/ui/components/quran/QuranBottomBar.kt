package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NumberSelector
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.viewModel.AyatViewModel

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
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
                // Translation Button
                BottomBarItem(
                    icon = Icons.Default.Translate,
                    label = "Language",
                    subtitle = displaySettings.translation,
                    onClick = { showTranslationDialog = true }
                )

                // Arabic Font Size Button
                BottomBarItem(
                    icon = Icons.Default.TextFields,
                    label = "Arabic",
                    subtitle = "${displaySettings.arabicFontSize.toInt()}sp",
                    onClick = { showArabicSizeDialog = true }
                )

                // Translation Font Size Button
                BottomBarItem(
                    icon = Icons.Default.FontDownload,
                    label = "Translation",
                    subtitle = "${displaySettings.translationFontSize.toInt()}sp",
                    onClick = { showTranslationSizeDialog = true }
                )

                // Font Style Button
                BottomBarItem(
                    icon = Icons.Default.Style,
                    label = "Font",
                    subtitle = displaySettings.arabicFont,
                    onClick = { showFontStyleDialog = true }
                )
            }
        }

    // Translation Language Dialog
    if (showTranslationDialog) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.document_icon),
            title = "Translation Language",
            contentDescription = "Select your preferred translation language",
            description = "Choose the language for Quran translation",
            contentHeight = 200.dp,
            cardContent = false,
            onDismissRequest = { showTranslationDialog = false },
            onConfirm = { showTranslationDialog = false },
            confirmButtonText = "Done",
            showDismissButton = false,
            onDismiss = { showTranslationDialog = false },
            action = {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = displaySettings.translation,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            },
            contentToShow = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    translationOptions.forEach { option ->
                        SelectionOptionCard(
                            title = option,
                            isSelected = displaySettings.translation == option,
                            onClick = {
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
            }
        )
    }

    // Arabic Font Size Dialog
    if (showArabicSizeDialog) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.quran_icon),
            title = "Arabic Font Size",
            contentDescription = "Adjust the Arabic text size",
            description = "Adjust the size of Arabic text for comfortable reading",
            contentHeight = 100.dp,
            cardContent = false,
            onDismissRequest = { showArabicSizeDialog = false },
            onConfirm = { showArabicSizeDialog = false },
            confirmButtonText = "Done",
            showDismissButton = false,
            onDismiss = { showArabicSizeDialog = false },
            action = {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${displaySettings.arabicFontSize.toInt()}sp",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            },
            contentToShow = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ){
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
            }
        )
    }

    // Translation Font Size Dialog
    if (showTranslationSizeDialog) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.document_icon),
            title = "Translation Font Size",
            contentDescription = "Adjust the translation text size",
            description = "Adjust the size of translation text for comfortable reading",
            contentHeight = 100.dp,
            cardContent = false,
            onDismissRequest = { showTranslationSizeDialog = false },
            onConfirm = { showTranslationSizeDialog = false },
            confirmButtonText = "Done",
            showDismissButton = false,
            onDismiss = { showTranslationSizeDialog = false },
            action = {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${displaySettings.translationFontSize.toInt()}sp",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            },
            contentToShow = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ){
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
            }
        )
    }

    // Font Style Dialog
    if (showFontStyleDialog) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.quran_icon),
            title = "Arabic Font Style",
            contentDescription = "Select Arabic font style",
            description = "Choose your preferred Arabic font for Quran reading",
            contentHeight = 320.dp,
            cardContent = false,
            onDismissRequest = { showFontStyleDialog = false },
            onConfirm = { showFontStyleDialog = false },
            confirmButtonText = "Done",
            showDismissButton = false,
            onDismiss = { showFontStyleDialog = false },
            action = {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = displaySettings.arabicFont,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            },
            contentToShow = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontOptions.forEach { option ->
                        SelectionOptionCard(
                            title = option,
                            subtitle = when (option) {
                                "Default" -> "Standard Arabic font"
                                "Quranme" -> "Clean modern style"
                                "Hidayat" -> "Traditional style"
                                "Amiri" -> "Elegant Naskh style"
                                "IndoPak" -> "South Asian style"
                                else -> null
                            },
                            isSelected = displaySettings.arabicFont == option,
                            onClick = {
                                val newSettings = displaySettings.copy(arabicFont = option)
                                val adjustedSettings = when (option) {
                                    "IndoPak" -> newSettings.copy(
                                        arabicFontSize = maxOf(32f, newSettings.arabicFontSize)
                                    )
                                    else -> newSettings.copy(
                                        arabicFontSize = minOf(46f, newSettings.arabicFontSize)
                                    )
                                }
                                onEvent(
                                    AyatViewModel.AyatEvent.UpdateDisplaySettings(adjustedSettings)
                                )
                                showFontStyleDialog = false
                            }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "backgroundColor"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Current value
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SelectionOptionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "backgroundColor"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuranBottomBarPreview() {
    NimazTheme {
        QuranBottomBar(
            displaySettings = DisplaySettings(
                translation = "English",
                arabicFontSize = 28f,
                translationFontSize = 18f,
                arabicFont = "Default"
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Bottom Bar Item")
@Composable
private fun BottomBarItemPreview() {
    NimazTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomBarItem(
                    icon = Icons.Default.Translate,
                    label = "Language",
                    subtitle = "English",
                    onClick = {}
                )
                BottomBarItem(
                    icon = Icons.Default.TextFields,
                    label = "Arabic",
                    subtitle = "28sp",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Selection Option Card")
@Composable
private fun SelectionOptionCardPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionOptionCard(
                title = "Default",
                subtitle = "Standard Arabic font",
                isSelected = true,
                onClick = {}
            )
            SelectionOptionCard(
                title = "Amiri",
                subtitle = "Elegant Naskh style",
                isSelected = false,
                onClick = {}
            )
        }
    }
}