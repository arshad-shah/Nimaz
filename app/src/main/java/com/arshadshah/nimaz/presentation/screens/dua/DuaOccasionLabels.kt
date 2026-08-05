package com.arshadshah.nimaz.presentation.screens.dua

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.DuaOccasion

/**
 * A [DuaOccasion]'s label, in the reader's language.
 *
 * `DuaOccasion.displayName()` on the domain model returns hardcoded English, which is fine for
 * a log line and wrong on a chip a German reader is looking at. The resource lookup has to
 * happen in the presentation layer — the domain model has no `Context` and should not want one
 * — so the mapping lives here and the domain function stays where the non-UI callers are.
 */
@StringRes
fun duaOccasionLabelRes(occasion: DuaOccasion): Int = when (occasion) {
    DuaOccasion.MORNING -> R.string.dua_occasion_morning
    DuaOccasion.EVENING -> R.string.dua_occasion_evening
    DuaOccasion.AFTER_PRAYER -> R.string.dua_occasion_after_prayer
    DuaOccasion.BEFORE_SLEEP -> R.string.dua_occasion_before_sleep
    DuaOccasion.WAKING_UP -> R.string.dua_occasion_waking_up
    DuaOccasion.EATING -> R.string.dua_occasion_eating
    DuaOccasion.TRAVELING -> R.string.dua_occasion_traveling
    DuaOccasion.ENTERING_MOSQUE -> R.string.dua_occasion_entering_mosque
    DuaOccasion.LEAVING_MOSQUE -> R.string.dua_occasion_leaving_mosque
    DuaOccasion.ENTERING_HOME -> R.string.dua_occasion_entering_home
    DuaOccasion.LEAVING_HOME -> R.string.dua_occasion_leaving_home
    DuaOccasion.RAIN -> R.string.dua_occasion_rain
    DuaOccasion.DISTRESS -> R.string.dua_occasion_distress
    DuaOccasion.FORGIVENESS -> R.string.dua_occasion_forgiveness
    DuaOccasion.PARENTS -> R.string.dua_occasion_parents
    DuaOccasion.GRATITUDE -> R.string.dua_occasion_gratitude
    DuaOccasion.GENERAL -> R.string.dua_occasion_general
}

/** [duaOccasionLabelRes], resolved. */
@Composable
fun DuaOccasion.label(): String = stringResource(duaOccasionLabelRes(this))
