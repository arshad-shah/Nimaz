package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithContentDescription
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.KhatamProgressCalculator
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class KhatamReadingActivityTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private val now = System.currentTimeMillis()
    private fun description(day: DailyLogEntry): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val date = SimpleDateFormat("EEEE, d MMMM", context.resources.configuration.locales[0]).format(Date(day.date))
        return date + ": " + context.resources.getQuantityString(R.plurals.khatam_ayahs_read_plural, day.ayahsRead, day.ayahsRead)
    }

    @Test fun `empty activity announces seven zero days`() {
        composeRule.setThemedContent { KhatamReadingActivity(emptyList(), now = now) }
        KhatamProgressCalculator.readingWeek(emptyList(), now).forEach {
            composeRule.onNodeWithContentDescription(description(it)).assertExists()
        }
    }

    @Test fun `reading activity announces actual counts and missing dates`() {
        val logs = listOf(DailyLogEntry(now, 20), DailyLogEntry(now - 172_800_000, 1))
        composeRule.setThemedContent { KhatamReadingActivity(logs, now = now) }
        KhatamProgressCalculator.readingWeek(logs, now).forEach {
            composeRule.onNodeWithContentDescription(description(it)).assertExists()
        }
    }
}
