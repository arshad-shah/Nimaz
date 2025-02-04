package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Section
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (icon != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    painter = icon,
                                    contentDescription = contentDescription,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (action != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                action()
                            }
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Description Section
                if (description != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Content Section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(contentHeight)
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
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item { contentToShow() }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = scrollState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item { contentToShow() }
                        }
                    }
                }

                // Buttons Section
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showDismissButton) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = dismissButtonText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (showConfirmButton) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Button(
                                    onClick = onConfirm,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = confirmButtonText,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


//alert dilog nimaz preview
@Preview
@Composable
fun AlertDialogNimazPreview() {
    AlertDialogNimaz(
        title = "Hello",
        icon = painterResource(id = R.drawable.mail_icon),
        contentDescription = "Add",
        contentToShow = { Text(text = "This is a content") },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        description = "This is a description"
    )
}

//preview of the alert dialog nimaz with action
@Preview
@Composable
fun AlertDialogNimazPreviewWithAction() {
    AlertDialogNimaz(
        title = "very long title to test the alert dialog",
        contentDescription = "Add",
        contentToShow = { Text(text = "This is a content") },
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        description = "This is a description",
        action = {
            IconButton(
                onClick = { },
                content = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.play_icon),
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    )
}

//with a oulinedTextField
@Preview
@Composable
fun AlertDialogNimazPreviewWithOutlinedTextField() {
    val textToShow = remember { mutableStateOf("") }
    AlertDialogNimaz(
        title = "Hello",
        contentDescription = "Add",
        topDivider = false,
        bottomDivider = false,
        contentToShow = {
            OutlinedTextField(
                label = { Text(text = "Enter your name") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                value = textToShow.value,
                onValueChange = {
                    textToShow.value = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        },
        cardContent = false,
        contentHeight = 100.dp,
        onDismissRequest = { },
        onConfirm = { },
        onDismiss = { },
        action = {
            IconButton(
                onClick = { },
                content = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.play_icon),
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    )
}