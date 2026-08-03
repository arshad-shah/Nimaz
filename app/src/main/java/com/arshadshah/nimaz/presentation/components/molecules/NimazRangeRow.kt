package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazMarginRule
import com.arshadshah.nimaz.presentation.components.atoms.NimazMarginTick
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.nimazMarginRules
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A row in a table of contents: what it covers, on the left of a ruled margin, and what it is
 * about, on the right.
 *
 * The reference is *not* a subtitle. In a document divided by number — a surah's passages, a
 * khatam's daily ranges — the number is how a reader finds their place, so it gets a column of
 * its own, right-aligned against the rule so 282 of them form a readable edge instead of a
 * ragged one. This is the same hairline the subject tree indents against, which is what makes
 * the two lists read as one apparatus rather than two unrelated screens.
 *
 * [marked] fills the tick — one row in the list is where the reader currently is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazRangeRow(
    reference: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    marked: Boolean = false,
    markerLabel: String? = null,
    contentDescription: String? = null,
) {
    val ruleColor = NimazMarginRule.color
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .nimazMarginRules(count = 1, color = ruleColor, start = RuleX, rtl = rtl),
    ) {
        NimazCard(
            style = NimazCardStyle.FILLED,
            tone = NimazTone.TRANSPARENT,
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (contentDescription != null) {
                        Modifier.clearAndSetSemantics {
                            this.contentDescription = contentDescription
                        }
                    } else {
                        Modifier
                    }
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .width(GutterWidth)
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = reference,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(TickLane)
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NimazMarginTick(filled = marked, ruleColor = ruleColor)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (markerLabel != null) {
                        NimazBadge(
                            text = markerLabel,
                            tone = NimazTone.WARNING,
                            size = NimazBadgeSize.SMALL,
                        )
                    }
                }
            }
        }
    }
}

private val GutterWidth = 64.dp
private val TickLane = 16.dp

/** Where the hairline falls: the centre of the tick lane, just past the reference gutter. */
private val RuleX = GutterWidth + TickLane / 2

@Preview(showBackground = true, widthDp = 390, name = "NimazRangeRow — Light")
@Composable
private fun NimazRangeRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { RangeRowSample() }
}

@Preview(
    showBackground = true, widthDp = 390, name = "NimazRangeRow — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazRangeRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { RangeRowSample() }
}

@Composable
private fun RangeRowSample() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        NimazRangeRow(
            reference = "1–5",
            supportingText = "5 verses",
            label = "The Qur'an is guidance for those conscious of God",
            onClick = {},
        )
        NimazRangeRow(
            reference = "153–157",
            supportingText = "5 verses",
            label = "Patience and prayer as the resources of hardship",
            marked = true,
            markerLabel = "Reading",
            onClick = {},
        )
        NimazRangeRow(
            reference = "282",
            supportingText = "1 verse",
            label = "Recording a debt: the longest verse of the Qur'an",
            onClick = {},
        )
    }
}
