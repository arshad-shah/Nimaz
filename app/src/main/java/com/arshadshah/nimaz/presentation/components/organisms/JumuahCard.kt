package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCountdownText
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.foundation.tokens.EventOccasion
import com.arshadshah.nimaz.presentation.foundation.tokens.eventCardVisualsFor
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Friday-only Jumu'ah highlight, built on [EventCard]. English/Arabic name, khutbah
 * time (trailing), a countdown-to-khutbah or "passed" acknowledgement (highlight),
 * and a hadith. Public signature unchanged so Home call sites are untouched.
 */
@Composable
fun JumuahCard(
    jumuahAt: Instant?,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    val v = eventCardVisualsFor(EventOccasion.JUMUAH)
    // "Has the khutbah started" is a function of now, derived here off the shared ticker rather
    // than pushed from the ViewModel once a second.
    val now by rememberNow(TickResolution.MINUTES)
    val isJumuahPassed = jumuahAt != null && now >= jumuahAt
    EventCard(
        accent = v.accent,
        containerAccent = v.containerAccent,
        icon = Icons.Filled.Mosque,
        ornament = v.ornament,
        eyebrow = stringResource(R.string.jumuah_mubarak),
        arabic = stringResource(R.string.jumuah_arabic),
        body = stringResource(R.string.jumuah_hadith_quote),
        fillHeight = fillHeight,
        trailing = if (jumuahAt != null) {
            {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = clockTimeText(jumuahAt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.khutbah_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        highlight = {
            if (isJumuahPassed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.jumuah_passed),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = v.accent,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (jumuahAt != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.time_until_jumuah),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NimazCountdownText(
                        target = jumuahAt,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = v.accent
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun JumuahCard_Preview() {
    NimazTheme {
        JumuahCard(
            jumuahAt = Clock.System.now() + 3.hours,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Passed")
@Composable
private fun JumuahCard_Passed_Preview() {
    NimazTheme {
        JumuahCard(
            jumuahAt = Clock.System.now() - 1.hours,
            modifier = Modifier.padding(16.dp)
        )
    }
}
