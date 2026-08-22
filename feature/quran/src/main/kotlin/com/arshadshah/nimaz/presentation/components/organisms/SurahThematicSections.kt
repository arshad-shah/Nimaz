package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.AyahTheme

/**
 * The heading the reader prints where a new passage opens.
 *
 * Deliberately quiet — a label and a line of prose, no card and no chevron. It sits between two
 * verses of scripture, and anything with a container would read as content of the same weight
 * as what it is annotating.
 *
 * The surah's *whole* outline used to live here too, as a `LazyListScope` extension the info
 * screen spread across its own list, and so did the long-form background. Both are screens now
 * ([com.arshadshah.nimaz.presentation.screens.quran.SurahPassagesScreen] and
 * [com.arshadshah.nimaz.presentation.screens.quran.SurahBackgroundScreen]) — 282 rows is a
 * table of contents, not a section. This heading stays because it is the one piece of the
 * outline that belongs *inside* the reading.
 */
@Composable
fun PassageHeading(passage: AyahTheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = passage.reference,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = passage.theme,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
