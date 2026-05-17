package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.molecules.FastingStatusCard
import com.arshadshah.nimaz.presentation.components.molecules.HadithOfTheDayCard
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Stacked "Today" info cards (fasting status + optional hadith of the day).
 * Used by the tablet home layout where vertical space is plentiful. Mobile
 * uses the swipeable [TodayCarousel] instead.
 */
@Composable
fun TodayInfoCards(
    fastingToday: Boolean,
    dailyHadith: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FastingStatusCard(fastingToday = fastingToday)
        if (dailyHadith != null) {
            HadithOfTheDayCard(hadith = dailyHadith)
        }
    }
}

private const val SAMPLE_HADITH =
    "The Prophet (peace be upon him) said: \"The best of you are those who learn the Quran and teach it.\" — Sahih al-Bukhari"

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun TodayInfoCards_Preview() {
    NimazTheme {
        TodayInfoCards(
            fastingToday = true,
            dailyHadith = SAMPLE_HADITH,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "No hadith")
@Composable
private fun TodayInfoCards_NoHadith_Preview() {
    NimazTheme {
        TodayInfoCards(
            fastingToday = false,
            dailyHadith = null,
            modifier = Modifier.padding(16.dp)
        )
    }
}
