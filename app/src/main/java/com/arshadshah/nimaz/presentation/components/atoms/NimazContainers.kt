package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Bottom sheet drag handle indicator.
 */
@Composable
fun BottomSheetHandle(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color)
    )
}

/**
 * Modal bottom sheet container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        dragHandle = if (showDragHandle) {
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetHandle()
                }
            }
        } else null,
        content = content
    )
}

/**
 * Simple bottom sheet content wrapper with title.
 */
@Composable
fun BottomSheetContent(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        content()
    }
}

/**
 * Primary alert dialog.
 */
@Composable
fun NimazDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    confirmDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismissRequest()
            }) {
                Text(
                    text = confirmText,
                    color = if (confirmDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
        modifier = modifier,
        icon = icon
    )
}

/**
 * Confirmation dialog for destructive actions.
 */
@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        modifier = modifier,
        confirmDestructive = true
    )
}

/**
 * Info dialog (single dismiss button).
 */
@Composable
fun InfoDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    dismissText: String = "OK",
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissText)
            }
        },
        modifier = modifier,
        icon = icon
    )
}

/**
 * Custom content dialog.
 */
@Composable
fun CustomDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            content()
        }
    }
}

/**
 * Full screen dialog for complex forms.
 */
@Composable
fun FullScreenDialogContent(
    title: String,
    onClose: () -> Unit,
    onSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    saveText: String = "Save",
    closeText: String = "Close",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header with close and optional save
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(closeText)
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (onSave != null) {
                    TextButton(
                        onClick = onSave,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(saveText)
                    }
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * Surface container with elevation and corner radius.
 */
@Composable
fun NimazSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 1.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content
    )
}

/**
 * Floating action button container styling.
 */
@Composable
fun FabContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        content()
    }
}


@Preview(showBackground = true, name = "Bottom Sheet Handle")
@Composable
private fun BottomSheetHandlePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BottomSheetHandle()
        }
    }
}

@Preview(showBackground = true, name = "Bottom Sheet Content")
@Composable
private fun BottomSheetContentPreview() {
    NimazTheme {
        Surface {
            BottomSheetContent(title = "Settings") {
                Text("Sheet content goes here")
                Text("More content...")
            }
        }
    }
}

@Preview(showBackground = true, name = "Dialog Preview")
@Composable
private fun NimazDialogPreview() {
    NimazTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            // Just showing the content, not actual dialog
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Delete Prayer Record", style = MaterialTheme.typography.titleLarge)
                    Text("Are you sure you want to delete this?", modifier = Modifier.padding(vertical = 16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {}) { Text("Cancel") }
                        TextButton(onClick = {}) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Info Dialog Preview")
@Composable
private fun InfoDialogPreview() {
    NimazTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Information", style = MaterialTheme.typography.titleLarge)
                    Text("This is an informational message.", modifier = Modifier.padding(vertical = 16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {}) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Full Screen Dialog Content")
@Composable
private fun FullScreenDialogContentPreview() {
    NimazTheme {
        FullScreenDialogContent(
            title = "Add Prayer Record",
            onClose = {},
            onSave = {}
        ) {
            Text("Form content goes here...")
        }
    }
}

@Preview(showBackground = true, name = "Nimaz Surface")
@Composable
private fun NimazSurfacePreview() {
    NimazTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NimazSurface {
                Text("Surface content", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
