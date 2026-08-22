package com.arshadshah.nimaz.widget.core

import com.arshadshah.nimaz.core.ui.R as UiR
import com.arshadshah.nimaz.feature.widget.R

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.DayOfWeek
import java.util.Locale

/**
 * The four colour providers every widget reads from `res/color`. Previously each
 * widget re-declared these four lines; they now share a single palette.
 */
data class WidgetPalette(
    val background: ColorProvider = ColorProvider(R.color.widget_background),
    val text: ColorProvider = ColorProvider(R.color.widget_text),
    val textSecondary: ColorProvider = ColorProvider(R.color.widget_text_secondary),
    val primary: ColorProvider = ColorProvider(R.color.widget_primary),
)

/**
 * Full-bleed, centered, tappable container shared by the widgets' loading and
 * error states.
 */
@Composable
fun WidgetMessageBox(
    background: ColorProvider,
    onClick: Action,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .cornerRadius(cornerRadius)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The identical "spinner + Loading…" state used by every widget. Only the corner
 * radius and tap target vary between widgets, so those are parameters.
 */
@Composable
fun WidgetLoadingBox(
    background: ColorProvider,
    textSecondary: ColorProvider,
    onClick: Action,
    cornerRadius: Dp = 16.dp,
) {
    val context = LocalContext.current
    WidgetMessageBox(background = background, onClick = onClick, cornerRadius = cornerRadius) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = context.getString(R.string.widget_loading),
                style = TextStyle(color = textSecondary, fontSize = 12.sp),
            )
        }
    }
}

/**
 * The standard solid, rounded, tappable widget surface. Pass a Column/Row that
 * calls `fillMaxSize()` so `defaultWeight()` distributes inside it.
 */
@Composable
fun WidgetCard(
    background: ColorProvider,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .cornerRadius(cornerRadius)
            .clickable(onClick)
            .padding(padding)
            .then(modifier),
        content = content,
    )
}

/** The single place widget icons are drawn — a tinted vector drawable. */
@Composable
fun WidgetIcon(
    resId: Int,
    tint: ColorProvider,
    size: Dp = 16.dp,
    contentDescription: String? = null,
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** Small medium-weight caption used for eyebrow labels. */
@Composable
fun WidgetLabel(text: String, color: ColorProvider, fontSize: TextUnit = 11.sp) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Medium),
    )
}

/** Rounded badge container for countdowns and the "next prayer" highlight. */
@Composable
fun WidgetPill(
    container: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .background(container)
            .cornerRadius(cornerRadius)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(modifier),
        content = content,
    )
}

/** Maps a prayer name to its celestial drawable. Defaults to the zenith sun. */
fun prayerIconRes(prayerName: String): Int = when (prayerName.trim().lowercase()) {
    "fajr" -> R.drawable.ic_widget_fajr
    "dhuhr", "zuhr" -> R.drawable.ic_widget_dhuhr
    "asr" -> R.drawable.ic_widget_asr
    "maghrib" -> R.drawable.ic_widget_maghrib
    "isha" -> R.drawable.ic_widget_isha
    else -> R.drawable.ic_widget_dhuhr
}


/**
 * Weekday initials for a Sunday-first grid, in [locale].
 *
 * `HijriCalendarWidget` captioned its columns with a hardcoded `listOf("Su", "Mo", …)`, so every
 * translation read the English abbreviations — German writes "So" for Sunday, which is what the
 * hardcoded "Su" was silently overriding.
 */
fun weekdayInitials(locale: Locale = Locale.getDefault()): List<String> =
    (0..6).map { offset ->
        DayOfWeek.SUNDAY.plus(offset.toLong()).getDisplayName(java.time.format.TextStyle.SHORT, locale).take(2)
    }
