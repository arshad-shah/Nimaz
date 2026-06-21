package com.arshadshah.nimaz.widget.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
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
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.R

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
