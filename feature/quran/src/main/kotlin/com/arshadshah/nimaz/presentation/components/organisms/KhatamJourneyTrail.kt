package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.presentation.foundation.tokens.KhatamAccent
import com.arshadshah.nimaz.presentation.foundation.tokens.rememberKhatamAccent
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Juz nodes per row before the trail doubles back. */
private const val COLUMNS = 5

private val ROW_HEIGHT = 68.dp
private val NODE_SIZE = 40.dp

/**
 * The 30 juz as a walked path — gold behind the reader, dashed ahead.
 *
 * Adapted from `QaidaCoursePath`, but snaking across [COLUMNS] columns rather than one
 * node per row: Qaida's layout at 30 nodes would be roughly 3,500dp tall. This keeps the
 * whole journey visible in about 400dp while preserving the "path you are walking" read.
 *
 * Colours are resolved in composition and passed into `drawBehind` because a `DrawScope`
 * cannot read `MaterialTheme` — the same reason `QaidaPalette` exists.
 */
@Composable
fun KhatamJourneyTrail(
    juzProgress: List<JuzProgressInfo>,
    modifier: Modifier = Modifier,
    accent: KhatamAccent = rememberKhatamAccent(),
    onJuzClick: ((Int) -> Unit)? = null,
) {
    if (juzProgress.isEmpty()) return

    val density = LocalDensity.current
    val rows = (juzProgress.size + COLUMNS - 1) / COLUMNS

    // The first juz not yet finished is "current"; everything before it is walked.
    val currentIndex = juzProgress.indexOfFirst { !it.isComplete }
        .let { if (it >= 0) it else juzProgress.lastIndex }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val rowPx = with(density) { ROW_HEIGHT.toPx() }
        val strokePx = with(density) { 7.dp.toPx() }
        val nodePx = with(density) { NODE_SIZE.toPx() }

        /** Snake ordering: even rows run left-to-right, odd rows right-to-left. */
        fun centerX(index: Int): Float {
            val row = index / COLUMNS
            val rawCol = index % COLUMNS
            val col = if (row % 2 == 0) rawCol else COLUMNS - 1 - rawCol
            return widthPx * (col + 0.5f) / COLUMNS
        }

        fun centerY(index: Int): Float = rowPx * (index / COLUMNS) + rowPx / 2f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT * rows)
                .drawBehind {
                    if (juzProgress.size < 2) return@drawBehind

                    fun segment(from: Int, to: Int): Path = Path().apply {
                        if (to <= from) return@apply
                        moveTo(centerX(from), centerY(from))
                        for (i in from until to) {
                            val x0 = centerX(i)
                            val y0 = centerY(i)
                            val x1 = centerX(i + 1)
                            val y1 = centerY(i + 1)
                            if (y0 == y1) {
                                lineTo(x1, y1)
                            } else {
                                // Row change: bow outward past the edge node so the
                                // turn reads as a curve rather than a corner.
                                val midY = (y0 + y1) / 2f
                                val bow = x0 + (x0 - x1) * 0.12f
                                cubicTo(bow, midY, bow, midY, x1, y1)
                            }
                        }
                    }

                    if (currentIndex > 0) {
                        drawPath(
                            path = segment(0, currentIndex),
                            brush = Brush.verticalGradient(accent.progressGradient),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                    if (currentIndex < juzProgress.lastIndex) {
                        drawPath(
                            path = segment(currentIndex, juzProgress.lastIndex),
                            color = accent.onMuted.copy(alpha = 0.35f),
                            style = Stroke(
                                width = strokePx * 0.8f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 18f)),
                            ),
                        )
                    }
                },
        ) {
            juzProgress.forEachIndexed { index, juz ->
                val isDone = juz.isComplete
                val isCurrent = index == currentIndex && !isDone

                val descriptionRes = when {
                    isDone -> R.string.khatam_a11y_juz_complete
                    isCurrent -> R.string.khatam_a11y_juz_current
                    else -> R.string.khatam_a11y_juz_locked
                }
                val description = stringResource(descriptionRes, juz.juzNumber)

                val xDp = with(density) { (centerX(index) - nodePx / 2f).toDp() }
                val yDp = with(density) { (centerY(index) - nodePx / 2f).toDp() }

                KhatamJuzNode(
                    juzNumber = juz.juzNumber,
                    isDone = isDone,
                    isCurrent = isCurrent,
                    accent = accent,
                    description = description,
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .then(
                            if (onJuzClick != null) {
                                Modifier.clickable { onJuzClick(juz.juzNumber) }
                            } else Modifier
                        ),
                )
            }
        }
    }
}

@Composable
private fun KhatamJuzNode(
    juzNumber: Int,
    isDone: Boolean,
    isCurrent: Boolean,
    accent: KhatamAccent,
    description: String,
    modifier: Modifier = Modifier,
) {
    // Every state needs an OPAQUE fill: these nodes sit on top of the drawn trail, so a
    // translucent one lets the path show through behind the number and it stops being
    // legible. The current node reads as "current" from its ring, not from a wash.
    val background = when {
        isDone -> accent.complete
        isCurrent -> MaterialTheme.colorScheme.surface
        else -> accent.muted
    }
    val content = when {
        isDone -> accent.onComplete
        isCurrent -> accent.progress
        else -> accent.onMuted
    }

    Box(
        modifier = modifier
            .size(NODE_SIZE)
            .background(background, CircleShape)
            .then(
                if (isCurrent) Modifier.border(3.dp, accent.progress, CircleShape)
                else Modifier
            )
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = juzNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

// ---- Previews ----

/** 30 juz with [completed] of them finished, for previewing the trail. */
private fun previewJuz(completed: Int): List<JuzProgressInfo> =
    (1..30).map { n ->
        val total = 200
        JuzProgressInfo(
            juzNumber = n,
            totalAyahs = total,
            readAyahs = when {
                n <= completed -> total
                n == completed + 1 -> total / 3
                else -> 0
            },
        )
    }

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Light")
@Composable
private fun KhatamJourneyTrailLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamJourneyTrail(juzProgress = previewJuz(8), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Dark")
@Composable
private fun KhatamJourneyTrailDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        KhatamJourneyTrail(juzProgress = previewJuz(8), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Just started")
@Composable
private fun KhatamJourneyTrailStartPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamJourneyTrail(juzProgress = previewJuz(0), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Complete")
@Composable
private fun KhatamJourneyTrailCompletePreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamJourneyTrail(juzProgress = previewJuz(30), modifier = Modifier.padding(16.dp))
    }
}
