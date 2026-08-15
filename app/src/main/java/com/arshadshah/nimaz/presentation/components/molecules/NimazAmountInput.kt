package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The amount field is **ASCII-canonical**: `.` is the decimal point and `,` groups thousands,
 * whatever the device locale is.
 *
 * That is deliberate, and it is not the same choice as `formatCurrency`, which *is* locale-aware.
 * The difference is direction. `formatCurrency` renders a finished `Double` for reading, so it can
 * follow the reader's conventions freely. This field has to parse back what someone is *still
 * typing*, and localised separators make that ambiguous: in German the grouping separator is `.`,
 * so a half-typed `1.234` is either one thousand two hundred and thirty-four or one-point-two-
 * three-four, and nothing in the string says which. Rather than guess at someone's money, the
 * field keeps one grammar and lets the *display* of the total localise.
 */
private const val GROUP_SEPARATOR = ','
private const val DECIMAL_POINT = '.'

/** The most decimal places a money field accepts. A third one is refused, not rounded away. */
private const val MAX_DECIMALS = 2

/**
 * Grouping applied to a partially-typed amount.
 *
 * Kept apart from the composable so the rule can be tested without a device — the awkward
 * cases are all mid-typing (`"42180."` with nothing after the point yet, a second point, a
 * third decimal), which a finished-value formatter never sees.
 *
 * The function is **idempotent**: the field feeds it its own previous output on every keystroke,
 * so `f(f(x)) == f(x)` or the separators would multiply as you type. That is what the
 * separator-stripping first pass is for.
 */
internal fun formatAmountInput(raw: String): String {
    val cleaned = buildString {
        var seenPoint = false
        var decimals = 0
        for (ch in raw) {
            when {
                // A third decimal is dropped where it was typed, rather than accepted and
                // rounded off later. Rounding someone's money without telling them is the
                // worst available option; refusing the keystroke at least shows.
                ch.isDigit() && seenPoint && decimals >= MAX_DECIMALS -> Unit
                ch.isDigit() -> {
                    append(ch)
                    if (seenPoint) decimals++
                }

                ch == DECIMAL_POINT && !seenPoint -> {
                    seenPoint = true
                    append(ch)
                }
                // Everything else — a second point, the grouping separators from the previous
                // pass, pasted currency symbols, letters — is dropped silently. Rejecting the
                // whole string would lose what the person had already typed.
                else -> Unit
            }
        }
    }
    if (cleaned.isEmpty()) return ""
    val point = cleaned.indexOf(DECIMAL_POINT)
    val whole = if (point >= 0) cleaned.substring(0, point) else cleaned
    val rest = if (point >= 0) cleaned.substring(point) else ""
    val grouped = whole.reversed().chunked(3).joinToString(GROUP_SEPARATOR.toString()).reversed()
    return grouped + rest
}

/**
 * The number behind what is currently in the field.
 *
 * A half-typed `"42,180."` parses to `42180.0` rather than to `0.0`: the calculator recomputes on
 * every keystroke, and a blank total the moment someone reaches for the decimal point reads as
 * the app having lost their figure.
 */
internal fun parseAmountInput(display: String): Double =
    display.filter { it.isDigit() || it == DECIMAL_POINT }
        .trimEnd(DECIMAL_POINT)
        .toDoubleOrNull()
        ?: 0.0

/**
 * A stored [amount] as field text.
 *
 * `0.0` renders as **empty**, not as `"0"`: an untouched asset row should show its muted
 * placeholder, and a literal zero someone did not type invites them to delete it first. Whole
 * amounts lose their `.0` for the same reason — nobody typed those decimals.
 */
internal fun amountToInput(amount: Double): String = when {
    amount == 0.0 -> ""
    amount == amount.toLong().toDouble() -> formatAmountInput(amount.toLong().toString())
    else -> formatAmountInput(amount.toString())
}

/** The width an in-row amount field occupies beside its label. */
private val AmountFieldWidth = 132.dp

