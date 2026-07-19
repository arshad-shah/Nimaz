package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.presentation.components.atoms.QaidaMedallion
import com.arshadshah.nimaz.presentation.components.atoms.QaidaMedallionState
import com.arshadshah.nimaz.presentation.components.atoms.QaidaStarRow
import com.arshadshah.nimaz.presentation.components.atoms.rememberQaidaPalette
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlin.math.roundToInt

/**
 * The Qaida course map: a serpentine trail drawn through the lesson medallions.
 * The walked stretch (up to the current lesson) is a solid gold→teal gradient;
 * the locked stretch is a faded dashed line. Medallions are real composables
 * positioned on the curve, so taps, stars and the current-node ring are normal
 * Compose. Scrolls vertically.
 */
@Composable
fun QaidaCoursePath(
    lessons: List<QaidaLessonState>,
    currentLessonId: Int?,
    onLessonClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberQaidaPalette()
    val density = LocalDensity.current

    val currentIndex = lessons.indexOfFirst { it.lesson.id == currentLessonId }
        .let { if (it >= 0) it else lessons.indexOfLast { l -> l.status != LessonStatus.LOCKED } }
        .coerceAtLeast(0)

    val rowH = 116.dp
    val medallion = 64.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        val rowPx = with(density) { rowH.toPx() }
        val medPx = with(density) { medallion.toPx() }
        val strokePx = with(density) { 10.dp.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val totalHeight = rowH * lessons.size.coerceAtLeast(1)

        fun frac(i: Int) = if (i % 2 == 0) 0.30f else 0.70f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .drawBehind {
                    if (lessons.size < 2) return@drawBehind
                    val cx = { i: Int -> size.width * frac(i) }
                    val cy = { i: Int -> rowPx * i + rowPx / 2f }
                    fun trail(from: Int, to: Int): Path = Path().apply {
                        if (to <= from) return@apply
                        moveTo(cx(from), cy(from))
                        for (i in from until to) {
                            val midY = (cy(i) + cy(i + 1)) / 2f
                            cubicTo(cx(i), midY, cx(i + 1), midY, cx(i + 1), cy(i + 1))
                        }
                    }
                    // Walked path: gold→teal gradient.
                    if (currentIndex > 0) {
                        drawPath(
                            path = trail(0, currentIndex),
                            brush = Brush.verticalGradient(listOf(palette.gold, palette.current)),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                    // Locked path: faded dashes.
                    if (currentIndex < lessons.lastIndex) {
                        drawPath(
                            path = trail(currentIndex, lessons.lastIndex),
                            color = palette.trailLocked,
                            style = Stroke(
                                width = strokePx * 0.85f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 22f)),
                            ),
                        )
                    }
                },
        ) {
            lessons.forEachIndexed { i, ls ->
                val state = when (ls.status) {
                    LessonStatus.COMPLETED -> QaidaMedallionState.DONE
                    LessonStatus.LOCKED -> QaidaMedallionState.LOCKED
                    else -> QaidaMedallionState.CURRENT
                }
                val desc = when (state) {
                    QaidaMedallionState.DONE -> pluralStringResource(
                        R.plurals.qaida_a11y_lesson_complete_format,
                        ls.stars,
                        ls.lesson.lessonNumber,
                        ls.lesson.titleEnglish,
                        ls.stars,
                    )

                    QaidaMedallionState.CURRENT -> stringResource(
                        R.string.qaida_a11y_lesson_current_format,
                        ls.lesson.lessonNumber,
                        ls.lesson.titleEnglish,
                    )

                    QaidaMedallionState.LOCKED -> stringResource(
                        R.string.qaida_a11y_lesson_locked_format,
                        ls.lesson.lessonNumber,
                        ls.lesson.titleEnglish,
                    )
                }
                val centerX = widthPx * frac(i)
                val centerY = rowPx * i + rowPx / 2f
                Box(
                    modifier = Modifier.offset {
                        IntOffset(
                            (centerX - medPx / 2f).roundToInt(),
                            (centerY - medPx / 2f).roundToInt(),
                        )
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        QaidaMedallion(
                            label = toArabicNumber(ls.lesson.lessonNumber),
                            state = state,
                            contentDescription = desc,
                            palette = palette,
                            size = medallion,
                            onClick = { onLessonClick(ls.lesson.id) },
                        )
                        if (state == QaidaMedallionState.DONE) {
                            QaidaStarRow(
                                filled = ls.stars,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==================== PREVIEWS ====================

private fun sampleLessonState(
    id: Int,
    status: LessonStatus,
    stars: Int,
): QaidaLessonState {
    val lesson = QaidaLesson(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "الدرس",
        titleTransliteration = "Dars $id",
        description = "",
        conceptTags = emptyList(),
        icon = "",
        displayOrder = id,
    )
    return QaidaLessonState(
        lesson = lesson,
        status = status,
        stars = stars,
        completedCells = if (status == LessonStatus.COMPLETED) 10 else 0,
        totalCells = 10,
        completionFraction = if (status == LessonStatus.COMPLETED) 1f else 0f,
        lastCellId = null,
    )
}

@Composable
private fun QaidaCoursePathShowcase() {
    val lessons = listOf(
        sampleLessonState(1, LessonStatus.COMPLETED, 3),
        sampleLessonState(2, LessonStatus.COMPLETED, 2),
        sampleLessonState(3, LessonStatus.IN_PROGRESS, 0),
        sampleLessonState(4, LessonStatus.LOCKED, 0),
        sampleLessonState(5, LessonStatus.LOCKED, 0),
    )
    QaidaCoursePath(
        lessons = lessons,
        currentLessonId = 3,
        onLessonClick = {},
        modifier = Modifier.height(600.dp),
    )
}

@Preview(showBackground = true, name = "Qaida Course Path — Light", heightDp = 600)
@Composable
private fun QaidaCoursePathLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaCoursePathShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Course Path — Dark", heightDp = 600)
@Composable
private fun QaidaCoursePathDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaCoursePathShowcase()
    }
}
