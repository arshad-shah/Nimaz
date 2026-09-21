package com.arshadshah.nimaz.presentation.screens.learnpray

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.model.PrayerFigure

@Composable
internal fun ClothingNotes(figure: PrayerFigure) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    NimazButton(
        text = stringResource(R.string.learn_pray_clothing),
        onClick = { expanded = !expanded },
        variant = NimazButtonVariant.QUIET,
        leadingIcon = if (expanded) NimazIcons.Collapse else NimazIcons.Expand,
        fullWidth = true,
    )
    if (expanded) {
        Text(
            stringResource(
                if (figure == PrayerFigure.WOMAN) R.string.learn_pray_female_clothing
                else R.string.learn_pray_male_clothing,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(stringResource(R.string.learn_pray_differences), style = MaterialTheme.typography.bodyMedium)
        if (figure == PrayerFigure.WOMAN) {
            Text(stringResource(R.string.learn_pray_female_source), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
