package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
            assertThat(it.verticalPadding.value).isGreaterThan(0f)
        }
    }

    @Test
    fun `NimazBadgeEmphasis covers the four weights`() {
        assertThat(NimazBadgeEmphasis.entries).containsExactly(
            NimazBadgeEmphasis.FILLED,
            NimazBadgeEmphasis.SOFT,
            NimazBadgeEmphasis.OUTLINED,
            NimazBadgeEmphasis.CUTOUT
        )
    }

    @Test
    fun `shapeOf resolves the badge silhouettes`() {
        assertThat(NimazBadgeDefaults.shapeOf(NimazBadgeShape.PILL))
            .isEqualTo(RoundedCornerShape(percent = 50))
        assertThat(NimazBadgeDefaults.shapeOf(NimazBadgeShape.ROUNDED))
            .isEqualTo(RoundedCornerShape(6.dp))
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
    fun `NimazBadge renders at its default tone and emphasis`() {
        composeRule.setThemedContent {
            NimazBadge(text = "Default")
        }
        composeRule.onNodeWithText("Default").assertExists()
    }

    @Test
    fun `NimazBadge renders every emphasis at a given tone`() {
        composeRule.setThemedContent {
            Row {
                NimazBadgeEmphasis.entries.forEach { emphasis ->
                    NimazBadge(
                        text = "Tone",
                        tone = NimazTone.ACCENT,
                        emphasis = emphasis,
                        size = NimazBadgeSize.LARGE
                    )
                }
            }
        }
        composeRule.onAllNodesWithText("Tone")
            .assertCountEquals(NimazBadgeEmphasis.entries.size)
    }

    @Test
    fun `NimazBadge renders every tone`() {
        composeRule.setThemedContent {
            Row {
                NimazTone.entries.forEach { tone ->
                    NimazBadge(text = "T", tone = tone, emphasis = NimazBadgeEmphasis.SOFT)
                }
            }
        }
        composeRule.onAllNodesWithText("T").assertCountEquals(NimazTone.entries.size)
    }

    @Test
    fun `NimazBadge renders a rounded cutout with an icon`() {
        composeRule.setThemedContent {
            NimazBadge(
                text = "21",
                tone = NimazTone.ACCENT,
                emphasis = NimazBadgeEmphasis.CUTOUT,
                shape = NimazBadgeShape.ROUNDED,
                size = NimazBadgeSize.SMALL,
                icon = Icons.Default.Star
            )
        }
        composeRule.onNodeWithText("21").assertExists()
    }

    @Test
    fun `NimazBadge invokes onClick when tapped`() {
        var clicks = 0
        composeRule.setThemedContent {
            NimazBadge(text = "Tappable", onClick = { clicks++ })
        }
        composeRule.onNodeWithText("Tappable").performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `NimazBadge renders in both selected states`() {
        composeRule.setThemedContent {
            Row {
                NimazBadge(text = "Off", tone = NimazTone.ACCENT, selected = false, onClick = {})
                NimazBadge(text = "On", tone = NimazTone.ACCENT, selected = true, onClick = {})
            }
        }
        composeRule.onNodeWithText("Off").assertExists()
        composeRule.onNodeWithText("On").assertExists()
    }

    @Test
    fun `NimazBadge accepts feature-art colors`() {
        composeRule.setThemedContent {
            NimazBadge(
                text = "Gold",
                colors = NimazBadgeDefaults.feature(
                    color = Color(0xFFD4AF37),
                    emphasis = NimazBadgeEmphasis.OUTLINED
                )
            )
        }
        composeRule.onNodeWithText("Gold").assertExists()
    }

    // ── StatusBadge ─────────────────────────────────────────────────────────

    @Test
    fun `StatusBadge renders from a badge type`() {
        composeRule.setThemedContent {
            StatusBadge(
                type = BadgeType.Sahih,
                emphasis = NimazBadgeEmphasis.OUTLINED,
                size = NimazBadgeSize.SMALL
            )
        }
        composeRule.onNodeWithText("Sahih").assertExists()
    }

    @Test
    fun `StatusBadge renders its type label at every emphasis`() {
        composeRule.setThemedContent {
            Row {
                NimazBadgeEmphasis.entries.forEach { emphasis ->
                    StatusBadge(
                        type = BadgeType.Meccan,
                        emphasis = emphasis,
                        shape = NimazBadgeShape.ROUNDED
                    )
                }
            }
        }
        composeRule.onAllNodesWithText(BadgeType.Meccan.label)
            .assertCountEquals(NimazBadgeEmphasis.entries.size)
    }

    // ── SurahNumberBadge ────────────────────────────────────────────────────

    @Test
    fun `SurahNumberBadge renders number`() {
        composeRule.setThemedContent {
            SurahNumberBadge(number = 114)
        }
        composeRule.onNodeWithText("114").assertExists()
    }

    @Test
    fun `SurahNumberBadge honours an explicit tone and size`() {
        composeRule.setThemedContent {
            SurahNumberBadge(number = 36, size = 56.dp, tone = NimazTone.SUCCESS)
        }
        composeRule.onNodeWithText("36").assertExists()
    }
}
