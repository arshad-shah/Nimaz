package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.KhatamProgressCalculator
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import java.text.SimpleDateFormat
import java.util.Date

/** The feature's reading log, reusing the domain's local-day aggregation and the shared card. */
@Composable
fun KhatamReadingActivity(logs: List<DailyLogEntry>, modifier: Modifier = Modifier, now: Long = System.currentTimeMillis()) {
    val days = KhatamProgressCalculator.readingWeek(logs, now)
    val max = days.maxOf { it.ayahsRead }.coerceAtLeast(1)
    val locale = LocalConfiguration.current.locales[0]
    val shortDate = SimpleDateFormat("EE", locale)
    val fullDate = SimpleDateFormat("EEEE, d MMMM", locale)
    NimazCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(NimazSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                val description = fullDate.format(Date(day.date)) + ": " + pluralStringResource(R.plurals.khatam_ayahs_read_plural, day.ayahsRead, day.ayahsRead)
                Column(
                    Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.ExtraSmall),
                ) {
                    Text(day.ayahsRead.toString(), style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.height(64.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.width(20.dp).height(if (day.ayahsRead == 0) 2.dp else (64f * day.ayahsRead / max).coerceAtLeast(4f).dp)
                                .background(if (day.ayahsRead == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                        )
                    }
                    Text(shortDate.format(Date(day.date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
