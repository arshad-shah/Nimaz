package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Layout shape of a [NimazNumberStepper].
 */
enum class NimazNumberStepperVariant {
    /**
     * A label on the left, with the decrement / value / increment controls grouped
     * compactly on the right. No surrounding container. The settings-row look
     * (prayer-time adjustments, daily targets).
     */
    INLINE,

    /**
     * A full-width tonal container with the decrement and increment buttons pushed
     * to the edges and a large, prominent value centred between them. The "dial in
     * a number" look (tasbih target counters).
     */
    SPREAD
}

/**
 * Size preset for a [NimazNumberStepper]: the +/- button diameter, the value field
 * width, and the value typography (resolved against [MaterialTheme.typography]).
 */
enum class NimazNumberStepperSize(
    val buttonSize: Dp,
    val iconSize: Dp,
    val minFieldWidth: Dp,
    val corner: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp
) {
    SMALL(32.dp, 18.dp, 46.dp, 8.dp, 8.dp, 6.dp),
    MEDIUM(40.dp, 24.dp, 60.dp, 14.dp, 8.dp, 6.dp),
    LARGE(48.dp, 28.dp, 84.dp, 14.dp, 12.dp, 8.dp)
}

/**
 * Colour emphasis of the displayed value.
 */
enum class NimazNumberStepperType {
    /** Neutral value. `onSurface`. */
    DEFAULT,

    /** Brand-accent value, drawing the eye to the number being set.
     *  `NimazColors.TasbihColors.Milestone`. */
    ACCENT
}

/**
 * A reusable number stepper: a decrement button, an **editable** value field, and an
 * increment button, with min/max clamping and a configurable step.
 *
 * Behaviours:
 * - **Tap the value to type** — opens the numeric keyboard, selects the current value,
 *   and commits (clamped to [minValue]..[maxValue]) on Done or when focus leaves.
 *   Invalid / empty input reverts. Disable with [editable] = false.
 * - **Hold −/+ to repeat** — press-and-hold auto-repeats with acceleration, so large
 *   ranges don't need dozens of taps.
 *
 * Two looks via [variant]:
 * - [NimazNumberStepperVariant.INLINE] — a [label] on the left and compact controls on
 *   the right (settings rows).
 * - [NimazNumberStepperVariant.SPREAD] — a full-width tonal container with a large centred
 *   value (target dials). [label] is ignored in this variant.
 *
 * @param value Current integer value
 * @param onValueChange Callback when value changes (already clamped to [minValue]..[maxValue])
 * @param variant Layout shape — see [NimazNumberStepperVariant]
 * @param size Control + typography size preset
 * @param type Value colour emphasis
 * @param label Text label shown on the left in [NimazNumberStepperVariant.INLINE]
 * @param editable Whether tapping the value opens the keyboard for direct entry
 * @param formatValue Custom formatter for the displayed (resting) value. Defaults to a "+"
 *   prefix for positive values in INLINE, and the plain number in SPREAD.
 * @param minValue Minimum allowed value (inclusive). Decrement disabled at this limit.
 * @param maxValue Maximum allowed value (inclusive). Increment disabled at this limit.
 * @param step Amount to increment/decrement per click / hold-tick
 */
@Composable
fun NimazNumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: NimazNumberStepperVariant = NimazNumberStepperVariant.INLINE,
    size: NimazNumberStepperSize = NimazNumberStepperSize.SMALL,
    type: NimazNumberStepperType = NimazNumberStepperType.DEFAULT,
    label: String? = null,
    editable: Boolean = true,
    formatValue: ((Int) -> String)? = null,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    step: Int = 1
) {
    val valueColor = when (type) {
        NimazNumberStepperType.DEFAULT -> MaterialTheme.colorScheme.onSurface
        NimazNumberStepperType.ACCENT -> NimazColors.TasbihColors.Milestone
    }
    val valueStyle = when (size) {
        NimazNumberStepperSize.SMALL -> MaterialTheme.typography.bodyMedium
        NimazNumberStepperSize.MEDIUM -> MaterialTheme.typography.titleMedium
        NimazNumberStepperSize.LARGE -> MaterialTheme.typography.headlineSmall
    }

    val decrement = { onValueChange((value - step).coerceIn(minValue, maxValue)) }
    val increment = { onValueChange((value + step).coerceIn(minValue, maxValue)) }
    val commit = { entered: Int -> onValueChange(entered.coerceIn(minValue, maxValue)) }

    val defaultText = when (variant) {
        NimazNumberStepperVariant.INLINE -> if (value > 0) "+$value" else "$value"
        NimazNumberStepperVariant.SPREAD -> "$value"
    }
    val displayText = formatValue?.invoke(value) ?: defaultText

    val field: @Composable () -> Unit = {
        EditableValue(
            value = value,
            displayText = displayText,
            editable = editable,
            valueStyle = valueStyle,
            valueColor = valueColor,
            minValue = minValue,
            maxValue = maxValue,
            minFieldWidth = size.minFieldWidth,
            onCommit = commit,
        )
    }

    when (variant) {
        NimazNumberStepperVariant.INLINE -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepperButton(
                    icon = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease),
                    tonal = false,
                    enabled = value > minValue,
                    size = size,
                    onStep = decrement,
                )
                field()
                StepperButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase),
                    tonal = false,
                    enabled = value < maxValue,
                    size = size,
                    onStep = increment,
                )
            }
        }

        NimazNumberStepperVariant.SPREAD -> NimazCard(
            modifier = modifier.fillMaxWidth(),
            style = NimazCardStyle.FILLED,
            shape = RoundedCornerShape(size.corner),
            colors = NimazCardDefaults.colors(
                container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepperButton(
                    icon = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease),
                    tonal = true,
                    enabled = value > minValue,
                    size = size,
                    onStep = decrement,
                )
                field()
                StepperButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase),
                    tonal = true,
                    enabled = value < maxValue,
                    size = size,
                    onStep = increment,
                )
            }
        }
    }
}

