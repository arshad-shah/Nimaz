package com.arshadshah.nimaz.ui.components.quran

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import es.dmoral.toasty.Toasty

@Composable
fun NoteInput(
    showNoteDialog: MutableState<Boolean>,
    onClick: () -> Unit,
    noteContent: MutableState<String>,
    titleOfDialog: String,
) {
    val context = LocalContext.current

    AlertDialogNimaz(
        contentDescription = "Note",
        title = titleOfDialog,
        topDivider = false,
        bottomDivider = false,
        contentHeight = 250.dp,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        action = {
            DeleteNoteButton(
                onClick = {
                    noteContent.value = ""
                    onClick()
                }
            )
        },
        contentToShow = {

            NoteInputField(
                value = noteContent.value,
                onValueChange = { noteContent.value = it }
            )
        },
        onDismissRequest = {
            showNoteDialog.value = false
            onClick()
        },
        confirmButtonText = "Save",
        onConfirm = {
            if (noteContent.value.isEmpty()) {
                showEmptyNoteWarning(context)
            } else {
                onClick()
            }
        },
        dismissButtonText = "Cancel",
        onDismiss = {
            showNoteDialog.value = false
            onClick()
        }
    )
}

@Composable
private fun DeleteNoteButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .size(24.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete_icon),
            contentDescription = "Delete note",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EnhancedNoteTextField(
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    // Animations
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

    val isError = value.length > maxLength

    Column(modifier = modifier) {
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
                value = value,
                onValueChange = { newValue ->
                    if (newValue.length <= maxLength + 10) { // Allow slight overflow
                        onValueChange(newValue)
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 3,
                maxLines = 5,
                isError = isError,
                supportingText = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedCharacterCounter(
                            current = value.length,
                            max = maxLength
                        )
                        if (isError) {
                            Text(
                                text = "Character limit exceeded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                trailingIcon = {
                    AnimatedClearButton(
                        visible = value.isNotEmpty(),
                        onClick = {
                            onClear()
                            focusManager.clearFocus()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .onFocusChanged { isFocused = it.isFocused },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

@Composable
private fun AnimatedCharacterCounter(
    current: Int,
    max: Int
) {
    val animatedCount by animateIntAsState(
        targetValue = current,
        label = "characterCount"
    )

    val percentage = current.toFloat() / max

    // Dynamic color selection based on percentage
    val counterColor by animateColorAsState(
        targetValue = when {
            percentage > 1f -> MaterialTheme.colorScheme.error
            percentage > 0.9f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            percentage > 0.8f -> MaterialTheme.colorScheme.tertiary
            percentage > 0.6f -> MaterialTheme.colorScheme.secondary
            percentage > 0.4f -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "counterColor"
    )

    // Animated scale for emphasis
    val scale by animateFloatAsState(
        targetValue = if (percentage > 0.9f) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        // Counter text with background pill for emphasis when near limit
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = counterColor.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Text(
                text = "$animatedCount/$max",
                style = MaterialTheme.typography.labelSmall,
                color = counterColor,
                modifier = Modifier.padding(
                    2.dp
                )
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
            modifier = Modifier.size(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxSize()
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

// Usage example
@Composable
fun NoteInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(value) }

    EnhancedNoteTextField(
        value = text,
        maxLength = 250,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        onClear = {
            text = ""
            onValueChange("")
        },
        modifier = modifier
    )
}

private fun showEmptyNoteWarning(context: Context) {
    Toasty.warning(
        context,
        "Note is empty. Closing without save.",
        Toasty.LENGTH_SHORT
    ).show()
}


@Preview(device = "id:small_phone")
@Composable
fun NoteInputPreview() {


    Column(
        modifier = Modifier
            .height(250.dp)
            .padding(16.dp)
    ) {
        NoteInputField(
            value = "",
            onValueChange = {}
        )
    }
}