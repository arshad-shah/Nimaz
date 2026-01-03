package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

/**
 * Input field types supported by NimazTextField
 */
enum class NimazTextFieldType {
    TEXT,           // Standard text input
    MULTILINE,      // Multi-line text (notes)
    NUMBER,         // Numeric input
    CURRENCY,       // Currency input with symbol
    SEARCH,         // Search field with action
    PASSWORD        // Password field (masked)
}

/**
 * A beautifully designed, reusable text field component following the Nimaz design system.
 *
 * Features:
 * - Multiple input types (text, multiline, number, currency, search, password)
 * - Optional label with proper styling
 * - Character counter for text inputs
 * - Clear button
 * - Leading/trailing icons
 * - Error state handling
 * - Consistent with design system (extraLarge shapes, surfaceVariant backgrounds, etc.)
 *
 * @param value Current text value
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for the component
 * @param type Type of input field
 * @param label Optional label displayed above the input
 * @param placeholder Placeholder text when empty
 * @param leadingIcon Leading icon (painter)
 * @param leadingIconVector Leading icon (vector)
 * @param leadingText Leading text (e.g., currency symbol)
 * @param trailingIcon Trailing icon (painter)
 * @param trailingIconVector Trailing icon (vector)
 * @param onTrailingIconClick Callback when trailing icon is clicked
 * @param showClearButton Whether to show clear button when text is not empty
 * @param maxLength Maximum character length (shows counter if set)
 * @param minLines Minimum lines for multiline input
 * @param maxLines Maximum lines for multiline input
 * @param isError Whether input is in error state
 * @param errorMessage Error message to display
 * @param enabled Whether input is enabled
 * @param readOnly Whether input is read-only
 * @param singleLine Whether input is single line
 * @param keyboardOptions Keyboard options
 * @param keyboardActions Keyboard actions
 * @param visualTransformation Visual transformation (e.g., password mask)
 * @param requestFocus Whether to request focus on composition
 * @param onSearchClick Callback for search action (for SEARCH type)
 * @param containerColor Background color of the container
 * @param contentPadding Padding inside the input container
 */
@Composable
fun NimazTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    type: NimazTextFieldType = NimazTextFieldType.TEXT,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: Painter? = null,
    leadingIconVector: ImageVector? = null,
    leadingText: String? = null,
    trailingIcon: Painter? = null,
    trailingIconVector: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    showClearButton: Boolean = true,
    maxLength: Int? = null,
    minLines: Int = 1,
    maxLines: Int = if (type == NimazTextFieldType.MULTILINE) 8 else 1,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = type != NimazTextFieldType.MULTILINE,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    requestFocus: Boolean = false,
    onSearchClick: ((String) -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    contentPadding: Dp = 12.dp
) {
    val focusRequester = remember { FocusRequester() }

    // Request focus if specified
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    // Determine keyboard options based on type
    val resolvedKeyboardOptions = keyboardOptions ?: when (type) {
        NimazTextFieldType.NUMBER, NimazTextFieldType.CURRENCY -> KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
        NimazTextFieldType.SEARCH -> KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        )
        NimazTextFieldType.PASSWORD -> KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        )
        else -> KeyboardOptions.Default
    }

    // Determine keyboard actions
    val resolvedKeyboardActions = keyboardActions ?: when (type) {
        NimazTextFieldType.SEARCH -> KeyboardActions(
            onSearch = { onSearchClick?.invoke(value) }
        )
        else -> KeyboardActions.Default
    }

    // Check if max length exceeded
    val isOverLimit = maxLength?.let { value.length > it } ?: false
    val showError = isError || isOverLimit

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (showError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Input Container
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Inner Input Surface
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Leading Content
                        LeadingContent(
                            icon = leadingIcon,
                            iconVector = leadingIconVector,
                            text = leadingText,
                            isError = showError
                        )

                        // Text Field
                        BasicTextField(
                            value = value,
                            onValueChange = { newValue ->
                                // Allow slightly over max length to show error
                                val limit = maxLength?.let { it + 10 } ?: Int.MAX_VALUE
                                if (newValue.length <= limit) {
                                    onValueChange(newValue)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .then(
                                    if (type == NimazTextFieldType.MULTILINE) {
                                        Modifier.padding(vertical = 12.dp)
                                    } else {
                                        Modifier.padding(vertical = 14.dp)
                                    }
                                ),
                            enabled = enabled,
                            readOnly = readOnly,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            ),
                            keyboardOptions = resolvedKeyboardOptions,
                            keyboardActions = resolvedKeyboardActions,
                            singleLine = singleLine,
                            minLines = minLines,
                            maxLines = maxLines,
                            visualTransformation = visualTransformation,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (value.isEmpty() && placeholder != null) {
                                        Text(
                                            text = placeholder,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Trailing Content
                        TrailingContent(
                            value = value,
                            type = type,
                            showClearButton = showClearButton,
                            onClear = { onValueChange("") },
                            trailingIcon = trailingIcon,
                            trailingIconVector = trailingIconVector,
                            onTrailingIconClick = onTrailingIconClick,
                            onSearchClick = { onSearchClick?.invoke(value) },
                            isError = showError
                        )
                    }
                }

                // Supporting Content (Character Counter / Error Message)
                SupportingContent(
                    value = value,
                    maxLength = maxLength,
                    isError = showError,
                    errorMessage = errorMessage
                )
            }
        }
    }
}

