package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun SettingsSelectionDialog(
    title: String,
    options: Map<String, String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean
) {
    // Temporary state to hold the selected option until confirmed
    var tempSelectedOption by remember(selectedOption) { mutableStateOf(selectedOption) }

    if (showDialog) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.settings_icon),
            title = title,
            contentDescription = "Select your preferred option",
            contentHeight = 400.dp,
            cardContent = false,
            onDismissRequest = onDismiss,
            onConfirm = {
                // Only apply the selection when confirmed
                onOptionSelected(tempSelectedOption)
                onDismiss()
            },
            confirmButtonText = "Apply",
            onDismiss = onDismiss,
            dismissButtonText = "Cancel",
            action = {
                // Show count badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${options.size} options",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            },
            contentToShow = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options.toList()) { (key, description) ->
                        SelectionOptionItem(
                            key = key,
                            description = description,
                            isSelected = key == tempSelectedOption,
                            onClick = { tempSelectedOption = key }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SelectionOptionItem(
    key: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "backgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "borderColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                role = Role.RadioButton,
                selected = isSelected,
                onClick = onClick
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radio button indicator
            RadioButtonCustom(
                selected = isSelected,
                onClick = onClick
            )

            // Description text
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Selection Option - Selected")
@Composable
private fun SelectionOptionItemSelectedPreview() {
    NimazTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        ) {
            SelectionOptionItem(
                key = "option1",
                description = "Muslim World League",
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Selection Option - Unselected")
@Composable
private fun SelectionOptionItemUnselectedPreview() {
    NimazTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        ) {
            SelectionOptionItem(
                key = "option2",
                description = "Islamic Society of North America",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Multiple Options")
@Composable
private fun MultipleOptionsPreview() {
    val options = listOf(
        "option1" to "Option One",
        "option2" to "Option Two - Selected",
        "option3" to "Option Three"
    )

    NimazTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (key, description) ->
                    SelectionOptionItem(
                        key = key,
                        description = description,
                        isSelected = key == "option2",
                        onClick = {}
                    )
                }
            }
        }
    }
}