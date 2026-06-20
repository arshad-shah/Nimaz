package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.TasbihViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPresetScreen(
    onNavigateBack: () -> Unit,
    viewModel: TasbihViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var name by remember { mutableStateOf("") }
    var arabicText by remember { mutableStateOf("") }
    var transliteration by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var targetCount by remember { mutableStateOf("33") }
    var selectedCategory by remember { mutableStateOf(TasbihCategory.CUSTOM) }
    var nameError by remember { mutableStateOf(false) }

    fun submit() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val preset = TasbihPreset(
            id = 0,
            name = name.trim(),
            arabicText = arabicText.ifBlank { null },
            transliteration = transliteration.ifBlank { null },
            translation = translation.ifBlank { null },
            targetCount = targetCount.toIntOrNull() ?: 33,
            category = selectedCategory,
            reference = null,
            isDefault = false,
            displayOrder = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModel.onEvent(TasbihEvent.CreateCustomPreset(preset))
        onNavigateBack()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.new_tasbih),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
                // No top-bar Save action — the prominent "Create Tasbih" button submits.
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name (required)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.name_required)) },
                placeholder = { Text(stringResource(R.string.preset_name_placeholder)) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.name_required_error)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Arabic Text (RTL, gold-tinted)
            OutlinedTextField(
                value = arabicText,
                onValueChange = { arabicText = it },
                label = { Text(stringResource(R.string.arabic_text)) },
                placeholder = { Text(stringResource(R.string.arabic_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                textStyle = TextStyle(
                    textAlign = TextAlign.End,
                    color = NimazColors.TasbihColors.Milestone,
                    fontSize = 20.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NimazColors.TasbihColors.Milestone,
                    unfocusedTextColor = NimazColors.TasbihColors.Milestone
                )
            )

            // Transliteration
            OutlinedTextField(
                value = transliteration,
                onValueChange = { transliteration = it },
                label = { Text(stringResource(R.string.transliteration)) },
                placeholder = { Text(stringResource(R.string.transliteration_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Translation
            OutlinedTextField(
                value = translation,
                onValueChange = { translation = it },
                label = { Text(stringResource(R.string.translation)) },
                placeholder = { Text(stringResource(R.string.translation_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Target Count stepper
            SectionLabel(stringResource(R.string.target_count))
            TargetCountStepper(
                value = targetCount.toIntOrNull() ?: 0,
                onValueChange = { targetCount = it.coerceAtLeast(1).toString() }
            )

            // Category pill chips
            SectionLabel(stringResource(R.string.category))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TasbihCategory.entries.forEach { category ->
                    CategoryPill(
                        label = categoryLabel(category),
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Create button (completion green tint)
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NimazColors.TasbihColors.Complete
                )
            ) {
                Text(
                    text = stringResource(R.string.create_tasbih),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun TargetCountStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledIconButton(
                onClick = { onValueChange(value - 1) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.tasbih_decrease_target),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = NimazColors.TasbihColors.Milestone
            )

            FilledIconButton(
                onClick = { onValueChange(value + 1) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.tasbih_increase_target),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun categoryLabel(category: TasbihCategory): String = when (category) {
    TasbihCategory.DAILY -> stringResource(R.string.tasbih_category_daily)
    TasbihCategory.AFTER_PRAYER -> stringResource(R.string.tasbih_category_after_prayer)
    TasbihCategory.MORNING -> stringResource(R.string.tasbih_category_morning)
    TasbihCategory.EVENING -> stringResource(R.string.tasbih_category_evening)
    TasbihCategory.CUSTOM -> stringResource(R.string.tasbih_category_custom)
}

@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) NimazColors.TasbihColors.Milestone.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.dp, NimazColors.TasbihColors.Milestone)
        } else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) NimazColors.TasbihColors.Milestone
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
