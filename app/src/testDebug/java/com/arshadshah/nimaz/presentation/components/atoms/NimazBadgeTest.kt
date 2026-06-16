package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazBadgeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // ── Enums / sealed types ────────────────────────────────────────────────

    @Test
    fun `NimazBadgeSize presets are defined`() {
        assertThat(NimazBadgeSize.entries).hasSize(3)
        NimazBadgeSize.entries.forEach {
            assertThat(it.height.value).isGreaterThan(0f)
            assertThat(it.horizontalPadding.value).isGreaterThan(0f)
        }
    }

    @Test
    fun `BadgeType variants carry labels and colors`() {
        val types = listOf(
            BadgeType.Sahih, BadgeType.Hasan, BadgeType.Daif, BadgeType.Mawdu,
            BadgeType.Meccan, BadgeType.Medinan, BadgeType.Prayed, BadgeType.Missed,
            BadgeType.Pending, BadgeType.Qada, BadgeType.Jamaah, BadgeType.Fasted,
            BadgeType.NotFasted, BadgeType.Makeup, BadgeType.Exempted
        )
        types.forEach { assertThat(it.label).isNotEmpty() }

        val custom = BadgeType.Custom("Custom", Color.Gray)
        assertThat(custom.label).isEqualTo("Custom")
        assertThat(custom.color).isEqualTo(Color.Gray)
    }

    // ── NimazBadge ──────────────────────────────────────────────────────────

    @Test
    fun `NimazBadge renders filled`() {
        composeRule.setThemedContent {
            NimazBadge(text = "Filled")
        }
        composeRule.onNodeWithText("Filled").assertExists()
    }

    @Test
    fun `NimazBadge renders outlined`() {
        composeRule.setThemedContent {
            NimazBadge(text = "Outlined", outlined = true, size = NimazBadgeSize.LARGE)
        }
        composeRule.onNodeWithText("Outlined").assertExists()
    }

    // ── StatusBadge ─────────────────────────────────────────────────────────

    @Test
    fun `StatusBadge renders from a badge type`() {
        composeRule.setThemedContent {
            StatusBadge(type = BadgeType.Sahih, outlined = true, size = NimazBadgeSize.SMALL)
        }
        composeRule.onNodeWithText("Sahih").assertExists()
    }

    // ── HadithGradeBadge ────────────────────────────────────────────────────

    @Test
    fun `HadithGradeBadge maps known grades`() {
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                HadithGradeBadge(grade = "sahih")
                HadithGradeBadge(grade = "hasan")
                HadithGradeBadge(grade = "daif")
                HadithGradeBadge(grade = "da'if")
                HadithGradeBadge(grade = "mawdu")
                HadithGradeBadge(grade = "mawdu'")
            }
        }
        composeRule.onNodeWithText("Sahih").assertExists()
        composeRule.onNodeWithText("Hasan").assertExists()
        // "daif"/"da'if" and "mawdu"/"mawdu'" each map to the same label.
        composeRule.onAllNodesWithText("Da'if").assertCountEquals(2)
        composeRule.onAllNodesWithText("Mawdu'").assertCountEquals(2)
    }

    @Test
    fun `HadithGradeBadge falls back to custom for unknown grade`() {
        composeRule.setThemedContent {
            HadithGradeBadge(grade = "Unknown")
        }
        composeRule.onNodeWithText("Unknown").assertExists()
    }

    // ── getHadithGradeBadgeColors (pure) ────────────────────────────────────

    @Test
    fun `getHadithGradeBadgeColors covers all grades`() {
        assertThat(getHadithGradeBadgeColors("sahih").first).isEqualTo(BadgeType.Sahih.color)
        assertThat(getHadithGradeBadgeColors("hasan").first).isEqualTo(BadgeType.Hasan.color)
        assertThat(getHadithGradeBadgeColors("daif").first).isEqualTo(BadgeType.Daif.color)
        assertThat(getHadithGradeBadgeColors("da'if").first).isEqualTo(BadgeType.Daif.color)
        assertThat(getHadithGradeBadgeColors("mawdu").first).isEqualTo(BadgeType.Mawdu.color)
        assertThat(getHadithGradeBadgeColors("mawdu'").first).isEqualTo(BadgeType.Mawdu.color)
        val fallback = getHadithGradeBadgeColors("???")
        assertThat(fallback.first).isEqualTo(Color.Gray)
        assertThat(fallback.second).isEqualTo(Color.White)
    }

    // ── SurahNumberBadge ────────────────────────────────────────────────────

    @Test
    fun `SurahNumberBadge renders number`() {
        composeRule.setThemedContent {
            SurahNumberBadge(number = 114)
        }
        composeRule.onNodeWithText("114").assertExists()
    }
}
