package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@Composable
fun EidUlAdhaCard(onNavigateToCalender: () -> Unit) {
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val currentYear = todayHijri[ChronoField.YEAR]

    val eidStart = HijrahDate.of(currentYear, 12, 9)
    val eidEnd = HijrahDate.of(currentYear, 12, 13)

    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    val isEidStarted = today.isAfter(LocalDate.from(eidStart))

    val daysLeft = if (isEidStarted) {
        eidEnd.toEpochDay() - todayHijri.toEpochDay()
    } else {
        eidStart.toEpochDay() - todayHijri.toEpochDay()
    }

    val images = remember {
        listOf(
            R.drawable.eid, R.drawable.eid2, R.drawable.eid3,
            R.drawable.eid4, R.drawable.eid5, R.drawable.eid_al_adha
        )
    }
    val selectedImage = remember { mutableIntStateOf(images.random()) }

    if (!(daysLeft <= 3 && (!isEidStarted || daysLeft > 0))) return

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onNavigateToCalender)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEidStarted) "Eid Mubarak" else "Eid ul Adha is coming",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(selectedImage.value),
                    contentDescription = "Eid Celebration",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .padding(4.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isEidStarted) "Ends in" else "Starts in",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${daysLeft.coerceAtLeast(0)} days",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = LocalDate.from(if (isEidStarted) eidEnd else eidStart)
                            .format(formatter),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun EidUlAdhaCardPreview() {
    EidUlAdhaCard(onNavigateToCalender = {})
}