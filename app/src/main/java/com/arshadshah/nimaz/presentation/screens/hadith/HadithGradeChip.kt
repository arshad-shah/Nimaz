package com.arshadshah.nimaz.presentation.screens.hadith

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.theme.NimazColors

// Hadith grade colour-coding, shared by the reader and settings preview.
val SahihGreen = NimazColors.Success
val HasanTeal = NimazColors.Primary600
val DaifAmber = NimazColors.Warning
val MawduRed = NimazColors.PrayerColors.Maghrib

data class HadithGradeDisplay(val label: String, val color: Color)

/**
 * Resolves a [HadithGrade] to its localized label + colour, or null for an
 * unknown/absent grade (in which case the grade chip should be hidden).
 */
@Composable
fun hadithGradeDisplay(grade: HadithGrade?): HadithGradeDisplay? = when (grade) {
    HadithGrade.SAHIH -> HadithGradeDisplay(stringResource(R.string.hadith_grade_sahih), SahihGreen)
    HadithGrade.HASAN -> HadithGradeDisplay(stringResource(R.string.hadith_grade_hasan), HasanTeal)
    HadithGrade.DAIF -> HadithGradeDisplay(stringResource(R.string.hadith_grade_daif), DaifAmber)
    HadithGrade.MAWDU -> HadithGradeDisplay(stringResource(R.string.hadith_grade_mawdu), MawduRed)
    else -> null
}

/**
 * A small colour-coded pill showing a hadith's authenticity grade.
 *
 * [onClick] makes the same pill a filter control on the collection screen; left null it is
 * the read-only chip the reader shows against a hadith.
 */
@Composable
fun HadithGradeChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    NimazBadge(
        text = label,
        modifier = modifier,
        size = NimazBadgeSize.LARGE,
        icon = Icons.Filled.FiberManualRecord,
        onClick = onClick,
        colors = NimazBadgeDefaults
            .feature(color = color, emphasis = NimazBadgeEmphasis.SOFT)
            .copy(borderColor = color.copy(alpha = 0.35f))
    )
}
