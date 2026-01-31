package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Text field style variants
 */
enum class NimazTextFieldStyle {
    FILLED,
    OUTLINED
}

/**
 * Primary text field component for Nimaz app.
 */
@Composable
fun NimazTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: NimazTextFieldStyle = NimazTextFieldStyle.OUTLINED,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors? = null
) {
    Column(modifier = modifier) {
        val keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = KeyboardCapitalization.Sentences
        )

        val keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onSearch = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() }
        )

        when (style) {
            NimazTextFieldStyle.FILLED -> {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = label?.let { { Text(it) } },
                    placeholder = placeholder?.let { { Text(it) } },
                    leadingIcon = leadingIcon?.let { icon ->
                        { Icon(imageVector = icon, contentDescription = null) }
                    },
                    trailingIcon = trailingIcon?.let { icon ->
                        {
                            if (onTrailingIconClick != null) {
                                IconButton(onClick = onTrailingIconClick) {
                                    Icon(imageVector = icon, contentDescription = null)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        }
                    },
                    isError = isError,
                    enabled = enabled,
                    readOnly = readOnly,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    shape = shape,
                    colors = colors ?: TextFieldDefaults.colors()
                )
            }
            NimazTextFieldStyle.OUTLINED -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = label?.let { { Text(it) } },
                    placeholder = placeholder?.let { { Text(it) } },
                    leadingIcon = leadingIcon?.let { icon ->
                        { Icon(imageVector = icon, contentDescription = null) }
                    },
                    trailingIcon = trailingIcon?.let { icon ->
                        {
                            if (onTrailingIconClick != null) {
                                IconButton(onClick = onTrailingIconClick) {
                                    Icon(imageVector = icon, contentDescription = null)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        }
                    },
                    isError = isError,
                    enabled = enabled,
                    readOnly = readOnly,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    shape = shape,
                    colors = colors ?: OutlinedTextFieldDefaults.colors()
                )
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Search text field with search icon and clear button.
 */
@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: () -> Unit = {},
    onClear: () -> Unit = {},
    enabled: Boolean = true
) {
    NimazTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        style = NimazTextFieldStyle.OUTLINED,
        placeholder = placeholder,
        leadingIcon = Icons.Default.Search,
        trailingIcon = if (value.isNotEmpty()) Icons.Default.Clear else null,
        onTrailingIconClick = {
            onValueChange("")
            onClear()
        },
        enabled = enabled,
        singleLine = true,
        imeAction = ImeAction.Search,
        onImeAction = onSearch,
        shape = RoundedCornerShape(28.dp)
    )
}

/**
 * Password text field with visibility toggle.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            isError = isError,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Number input text field.
 */
@Composable
fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    allowDecimal: Boolean = true
) {
    NimazTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = if (allowDecimal) {
                newValue.filter { it.isDigit() || it == '.' }
            } else {
                newValue.filter { it.isDigit() }
            }
            onValueChange(filtered)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number
    )
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Outlined TextField")
@Composable
private fun NimazTextFieldOutlinedPreview() {
    NimazTheme {
        var text by remember { mutableStateOf("") }
        Box(modifier = Modifier.padding(16.dp)) {
            NimazTextField(
                value = text,
                onValueChange = { text = it },
                label = "Name",
                placeholder = "Enter your name"
            )
        }
    }
}

@Preview(showBackground = true, name = "Filled TextField")
@Composable
private fun NimazTextFieldFilledPreview() {
    NimazTheme {
        var text by remember { mutableStateOf("") }
        Box(modifier = Modifier.padding(16.dp)) {
            NimazTextField(
                value = text,
                onValueChange = { text = it },
                label = "Email",
                placeholder = "Enter your email",
                style = NimazTextFieldStyle.FILLED
            )
        }
    }
}

@Preview(showBackground = true, name = "TextField with Error")
@Composable
private fun NimazTextFieldErrorPreview() {
    NimazTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NimazTextField(
                value = "invalid",
                onValueChange = {},
                label = "Email",
                isError = true,
                errorMessage = "Please enter a valid email"
            )
        }
    }
}

@Preview(showBackground = true, name = "Search TextField")
@Composable
private fun SearchTextFieldPreview() {
    NimazTheme {
        var text by remember { mutableStateOf("") }
        Box(modifier = Modifier.padding(16.dp)) {
            SearchTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Search surahs..."
            )
        }
    }
}

@Preview(showBackground = true, name = "Password TextField")
@Composable
private fun PasswordTextFieldPreview() {
    NimazTheme {
        var password by remember { mutableStateOf("") }
        Box(modifier = Modifier.padding(16.dp)) {
            PasswordTextField(
                value = password,
                onValueChange = { password = it }
            )
        }
    }
}

@Preview(showBackground = true, name = "Number TextField")
@Composable
private fun NumberTextFieldPreview() {
    NimazTheme {
        var number by remember { mutableStateOf("") }
        Box(modifier = Modifier.padding(16.dp)) {
            NumberTextField(
                value = number,
                onValueChange = { number = it },
                label = "Amount",
                placeholder = "Enter amount"
            )
        }
    }
}
