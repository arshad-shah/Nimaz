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
 * Reachable from Quran settings and the reader's overflow menu.
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
                name = tajweedRuleName(rule),
                explanation = tajweedRuleExplanation(rule),
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
        title = tajweedRuleName(rule),
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
                text = tajweedRuleExplanation(rule),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

/**
 * Localized display name for a rule, resolved from string resources so the
 * legend/tap-to-explain honour the app language (#294). Falls back to the
 * English name baked into [TajweedParser.rules] for any unmapped code. The
 * transliterated technical terms (Ghunnah, Idgham, Madd…) are shared across
 * locales; only the plain-word names (Silent, Waqf sign) and every explanation
 * are translated.
 */
@Composable
fun tajweedRuleName(rule: TajweedParser.TajweedRuleInfo): String = when (rule.code) {
    "g" -> stringResource(R.string.tajweed_rule_g_name)
    "if" -> stringResource(R.string.tajweed_rule_if_name)
    "is" -> stringResource(R.string.tajweed_rule_is_name)
    "dg" -> stringResource(R.string.tajweed_rule_dg_name)
    "dn" -> stringResource(R.string.tajweed_rule_dn_name)
    "ds" -> stringResource(R.string.tajweed_rule_ds_name)
    "dj" -> stringResource(R.string.tajweed_rule_dj_name)
    "dk" -> stringResource(R.string.tajweed_rule_dk_name)
    "dm" -> stringResource(R.string.tajweed_rule_dm_name)
    "qs" -> stringResource(R.string.tajweed_rule_qs_name)
    "qk" -> stringResource(R.string.tajweed_rule_qk_name)
    "mn" -> stringResource(R.string.tajweed_rule_mn_name)
    "mf" -> stringResource(R.string.tajweed_rule_mf_name)
    "mt" -> stringResource(R.string.tajweed_rule_mt_name)
    "ma" -> stringResource(R.string.tajweed_rule_ma_name)
    "ml" -> stringResource(R.string.tajweed_rule_ml_name)
    "my" -> stringResource(R.string.tajweed_rule_my_name)
    "l" -> stringResource(R.string.tajweed_rule_l_name)
    "ls" -> stringResource(R.string.tajweed_rule_ls_name)
    "sl" -> stringResource(R.string.tajweed_rule_sl_name)
    "hw" -> stringResource(R.string.tajweed_rule_hw_name)
    "wq" -> stringResource(R.string.tajweed_rule_wq_name)
    "tk" -> stringResource(R.string.tajweed_rule_tk_name)
    "tq" -> stringResource(R.string.tajweed_rule_tq_name)
    else -> rule.displayName
}

/**
 * Localized one-line explanation for a rule, resolved from string resources
 * (#294). Falls back to the English explanation in [TajweedParser.rules].
 */
@Composable
fun tajweedRuleExplanation(rule: TajweedParser.TajweedRuleInfo): String = when (rule.code) {
    "g" -> stringResource(R.string.tajweed_rule_g_desc)
    "if" -> stringResource(R.string.tajweed_rule_if_desc)
    "is" -> stringResource(R.string.tajweed_rule_is_desc)
    "dg" -> stringResource(R.string.tajweed_rule_dg_desc)
    "dn" -> stringResource(R.string.tajweed_rule_dn_desc)
    "ds" -> stringResource(R.string.tajweed_rule_ds_desc)
    "dj" -> stringResource(R.string.tajweed_rule_dj_desc)
    "dk" -> stringResource(R.string.tajweed_rule_dk_desc)
    "dm" -> stringResource(R.string.tajweed_rule_dm_desc)
    "qs" -> stringResource(R.string.tajweed_rule_qs_desc)
    "qk" -> stringResource(R.string.tajweed_rule_qk_desc)
    "mn" -> stringResource(R.string.tajweed_rule_mn_desc)
    "mf" -> stringResource(R.string.tajweed_rule_mf_desc)
    "mt" -> stringResource(R.string.tajweed_rule_mt_desc)
    "ma" -> stringResource(R.string.tajweed_rule_ma_desc)
    "ml" -> stringResource(R.string.tajweed_rule_ml_desc)
    "my" -> stringResource(R.string.tajweed_rule_my_desc)
    "l" -> stringResource(R.string.tajweed_rule_l_desc)
    "ls" -> stringResource(R.string.tajweed_rule_ls_desc)
    "sl" -> stringResource(R.string.tajweed_rule_sl_desc)
    "hw" -> stringResource(R.string.tajweed_rule_hw_desc)
    "wq" -> stringResource(R.string.tajweed_rule_wq_desc)
    "tk" -> stringResource(R.string.tajweed_rule_tk_desc)
    "tq" -> stringResource(R.string.tajweed_rule_tq_desc)
    else -> rule.explanation
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