@Composable
private fun LeadingContent(
    icon: Painter?,
    iconVector: ImageVector?,
    text: String?,
    isError: Boolean
) {
    when {
        icon != null -> {
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        iconVector != null -> {
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        text != null -> {
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailingContent(
    value: String,
    type: NimazTextFieldType,
    showClearButton: Boolean,
    onClear: () -> Unit,
    trailingIcon: Painter?,
    trailingIconVector: ImageVector?,
    onTrailingIconClick: (() -> Unit)?,
    onSearchClick: () -> Unit,
    isError: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear Button
        AnimatedVisibility(
            visible = value.isNotEmpty() && showClearButton,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Search Button (for search type)
        if (type == NimazTextFieldType.SEARCH) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Custom Trailing Icon
        when {
            trailingIcon != null && onTrailingIconClick != null -> {
                IconButton(
                    onClick = onTrailingIconClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailingIconVector != null && onTrailingIconClick != null -> {
                IconButton(
                    onClick = onTrailingIconClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = trailingIconVector,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportingContent(
    value: String,
    maxLength: Int?,
    isError: Boolean,
    errorMessage: String?
) {
    // Only show if we have max length or error
    if (maxLength == null && !isError) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Character Counter
        if (maxLength != null) {
            CharacterCounter(
                current = value.length,
                max = maxLength
            )
        }

        // Error Message
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CharacterCounter(
    current: Int,
    max: Int
) {
    val animatedCount by animateIntAsState(
        targetValue = current,
        label = "characterCount"
    )

    val percentage = current.toFloat() / max
    val counterColor by animateColorAsState(
        targetValue = when {
            percentage > 1f -> MaterialTheme.colorScheme.error
            percentage > 0.9f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            percentage > 0.8f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "counterColor"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = counterColor.copy(alpha = 0.1f)
    ) {
        Text(
            text = "$animatedCount/$max",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = counterColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(name = "Standard Text Input")
@Composable
private fun NimazTextFieldPreviewStandard() {
    MaterialTheme {
        var text by remember { mutableStateOf("Hello World") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            label = "Name",
            placeholder = "Enter your name",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Multiline Note Input")
@Composable
private fun NimazTextFieldPreviewMultiline() {
    MaterialTheme {
        var text by remember { mutableStateOf("This is a sample note that demonstrates the multiline input capability.") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            type = NimazTextFieldType.MULTILINE,
            label = "Note",
            placeholder = "Enter your note...",
            maxLength = 250,
            minLines = 3,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Currency Input")
@Composable
private fun NimazTextFieldPreviewCurrency() {
    MaterialTheme {
        var text by remember { mutableStateOf("1500") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            type = NimazTextFieldType.CURRENCY,
            label = "Cash at Home & Bank",
            placeholder = "0.00",
            leadingText = "$",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Number Input")
@Composable
private fun NimazTextFieldPreviewNumber() {
    MaterialTheme {
        var text by remember { mutableStateOf("33") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            type = NimazTextFieldType.NUMBER,
            label = "Daily Goal",
            placeholder = "Enter target count",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Search Input")
@Composable
private fun NimazTextFieldPreviewSearch() {
    MaterialTheme {
        var text by remember { mutableStateOf("") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            type = NimazTextFieldType.SEARCH,
            placeholder = "Search currency...",
            leadingIconVector = Icons.Rounded.Search,
            onSearchClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Input with Leading Icon")
@Composable
private fun NimazTextFieldPreviewWithIcon() {
    MaterialTheme {
        var text by remember { mutableStateOf("Dublin, Ireland") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            label = "Location",
            placeholder = "Enter city or address",
            leadingIcon = painterResource(id = R.drawable.marker_icon),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Error State")
@Composable
private fun NimazTextFieldPreviewError() {
    MaterialTheme {
        var text by remember { mutableStateOf("") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            label = "Required Field",
            placeholder = "This field is required",
            isError = true,
            errorMessage = "This field cannot be empty",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Character Limit Exceeded")
@Composable
private fun NimazTextFieldPreviewOverLimit() {
    MaterialTheme {
        var text by remember { mutableStateOf("This text exceeds the maximum character limit that was set for this input field and should show an error state.") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            type = NimazTextFieldType.MULTILINE,
            label = "Description",
            maxLength = 50,
            minLines = 2,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Disabled Input")
@Composable
private fun NimazTextFieldPreviewDisabled() {
    MaterialTheme {
        NimazTextField(
            value = "Disabled value",
            onValueChange = { },
            label = "Disabled Field",
            enabled = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Empty with Placeholder")
@Composable
private fun NimazTextFieldPreviewEmpty() {
    MaterialTheme {
        var text by remember { mutableStateOf("") }
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            label = "Email",
            placeholder = "Enter your email address",
            modifier = Modifier.padding(16.dp)
        )
    }
}

