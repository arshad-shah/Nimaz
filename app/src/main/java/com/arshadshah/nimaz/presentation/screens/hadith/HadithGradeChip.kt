package com.arshadshah.nimaz.presentation.screens.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.theme.NimazColors

// Hadith grade colour-coding, shared by the reader and settings preview.
// Backed by the single source of truth in NimazColors.HadithGradeColors.
val SahihGreen = NimazColors.HadithGradeColors.Sahih
val HasanTeal = NimazColors.HadithGradeColors.Hasan
val DaifAmber = NimazColors.HadithGradeColors.Daif
val MawduRed = NimazColors.HadithGradeColors.Mawdu

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

/** A small colour-coded pill showing a hadith's authenticity grade. */
@Composable
fun HadithGradeChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
