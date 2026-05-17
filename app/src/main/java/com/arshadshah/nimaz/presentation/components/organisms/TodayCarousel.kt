package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.molecules.DailySummaryCard
import com.arshadshah.nimaz.presentation.components.molecules.HadithOfTheDayCard
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay

/**
 * Identifiers for the carousel's pages. Adding a new page = adding a value
 * here, mapping its data in [TodayCarousel], and rendering it in the pager.
 *
 * [SUMMARY] combines prayer progress + fasting status — two "today snapshot"
 * facts that read naturally together. [HADITH] is its own page because it's a
 * different reading mode (longer content, dwell time).
 */
enum class TodayCarouselPage { SUMMARY, HADITH }

/**
 * Swipeable carousel for the home screen's "Today" section. Replaces a
 * vertical stack of cards with a single card-height pager so adding more
 * widgets in the future doesn't bloat vertical space.
 *
 * Page indicator dots sit below the pager; the active dot widens into a
 * pill for a clearer affordance. Pages that don't apply (e.g. no hadith of
 * the day) are filtered out automatically rather than rendered empty.
 */
@Composable
fun TodayCarousel(
    prayerTimes: List<PrayerTimeDisplay>,
    fastingToday: Boolean,
    dailyHadith: String?,
    modifier: Modifier = Modifier,
    // Bumped from 180 → 240 because the combined SUMMARY page packs more in
    // (progress + fasting) and the HADITH page now uses larger body text.
    pageHeight: androidx.compose.ui.unit.Dp = 180.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp,
) {
    val pages = remember(dailyHadith) {
        buildList {
            add(TodayCarouselPage.SUMMARY)
            if (!dailyHadith.isNullOrBlank()) add(TodayCarouselPage.HADITH)
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight),
            // Edge peek so users see there's more to swipe to.
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            pageSpacing = 12.dp,
        ) { pageIndex ->
            when (pages[pageIndex]) {
                TodayCarouselPage.SUMMARY -> DailySummaryCard(
                    prayerTimes = prayerTimes,
                    fastingToday = fastingToday,
                    fillHeight = true,
                )
                TodayCarouselPage.HADITH -> HadithOfTheDayCard(
                    hadith = dailyHadith.orEmpty(),
                    fillHeight = true,
                    // Higher cap since the page is taller now and body text
                    // got bigger; lets longer ahadith breathe.
                    maxLines = 8,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PageIndicators(
            count = pages.size,
            current = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PageIndicators(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isCurrent = index == current
            // Active dot widens into a pill — clearer "you are here" cue
            // than a colour-only change for users with reduced colour vision.
            val width by animateDpAsState(
                targetValue = if (isCurrent) 20.dp else 8.dp,
                label = "indicator_width_$index"
            )
            val color by animateColorAsState(
                targetValue = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                label = "indicator_color_$index"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

private val samplePrayerTimes = listOf(
    PrayerTimeDisplay(PrayerType.FAJR, "Fajr", "5:23 AM", isPassed = true, isCurrent = false, isNext = false, prayerStatus = PrayerStatus.PRAYED),
    PrayerTimeDisplay(PrayerType.SUNRISE, "Sunrise", "6:45 AM", isPassed = true, isCurrent = false, isNext = false),
    PrayerTimeDisplay(PrayerType.DHUHR, "Dhuhr", "1:15 PM", isPassed = true, isCurrent = false, isNext = false, prayerStatus = PrayerStatus.PRAYED),
    PrayerTimeDisplay(PrayerType.ASR, "Asr", "4:30 PM", isPassed = false, isCurrent = true, isNext = true),
    PrayerTimeDisplay(PrayerType.MAGHRIB, "Maghrib", "6:12 PM", isPassed = false, isCurrent = false, isNext = false),
    PrayerTimeDisplay(PrayerType.ISHA, "Isha", "7:45 PM", isPassed = false, isCurrent = false, isNext = false),
)

private const val SAMPLE_HADITH =
    "The Prophet (peace be upon him) said: \"The best of you are those who learn the Quran and teach it.\" — Sahih al-Bukhari"

@Preview(showBackground = true, widthDp = 412, name = "Carousel — full")
@Composable
private fun TodayCarousel_Full_Preview() {
    NimazTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TodayCarousel(
                prayerTimes = samplePrayerTimes,
                fastingToday = true,
                dailyHadith = SAMPLE_HADITH,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "Carousel — no hadith")
@Composable
private fun TodayCarousel_NoHadith_Preview() {
    NimazTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TodayCarousel(
                prayerTimes = samplePrayerTimes,
                fastingToday = false,
                dailyHadith = null,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
