package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IslamicEventCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `primitive overload renders core fields with all optionals`() {
        composeRule.setThemedContent {
            IslamicEventCard(
                eventName = "Eid al-Fitr",
                eventNameArabic = "Arabic-Eid",
                eventType = HijriDateCalculator.EventType.EID,
                hijriDate = "1 Shawwal 1447",
                gregorianDate = "30 March 2026",
                daysUntil = 58,
                description = "Festival of Breaking the Fast.",
                onClick = {}
            )
        }
        composeRule.onNodeWithText("Eid al-Fitr").assertExists()
        composeRule.onNodeWithText("Arabic-Eid").assertExists()
        composeRule.onNodeWithText("1 Shawwal 1447").assertExists()
        composeRule.onNodeWithText("30 March 2026").assertExists()
        composeRule.onNodeWithText("58 days").assertExists()
        composeRule.onNodeWithText("Festival of Breaking the Fast.").assertExists()
    }

    @Test
    fun `primitive overload omits optional badge and description when null`() {
        composeRule.setThemedContent {
            IslamicEventCard(
                eventName = "Ramadan Begins",
                eventNameArabic = "Arabic-Ramadan",
                eventType = HijriDateCalculator.EventType.RAMADAN,
                hijriDate = "1 Ramadan 1447",
                gregorianDate = "1 March 2026"
            )
        }
        composeRule.onNodeWithText("Ramadan Begins").assertExists()
        composeRule.onNodeWithText("1 Ramadan 1447").assertExists()
        composeRule.onNodeWithText("days").assertDoesNotExist()
    }

    @Test
    fun `daysUntil zero shows Today`() {
        composeRule.setThemedContent {
            IslamicEventCard(
                eventName = "Laylat al-Qadr",
                eventNameArabic = "Arabic-Qadr",
                eventType = HijriDateCalculator.EventType.SPECIAL_NIGHT,
                hijriDate = "27 Ramadan 1447",
                gregorianDate = "27 March 2026",
                daysUntil = 0
            )
        }
        composeRule.onNodeWithText("Today").assertExists()
    }

    @Test
    fun `daysUntil one shows Tomorrow`() {
        composeRule.setThemedContent {
            IslamicEventCard(
                eventName = "Some Event",
                eventNameArabic = "Arabic",
                eventType = HijriDateCalculator.EventType.HOLIDAY,
                hijriDate = "10 Muharram",
                gregorianDate = "1 Jan 2026",
                daysUntil = 1
            )
        }
        composeRule.onNodeWithText("Tomorrow").assertExists()
    }

    @Test
    fun `recommended fast and commemoration event types render`() {
        composeRule.setThemedContent {
            IslamicEventCard(
                eventName = "Day of Arafah",
                eventNameArabic = "Arabic-Arafah",
                eventType = HijriDateCalculator.EventType.RECOMMENDED_FAST,
                hijriDate = "9 Dhu al-Hijjah",
                gregorianDate = "5 June 2026"
            )
        }
        composeRule.onNodeWithText("Day of Arafah").assertExists()
    }

    @Test
    fun `primitive overload click invokes callback`() {
        var clicked = false
        composeRule.setThemedContent {
            IslamicEventCard(
                eventName = "Clickable Event",
                eventNameArabic = "Arabic",
                eventType = HijriDateCalculator.EventType.COMMEMORATION,
                hijriDate = "12 Rabi",
                gregorianDate = "1 Jan 2026",
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Clickable Event").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `domain model overload maps fields with gregorian date and description`() {
        val event = IslamicEvent(
            id = "eid_al_fitr",
            nameArabic = "Arabic-Eid-Model",
            nameEnglish = "Eid al-Fitr Model",
            description = "Festival of Breaking the Fast.",
            hijriMonth = 10,
            hijriDay = 1,
            eventType = IslamicEventType.HOLIDAY,
            isHoliday = true,
            isFastingDay = false,
            isNightOfPower = false,
            gregorianDate = LocalDate.of(2026, 3, 30),
            year = 2026,
            notes = null,
            priority = 1
        )
        var clicked = false
        composeRule.setThemedContent {
            IslamicEventCard(event = event, onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Eid al-Fitr Model").assertExists()
        composeRule.onNodeWithText("Arabic-Eid-Model").assertExists()
        // hijriDate is "1 Shawwal" (month 10 -> Shawwal)
        composeRule.onNodeWithText("1 Shawwal").assertExists()
        // gregorianDate formatted as "30 March 2026"
        composeRule.onNodeWithText("30 March 2026").assertExists()
        composeRule.onNodeWithText("Festival of Breaking the Fast.").assertExists()

        composeRule.onNodeWithText("Eid al-Fitr Model").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `domain model overload maps FAST NIGHT and HISTORICAL types with null gregorian date`() {
        val event = IslamicEvent(
            id = "ashura",
            nameArabic = "Arabic-Ashura",
            nameEnglish = "Day of Ashura",
            description = null,
            hijriMonth = 1,
            hijriDay = 10,
            eventType = IslamicEventType.FAST,
            isHoliday = false,
            isFastingDay = true,
            isNightOfPower = false,
            gregorianDate = null,
            year = null,
            notes = null,
            priority = 1
        )
        composeRule.setThemedContent {
            IslamicEventCard(event = event)
        }
        composeRule.onNodeWithText("Day of Ashura").assertExists()
        composeRule.onNodeWithText("10 Muharram").assertExists()
    }
}
