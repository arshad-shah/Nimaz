package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet

/**
 * Bottom-sheet legend for the tajweed colour-coding (issue #294).
 *
 * Lists every v3 rule with its colour swatch (theme-aware), display name and
 * one-line explanation, driven entirely by [TajweedParser.rules] — the single
 * source of truth shared with the renderer — so the legend can never fall out
 * of sync with what is painted on the page.
 *
 * Reachable from Quran settings; reusable in the reader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedLegendSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.tajweed_legend_title),
        subtitle = stringResource(R.string.tajweed_legend_subtitle),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        onClose = onDismiss,
    ) {
        TajweedParser.rules.forEach { rule ->
            TajweedLegendRow(
                swatchColor = rule.color(isDark),
                name = rule.displayName,
                explanation = rule.explanation,
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

/**
 * Single-rule sheet shown when a coloured word is tapped in the reader
 * (#294 tap-to-explain). Resolves [ruleCode] (v3 or a legacy code) via
 * [TajweedParser.resolveRule]; renders nothing if the code is unknown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedRuleSheet(
    ruleCode: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rule = TajweedParser.resolveRule(ruleCode) ?: return
    val isDark = isSystemInDarkTheme()
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = rule.displayName,
        subtitle = stringResource(R.string.tajweed_legend_subtitle),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        onClose = onDismiss,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(rule.color(isDark)),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = rule.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun TajweedLegendRow(
    swatchColor: androidx.compose.ui.graphics.Color,
    name: String,
    explanation: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(swatchColor),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = swatchColor,
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
