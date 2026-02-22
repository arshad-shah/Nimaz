package com.arshadshah.nimaz.presentation.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SyncEvent
import com.arshadshah.nimaz.presentation.viewmodel.SyncMode
import com.arshadshah.nimaz.presentation.viewmodel.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Runtime permissions for Nearby Connections
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted or denied — connection will fail gracefully if denied */ }

    val nearbyPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = "Sync Data",
                onBackClick = {
                    viewModel.onEvent(SyncEvent.Cancel)
                    onNavigateBack()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                state.mode == SyncMode.NONE -> {
                    // Mode selection
                    ModeSelectionContent(
                        onSendClick = {
                            permissionLauncher.launch(nearbyPermissions.toTypedArray())
                            viewModel.onEvent(SyncEvent.StartSend)
                        },
                        onReceiveClick = {
                            permissionLauncher.launch(nearbyPermissions.toTypedArray())
                            viewModel.onEvent(SyncEvent.StartReceive)
                        }
                    )
                }
                state.connectionState is ConnectionState.Connecting -> {
                    // Auth token confirmation
                    val connecting = state.connectionState as ConnectionState.Connecting
                    AuthTokenContent(
                        endpointName = connecting.endpointName,
                        authToken = connecting.authToken,
                        onAccept = { viewModel.onEvent(SyncEvent.AcceptConnection(connecting.endpointId)) },
                        onReject = { viewModel.onEvent(SyncEvent.RejectConnection(connecting.endpointId)) }
                    )
                }
                state.connectionState is ConnectionState.Completed -> {
                    // Success
                    CompletedContent(
                        message = state.statusMessage,
                        onDone = {
                            viewModel.onEvent(SyncEvent.Cancel)
                            onNavigateBack()
                        }
                    )
                }
                state.error != null -> {
                    // Error
                    ErrorContent(
                        error = state.error!!,
                        onRetry = { viewModel.onEvent(SyncEvent.Cancel) },
                        onDismiss = {
                            viewModel.onEvent(SyncEvent.Cancel)
                            onNavigateBack()
                        }
                    )
                }
                else -> {
                    // Advertising/Discovering/Connected/Transferring
                    TransferringContent(
                        state = state.connectionState,
                        mode = state.mode,
                        statusMessage = state.statusMessage,
                        onCancel = { viewModel.onEvent(SyncEvent.Cancel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionContent(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    Icon(
        imageVector = Icons.Default.Sync,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Device-to-Device Sync",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "Transfer your data between devices using a direct connection. Both devices must be nearby.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSendClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Data")
            }

            Text(
                text = "Send this device's data to another device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onReceiveClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Receive Data")
            }

            Text(
                text = "Receive data from another device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthTokenContent(
    endpointName: String,
    authToken: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    Icon(
        imageVector = Icons.Default.PhoneAndroid,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Confirm Connection",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "Connecting to: $endpointName",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verification Code",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = authToken,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Make sure this code matches on both devices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reject")
        }

        Button(
            onClick = onAccept,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Accept")
        }
    }
}

@Composable
private fun TransferringContent(
    state: ConnectionState,
    mode: SyncMode,
    statusMessage: String,
    onCancel: () -> Unit
) {
    Spacer(modifier = Modifier.height(48.dp))

    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        if (state is ConnectionState.Transferring) {
            Text(
                text = "${(state.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = when (state) {
            is ConnectionState.Advertising -> "Waiting for receiver..."
            is ConnectionState.Discovering -> "Searching for sender..."
            is ConnectionState.Connected -> if (mode == SyncMode.SEND) "Connected! Preparing data..." else "Connected! Waiting for data..."
            is ConnectionState.Transferring -> if (mode == SyncMode.SEND) "Sending data..." else "Receiving data..."
            else -> statusMessage
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    if (statusMessage.isNotEmpty()) {
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    if (state is ConnectionState.Transferring) {
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    TextButton(onClick = onCancel) {
        Text("Cancel")
    }
}

@Composable
private fun CompletedContent(
    message: String,
    onDone: () -> Unit
) {
    Spacer(modifier = Modifier.height(48.dp))

    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Sync Complete",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onDone,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Done")
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Spacer(modifier = Modifier.height(48.dp))

    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Sync Failed",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error
    )

    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Close")
        }
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again")
        }
    }
}
