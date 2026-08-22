package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Chip style variants
 */
enum class NimazChipStyle {
    ASSIST,
    FILTER,
    INPUT,
    SUGGESTION
}

/**
 * Chip variant for unified NimazChip function
 */
enum class NimazChipVariant {
    FILTER,
    SUGGESTION,
    ASSIST,
    INPUT
}

/**
 * Unified chip component that selects the appropriate chip type based on variant.
 */
@Composable
fun NimazChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    /**
     * Whether a selected chip grows a check mark.
     *
     * False turns a filter chip into a **position indicator**: the background reader's sticky
     * index marks which section is on screen, which is a statement about where you are, not a
     * filter you applied — and a tick claims you chose it.
     */
    showSelectedIcon: Boolean = true,
    variant: NimazChipVariant = NimazChipVariant.FILTER,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    when (variant) {
        NimazChipVariant.FILTER -> NimazFilterChip(
            selected = selected,
            showSelectedIcon = showSelectedIcon,
            onClick = onClick,
            label = text,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
        )

        NimazChipVariant.SUGGESTION -> NimazSuggestionChip(
            onClick = onClick,
            label = text,
            modifier = modifier,
            enabled = enabled,
            icon = leadingIcon
        )

        NimazChipVariant.ASSIST -> NimazAssistChip(
            onClick = onClick,
            label = text,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon
        )

        NimazChipVariant.INPUT -> NimazInputChip(
            selected = selected,
            onClick = onClick,
            label = text,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon
        )
    }
}

/**
 * Filter chip for selecting categories or filters.
 */
@Composable
fun NimazFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    showSelectedIcon: Boolean = true,
    elevated: Boolean = false,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val leadingIconContent: (@Composable () -> Unit)? = when {
        showSelectedIcon && selected -> {
            {
                NimazIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    size = NimazIconSize.SMALL
                )
            }
        }

        leadingIcon != null -> {
            {
                NimazIcon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    size = NimazIconSize.SMALL
                )
            }
        }

        else -> null
    }

    if (elevated) {
        ElevatedFilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIconContent,
            shape = shape
        )
    } else {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIconContent,
            shape = shape
        )
    }
}

/**
 * Assist chip for actions.
 */
@Composable
fun NimazAssistChip(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    elevated: Boolean = false,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val leadingIconContent: (@Composable () -> Unit)? = leadingIcon?.let { icon ->
        {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                size = NimazIconSize.SMALL
            )
        }
    }

    if (elevated) {
        ElevatedAssistChip(
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIconContent,
            shape = shape
        )
    } else {
        AssistChip(
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIconContent,
            shape = shape
        )
    }
}

/**
 * Input chip for user input tags (dismissible).
 */
@Composable
fun NimazInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    avatar: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    InputChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon?.let { icon ->
            {
                NimazIcon(
                    imageVector = icon,
                    contentDescription = null,
                    size = NimazIconSize.SMALL
                )
            }
        },
        avatar = avatar,
        trailingIcon = onDismiss?.let {
            {
                NimazIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss),
                    size = NimazIconSize.SMALL
                )
            }
        },
        shape = shape
    )
}

/**
 * Suggestion chip for auto-complete or recommendations.
 */
@Composable
fun NimazSuggestionChip(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    elevated: Boolean = false,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val iconContent: (@Composable () -> Unit)? = icon?.let {
        {
            NimazIcon(
                imageVector = it,
                contentDescription = null,
                size = NimazIconSize.SMALL
            )
        }
    }

    if (elevated) {
        ElevatedSuggestionChip(
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
            icon = iconContent,
            shape = shape
        )
    } else {
        SuggestionChip(
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
            icon = iconContent,
            shape = shape
        )
    }
}

/**
 * Revelation type chip for Quran (Meccan/Medinan).
 */
@Composable
fun RevelationTypeChip(
    isMeccan: Boolean,
    modifier: Modifier = Modifier
) {
    val (label, color) = if (isMeccan) {
        stringResource(R.string.quran_meccan) to NimazColors.QuranColors.Meccan
    } else {
        stringResource(R.string.quran_medinan) to NimazColors.QuranColors.Medinan
    }

    FilterChip(
        selected = true,
        onClick = { },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        ),
        shape = RoundedCornerShape(4.dp)
    )
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "NimazChip Variants")
@Composable
private fun NimazChipVariantsPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazChip(text = "Filter", onClick = {}, variant = NimazChipVariant.FILTER)
            NimazChip(text = "Suggestion", onClick = {}, variant = NimazChipVariant.SUGGESTION)
            NimazChip(text = "Assist", onClick = {}, variant = NimazChipVariant.ASSIST)
            NimazChip(text = "Input", onClick = {}, variant = NimazChipVariant.INPUT)
        }
    }
}

@Preview(showBackground = true, name = "Filter Chips")
@Composable
private fun FilterChipPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazFilterChip(selected = false, onClick = {}, label = "Unselected")
            NimazFilterChip(selected = true, onClick = {}, label = "Selected")
            NimazFilterChip(
                selected = true,
                onClick = {},
                label = "With Icon",
                leadingIcon = Icons.Default.Star,
            )
            NimazFilterChip(selected = false, onClick = {}, label = "Elevated", elevated = true)
        }
    }
}

@Preview(showBackground = true, name = "Assist Chips")
@Composable
private fun AssistChipPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazAssistChip(onClick = {}, label = "Action")
            NimazAssistChip(onClick = {}, label = "With Icon", leadingIcon = Icons.Default.Favorite)
            NimazAssistChip(onClick = {}, label = "Elevated", elevated = true)
        }
    }
}

@Preview(showBackground = true, name = "Input Chips")
@Composable
private fun InputChipPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazInputChip(selected = false, onClick = {}, label = "Tag 1")
            NimazInputChip(selected = true, onClick = {}, label = "Selected Tag")
            NimazInputChip(selected = false, onClick = {}, label = "Dismissible", onDismiss = {})
        }
    }
}

@Preview(showBackground = true, name = "Suggestion Chips")
@Composable
private fun SuggestionChipPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazSuggestionChip(onClick = {}, label = "Suggestion")
            NimazSuggestionChip(onClick = {}, label = "With Icon", icon = Icons.Default.Star)
            NimazSuggestionChip(onClick = {}, label = "Elevated", elevated = true)
        }
    }
}

@Preview(showBackground = true, name = "Revelation Type Chips")
@Composable
private fun RevelationTypeChipPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RevelationTypeChip(isMeccan = true)
            RevelationTypeChip(isMeccan = false)
        }
    }
}
