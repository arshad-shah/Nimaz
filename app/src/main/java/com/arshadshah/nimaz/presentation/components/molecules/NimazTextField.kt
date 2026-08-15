package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * What kind of answer a field takes.
 *
 * A variant chooses typeface, direction, alignment and keyboard — **never** the geometry, which
 * belongs to [NimazFieldShell] and is the same for every member of the family.
 *
 * There is deliberately no variant per *feature*: a khatam name, a preset name and a fasting
 * reason are all [TEXT]. A new variant is only justified when the text itself behaves
 * differently, which so far means Arabic, numbers and long-form notes.
 */
enum class NimazFieldVariant {
    /** Ordinary single-line prose. */
    TEXT,

    /**
     * Arabic entry: Amiri at 22sp, right-aligned, RTL, in the theme's gold.
     *
     * This is the variant that pays for itself. Before it, every Arabic input hand-set a
     * `textStyle` *and* an `OutlinedTextFieldDefaults.colors` block, at a different size each
     * time — four lines of styling per field, repeated per screen, drifting per screen.
     */
    ARABIC,

    /** A number: tabular figures, semi-bold, right-aligned, decimal keyboard. */
    NUMERIC,

    /** Long-form text over several lines. No clear button — see [NimazTextField]'s docs. */
    NOTE,
}

/**
 * The app's text field. One shell shared with [NimazDropdownField] and [NimazAmountInput], so a
 * form no longer shows two different ideas of what a field looks like.
 *
 * There is no `shape`, no `colors` and no `textStyle` parameter, and that absence is the point:
 * those are exactly how the twelve `OutlinedTextField` call sites this replaced ended up
 * hand-setting a 14dp radius on four fields in one screen and styling the Arabic one inline. If
 * a call site needs one of them, the [NimazFieldVariant] list is missing something.
 *
 * **Errors show on blur or on submit, never on the first keystroke.** Pass [validator] for the
 * per-field rule and the field runs it when focus leaves — then, once it is showing an error, on
 * every keystroke so the message clears the moment the input is good. Pass [error] for a message
 * the *screen* decided (a failed save, a submit-time check); a caller-supplied [error] always
 * wins over the validator's.
 *
 * @param error a message, not a boolean. Material's `isError` + a conditional `supportingText`
 *   lets a field turn red with nothing on screen explaining why, and three of the call sites
 *   this replaced did exactly that.
 * @param maxLength shows the counter and marks an over-long value — it does **not** truncate.
 *   Cutting off what somebody typed is worse than letting them cut it.
 * @param clearable a trailing clear button. Defaults on for single-line fields and off for
 *   [NimazFieldVariant.NOTE], where one tap can destroy a paragraph and undo is a long press away.
 * @param prefix leads the value (a currency symbol); [suffix] follows it (a unit). These replace
 *   `currencySymbol`/`unitSuffix`, which were the same parameter written twice.
 */
