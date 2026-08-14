package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.WorshipReminderType
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
 * Jumu'ah Al-Kahf (Friday only), Hadith of the day, Dua of the moment,
 * next Worship reminder, and Qibla (always shown).
 *
 * Renders nothing when all contextual items are absent (Friday=false, no hadith, no dua,
 * no worship) — but since Qibla is always shown, the section is always present.
 */
@Composable
fun HomeAlsoTodaySection(
    isFriday: Boolean,
    dailyHadith: String?,
    dailyDua: DailyDua?,
    worshipCard: WorshipCardUi?,
    onNavigateToQuran: () -> Unit,
    onOpenHadith: () -> Unit,
    onNavigateToDua: () -> Unit,
    onOpenWorship: (WorshipReminderType) -> Unit,
    onNavigateToQibla: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            if (isFriday) {
                NimazMenuItem(
                    title = stringResource(R.string.home_read_al_kahf),
                    subtitle = stringResource(R.string.home_recommended_on_fridays),
                    icon = Icons.Default.Star,
                    iconTint = NimazColors.GoldDark,
                    onClick = onNavigateToQuran,
                )
            }

            if (dailyHadith != null) {
                NimazMenuItem(
                    title = stringResource(R.string.hadith_of_the_day),
                    subtitle = dailyHadith.take(80) + if (dailyHadith.length > 80) "…" else "",
                    icon = Icons.Default.AutoStories,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenHadith,
                )
            }

            if (dailyDua != null) {
                NimazMenuItem(
                    title = stringResource(R.string.dua_of_the_moment),
                    subtitle = dailyDua.translation.take(60) + if (dailyDua.translation.length > 60) "…" else "",
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFF7C4DFF),
                    onClick = onNavigateToDua,
                )
            }

            if (worshipCard != null) {
                NimazMenuItem(
                    title = worshipCard.name,
                    subtitle = worshipCard.body,
                    icon = Icons.Default.Bedtime,
                    iconTint = Color(0xFF8B5CF6),
                    onClick = { onOpenWorship(worshipCard.type) },
                )
            }

            NimazMenuItem(
                title = stringResource(R.string.more_pin_qibla),
                subtitle = stringResource(R.string.home_find_the_direction),
                icon = Icons.Default.Explore,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToQibla,
            )
        }
    }
}
