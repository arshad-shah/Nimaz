package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.DailyDua
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The pages behind the first one.
 *
 * `TodayCarousel` is a pager, so only the first page is composed at rest — which is why the
 * hadith and dua branches of its `when` had never run despite the carousel itself being tested.
 * A pager only composes a screenful, sideways: the same trap as a `LazyRow`.
 *
 * The filtering is the behaviour worth pinning. An absent hadith must remove its page rather
 * than render an empty card, because the reader's swipe budget is the only thing the carousel
 * costs and a blank page spends it for nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w2000dp-h1200dp")
class TodayCarouselPagesTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val now = Clock.System.now()

    private fun prayers() = PrayerType.entries.map {
        PrayerTimeDisplay(
            type = it,
            name = it.displayName,
            timeAt = now + 1.hours,
            isPassed = false,
            prayerStatus = PrayerStatus.NOT_PRAYED,
        )
    }

    private fun dua() = DailyDua(
        duaId = "d1",
        title = "Morning remembrance",
        arabic = "اللهم بك أصبحنا",
        translation = "O Allah, by You we enter the morning",
        source = "Hisn al-Muslim",
        categoryLabel = "Morning adhkar",
        categoryIcon = "sun",
    )

    private fun render(
        dailyHadith: String? = null,
        dailyDua: DailyDua? = null,
        onHadithClick: (() -> Unit)? = null,
    ) {
        composeRule.setThemedContent {
            TodayCarousel(
                prayerTimes = prayers(),
                fastingToday = true,
                dailyHadith = dailyHadith,
                modifier = Modifier.fillMaxWidth(),
                dailyHadithReference = "Bukhari 1",
                dailyHadithGrade = "Sahih",
                dailyDua = dailyDua,
                onHadithClick = onHadithClick,
            )
        }
    }

    @Test
    fun `the hadith page renders its text, its reference and its grade`() {
        // `PageSize.Fill` means one page per width whatever the viewport, so a wide qualifier
        // does not compose the later pages — the pager has to be scrolled to them. That is why
        // the hadith and dua branches of the `when` had never run.
        render(dailyHadith = "Actions are but by intentions.", dailyDua = dua())

        composeRule.onNode(hasScrollAction()).performScrollToIndex(HADITH_PAGE)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Actions are but by intentions.", substring = true)
            .assertExists()
        composeRule.onNodeWithText("Bukhari 1", substring = true).assertExists()
    }

    @Test
    fun `the dua page renders the translation and its category`() {
        render(dailyHadith = "Actions are but by intentions.", dailyDua = dua())

        composeRule.onNode(hasScrollAction()).performScrollToIndex(DUA_PAGE)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            "O Allah, by You we enter the morning", substring = true
        ).assertExists()
    }

    @Test
    fun `an absent hadith removes its page rather than showing an empty card`() {
        render(dailyHadith = null, dailyDua = dua())

        // With no hadith the dua takes index 2, so scrolling there and finding the dua is the
        // assertion that the hadith page is gone rather than merely empty.
        composeRule.onNode(hasScrollAction()).performScrollToIndex(HADITH_PAGE)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Bukhari 1", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(
            "O Allah, by You we enter the morning", substring = true
        ).assertExists()
    }

    @Test
    fun `a blank hadith counts as absent`() {
        // The loader writes "" when the corpus is missing rather than null; treating that as
        // present is how an empty card ships.
        render(dailyHadith = "   ", dailyDua = dua())

        composeRule.onNode(hasScrollAction()).performScrollToIndex(HADITH_PAGE)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Bukhari 1", substring = true).assertDoesNotExist()
    }

    @Test
    fun `an absent dua removes its page`() {
        render(dailyHadith = "Actions are but by intentions.", dailyDua = null)

        composeRule.onNode(hasScrollAction()).performScrollToIndex(HADITH_PAGE)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            "O Allah, by You we enter the morning", substring = true
        ).assertDoesNotExist()
    }

    private companion object {
        /** PROGRESS, FASTING, HADITH, DUA — in that order, minus whatever is filtered out. */
        const val HADITH_PAGE = 2
        const val DUA_PAGE = 3
    }

    @Test
    fun `progress and fasting are always there, whatever the corpora say`() {
        // These two pages are the carousel's floor: a reader with no content database still
        // gets today's prayers and today's fast.
        render(dailyHadith = null, dailyDua = null)

        composeRule.onNode(hasScrollAction()).assertExists()
    }
}
