package com.arshadshah.nimaz.ui.components.quran.aya.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FloatingNavigationPanel(
    isVisible: Boolean,
    currentSurah: Int,
    currentAya: Int,
    totalAyas: Int,
    onDismiss: () -> Unit,
    onJumpToAya: (Int) -> Unit,
    onShowBookmarks: () -> Unit,
    onShowSearch: () -> Unit,
    onShowQuickJumps: () -> Unit
) {

    Log.d("JumpToAya", "FloatingNavigationPanel: isVisible=$isVisible, currentSurah=$currentSurah, currentAya=$currentAya, totalAyas=$totalAyas")
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Header
                    Text(
                        text = "Quick Navigation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Current position info
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Current Position",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Surah $currentSurah, Aya $currentAya",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$currentAya of $totalAyas verses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Jump to aya input
                    var ayaInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = ayaInput,
                        onValueChange = { ayaInput = it },
                        label = { Text("Jump to Aya (1-$totalAyas)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    ayaInput.toIntOrNull()?.let { aya ->
                                        if (aya in 1..totalAyas) {
                                            onJumpToAya(aya)
                                            onDismiss()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, "Jump")
                            }
                        },
                        supportingText = {
                            if (ayaInput.isNotEmpty()) {
                                val ayaNum = ayaInput.toIntOrNull()
                                if (ayaNum == null || ayaNum !in 1..totalAyas) {
                                    Text(
                                        text = "Please enter a number between 1 and $totalAyas",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quick actions grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            NavigationAction(
                                icon = Icons.Default.Navigation,
                                text = "Quick Jumps",
                                onClick = {
                                    onShowQuickJumps()
                                    onDismiss()
                                }
                            )
                            NavigationAction(
                                icon = Icons.Default.SkipPrevious,
                                text = "First Aya",
                                onClick = {
                                    onJumpToAya(1)
                                    onDismiss()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            NavigationAction(
                                icon = Icons.Default.SkipNext,
                                text = "Last Aya",
                                onClick = {
                                    onJumpToAya(totalAyas)
                                    onDismiss()
                                }
                            )
                            NavigationAction(
                                icon = Icons.Default.Shuffle,
                                text = "Random",
                                onClick = {
                                    onJumpToAya((1..totalAyas).random())
                                    onDismiss()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
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
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp)
        )
    }
}