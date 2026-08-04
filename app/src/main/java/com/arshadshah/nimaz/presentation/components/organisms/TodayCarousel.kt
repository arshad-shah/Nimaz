package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.molecules.DuaOfTheMomentCard
import com.arshadshah.nimaz.presentation.components.molecules.FastingStatusCard
import com.arshadshah.nimaz.presentation.components.molecules.HadithOfTheDayCard
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.home.DailyDua
import com.arshadshah.nimaz.presentation.viewmodel.home.PrayerTimeDisplay
import kotlin.time.Instant
import androidx.compose.runtime.getValue
import com.arshadshah.nimaz.core.util.prayerTimelineProgressAt
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow

/**
 * Identifiers for the carousel's pages. Adding a new page = adding a value
 * here, mapping its data in [TodayCarousel], and rendering it in the pager.
 *
 * Prayer [PROGRESS] and [FASTING] are now separate cards (previously combined
 * on one page with a divider). [HADITH] and [DUA] are each their own page
 * because they're a different reading mode (longer content, dwell time). The
 * [DUA] page is time-of-day aware (morning / evening / before-sleep adhkar).
 */
enum class TodayCarouselPage { PROGRESS, FASTING, HADITH, DUA }

/**
 * The home screen's "Today" section: a single card-height pager combining
 * progress, fasting, hadith and dua so adding more widgets doesn't bloat
 * vertical space. Pages that don't apply (e.g. no hadith) are filtered out.
 *
 * Built on the shared [NimazCarousel] (edge peek + indicator dots, swipe-only).
 */
@Composable
fun TodayCarousel(
    prayerTimes: List<PrayerTimeDisplay>,
    fastingToday: Boolean,
    dailyHadith: String?,
    modifier: Modifier = Modifier,
    dailyHadithReference: String? = null,
    dailyHadithGrade: String? = null,
    dailyDua: DailyDua? = null,
    onHadithClick: (() -> Unit)? = null,
    pageHeight: Dp = 160.dp,
    horizontalPadding: Dp = 20.dp,
) {
    val pages = remember(dailyHadith, dailyDua) {
        buildList {
            add(TodayCarouselPage.PROGRESS)
            add(TodayCarouselPage.FASTING)
            if (!dailyHadith.isNullOrBlank()) add(TodayCarouselPage.HADITH)
            if (dailyDua != null) add(TodayCarouselPage.DUA)
        }
    }

    NimazCarousel(
        count = pages.size,
        modifier = modifier,
        pageHeight = pageHeight,
        horizontalPadding = horizontalPadding,
        pageSpacing = 12.dp,
    ) { pageIndex ->
        when (pages[pageIndex]) {
            TodayCarouselPage.PROGRESS -> TodaysProgressCard(
                prayerTimes = prayerTimes,
                fillHeight = true,
            )

            TodayCarouselPage.FASTING -> FastingStatusCard(
                fastingToday = fastingToday,
                fillHeight = true,
            )

            TodayCarouselPage.HADITH -> HadithOfTheDayCard(
                hadith = dailyHadith.orEmpty(),
                reference = dailyHadithReference,
                grade = dailyHadithGrade,
                onClick = onHadithClick,
                fillHeight = true,
                // Capped to what fits the page height without leaving the card
                // looking empty; longer ahadith ellipsize and open in the reader.
                maxLines = 4,
            )

            TodayCarouselPage.DUA -> DuaOfTheMomentCard(
                arabic = dailyDua?.arabic.orEmpty(),
                translation = dailyDua?.translation.orEmpty(),
                categoryLabel = dailyDua?.categoryLabel.orEmpty(),
                source = dailyDua?.source,
                fillHeight = true,
            )
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

private val samplePrayerTimes = listOf(
    PrayerTimeDisplay(
        PrayerType.FAJR,
        "Fajr",
        previewInstant(5, 23),
        isPassed = true,
        isCurrent = false,
        isNext = false,
        prayerStatus = PrayerStatus.PRAYED
    ),
    PrayerTimeDisplay(
        PrayerType.SUNRISE,
        "Sunrise",
        previewInstant(6, 45),
        isPassed = true,
        isCurrent = false,
        isNext = false
    ),
    PrayerTimeDisplay(
        PrayerType.DHUHR,
        "Dhuhr",
        previewInstant(13, 15),
        isPassed = true,
        isCurrent = false,
        isNext = false,
        prayerStatus = PrayerStatus.PRAYED
    ),
    PrayerTimeDisplay(
        PrayerType.ASR,
        "Asr",
        previewInstant(16, 30),
        isPassed = false,
        isCurrent = true,
        isNext = true
    ),
    PrayerTimeDisplay(
        PrayerType.MAGHRIB,
        "Maghrib",
        previewInstant(18, 12),
        isPassed = false,
        isCurrent = false,
        isNext = false
    ),
    PrayerTimeDisplay(
        PrayerType.ISHA,
        "Isha",
        previewInstant(19, 45),
        isPassed = false,
        isCurrent = false,
        isNext = false
    ),
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

/** Fixed wall-clock instants for previews, so sample rows read like a real day. */
private fun previewInstant(hour: Int, minute: Int): Instant =
    Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
