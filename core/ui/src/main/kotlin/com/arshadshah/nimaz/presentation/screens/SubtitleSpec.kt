package com.arshadshah.nimaz.presentation.screens

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * One argument inside a [SubtitleSpec].
 *
 * A plain `List<Any>` would not do: some subtitles interpolate a **string resource** — a worship
 * reminder's own translated name — and an `Int` in an untyped arg list is indistinguishable from a
 * count. That distinction only shows up as a bug in a non-English locale, where the row renders a
 * resource id where a name should be.
 */
sealed interface SubtitleArg {
    /** A number to interpolate — and, for a plural, also the quantity. */
    data class Count(val value: Int) : SubtitleArg

    /** Text already resolved by the layer that had it (a formatted amount, a Hijri date). */
    data class Text(val value: String) : SubtitleArg

    /** A string resource to resolve at render time, so it lands in the reader's language. */
    data class Resource(@StringRes val res: Int) : SubtitleArg
}

/**
 * What a row should say, before anything has resolved it into text.
 *
 * Shared by More and Fasting, which both replaced static self-describing subtitles with live
 * reports. Keeping one contract means the "plural iff quantity" invariant is stated once, and the
 * resolver below is the only place that has to honour it.
 *
 * [quantity] is non-null exactly when [res] is a `plurals` resource. [resolve] switches on it to
 * pick `pluralStringResource` over `stringResource`; getting that wrong throws at render time, in
 * a locale the author may well not read, which is why the mappers' tests assert the invariant
 * rather than trusting each call site.
 */
data class SubtitleSpec(
    /** A `@StringRes` when [quantity] is null, a `@PluralsRes` when it is not. */
    @StringRes @PluralsRes val res: Int,
    val args: List<SubtitleArg> = emptyList(),
    val quantity: Int? = null,
)

/**
 * A [SubtitleSpec] as text, or null when there is nothing to say.
 *
 * Null in, null out — so a row passes `subtitle = spec.resolve()` and renders nothing at all when
 * the figure has not arrived. That is the loading contract: absent, never a dash or a spinner.
 */
@Composable
fun SubtitleSpec?.resolve(): String? {
    val spec = this ?: return null
    val args: Array<Any> = spec.args.map<SubtitleArg, Any> { arg ->
        when (arg) {
            is SubtitleArg.Count -> arg.value
            is SubtitleArg.Text -> arg.value
            is SubtitleArg.Resource -> stringResource(arg.res)
        }
    }.toTypedArray()
    return spec.quantity
        ?.let { pluralStringResource(spec.res, it, *args) }
        ?: stringResource(spec.res, *args)
}
