package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R

@Composable
fun AlertDialogNimaz(
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    action: @Composable (() -> Unit)? = null,
    title: String,
    contentDescription: String,
    description: String? = null,
    contentToShow: @Composable () -> Unit,
    contentHeight: Dp = 300.dp,
    cardContent: Boolean = true,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    topDivider: Boolean = true,
    bottomDivider: Boolean = true,
    showConfirmButton: Boolean = true,
    showDismissButton: Boolean = true,
    onConfirm: () -> Unit,
    confirmButtonText: String = "Done",
    onDismiss: () -> Unit,
    dismissButtonText: String = "Cancel",
    scrollState: LazyListState = rememberLazyListState(),
    isFullScreen: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = if (isFullScreen) RoundedCornerShape(0.dp) else MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Section
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (icon != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = icon,
                                            contentDescription = contentDescription,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (action != null) {
                            action()
                        }
                    }
                }

                // Description Section
                if (description != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Content Section
                val contentModifier = if (isFullScreen) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                } else {
                    Modifier
                        .height(contentHeight)
                        .fillMaxWidth()
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = contentModifier
                ) {
                    if (cardContent) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            LazyColumn(
                                state = scrollState,
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item { contentToShow() }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = scrollState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item { contentToShow() }
                        }
                    }
                }

                // Action Buttons Section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showDismissButton) {
                            TextButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = dismissButtonText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (showConfirmButton) {
                            Button(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = confirmButtonText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

//alert dialog nimaz preview with fullscreen option
@Preview(name = "Fullscreen Dialog")
@Composable
fun AlertDialogNimazPreviewFullScreen() {
    AlertDialogNimaz(
        title = "Fullscreen Dialog",
        icon = painterResource(id = R.drawable.mail_icon),
        contentDescription = "Fullscreen",
        contentToShow = { Text(text = "This is a fullscreen dialog content") },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        description = "This dialog takes up the full screen",
        isFullScreen = true
    )
}

@Preview(name = "Standard Dialog with Icon")
@Composable
fun AlertDialogNimazPreviewStandard() {
    AlertDialogNimaz(
        title = "Standard Dialog",
        icon = painterResource(id = R.drawable.settings_icon),
        contentDescription = "Settings",
        contentToShow = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "This is the standard dialog with an icon.")
                Text(text = "It has a fixed content height of 300dp.")
            }
        },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        contentHeight = 200.dp
    )
}

@Preview(name = "Dialog with Description")
@Composable
fun AlertDialogNimazPreviewWithDescription() {
    AlertDialogNimaz(
        title = "Confirmation Required",
        icon = painterResource(id = R.drawable.info_icon),
        contentDescription = "Alert",
        description = "Please review the information below before confirming your action.",
        contentToShow = {
            Text(text = "Are you sure you want to proceed with this action?")
        },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        confirmButtonText = "Confirm",
        dismissButtonText = "Go Back",
        contentHeight = 150.dp
    )
}

@Preview(name = "Dialog without Icon")
@Composable
fun AlertDialogNimazPreviewNoIcon() {
    AlertDialogNimaz(
        title = "Simple Dialog",
        contentDescription = "Simple",
        contentToShow = {
            Text(text = "This dialog does not have an icon in the header.")
        },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        contentHeight = 120.dp
    )
}

@Preview(name = "Dialog - Confirm Only")
@Composable
fun AlertDialogNimazPreviewConfirmOnly() {
    AlertDialogNimaz(
        title = "Information",
        icon = painterResource(id = R.drawable.info_icon),
        contentDescription = "Info",
        contentToShow = {
            Text(text = "This is an informational dialog with only a confirm button.")
        },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        showDismissButton = false,
        confirmButtonText = "Got it",
        contentHeight = 120.dp
    )
}

@Preview(name = "Dialog with Action Button")
@Composable
fun AlertDialogNimazPreviewWithAction() {
    AlertDialogNimaz(
        title = "Select Option",
        icon = painterResource(id = R.drawable.menu_burger_icon),
        contentDescription = "Select",
        action = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "3 selected",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        contentToShow = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Option 1")
                Text(text = "Option 2")
                Text(text = "Option 3")
            }
        },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        contentHeight = 180.dp
    )
}

@Preview(name = "Dialog - No Card Content")
@Composable
fun AlertDialogNimazPreviewNoCardContent() {
    AlertDialogNimaz(
        title = "Plain Content",
        icon = painterResource(id = R.drawable.document_icon),
        contentDescription = "Document",
        cardContent = false,
        contentToShow = {
            Text(
                text = "This dialog has plain content without the inner card wrapper.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        contentHeight = 150.dp
    )
}

