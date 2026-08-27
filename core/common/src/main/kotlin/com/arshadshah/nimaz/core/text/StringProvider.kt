package com.arshadshah.nimaz.core.text

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Localized strings, without a `Context`.
 *
 * Most user-facing text belongs in the composable that shows it, resolved with
 * `stringResource`. This seam is for the cases where it genuinely cannot be: a ViewModel that
 * **searches or sorts** on a label has to have the resolved string in hand, because the
 * comparison happens where the list is filtered, not where it is drawn.
 *
 * `BookmarksViewModel` is the worked example — its search matches a bookmark's title and its
 * ALPHABETICAL order sorts by it, so moving the labels to the screen would have quietly
 * changed what "Quran" matches.
 *
 * The point is the narrowing. A ViewModel holding `@ApplicationContext` can reach the whole
 * platform — system services, the package manager, the file system. One holding a
 * [StringProvider] can resolve a string and nothing else, and a test can hand it a fake.
 *
 * **Do not reach for this to avoid `stringResource`.** If a label is only ever displayed,
 * resolve it in the composable.
 */
interface StringProvider {

    /** The string for [id], with [args] substituted into its format specifiers. */
    fun get(@StringRes id: Int, vararg args: Any): String

    /** The plural form of [id] appropriate to [count], with [args] substituted. */
    fun quantity(@PluralsRes id: Int, count: Int, vararg args: Any): String
}