@Composable
fun NimazTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    variant: NimazFieldVariant = NimazFieldVariant.TEXT,
    density: NimazFieldDensity = NimazFieldDensity.STANDARD,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    validator: ((String) -> String?)? = null,
    required: Boolean = false,
    optionalLabel: String? = null,
    maxLength: Int? = null,
    prefix: String? = null,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
    clearable: Boolean = variant != NimazFieldVariant.NOTE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    minLines: Int = if (variant == NimazFieldVariant.NOTE) 3 else 1,
    maxLines: Int = if (variant == NimazFieldVariant.NOTE) 8 else 1,
    imeAction: ImeAction? = null,
    onImeAction: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // The validator's verdict, held from one blur to the next. Null until the field has been
    // focused *and then left*, which is what keeps a required field from going red the instant
    // the form appears — `onFocusChanged` reports "not focused" on the first composition too,
    // and treating that as a blur would flag every empty field on screen before anyone typed.
    var blurError by remember(validator) { mutableStateOf<String?>(null) }
    var hasBeenFocused by remember(validator) { mutableStateOf(false) }

    val overLimit = maxLength != null && value.length > maxLength
    val shownError = error ?: blurError
    val singleLine = variant != NimazFieldVariant.NOTE

    val contentColor = when {
        variant == NimazFieldVariant.ARABIC -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val textStyle = when (variant) {
        NimazFieldVariant.TEXT, NimazFieldVariant.NOTE ->
            MaterialTheme.typography.bodyLarge.copy(color = contentColor)

        NimazFieldVariant.ARABIC -> MaterialTheme.typography.bodyLarge.copy(
            fontFamily = AmiriFontFamily,
            fontSize = 22.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.End,
            textDirection = TextDirection.Rtl,
            color = contentColor,
        )

        NimazFieldVariant.NUMERIC -> MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = contentColor,
        )
    }
    val keyboardType = when (variant) {
        NimazFieldVariant.NUMERIC -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }

    NimazFieldShell(
        modifier = modifier,
        label = label,
        required = required,
        optionalLabel = optionalLabel,
        helper = helper,
        error = shownError,
        counter = maxLength?.let {
            stringResource(R.string.field_character_count, value.length, it)
        },
        counterIsOver = overLimit,
        focused = focused,
        enabled = enabled,
        readOnly = readOnly,
        density = density,
        // The caret occupies the middle of the box; a tap on the padding around it has to
        // reach the field too, or the field reads as broken at its own edges.
        onBoxTap = if (readOnly) null else {
            { focusRequester.requestFocus() }
        },
        // A multi-line note grows downwards; its affixes and clear button must stay level with
        // the first line rather than drift to the vertical middle of a paragraph.
        boxVerticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leadingIcon != null) {
            NimazIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                containerSize = 32.dp,
                iconSize = 18.dp,
                cornerRadius = 9.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        if (prefix != null) {
            Affix(prefix)
            Spacer(modifier = Modifier.width(4.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = { next ->
                onValueChange(next)
                // Only re-validate while an error is already on screen: this is the "clears as
                // you fix it" half of the rule, not a licence to validate as you type.
                if (blurError != null && validator != null) blurError = validator(next)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        hasBeenFocused = true
                    } else if (hasBeenFocused) {
                        blurError = validator?.invoke(value)
                    }
                },
            enabled = enabled && !readOnly,
            readOnly = readOnly,
            textStyle = textStyle,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction ?: if (singleLine) ImeAction.Next else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions {
                if (onImeAction != null) onImeAction() else focusManager.clearFocus()
            },
            decorationBox = { inner ->
                Box(
                    contentAlignment = when (variant) {
                        NimazFieldVariant.NUMERIC, NimazFieldVariant.ARABIC ->
                            Alignment.CenterEnd

                        else -> Alignment.CenterStart
                    },
                ) {
                    if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.75f),
                            ),
                        )
                    }
                    inner()
                }
            },
        )

        if (suffix != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Affix(suffix)
        }
        if (clearable && value.isNotEmpty() && enabled && !readOnly) {
            Spacer(modifier = Modifier.width(4.dp))
            NimazIconButton(
                icon = Icons.Default.Clear,
                onClick = { onValueChange("") },
                contentDescription = stringResource(R.string.cd_clear),
                size = NimazIconButtonSize.SMALL,
            )
        }
    }
}

/** The unit or currency that leads or follows a value. Muted — it is not the answer. */
@Composable
private fun Affix(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Composable
private fun NimazTextFieldShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(NimazFieldDefaults.FieldGap),
    ) {
        NimazTextField(
            value = "",
            onValueChange = {},
            label = "Preset name",
            required = true,
            placeholder = "Morning adhkar",
            helper = "Shown in your list of presets",
            maxLength = 40,
        )
        NimazTextField(
            value = "Morning adhkar",
            onValueChange = {},
            label = "Filled",
            leadingIcon = Icons.Default.Person,
        )
        NimazTextField(
            value = "",
            onValueChange = {},
            label = "Arabic text",
            variant = NimazFieldVariant.ARABIC,
            placeholder = "سُبْحَانَ ٱللَّٰهِ",
        )
        NimazTextField(
            value = "12,480.00",
            onValueChange = {},
            label = "Zakat on savings",
            variant = NimazFieldVariant.NUMERIC,
            prefix = "€",
            suffix = "EUR",
        )
        NimazTextField(
            value = "86.40",
            onValueChange = {},
            variant = NimazFieldVariant.NUMERIC,
            density = NimazFieldDensity.COMPACT,
            suffix = "g",
            modifier = Modifier.width(132.dp),
        )
        NimazTextField(
            value = "",
            onValueChange = {},
            label = "Note",
            optionalLabel = "optional",
            variant = NimazFieldVariant.NOTE,
            placeholder = "Ask about the weight of speech here.",
            helper = "Attached to this highlight",
            maxLength = 280,
        )
        NimazTextField(
            value = "",
            onValueChange = {},
            label = "Preset name",
            required = true,
            error = "A name is needed, at least 3 characters.",
        )
        NimazTextField(
            value = "This note runs past the limit it was given",
            onValueChange = {},
            label = "Over the limit",
            maxLength = 20,
        )
        NimazTextField(
            value = "€312.00",
            onValueChange = {},
            label = "Calculated from your entries",
            readOnly = true,
        )
        NimazTextField(
            value = "Set by your region",
            onValueChange = {},
            label = "Fidya rate",
            helper = "Change your region to edit this",
            enabled = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 1400, name = "NimazTextField — Light")
@Composable
private fun NimazTextFieldLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazTextFieldShowcase() }
}

@Preview(
    showBackground = true, widthDp = 380, heightDp = 1400, name = "NimazTextField — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazTextFieldDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazTextFieldShowcase() }
}
