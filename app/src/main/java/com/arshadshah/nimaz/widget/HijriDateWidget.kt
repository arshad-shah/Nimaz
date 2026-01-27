package com.arshadshah.nimaz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class HijriDateWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dateData = getHijriDateData()

        provideContent {
            GlanceTheme {
                HijriDateWidgetContent(dateData)
            }
        }
    }

    private fun getHijriDateData(): HijriDateData {
        return try {
            val hijriDate = HijriDateCalculator.today()
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
            val gregorianDate = "${today.dayOfMonth} ${today.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())}"

            HijriDateData(
                hijriDay = hijriDate.day,
                hijriMonth = hijriDate.monthName,
                hijriYear = hijriDate.year,
                gregorianDayOfWeek = dayOfWeek,
                gregorianDate = gregorianDate
            )
        } catch (e: Exception) {
            HijriDateData(
                hijriDay = 1,
                hijriMonth = "—",
                hijriYear = 1446,
                gregorianDayOfWeek = "—",
                gregorianDate = "—"
            )
        }
    }
}

data class HijriDateData(
    val hijriDay: Int,
    val hijriMonth: String,
    val hijriYear: Int,
    val gregorianDayOfWeek: String,
    val gregorianDate: String
)

@Composable
private fun HijriDateWidgetContent(data: HijriDateData) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day of week
            Text(
                text = data.gregorianDayOfWeek,
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Hijri day (large)
            Text(
                text = data.hijriDay.toString(),
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Hijri month and year
            Text(
                text = "${data.hijriMonth} ${data.hijriYear}",
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Gregorian date
            Text(
                text = data.gregorianDate,
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

class HijriDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HijriDateWidget()
}
