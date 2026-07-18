package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import java.text.SimpleDateFormat
import java.util.Date

/**
 * A date formatter hoisted out of composition.
 *
 * The previous screens constructed a [SimpleDateFormat] inside each row's composable, so
 * every recomposition of every list row allocated a fresh one — and [SimpleDateFormat] is
 * both expensive to build and not thread-safe.
 */
@Composable
fun rememberKhatamDateFormatter(pattern: String = "d MMM yyyy"): KhatamDateFormatter {
    val locale = Locale.current.platformLocale
    return remember(pattern, locale) { KhatamDateFormatter(pattern, locale) }
}

class KhatamDateFormatter(pattern: String, locale: java.util.Locale) {
    private val delegate = SimpleDateFormat(pattern, locale)

    fun format(epochMillis: Long): String = delegate.format(Date(epochMillis))
}
