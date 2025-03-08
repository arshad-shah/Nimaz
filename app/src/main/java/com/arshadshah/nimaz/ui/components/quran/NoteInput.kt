package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.viewModel.AyatViewModel

@Composable
fun NoteDialog(
    aya: LocalAya,
    initialNote: String,
    onDismiss: () -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
) {
    val text = remember { mutableStateOf(initialNote) }
    AlertDialogNimaz(
        title = if (initialNote.isEmpty()) "Add Note" else "Edit Note",
        contentHeight = 250.dp,
        action = {
            DeleteNoteButton(
                visible = initialNote.isNotEmpty(),
                onClick = {
                    onEvent(AyatViewModel.AyatEvent.UpdateNote(aya, ""))
                    onDismiss()
                }
            )
        },
        contentDescription = if (initialNote.isEmpty()) "Add Note" else "Edit Note",
        cardContent = false,
        contentToShow = {
            EnhancedNoteInput(
                text = text,
            )
        },
        onDismissRequest = onDismiss,
        dismissButtonText = if (initialNote.isEmpty()) "Cancel" else "Close",
        showConfirmButton = if (initialNote.isEmpty()) text.value.isNotEmpty() else text.value != initialNote,
        confirmButtonText = if (initialNote.isEmpty()) "Save" else "Update",
        onConfirm = {
            onEvent(
                AyatViewModel.AyatEvent.UpdateNote(
                    aya = aya,
                    note = text.value
                )
            )
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun EnhancedNoteInput(
    modifier: Modifier = Modifier,
    text: MutableState<String>,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val maxLength = 250

    val containerElevation by animateDpAsState(
        targetValue = if (isFocused) 4.dp else 1.dp,
        label = "elevation"
    )

    val textFieldScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = containerElevation,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = textFieldScale
                    scaleY = textFieldScale
                }
        ) {
            OutlinedTextField(
                value = text.value,
                onValueChange = { newValue ->
                    if (newValue.length <= maxLength + 10) {
                        text.value = newValue
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 2,
                maxLines = 8,
                isError = text.value.length > maxLength,
                supportingText = {
                    CharacterCounter(
                        current = text.value.length,
                        max = maxLength
                    )
                },
                trailingIcon = {
                    AnimatedClearButton(
                        visible = text.value.isNotEmpty(),
                        onClick = {
                            text.value = ""
                            focusManager.clearFocus()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
}

@Composable
private fun DeleteNoteButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete_icon),
                contentDescription = "Delete note",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CharacterCounter(
    current: Int,
    max: Int
) {
    val animatedCount by animateIntAsState(
        targetValue = current,
        label = "characterCount"
    )

    val percentage = current.toFloat() / max
    val counterColor by animateColorAsState(
        targetValue = when {
            percentage > 1f -> MaterialTheme.colorScheme.error
            percentage > 0.9f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            percentage > 0.8f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "counterColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = counterColor.copy(alpha = 0.1f)
        ) {
            Text(
                text = "$animatedCount/$max",
                style = MaterialTheme.typography.labelSmall,
                color = counterColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (current > max) {
            Text(
                text = "Character limit exceeded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AnimatedClearButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cross_icon),
                    contentDescription = "Clear note",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
        }
    }
}