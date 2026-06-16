package com.arshadshah.nimaz.presentation.screens.help

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.BugCategory
import com.arshadshah.nimaz.domain.model.BugDiagnostics
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.BugReportViewModel
import com.arshadshah.nimaz.presentation.viewmodel.BugSubmitStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: BugReportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCategory by remember { mutableStateOf(BugCategory.PRAYER_TIMES) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var screenshotUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var descriptionError by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> screenshotUri = uri }

    val isSubmitting = uiState.status == BugSubmitStatus.Submitting

    // Surface success / error and return to Help on success.
    val successQueuedMessage = stringResource(R.string.bug_report_success_queued)
    val successSentMessage = stringResource(R.string.bug_report_success_sent)
    val errorMessage = stringResource(R.string.bug_report_error)
    LaunchedEffect(uiState.status) {
        when (val status = uiState.status) {
            is BugSubmitStatus.Success -> {
                snackbarHostState.showSnackbar(
                    if (status.queuedOffline) successQueuedMessage else successSentMessage
                )
                onNavigateBack()
            }
            is BugSubmitStatus.Error -> {
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.bug_report_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.bug_report_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category dropdown
            NimazSectionHeader(title = stringResource(R.string.bug_report_category))
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = stringResource(selectedCategory.labelResId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.bug_report_category)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    BugCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(stringResource(category.labelResId)) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Description (required)
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    if (descriptionError && it.isNotBlank()) descriptionError = false
                },
                label = { Text(stringResource(R.string.bug_report_description)) },
                placeholder = { Text(stringResource(R.string.bug_report_description_hint)) },
                isError = descriptionError,
                supportingText = if (descriptionError) {
                    { Text(stringResource(R.string.bug_report_description_required)) }
                } else null,
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Steps to reproduce (optional)
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text(stringResource(R.string.bug_report_steps)) },
                placeholder = { Text(stringResource(R.string.bug_report_steps_hint)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Contact email (optional)
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text(stringResource(R.string.bug_report_email)) },
                placeholder = { Text(stringResource(R.string.bug_report_email_hint)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Screenshot (optional)
            ScreenshotPicker(
                screenshotUri = screenshotUri,
                onPick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { screenshotUri = null }
            )

            // Diagnostics
            DiagnosticsCard(
                diagnostics = uiState.diagnostics,
                include = includeDiagnostics,
                onToggle = { includeDiagnostics = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (description.isBlank()) {
                        descriptionError = true
                    } else {
                        viewModel.submit(
                            category = selectedCategory,
                            description = description,
                            stepsToReproduce = steps,
                            contactEmail = contactEmail,
                            includeDiagnostics = includeDiagnostics,
                            screenshotUri = screenshotUri,
                        )
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.bug_report_submitting))
                } else {
                    Text(stringResource(R.string.bug_report_submit))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScreenshotPicker(
    screenshotUri: android.net.Uri?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    NimazSectionHeader(title = stringResource(R.string.bug_report_screenshot))

    if (screenshotUri == null) {
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.bug_report_attach_screenshot))
        }
    } else {
        val imageBitmap = remember(screenshotUri) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, screenshotUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(4)
                }.asImageBitmap()
            }.getOrNull()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = stringResource(R.string.bug_report_screenshot),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.bug_report_screenshot_attached),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.bug_report_remove_screenshot),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onRemove() }
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(
    diagnostics: BugDiagnostics?,
    include: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    NimazSectionHeader(title = stringResource(R.string.bug_report_diagnostics))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bug_report_include_diagnostics),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.bug_report_diagnostics_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = include, onCheckedChange = onToggle)
            }

            if (diagnostics != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val rows = listOf(
                    stringResource(R.string.bug_report_diag_app_version) to
                        "${diagnostics.appVersionName} (${diagnostics.appVersionCode})",
                    stringResource(R.string.bug_report_diag_device) to
                        "${diagnostics.deviceManufacturer} ${diagnostics.deviceModel}",
                    stringResource(R.string.bug_report_diag_android) to
                        "${diagnostics.androidVersion} (API ${diagnostics.apiLevel})",
                    stringResource(R.string.bug_report_diag_locale) to diagnostics.locale,
                    stringResource(R.string.bug_report_diag_timezone) to diagnostics.timezone,
                    stringResource(R.string.bug_report_diag_calc_method) to diagnostics.calculationMethod,
                    stringResource(R.string.bug_report_diag_asr_method) to diagnostics.asrMethod,
                    stringResource(R.string.bug_report_diag_high_latitude) to diagnostics.highLatitudeRule,
                    stringResource(R.string.bug_report_diag_location_mode) to diagnostics.locationMode,
                    stringResource(R.string.bug_report_diag_notifications) to
                        diagnostics.notificationsPermissionGranted.toString(),
                    stringResource(R.string.bug_report_diag_exact_alarm) to
                        diagnostics.exactAlarmPermissionGranted.toString(),
                    stringResource(R.string.bug_report_diag_battery) to
                        diagnostics.batteryOptimizationExempt.toString(),
                )
                rows.forEach { (label, value) ->
                    DiagnosticRow(label = label, value = value)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
