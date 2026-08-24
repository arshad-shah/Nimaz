package com.arshadshah.nimaz.presentation.screens

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider

/**
 * The screen tests' one rule about strings: **no test owns a literal.**
 *
 * A Compose test finds a node by the text on it, so the obvious spelling is
 * `onNodeWithText("Nothing saved yet")` — and that test then fails the day someone improves the
 * copy, on a screen whose behaviour did not change. Worse, it passes for the wrong reason if two
 * screens happen to share a phrase. Resolving through `R` means a test names the *string*, which
 * is what it actually depends on, and a rename is a compile error rather than a red run.
 *
 * The same rule the instrumented suite already follows through `support/Selectors`.
 */
fun str(@StringRes id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id)

fun str(@StringRes id: Int, vararg args: Any): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id, *args)
