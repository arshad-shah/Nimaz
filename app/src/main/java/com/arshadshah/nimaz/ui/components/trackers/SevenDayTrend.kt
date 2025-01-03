import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.components.common.ProgressBarCustom
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SevenDayTrend(
    trackersForWeek: State<List<LocalPrayersTracker>>,
    dateState: State<LocalDate>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                trackersForWeek.value.forEachIndexed { index, prayerTracker ->
                    ProgressBarCustom(
                        progress = prayerTracker.progress.toFloat(),
                        progressColor = determineColor(prayerTracker),
                        radius = 24.dp,
                        label = prayerTracker.date.dayOfWeek.name.take(3),
                        strokeWidth = 6.dp,
                        strokeBackgroundWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun determineColor(prayerTracker: LocalPrayersTracker): Color {
    return when {
        prayerTracker.progress == 0 && !prayerTracker.isMenstruating ->
            MaterialTheme.colorScheme.surfaceVariant
        prayerTracker.isMenstruating -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }
}