/**
 * The value display. When [editable], tapping focuses a numeric [BasicTextField]
 * (selecting the current value) and commits on Done / blur; otherwise it is a
 * static field-styled label.
 */
@Composable
private fun EditableValue(
    value: Int,
    displayText: String,
    editable: Boolean,
    valueStyle: TextStyle,
    valueColor: Color,
    minValue: Int,
    maxValue: Int,
    minFieldWidth: Dp,
    onCommit: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)

    if (!editable) {
        Box(
            modifier = Modifier
                .widthIn(min = minFieldWidth)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = valueStyle,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val allowNegative = minValue < 0
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(displayText)) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Reflect external value changes while not editing (resting shows the formatted text).
    LaunchedEffect(displayText, focused) {
        if (!focused) field = TextFieldValue(displayText)
    }

    fun commit() {
        field.text.trim().toIntOrNull()?.let { onCommit(it) }
        // Invalid / empty input is dropped; the resting sync restores displayText.
    }

    // A tap always (re)requests focus and re-shows the keyboard. This is what keeps
    // the field from getting "stuck": if focus was retained after the IME was
    // dismissed (e.g. system back), the manual `focused` flag stays true and
    // onFocusChanged never fires again — so re-tapping must drive the IME directly.
    val activate: () -> Unit = {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    BasicTextField(
        value = field,
        onValueChange = { new ->
            if (focused) {
                val filtered = new.text.filterIndexed { i, c ->
                    c.isDigit() || (c == '-' && i == 0 && allowNegative)
                }
                field = new.copy(text = filtered)
            }
        },
        singleLine = true,
        textStyle = valueStyle.copy(
            color = if (focused) primary else valueColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier
            .widthIn(min = minFieldWidth)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused && !focused) {
                    focused = true
                    // Switch to raw digits and select all so a tap-then-type replaces.
                    val raw = value.toString()
                    field = TextFieldValue(raw, selection = TextRange(0, raw.length))
                } else if (!state.isFocused && focused) {
                    focused = false
                    commit()
                }
            },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .clip(shape)
                    .clickable(onClick = activate)
                    .background(
                        if (focused) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .border(
                        width = if (focused) 1.5.dp else 1.dp,
                        color = if (focused) primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = shape
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { inner() }
        }
    )
}

/**
 * A circular +/- button that fires once on tap and **auto-repeats while held**
 * (accelerating). Reuses [NimazIcon] for the glyph; the press behaviour is custom
 * because it needs press-level control the icon-button atom doesn't expose.
 */
@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    tonal: Boolean,
    enabled: Boolean,
    size: NimazNumberStepperSize,
    onStep: () -> Unit,
) {
    val container = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        tonal -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        tonal -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }
    // The gesture coroutine in repeatingClickable outlives recompositions and is not
    // re-keyed on `onStep`; read the latest lambda so it never steps from a stale value.
    val currentStep by rememberUpdatedState(onStep)
    Box(
        modifier = Modifier
            .size(size.buttonSize)
            .clip(CircleShape)
            .background(container)
            .repeatingClickable(enabled = enabled) { currentStep() }
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        NimazIcon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            iconSize = size.iconSize
        )
    }
}

/**
 * Fires [onClick] once on press, then repeats it at an accelerating interval while
 * the pointer stays down. A quick tap fires exactly once.
 */
private fun Modifier.repeatingClickable(
    enabled: Boolean,
    initialDelayMillis: Long = 400L,
    minDelayMillis: Long = 55L,
    onClick: () -> Unit,
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onClick()
        var delay = initialDelayMillis
        while (true) {
            // Null → timed out (still held) → repeat. Non-null → pointer went up
            // or the gesture was cancelled → stop.
            val ended = withTimeoutOrNull(delay) { waitForUpOrCancellation(); true }
            if (ended != null) break
            onClick()
            delay = (delay * 4 / 5).coerceAtLeast(minDelayMillis)
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────
// Note: the tap-to-type keyboard and hold-to-repeat can't be exercised in a static
// preview — run the app or an interactive preview to feel them.

@Preview(showBackground = true, widthDp = 400, name = "Inline — sizes")
@Composable
private fun NimazNumberStepperInlinePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazNumberStepper(label = "Fajr adjustment", value = 5, onValueChange = {}, minValue = -30, maxValue = 30)
            NimazNumberStepper(
                label = "Daily target", value = 20, onValueChange = {},
                size = NimazNumberStepperSize.MEDIUM,
                formatValue = { "$it ayahs" }, minValue = 1, maxValue = 200
            )
            NimazNumberStepper(
                label = "Lead time", value = 15, onValueChange = {}, editable = false,
                minValue = 5, maxValue = 60, step = 5
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Spread — default & accent")
@Composable
private fun NimazNumberStepperSpreadPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazNumberStepper(
                value = 33, onValueChange = {},
                variant = NimazNumberStepperVariant.SPREAD,
                size = NimazNumberStepperSize.MEDIUM, minValue = 1, maxValue = 9999
            )
            NimazNumberStepper(
                value = 100, onValueChange = {},
                variant = NimazNumberStepperVariant.SPREAD,
                size = NimazNumberStepperSize.LARGE,
                type = NimazNumberStepperType.ACCENT, minValue = 1
            )
        }
    }
}
