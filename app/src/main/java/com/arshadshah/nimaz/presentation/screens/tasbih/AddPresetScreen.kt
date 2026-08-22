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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperSize
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperType
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazTextField
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihViewModel

/**
 * The custom-dhikr form, in both of its modes.
 *
 * With [presetId] set it edits that preset instead of creating one — the third of the trio
 * that had a handler (`TasbihEvent.UpdateCustomPreset`) but no way in, next to create and
 * delete which have had UI since they shipped. A typo in a dhikr's name or a target of 33
 * that should have been 100 meant deleting it and typing it again.
 *
 * The fields are seeded from the loaded preset the first time it arrives rather than on every
 * recomposition: the presets flow re-emits on any write to the table, and re-seeding would
 * throw away whatever the user had typed since.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPresetScreen(
    onNavigateBack: () -> Unit,
    presetId: Long? = null,
    viewModel: TasbihViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val presetsState by viewModel.presetsState.collectAsStateWithLifecycle()
    val editing = presetId?.let { id ->
        presetsState.customPresets.firstOrNull { it.id == id }
    }

    var name by remember { mutableStateOf("") }
    var arabicText by remember { mutableStateOf("") }
    var transliteration by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var targetCount by remember { mutableStateOf("33") }
    var selectedCategory by remember { mutableStateOf(TasbihCategory.CUSTOM) }
    var nameError by remember { mutableStateOf(false) }
    var seeded by remember(presetId) { mutableStateOf(false) }

    LaunchedEffect(editing?.id) {
        val preset = editing ?: return@LaunchedEffect
        if (seeded) return@LaunchedEffect
        name = preset.name
        arabicText = preset.arabicText.orEmpty()
        transliteration = preset.transliteration.orEmpty()
        translation = preset.translation.orEmpty()
        targetCount = preset.targetCount.toString()
        selectedCategory = preset.category ?: TasbihCategory.CUSTOM
        seeded = true
    }

    fun submit() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val now = System.currentTimeMillis()
        val preset = TasbihPreset(
            id = editing?.id ?: 0,
            name = name.trim(),
            arabicText = arabicText.ifBlank { null },
            transliteration = transliteration.ifBlank { null },
            translation = translation.ifBlank { null },
            targetCount = targetCount.toIntOrNull() ?: 33,
            category = selectedCategory,
            reference = editing?.reference,
            isDefault = false,
            displayOrder = editing?.displayOrder ?: 0,
            createdAt = editing?.createdAt ?: now,
            updatedAt = now
        )
        viewModel.onEvent(
            if (editing != null) {
                TasbihEvent.UpdateCustomPreset(preset)
            } else {
                TasbihEvent.CreateCustomPreset(preset)
            }
        )
        onNavigateBack()
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(
                    if (editing != null) R.string.edit_tasbih else R.string.new_tasbih
                ),
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
            // The field family's rhythm: one field's label must not crowd the helper line of
            // the field above it.
            verticalArrangement = Arrangement.spacedBy(NimazFieldDefaults.FieldGap)
        ) {
            // Name (required). The 14dp radius, the RTL/gold Arabic styling and the error
            // wiring all used to be set here, per field; they are the shell's and the
            // variant's job now.
            NimazTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = stringResource(R.string.field_name),
                required = true,
                placeholder = stringResource(R.string.preset_name_placeholder),
                error = if (nameError) stringResource(R.string.name_required_error) else null,
                modifier = Modifier.fillMaxWidth(),
            )

            NimazTextField(
                value = arabicText,
                onValueChange = { arabicText = it },
                label = stringResource(R.string.arabic_text),
                variant = NimazFieldVariant.ARABIC,
                placeholder = stringResource(R.string.arabic_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            NimazTextField(
                value = transliteration,
                onValueChange = { transliteration = it },
                label = stringResource(R.string.transliteration),
                placeholder = stringResource(R.string.transliteration_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            NimazTextField(
                value = translation,
                onValueChange = { translation = it },
                label = stringResource(R.string.translation),
                placeholder = stringResource(R.string.translation_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            // Target Count stepper
            SectionLabel(stringResource(R.string.target_count))
            NimazNumberStepper(
                value = targetCount.toIntOrNull() ?: 0,
                onValueChange = { targetCount = it.coerceAtLeast(1).toString() },
                variant = NimazNumberStepperVariant.SPREAD,
                size = NimazNumberStepperSize.LARGE,
                type = NimazNumberStepperType.ACCENT,
                minValue = 1
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

            // Create button (primary CTA)
            NimazButton(
                text = stringResource(
                    if (editing != null) R.string.save_tasbih else R.string.create_tasbih
                ),
                onClick = { submit() },
                variant = NimazButtonVariant.FILLED,
                size = NimazButtonSize.LARGE,
                fullWidth = true
            )

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
    NimazBadge(
        text = label,
        size = NimazBadgeSize.LARGE,
        onClick = onClick,
        // Milestone gold is Islamic feature art, so the selected state uses the
        // feature escape hatch; unselected falls back to the neutral tone.
        colors = if (selected) {
            NimazBadgeDefaults.feature(
                color = NimazColors.TasbihColors.Milestone,
                emphasis = NimazBadgeEmphasis.SOFT
            )
        } else {
            NimazBadgeDefaults.colors(
                tone = NimazTone.NEUTRAL,
                emphasis = NimazBadgeEmphasis.SOFT
            )
        }
    )
}
