package com.arshadshah.nimaz.presentation.components.molecules.tasbih

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperSize
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.screens.tasbih.BeadDesigns
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * Bottom sheet for picking the bead material. Re-skins the strand live; the chosen
 * design is persisted by the view-model. Beads-mode only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeadDesignPickerSheet(
    selectedKey: String,
    onSelect: (String) -> Unit,
    leftHanded: Boolean,
    onToggleHanded: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = stringResource(R.string.tasbih_bead_design),
        scrollable = false,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Column {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(BeadDesigns.all, key = { it.key }) { design ->
                    val selected = design.key == selectedKey
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelect(design.key) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(design.resting))
                                .then(
                                    if (selected) Modifier.border(
                                        width = 2.5.dp,
                                        color = NimazColors.TasbihColors.Milestone,
                                        shape = CircleShape
                                    ) else Modifier
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = beadDesignLabel(design.key),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tasbih_left_handed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            if (leftHanded) R.string.tasbih_beads_advance_ltr
                            else R.string.tasbih_beads_advance_rtl
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = leftHanded, onCheckedChange = onToggleHanded)
            }
        }
    }
}

/**
 * Expanded detail of the current tasbih: Arabic, transliteration, translation,
 * Target / Today / Laps tiles, an optional reference, and a Change-dhikr action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTasbihSheet(
    preset: TasbihPreset?,
    targetCount: Int,
    totalToday: Int,
    laps: Int,
    onChangeDhikr: () -> Unit,
    onTargetChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!preset?.arabicText.isNullOrEmpty()) {
                ArabicText(
                    text = preset.arabicText,
                    size = ArabicTextSize.LARGE,
                    color = NimazColors.TasbihColors.Milestone,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
            }
            preset?.transliteration?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = preset?.translation ?: preset?.name ?: "Free Count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    stringResource(R.string.tasbih_stat_target),
                    targetCount.toString(),
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.tasbih_stat_today),
                    totalToday.toString(),
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.tasbih_stat_laps),
                    laps.toString(),
                    Modifier.weight(1f)
                )
            }

            // Free count: let the user dial in any target.
            if (preset == null) {
                Spacer(Modifier.height(12.dp))
                NimazNumberStepper(
                    value = targetCount,
                    onValueChange = onTargetChange,
                    variant = NimazNumberStepperVariant.SPREAD,
                    size = NimazNumberStepperSize.MEDIUM,
                    minValue = 1,
                    maxValue = 9999
                )
            }

            preset?.reference?.takeIf { it.isNotBlank() }?.let { ref ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = ref,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onChangeDhikr() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NimazIcon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.tasbih_change_dhikr),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun beadDesignLabel(key: String): String = when (key) {
    "wood" -> stringResource(R.string.tasbih_bead_wood)
    "marble" -> stringResource(R.string.tasbih_bead_marble)
    "amethyst" -> stringResource(R.string.tasbih_bead_amethyst)
    "onyx" -> stringResource(R.string.tasbih_bead_onyx)
    "pearl" -> stringResource(R.string.tasbih_bead_pearl)
    "jade" -> stringResource(R.string.tasbih_bead_jade)
    else -> key
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NimazColors.TasbihColors.Milestone
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
