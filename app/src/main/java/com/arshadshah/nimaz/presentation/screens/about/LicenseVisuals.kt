package com.arshadshah.nimaz.presentation.screens.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone

/**
 * How a [LicenseFamily] is spoken and coloured on the About screens.
 *
 * Colour goes through [NimazTone] rather than a palette of its own: the tone vocabulary is
 * already the app's one mapping from meaning to colour, and it carries the dark theme, the
 * container/foreground pairing and the contrast work with it. A licence-family palette would
 * be a seventh dialect of the same six colours.
 */
internal val LicenseFamily.tone: NimazTone
    get() = when (this) {
        LicenseFamily.APACHE_2 -> NimazTone.ACCENT
        LicenseFamily.MIT -> NimazTone.WARNING
        LicenseFamily.OFL -> NimazTone.SUCCESS
        LicenseFamily.BSD -> NimazTone.PROMINENT
        // Not a judgement on copyleft — ERROR is simply the tone that reads as the mockup's
        // rose, and the family needs a colour distinct from the other five.
        LicenseFamily.GPL -> NimazTone.ERROR
        LicenseFamily.OTHER -> NimazTone.MUTED
    }

/** The family's short display name — "Apache 2.0", "Open Font". */
@Composable
internal fun LicenseFamily.label(): String = stringResource(
    when (this) {
        LicenseFamily.APACHE_2 -> R.string.license_family_apache
        LicenseFamily.MIT -> R.string.license_family_mit
        LicenseFamily.BSD -> R.string.license_family_bsd
        LicenseFamily.OFL -> R.string.license_family_ofl
        LicenseFamily.GPL -> R.string.license_family_gpl
        LicenseFamily.OTHER -> R.string.license_family_other
    }
)

/**
 * A one-sentence, non-legal gloss of what the family permits, or null for [LicenseFamily.OTHER].
 *
 * Paraphrase, never substitute: the detail screen shows this above the full text and says so
 * in [R.string.license_detail_governs_note]. A family we cannot name we do not summarise.
 */
@Composable
internal fun LicenseFamily.plainSummary(): String? = when (this) {
    LicenseFamily.APACHE_2 -> stringResource(R.string.license_plain_apache)
    LicenseFamily.MIT -> stringResource(R.string.license_plain_mit)
    LicenseFamily.BSD -> stringResource(R.string.license_plain_bsd)
    LicenseFamily.OFL -> stringResource(R.string.license_plain_ofl)
    LicenseFamily.GPL -> stringResource(R.string.license_plain_gpl)
    LicenseFamily.OTHER -> null
}

/**
 * [text] with every occurrence of [query] painted as a search hit.
 *
 * Case-insensitive and every-occurrence, because the list searches name, author and coordinate
 * — highlighting only the first match makes a two-word query look like it half-matched.
 */
@Composable
internal fun highlighted(text: String, query: String): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)

    val hit = SpanStyle(
        background = MaterialTheme.colorScheme.secondaryContainer,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    return buildAnnotatedString {
        var cursor = 0
        while (cursor <= text.length) {
            val match = text.indexOf(needle, cursor, ignoreCase = true)
            if (match < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, match))
            withStyle(hit) { append(text.substring(match, match + needle.length)) }
            cursor = match + needle.length
        }
    }
}
