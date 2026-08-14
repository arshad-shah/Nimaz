package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.model.DailyDua
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * "Also today" section for the home compact layout.
 *
 * Renders a [NimazSectionHeader] + a [NimazCard] containing contextual rows:
 * Jumu'ah Al-Kahf (Friday only), Hadith of the day, Dua of the moment, and
 * next Worship reminder. Qibla is in the bottom nav.
 *
 * Renders nothing when all contextual items are absent.
 */
@Composable
fun HomeAlsoTodaySection(
    isFriday: Boolean,
    dailyHadith: String?,
    dailyDua: DailyDua?,
    worshipCard: WorshipCardUi?,
    onNavigateToAlKahf: () -> Unit,
    onOpenHadith: () -> Unit,
    onNavigateToDua: (duaId: String) -> Unit,
    onOpenWorship: (WorshipReminderType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAny = isFriday || dailyHadith != null || dailyDua != null || worshipCard != null
    if (!hasAny) return

    Column(modifier = modifier.fillMaxWidth()) {
        NimazSectionHeader(
            title = stringResource(R.string.home_also_today),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        NimazCard(
            style = NimazCardStyle.FILLED,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (worshipCard != null) {
                NimazMenuItem(
                    title = worshipCard.name,
                    subtitle = stringResource(R.string.home_worship_subtitle),
                    icon = Icons.Default.Bedtime,
                    iconTint = Color(0xFF7C4DFF),
                    onClick = { onOpenWorship(worshipCard.type) },
                )
            }

            if (isFriday) {
                NimazMenuItem(
                    title = stringResource(R.string.home_read_al_kahf),
                    subtitle = stringResource(R.string.home_recommended_on_fridays),
                    icon = Icons.Default.Star,
                    iconTint = NimazColors.GoldDark,
                    onClick = onNavigateToAlKahf,
                    trailing = {
                        ArabicText(
                            text = "سورة الكهف",
                            size = ArabicTextSize.SMALL,
                        )
                    },
                )
            }

            if (dailyHadith != null) {
                NimazMenuItem(
                    title = stringResource(R.string.hadith_of_the_day),
                    subtitle = dailyHadith.take(80) + if (dailyHadith.length > 80) "…" else "",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenHadith,
                )
            }

            if (dailyDua != null) {
                NimazMenuItem(
                    title = stringResource(R.string.dua_of_the_moment),
                    subtitle = dailyDua.translation.take(60) + if (dailyDua.translation.length > 60) "…" else "",
                    icon = Icons.Default.SelfImprovement,
                    iconTint = Color(0xFF7C4DFF),
                    onClick = { onNavigateToDua(dailyDua.duaId) },
                )
            }
        }
    }
}
