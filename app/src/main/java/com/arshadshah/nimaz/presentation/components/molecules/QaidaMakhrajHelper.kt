package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Child-friendly label for where a letter is articulated. */
@Composable
fun makhrajLabel(area: MakhrajArea): String = when (area) {
    MakhrajArea.JAWF -> stringResource(R.string.makhraj_jawf)
    MakhrajArea.HALQ -> stringResource(R.string.makhraj_halq)
    MakhrajArea.LISAN -> stringResource(R.string.makhraj_lisan)
    MakhrajArea.SHAFATAIN -> stringResource(R.string.makhraj_shafatain)
    MakhrajArea.KHAYSHUM -> stringResource(R.string.makhraj_khayshum)
}

private fun makhrajEmoji(area: MakhrajArea): String = when (area) {
    MakhrajArea.JAWF -> "🗣️"
    MakhrajArea.HALQ -> "🗣️"
    MakhrajArea.LISAN -> "👅"
    MakhrajArea.SHAFATAIN -> "👄"
    MakhrajArea.KHAYSHUM -> "👃"
}

/**
 * "Where it's made" helper in the letter detail sheet — a friendly line about
 * the makhraj (articulation point), with an emoji cue and the specific detail.
 */
@Composable
fun QaidaMakhrajHelper(
    area: MakhrajArea,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NimazCornerRadius.Medium),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(NimazSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = makhrajEmoji(area),
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.qaida_made_with_format, makhrajLabel(area)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun QaidaMakhrajHelperShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QaidaMakhrajHelper(
            area = MakhrajArea.SHAFATAIN,
            detail = "Pressing the lips together gently.",
        )
        QaidaMakhrajHelper(
            area = MakhrajArea.LISAN,
            detail = "Tip of the tongue behind the front teeth.",
        )
    }
}

@Preview(showBackground = true, name = "Qaida Makhraj Helper — Light")
@Composable
private fun QaidaMakhrajHelperLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaMakhrajHelperShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Makhraj Helper — Dark")
@Composable
private fun QaidaMakhrajHelperDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaMakhrajHelperShowcase()
    }
}
