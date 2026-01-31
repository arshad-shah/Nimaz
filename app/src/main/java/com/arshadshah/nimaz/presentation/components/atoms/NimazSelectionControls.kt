package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.arshadshah.nimaz.presentation.theme.NimazTheme


/**
 * Primary checkbox component.
 */
@Composable
fun NimazCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

/**
 * Checkbox with label.
 */
@Composable
fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazCheckbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = colors
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tri-state checkbox for parent-child selections.
 */
@Composable
fun NimazTriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    TriStateCheckbox(
        state = state,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

/**
 * Primary radio button component.
 */
@Composable
fun NimazRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

/**
 * Radio button with label.
 */
@Composable
fun LabeledRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazRadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = colors
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Radio button group.
 */
@Composable
fun <T> RadioGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelProvider: (T) -> String = { it.toString() },
    descriptionProvider: ((T) -> String?)? = null
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        options.forEach { option ->
            LabeledRadioButton(
                selected = option == selectedOption,
                onClick = { onOptionSelected(option) },
                label = labelProvider(option),
                enabled = enabled,
                description = descriptionProvider?.invoke(option)
            )
        }
    }
}

/**
 * Checkbox group for multi-selection.
 */
@Composable
fun <T> CheckboxGroup(
    options: List<T>,
    selectedOptions: Set<T>,
    onOptionToggled: (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelProvider: (T) -> String = { it.toString() },
    descriptionProvider: ((T) -> String?)? = null
) {
    Column(modifier = modifier) {
        options.forEach { option ->
            LabeledCheckbox(
                checked = option in selectedOptions,
                onCheckedChange = { isChecked -> onOptionToggled(option, isChecked) },
                label = labelProvider(option),
                enabled = enabled,
                description = descriptionProvider?.invoke(option)
            )
        }
    }
}

/**
 * Prayer selection checkboxes.
 */
@Composable
fun PrayerSelectionCheckboxes(
    selectedPrayers: Set<String>,
    onPrayerToggled: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    prayers: List<String> = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
) {
    Column(modifier = modifier) {
        prayers.forEach { prayer ->
            LabeledCheckbox(
                checked = prayer in selectedPrayers,
                onCheckedChange = { isChecked -> onPrayerToggled(prayer, isChecked) },
                label = prayer
            )
        }
    }
}

/**
 * Calculation method radio selection.
 */
@Composable
fun CalculationMethodRadioGroup(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    methods: List<Pair<String, String>> = listOf(
        "MUSLIM_WORLD_LEAGUE" to "Muslim World League",
        "EGYPTIAN" to "Egyptian General Authority",
        "UMM_AL_QURA" to "Umm Al-Qura, Makkah",
        "KARACHI" to "University of Karachi",
        "DUBAI" to "Dubai",
        "NORTH_AMERICA" to "ISNA (North America)",
        "KUWAIT" to "Kuwait",
        "QATAR" to "Qatar",
        "SINGAPORE" to "Singapore",
        "TURKEY" to "Turkey"
    )
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        methods.forEach { (code, displayName) ->
            LabeledRadioButton(
                selected = code == selectedMethod,
                onClick = { onMethodSelected(code) },
                label = displayName
            )
        }
    }
}

/**
 * Day of week checkbox selection.
 */
@Composable
fun DayOfWeekSelection(
    selectedDays: Set<Int>,
    onDayToggled: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = true
) {
    val days = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    if (horizontal) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { (dayNum, dayName) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NimazCheckbox(
                        checked = dayNum in selectedDays,
                        onCheckedChange = { isChecked -> onDayToggled(dayNum, isChecked) }
                    )
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier) {
            days.forEach { (dayNum, dayName) ->
                LabeledCheckbox(
                    checked = dayNum in selectedDays,
                    onCheckedChange = { isChecked -> onDayToggled(dayNum, isChecked) },
                    label = dayName
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Checkbox")
@Composable
private fun NimazCheckboxPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var checked by remember { mutableStateOf(false) }
            NimazCheckbox(checked = checked, onCheckedChange = { checked = it })
            NimazCheckbox(checked = true, onCheckedChange = {})
            NimazCheckbox(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Labeled Checkbox")
@Composable
private fun LabeledCheckboxPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            var checked1 by remember { mutableStateOf(false) }
            var checked2 by remember { mutableStateOf(true) }

            LabeledCheckbox(
                checked = checked1,
                onCheckedChange = { checked1 = it },
                label = "Enable notifications"
            )
            LabeledCheckbox(
                checked = checked2,
                onCheckedChange = { checked2 = it },
                label = "Show time remaining",
                description = "Display countdown to next prayer"
            )
        }
    }
}

@Preview(showBackground = true, name = "Tri-State Checkbox")
@Composable
private fun TriStateCheckboxPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NimazTriStateCheckbox(state = ToggleableState.Off, onClick = {})
            NimazTriStateCheckbox(state = ToggleableState.Indeterminate, onClick = {})
            NimazTriStateCheckbox(state = ToggleableState.On, onClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Radio Button")
@Composable
private fun RadioButtonPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NimazRadioButton(selected = false, onClick = {})
            NimazRadioButton(selected = true, onClick = {})
            NimazRadioButton(selected = false, onClick = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Labeled Radio Button")
@Composable
private fun LabeledRadioButtonPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            var selected by remember { mutableStateOf("option1") }

            LabeledRadioButton(
                selected = selected == "option1",
                onClick = { selected = "option1" },
                label = "Hanafi"
            )
            LabeledRadioButton(
                selected = selected == "option2",
                onClick = { selected = "option2" },
                label = "Shafi'i"
            )
        }
    }
}

@Preview(showBackground = true, name = "Radio Group")
@Composable
private fun RadioGroupPreview() {
    NimazTheme {
        var selected by remember { mutableStateOf("Medium") }
        Box(modifier = Modifier.padding(16.dp)) {
            RadioGroup(
                options = listOf("Small", "Medium", "Large"),
                selectedOption = selected,
                onOptionSelected = { selected = it }
            )
        }
    }
}

@Preview(showBackground = true, name = "Checkbox Group")
@Composable
private fun CheckboxGroupPreview() {
    NimazTheme {
        var selected by remember { mutableStateOf(setOf("Fajr", "Isha")) }
        Box(modifier = Modifier.padding(16.dp)) {
            CheckboxGroup(
                options = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
                selectedOptions = selected,
                onOptionToggled = { option, isChecked ->
                    selected = if (isChecked) selected + option else selected - option
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Prayer Selection")
@Composable
private fun PrayerSelectionCheckboxesPreview() {
    NimazTheme {
        var selected by remember { mutableStateOf(setOf("Fajr", "Maghrib")) }
        Box(modifier = Modifier.padding(16.dp)) {
            PrayerSelectionCheckboxes(
                selectedPrayers = selected,
                onPrayerToggled = { prayer, isChecked ->
                    selected = if (isChecked) selected + prayer else selected - prayer
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Day of Week Selection")
@Composable
private fun DayOfWeekSelectionPreview() {
    NimazTheme {
        var selected by remember { mutableStateOf(setOf(1, 3, 5)) }
        Box(modifier = Modifier.padding(16.dp)) {
            DayOfWeekSelection(
                selectedDays = selected,
                onDayToggled = { day, isChecked ->
                    selected = if (isChecked) selected + day else selected - day
                }
            )
        }
    }
}
