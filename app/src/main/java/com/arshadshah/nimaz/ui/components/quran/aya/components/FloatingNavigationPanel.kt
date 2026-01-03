package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType

/**
 * Navigation dialog for Quran reading - allows jumping to specific ayas,
 * and navigating between surahs
 */
@Composable
fun QuranNavigationDialog(
    isVisible: Boolean,
    currentSurah: Int,
    currentAya: Int,
    totalAyas: Int,
    onDismiss: () -> Unit,
    onJumpToAya: (Int) -> Unit,
    onNextSurah: (() -> Unit)? = null,
    onPreviousSurah: (() -> Unit)? = null
) {
    if (!isVisible) return

    var ayaInput by remember { mutableStateOf("") }

    AlertDialogNimaz(
        icon = painterResource(id = R.drawable.quran_icon),
        title = "Quick Navigation",
        contentDescription = "Navigate to specific aya or surah",
        description = "Surah $currentSurah • Aya $currentAya of $totalAyas",
        contentHeight = 320.dp,
        cardContent = false,
        onDismissRequest = onDismiss,
        onConfirm = {
            ayaInput.toIntOrNull()?.let { aya ->
                if (aya in 1..totalAyas) {
                    onJumpToAya(aya)
                    onDismiss()
                }
            }
        },
        confirmButtonText = "Jump",
        showConfirmButton = ayaInput.isNotBlank() && ayaInput.toIntOrNull()?.let { it in 1..totalAyas } == true,
        onDismiss = onDismiss,
        dismissButtonText = "Close",
        action = {
            // Progress indicator as action badge
            ProgressBadge(
                label = "${((currentAya.toFloat() / totalAyas.toFloat()) * 100).toInt()}%"
            )
        },
        contentToShow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Jump to aya input
                NimazTextField(
                    value = ayaInput,
                    onValueChange = { ayaInput = it },
                    type = NimazTextFieldType.NUMBER,
                    label = "Jump to Aya",
                    placeholder = "Enter aya number (1-$totalAyas)",
                    isError = ayaInput.isNotBlank() && ayaInput.toIntOrNull()?.let { it !in 1..totalAyas } == true,
                    errorMessage = "Enter a number between 1 and $totalAyas"
                )

                // Quick actions
                QuickActionsSection(
                    totalAyas = totalAyas,
                    onJumpToAya = onJumpToAya,
                    onDismiss = onDismiss
                )

                // Surah navigation buttons
                if (onPreviousSurah != null || onNextSurah != null) {
                    SurahNavigationSection(
                        currentSurah = currentSurah,
                        onPreviousSurah = onPreviousSurah,
                        onNextSurah = onNextSurah,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    )
}

@Composable
private fun ProgressBadge(
    label: String
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QuickActionsSection(
    totalAyas: Int,
    onJumpToAya: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationAction(
                    icon = Icons.Default.FirstPage,
                    text = "First",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        onJumpToAya(1)
                        onDismiss()
                    }
                )
                NavigationAction(
                    icon = Icons.AutoMirrored.Filled.LastPage,
                    text = "Last",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        onJumpToAya(totalAyas)
                        onDismiss()
                    }
                )
                NavigationAction(
                    icon = Icons.Default.Shuffle,
                    text = "Random",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        onJumpToAya((1..totalAyas).random())
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SurahNavigationSection(
    currentSurah: Int,
    onPreviousSurah: (() -> Unit)?,
    onNextSurah: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Surah Navigation",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentSurah > 1 && onPreviousSurah != null) {
                    FilledTonalButton(
                        onClick = {
                            onPreviousSurah()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Previous",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (currentSurah < 114 && onNextSurah != null) {
                    FilledTonalButton(
                        onClick = {
                            onNextSurah()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            "Next",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationAction(
    icon: ImageVector,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            shadowElevation = 2.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuranNavigationDialogPreview() {
    QuranNavigationDialog(
        isVisible = true,
        currentSurah = 2,
        currentAya = 255,
        totalAyas = 286,
        onDismiss = {},
        onJumpToAya = {},
        onNextSurah = {},
        onPreviousSurah = {}
    )
}

@Preview(showBackground = true, name = "First Surah")
@Composable
fun QuranNavigationDialogFirstSurahPreview() {
    QuranNavigationDialog(
        isVisible = true,
        currentSurah = 1,
        currentAya = 5,
        totalAyas = 7,
        onDismiss = {},
        onJumpToAya = {},
        onNextSurah = {},
        onPreviousSurah = null
    )
}

@Preview(showBackground = true, name = "Last Surah")
@Composable
fun QuranNavigationDialogLastSurahPreview() {
    QuranNavigationDialog(
        isVisible = true,
        currentSurah = 114,
        currentAya = 3,
        totalAyas = 6,
        onDismiss = {},
        onJumpToAya = {},
        onNextSurah = null,
        onPreviousSurah = {}
    )
}

