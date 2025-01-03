package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@Composable
fun RamadanCard(onNavigateToCalender: () -> Unit) {
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val currentYear = todayHijri[ChronoField.YEAR]

    val ramadanStart = HijrahDate.of(currentYear, 9, 1)
    val ramadanEnd = HijrahDate.of(currentYear, 9, ramadanStart.lengthOfMonth())

    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    val isRamadanStarted = todayHijri.isAfter(ramadanStart)

    val daysLeft = if (isRamadanStarted) {
        ramadanEnd.toEpochDay() - todayHijri.toEpochDay()
    } else {
        ramadanStart.toEpochDay() - todayHijri.toEpochDay()
    }

    val images = remember { listOf(R.drawable.ramadan, R.drawable.ramadan2, R.drawable.ramadan3, R.drawable.ramadan4, R.drawable.ramadan5) }
    val selectedImage = remember { mutableIntStateOf(images.random()) }

    if (todayHijri[ChronoField.MONTH_OF_YEAR] < 10 && daysLeft < 40) {
        Card(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable { onNavigateToCalender() }
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (todayHijri[ChronoField.DAY_OF_MONTH] == 1 && todayHijri[ChronoField.MONTH_OF_YEAR] == 9)
                        "Ramadan Mubarak" else "Ramadan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = painterResource(id = selectedImage.intValue),
                        contentDescription = "Ramadan Icon",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isRamadanStarted) "Estimated end" else "Estimated start",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = when(daysLeft) {
                                0L -> "Today"
                                1L -> "Tomorrow"
                                else -> "In $daysLeft days"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = LocalDate.from(if (isRamadanStarted) ramadanEnd else ramadanStart)
                                .format(formatter),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RamadanCardPreview() {
    NimazTheme {
        RamadanCard(onNavigateToCalender = { })
    }
}