/**
 * A currency-aware amount field — the input only.
 *
 * The label and hint belong to the screen: Zakat is the one caller today, and baking its row
 * arrangement into the component would fix a layout that only one screen needs. That is also
 * why it is [NimazFieldDensity.COMPACT] — it sits *beside* a label rather than under one, which
 * is the whole reason this looked different from [NimazDropdownField] before the two were put
 * on the same shell.
 *
 * Replaces the per-keystroke `text.toDoubleOrNull() ?: 0.0` the Zakat form used to do, which made
 * a decimal amount literally unenterable — the `.` was parsed away before the next digit arrived.
 *
 * A currency symbol leads and a unit follows, which is not a styling preference: "$ 1,200" and
 * "1,200 g" are how each is read, and the field that replaced this used to decide between them by
 * comparing its suffix against the string `"$"`. Passing both, or neither, is a call-site bug —
 * hence the requirement stated in the parameter docs rather than a silent fallback.
 *
 * Most callers want [NimazAmountField] instead, which owns the `Double` binding as well.
 */
@Composable
fun NimazAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Leads the number, e.g. `$`. Mutually exclusive with [unitSuffix]; supply exactly one. */
    currencySymbol: String? = null,
    /** Follows the number, e.g. `g` for grams. Mutually exclusive with [currencySymbol]. */
    unitSuffix: String? = null,
    enabled: Boolean = true,
    placeholder: String = "0.00",
) {
    NimazTextField(
        value = value,
        // Grouping is applied on the way in, so the field is fed its own previous output on
        // every keystroke — which is what [formatAmountInput]'s idempotence is for.
        onValueChange = { onValueChange(formatAmountInput(it)) },
        modifier = modifier.width(AmountFieldWidth),
        variant = NimazFieldVariant.NUMERIC,
        density = NimazFieldDensity.COMPACT,
        placeholder = placeholder,
        prefix = currencySymbol,
        suffix = unitSuffix,
        // No clear button: the field is 132dp wide and shares that space with a symbol and a
        // unit, and an amount is quicker to correct than to re-enter.
        clearable = false,
        enabled = enabled,
    )
}

/**
 * The same amount field, bound to a `Double` rather than to text.
 *
 * This is what the Zakat calculator and the Zakat settings screen each used to keep a private
 * copy of — `AmountField` and `PriceField`, byte-for-byte the same nineteen lines, including the
 * same two comments explaining the same guard. One of them was going to be fixed without the
 * other eventually.
 *
 * The text is **local state**, and that is the whole point. A field that parsed every keystroke
 * straight to a `Double` and re-rendered the result turned "10." into "10" before the next digit
 * landed, so a decimal amount was literally unenterable — and a per-gram silver price is nothing
 * but decimals.
 *
 * The sync back the other way is guarded: an incoming [value] only overwrites the text when it
 * disagrees with what the text already parses to. Without that guard, the ViewModel echoing back
 * the user's own keystroke would erase the trailing point as fast as it was typed — while
 * "Clear all" and a restored calculation, which genuinely differ, still win.
 */
@Composable
fun NimazAmountField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String? = null,
    unitSuffix: String? = null,
    enabled: Boolean = true,
    placeholder: String = "0.00",
) {
    var text by rememberSaveable { mutableStateOf(amountToInput(value)) }
    LaunchedEffect(value) {
        if (parseAmountInput(text) != value) text = amountToInput(value)
    }
    NimazAmountInput(
        value = text,
        onValueChange = { next ->
            text = next
            onValueChange(parseAmountInput(next))
        },
        modifier = modifier,
        currencySymbol = currencySymbol,
        unitSuffix = unitSuffix,
        enabled = enabled,
        placeholder = placeholder,
    )
}

@Composable
private fun NimazAmountInputShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Empty, so the muted placeholder is visible rather than assumed.
        NimazAmountInput(value = "", onValueChange = {}, currencySymbol = "€")
        NimazAmountInput(value = "42,180.50", onValueChange = {}, currencySymbol = "€")
        // Mid-typing: the trailing point the old field could not hold.
        NimazAmountInput(value = "1,284.", onValueChange = {}, currencySymbol = "£")
        // A weight, not money: the unit follows the number rather than leading it.
        NimazAmountInput(
            value = "87.48",
            onValueChange = {},
            unitSuffix = "g",
            placeholder = "0",
        )
        NimazAmountInput(
            value = "65.00",
            onValueChange = {},
            currencySymbol = "$",
            enabled = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Amount input — Light")
@Composable
private fun NimazAmountInputLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazAmountInputShowcase()
    }
}

@Preview(
    showBackground = true, widthDp = 360, name = "Amount input — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazAmountInputDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazAmountInputShowcase()
    }
}
