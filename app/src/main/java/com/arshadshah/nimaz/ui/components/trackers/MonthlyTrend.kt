import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val prayers = listOf(
    "Fajr",
    "Dhuhr",
    "Asr",
    "Maghrib",
    "Isha"
)
@Composable
fun PrayerTrackerGrid(
    progressForMonth: State<List<LocalPrayersTracker>>,
    dateState: State<LocalDate>
) {
    val elevation = 4.dp
    val yearMonth = YearMonth.of(dateState.value.year, dateState.value.month)
    val daysInMonth = yearMonth.lengthOfMonth()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .shadow(elevation = elevation, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = dateState.value.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            prayers
                .forEach { prayer ->
                PrayerRow(
                    prayer = prayer,
                    yearMonth = yearMonth,
                    daysInMonth = daysInMonth,
                    currentDate = dateState.value,
                    progressForMonth = progressForMonth
                )
            }
        }
    }
}

@Composable
fun PrayerRow(
    prayer: String,
    yearMonth: YearMonth,
    daysInMonth: Int,
    currentDate: LocalDate,
    progressForMonth: State<List<LocalPrayersTracker>>
) {
    val prayerColor = when (prayer) {
        AppConstants.PRAYER_NAME_FAJR -> Color(0xFF81D4FA)
        AppConstants.PRAYER_NAME_DHUHR -> Color(0xFFFFB74D)
        AppConstants.PRAYER_NAME_ASR -> Color(0xFF81C784)
        AppConstants.PRAYER_NAME_MAGHRIB -> Color(0xFFE57373)
        AppConstants.PRAYER_NAME_ISHA -> Color(0xFF9575CD)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = prayerColor.copy(alpha = 0.1f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prayer,
                style = MaterialTheme.typography.labelMedium,
                color = prayerColor,
                modifier = Modifier.weight(0.2f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(0.7f)
            ) {
                for (i in 0 until daysInMonth) {
                    val date = yearMonth.atDay(i + 1)
                    val prayerTracker = progressForMonth.value.find { it.date == date }
                    DayDot(
                        date = date,
                        isHighlighted = prayerTracker?.isPrayerCompleted(prayer) == true,
                        isMenstruating = prayerTracker?.isMenstruating == true,
                        currentDate = currentDate,
                        prayerColor = prayerColor
                    )
                }
            }
        }
    }
}

@Composable
fun DayDot(
    date: LocalDate,
    isHighlighted: Boolean,
    isMenstruating: Boolean,
    currentDate: LocalDate,
    prayerColor: Color
) {
    val isCurrentDay = date == currentDate || date == LocalDate.now()
    val dotSize by animateDpAsState(
        targetValue = if (isCurrentDay) 10.dp else 7.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val backgroundColor = when {
        isMenstruating -> Color(0xFFE91E63)
        isHighlighted -> prayerColor
        isCurrentDay -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(dotSize)
            .shadow(
                elevation = if (isHighlighted) 3.dp else 1.dp,
                shape = CircleShape
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.7f)
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = backgroundColor.copy(alpha = 0.5f),
                shape = CircleShape
            )
    )
}